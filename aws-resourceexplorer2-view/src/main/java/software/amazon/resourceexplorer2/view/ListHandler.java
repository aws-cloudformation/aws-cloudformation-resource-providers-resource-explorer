package software.amazon.resourceexplorer2.view;

// CloudFormation package
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;

//Import Resource Explorer package
import software.amazon.awssdk.services.resourceexplorer2.ResourceExplorer2Client;
import software.amazon.awssdk.services.resourceexplorer2.model.ListViewsRequest;
import software.amazon.awssdk.services.resourceexplorer2.model.ListViewsResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class ListHandler extends REBaseHandler<CallbackContext> {

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

        logRequestInfo(request, logger);

        final ResourceModel model = request.getDesiredResourceState();
        List<ResourceModel> models = new ArrayList<>();

        ListViewsResponse listViewsResponse;
        String thisNextToken = null;

        do {
            ListViewsRequest listViewsRequest = ListViewsRequest.builder()
                    .nextToken(thisNextToken)
                    .build();
            try {
                listViewsResponse = proxy.injectCredentialsAndInvokeV2(listViewsRequest, client::listViews);
            } catch (RuntimeException e) {
                HandlerErrorCode errorCode = Convertor.convertExceptionToErrorCode(e, logger);
                logger.log(String.format("[LIST] Error Code: %s.", errorCode));
                return ProgressEvent.failed(model, callbackContext, errorCode,
                    "Could not list views: " + e.getMessage());
            }

            List<ResourceModel> listOfResponse = new ArrayList<>();
            listOfResponse = listViewsResponse.views().stream()
                    .map(viewArn -> ResourceModel.builder().viewArn(viewArn).build())
                    .collect(Collectors.toList());
            models.addAll(listOfResponse);
            thisNextToken = listViewsResponse.nextToken();
        } while (thisNextToken != null);


        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(models)
                .nextToken(null)
                .status(OperationStatus.SUCCESS)
                .build();
    }
}
