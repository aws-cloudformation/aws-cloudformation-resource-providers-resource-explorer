package software.amazon.resourceexplorer2.index;

// CloudFormation package
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.HandlerErrorCode;

// Resource Explorer package
import software.amazon.awssdk.services.resourceexplorer.ResourceExplorerClient;
import software.amazon.awssdk.services.resourceexplorer.model.DeleteIndexRequest;
import software.amazon.awssdk.services.resourceexplorer.model.GetIndexRequest;
import software.amazon.awssdk.services.resourceexplorer.model.GetIndexResponse;
import software.amazon.awssdk.services.resourceexplorer.model.UpdateIndexTypeRequest;


// TODO: Delete these import before publishing.
import software.amazon.awssdk.regions.Region;
import java.net.URI;

import static software.amazon.resourceexplorer2.index.IndexUtils.*;

public class DeleteHandler extends BaseHandler<CallbackContext> {

    private final ResourceExplorerClient client;

    public DeleteHandler() {
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

        // First, we need to check if there is an index existed in this region. This to make
        // sure that we do not miss any "DELETED" or "DELETING" index.
        final GetIndexRequest getIndexRequest = GetIndexRequest.builder().build();
        GetIndexResponse getIndexResponse;
        try {
            getIndexResponse = proxy.injectCredentialsAndInvokeV2(getIndexRequest, client::getIndex);
        } catch (RuntimeException e){
            HandlerErrorCode thisErrorCode = Convertor.convertExceptionToErrorCode(e, logger);
            return ProgressEvent.failed(model, callbackContext, thisErrorCode, null);
        }

        // Check if the existed index is the one that users want to delete.
        if( !getIndexResponse.arn().equals(model.getArn())){
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.NotFound, null);
        }

        // When the existed index is the one that users want to delete, we need to check
        // if it is already "DELETING" or "DELETED".
        if (getIndexResponse.stateAsString().equalsIgnoreCase(DELETING) ||
                getIndexResponse.stateAsString().equalsIgnoreCase(DELETED)) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.NotFound, null);
        }

        // Check if the index is not ACTIVE, we wait for the ACTIVE state.
        // . We want to make sure that we delete an ACTIVE index.
        if ( !getIndexResponse.stateAsString().equalsIgnoreCase(ACTIVE)){
            if(callbackContext != null){
                logger.log("[DELETE handler] In progress waiting for being ACTIVE before deleting.");
                callbackContext.setRetryCount(callbackContext.getRetryCount()+1);
                if (callbackContext.getRetryCount() >= MAX_RETRIES){
                    return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InternalFailure,
                            "Delete handler exceeded the maximum retries count");
                }
                return ProgressEvent.defaultInProgressHandler(callbackContext, DELAY_CONSTANT, model);
            }
            CallbackContext newCallbackContext = CallbackContext.builder()
                    .retryCount(1)
                    .build();
            return ProgressEvent.defaultInProgressHandler(newCallbackContext, DELAY_CONSTANT, model);

        }

        // Check if the index is AGGREGATOR, we need to update it to be LOCAL before deleting it.
        if (getIndexResponse.typeAsString().equalsIgnoreCase(AGGREGATOR)){
            UpdateIndexTypeRequest updateIndexTypeRequest = UpdateIndexTypeRequest.builder()
                    .arn(model.getArn())
                    .type(LOCAL)
                    .build();
            logger.log("[DELETE handler] UpdateIndexTypeRequest invokes to update AGGREGATOR " +
                    "to LOCAL before deleting.");
            try{
                proxy.injectCredentialsAndInvokeV2(updateIndexTypeRequest, client::updateIndexType);
            } catch (RuntimeException e) {
                HandlerErrorCode thisErrorCode = Convertor.convertExceptionToErrorCode(e, logger);
                logger.log(String.format("[DELETE handler] Error code at UpdateIndexType: %s.", thisErrorCode));
                // If this error is AlreadyExist, it meant that there is an existed aggregator,
                // users need to update that aggregator to be local before updating a new aggregator.
                return ProgressEvent.failed(model, null, thisErrorCode, e.getMessage());
            }

            logger.log("[DELETE handler] UpdateIndexType invoked successfully.");
            CallbackContext newCallbackContext = CallbackContext.builder()
                    .retryCount(1)
                    .build();
            return ProgressEvent.defaultInProgressHandler(newCallbackContext, DELAY_CONSTANT, model);
        }
        // When the existed index is the one that users want to delete, and it is not "DELETING"
        // or "DELETED", we delete it.
        final DeleteIndexRequest deleteIndexRequest = DeleteIndexRequest.builder()
                .arn(model.getArn())
                .build();
        try {
            proxy.injectCredentialsAndInvokeV2(deleteIndexRequest, client::deleteIndex);
        } catch (RuntimeException e){
            HandlerErrorCode thisErrorCode = Convertor.convertExceptionToErrorCode(e, logger);
            String errorMessage = e.getMessage();
            return ProgressEvent.failed(model, null, thisErrorCode, errorMessage);
        }
        return ProgressEvent.defaultSuccessHandler(null);
    }
}
