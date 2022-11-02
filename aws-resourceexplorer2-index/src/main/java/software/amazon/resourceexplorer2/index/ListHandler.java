package software.amazon.resourceexplorer2.index;

// CloudFormation package
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;

// Resource Explorer package
import software.amazon.awssdk.services.resourceexplorer2.ResourceExplorer2Client;
import software.amazon.awssdk.services.resourceexplorer2.model.ListIndexesRequest;
import software.amazon.awssdk.services.resourceexplorer2.model.ListIndexesResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class ListHandler extends BaseHandler<CallbackContext> {

    private final ResourceExplorer2Client client;

    public ListHandler() {
        client = ClientFactory.getClient();
    }
    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();
        List<ResourceModel> models = new ArrayList<>();

        ListIndexesResponse listIndexesResponse;
        String thisNextToken = null;

        // ListIndexes have a maximum amount of indexes that it can list for every request.
        // If the existing indexes in the account are more than the maximum amount, ListIndexesResponse
        // returns with a token, so users can list the rest of the indexes if they want.
        // List Handler returns all indexes, therefore, we need to repeat the ListIndexesRequest until
        // there is no more token in the response.
        do {
            // Build the ListIndexesRequest based on the Index type of the current model,
            // so we can make sure the model existed in the result list.
            ListIndexesRequest listIndexesRequest = ListIndexesRequest.builder()
                    .nextToken(thisNextToken)
                    .build();
            try {
                listIndexesResponse = proxy.injectCredentialsAndInvokeV2(listIndexesRequest, client::listIndexes);
            } catch (RuntimeException e) {
                HandlerErrorCode errorCode = Convertor.convertExceptionToErrorCode(e, logger);
                logger.log(String.format("[LIST] Error Code: %s.", errorCode));
                return ProgressEvent.failed(model, callbackContext, errorCode, "Could not list indexes: " + e.getMessage());
            }

            List<ResourceModel> listOfResponse = new ArrayList<>();
            listOfResponse = listIndexesResponse.indexes().stream()
                    .map(index -> ResourceModel.builder()
                            .arn(index.arn())
                            .build())
                    .collect(Collectors.toList());
            models.addAll(listOfResponse);
            thisNextToken = listIndexesResponse.nextToken();
        } while (thisNextToken != null);

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(models)
                .status(OperationStatus.SUCCESS)
                .build();
    }
}
