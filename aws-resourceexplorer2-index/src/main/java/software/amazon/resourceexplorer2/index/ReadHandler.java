package software.amazon.resourceexplorer2.index;

// CloudFormation package
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;

// Resource Explorer package
import software.amazon.awssdk.services.resourceexplorer.ResourceExplorerClient;
import software.amazon.awssdk.services.resourceexplorer.model.GetIndexRequest;
import software.amazon.awssdk.services.resourceexplorer.model.GetIndexResponse;

import static software.amazon.resourceexplorer2.index.IndexUtils.DELETING;
import static software.amazon.resourceexplorer2.index.IndexUtils.DELETED;

// TODO: Delete these import before publishing.
import java.net.URI;
import software.amazon.awssdk.regions.Region;

public class ReadHandler extends BaseHandler<CallbackContext> {

    private final ResourceExplorerClient client;

    public ReadHandler() {
        // TODO: Delete endpointOverride and region before publishing.
        client = ResourceExplorerClient.builder()
                .endpointOverride(URI.create("https://resource-explorer-2.us-west-2.api.aws"))
                .region(Region.US_WEST_2)
                .build();
    }
    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();
        final GetIndexRequest getIndexRequest = GetIndexRequest.builder().build();
        final GetIndexResponse getIndexResponse;
        try{
            getIndexResponse = proxy.injectCredentialsAndInvokeV2(getIndexRequest, client::getIndex);
        } catch (RuntimeException e){
            HandlerErrorCode thisErrorCode = Convertor.convertExceptionToErrorCode(e, logger);
            logger.log(String.format("[READ] Error code: %s.", thisErrorCode));
            return ProgressEvent.failed(model, callbackContext, thisErrorCode, "Could not get the index: " + e.getMessage());
        }

        // If the existing index has "DELETING" or "DELETING" state, we consider it as NotExist/NotFound.
        if (getIndexResponse.stateAsString().equalsIgnoreCase(DELETING) ||
                getIndexResponse.stateAsString().equalsIgnoreCase(DELETED)) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.NotFound, "The index has been deleted.");
        }

        model.setArn(getIndexResponse.arn());
        model.setIndexState(getIndexResponse.stateAsString());
        model.setType(getIndexResponse.typeAsString());
        model.setTags(getIndexResponse.tags());

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .status(OperationStatus.SUCCESS)
                .build();
    }
}
