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

import static software.amazon.resourceexplorer2.index.IndexUtils.*;


public class DeleteHandler extends BaseHandler<CallbackContext> {

    private final ResourceExplorerClient client;

    public DeleteHandler() {
        client = ClientFactory.getClient();
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
            return ProgressEvent.failed(model, callbackContext, thisErrorCode,
                "Could not get the index to be deleted: " + e.getMessage());
        }

        // Check if the existing index is the one that users want to delete.
        if( !getIndexResponse.arn().equals(model.getArn())){
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.NotFound,
                "The index does not exist.");
        }

        // When the existing index is the one that users want to delete, we need to check
        // if it is already "DELETING" or "DELETED".
        if (getIndexResponse.stateAsString().equalsIgnoreCase(DELETING) ||
                getIndexResponse.stateAsString().equalsIgnoreCase(DELETED)) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.NotFound,
                "The index has already been deleted.");
        }

        // Check if the index is not ACTIVE, we wait for the ACTIVE state.
        // . We want to make sure that we delete an ACTIVE index.
        if ( !getIndexResponse.stateAsString().equalsIgnoreCase(ACTIVE)){
            if(callbackContext != null){
                logger.log("[DELETE] In progress waiting for the index to be ACTIVE before deleting.");
                callbackContext.setRetryCount(callbackContext.getRetryCount()+1);
                if (callbackContext.getRetryCount() >= MAX_RETRIES){
                    return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InternalFailure,
                            "Exceeded the max retry count while deleting the index.");
                }
                return ProgressEvent.defaultInProgressHandler(callbackContext, DELAY_CONSTANT, model);
            }
            CallbackContext newCallbackContext = CallbackContext.builder()
                    .retryCount(1)
                    .build();
            return ProgressEvent.defaultInProgressHandler(newCallbackContext, DELAY_CONSTANT, model);

        }

        // When the existing index is the one that users want to delete, and it is not "DELETING"
        // or "DELETED", we delete it.
        final DeleteIndexRequest deleteIndexRequest = DeleteIndexRequest.builder()
                .arn(model.getArn())
                .build();
        try {
            proxy.injectCredentialsAndInvokeV2(deleteIndexRequest, client::deleteIndex);
        } catch (RuntimeException e){
            HandlerErrorCode thisErrorCode = Convertor.convertExceptionToErrorCode(e, logger);
            String errorMessage = e.getMessage();
            return ProgressEvent.failed(model, null, thisErrorCode, "Could not delete the index: " + errorMessage);
        }
        return ProgressEvent.defaultSuccessHandler(null);
    }
}
