package software.amazon.resourceexplorer2.index;

// CLoudFormation package
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.HandlerErrorCode;

// Resource Explorer package
import software.amazon.awssdk.services.resourceexplorer.model.GetIndexRequest;
import software.amazon.awssdk.services.resourceexplorer.model.GetIndexResponse;
import software.amazon.awssdk.services.resourceexplorer.model.TagResourceRequest;
import software.amazon.awssdk.services.resourceexplorer.model.UntagResourceRequest;
import software.amazon.awssdk.services.resourceexplorer.model.UpdateIndexTypeRequest;
import software.amazon.awssdk.services.resourceexplorer.model.UpdateIndexTypeResponse;
import software.amazon.awssdk.services.resourceexplorer.ResourceExplorerClient;

import static software.amazon.resourceexplorer2.index.IndexUtils.DELAY_CONSTANT;
import static software.amazon.resourceexplorer2.index.IndexUtils.MAX_RETRIES;
import static software.amazon.resourceexplorer2.index.IndexUtils.ACTIVE;
import static software.amazon.resourceexplorer2.index.IndexUtils.DELETING;
import static software.amazon.resourceexplorer2.index.IndexUtils.DELETED;
import static software.amazon.resourceexplorer2.index.IndexUtils.UPDATING;


import com.google.common.annotations.VisibleForTesting;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


public class UpdateHandler extends BaseHandler<CallbackContext> {

    private final ResourceExplorerClient client;

    public UpdateHandler() {
        client = ClientFactory.getClient();
    }
    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            CallbackContext callbackContext,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        if (TagTools.containsSystemTags(model)) {
            return ProgressEvent.failed(model, null, HandlerErrorCode.InvalidRequest, TagTools.INVALID_SYSTEM_TAG);
        }

        // Check if an index exists in this region by GetIndex before updating.
        GetIndexRequest getIndexRequest = GetIndexRequest.builder().build();
        GetIndexResponse getIndexResponse;
        try {
            getIndexResponse = proxy.injectCredentialsAndInvokeV2(getIndexRequest, client::getIndex);
        } catch (RuntimeException e){
            HandlerErrorCode thisErrorCode = Convertor.convertExceptionToErrorCode(e, logger);
            logger.log(String.format("[UPDATE] Error code at GetIndex: %s.", thisErrorCode));
            return ProgressEvent.failed(model, callbackContext, thisErrorCode,
                "Could not get the index to delete: " + e.getMessage());
        }

        logger.log("[UPDATE] An index exists in this region.");
        // Check if the index wanted to be updated is the same as the existed one.
        if ( !getIndexResponse.arn().equals(model.getArn()) || getIndexResponse.stateAsString().equalsIgnoreCase(DELETING) ||
            getIndexResponse.stateAsString().equalsIgnoreCase(DELETED)){
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.NotFound,
                    "The index to update does not exist or is being deleted.");
        }
        logger.log("[UPDATE]The existing index is the index that users want to update.");

        // If there is no callbackContext, this is a new UPDATE handler call.
        if (callbackContext == null ) {
            return invokeUpdateIndexType(model, request, getIndexResponse, logger, proxy);
        }

        // If the UPDATE handler is IN_PROGRESS, we increment retryCount.
        callbackContext.setRetryCount(callbackContext.getRetryCount() +1);

        // If UpdateIndexType finished, we update tags if required by calling updateTagsHelper().
        if (getIndexResponse.stateAsString().equalsIgnoreCase(ACTIVE) &&
                getIndexResponse.typeAsString().equalsIgnoreCase(model.getType())){
            model.setIndexState(ACTIVE);
            return updateTagsHelper(proxy, model, request, logger);
        }

        // If UpdateIndexType has not finished, and it already exceeded MAX_RETRIES, we return failed.
        if (callbackContext.getRetryCount() >= MAX_RETRIES){
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InternalFailure,
                "Exceeded the max retry count while updating the index.");
        }

        return ProgressEvent.defaultInProgressHandler(callbackContext, DELAY_CONSTANT, model);
    }

    private ProgressEvent<ResourceModel, CallbackContext> invokeUpdateIndexType (
            ResourceModel model, final ResourceHandlerRequest<ResourceModel> request,
            GetIndexResponse getIndexResponse, final Logger logger,
            final AmazonWebServicesClientProxy proxy){

        // This is a new UPDATE call. We should only update when IndexState is ACTIVE.
        if ( !getIndexResponse.stateAsString().equalsIgnoreCase(ACTIVE)){

            // If the index is not ready to be updated, return IN_PROGRESS but callback is null
            // because we do not actually update.
            return ProgressEvent.defaultInProgressHandler(null, DELAY_CONSTANT, model);
        }

        // If the current type is the same as the desired type, we do not need to update index type.
        // We execute UpdateTagsHelper before return success.
        if (getIndexResponse.typeAsString().equalsIgnoreCase(model.getType()) ){
            return updateTagsHelper(proxy, model, request, logger);
        }

        UpdateIndexTypeRequest updateIndexTypeRequest = UpdateIndexTypeRequest.builder()
                .arn(model.getArn())
                .type(model.getType().toUpperCase())
                .build();
        UpdateIndexTypeResponse updateIndexTypeResponse;
        logger.log("[UPDATE] UpdateIndexTypeRequest invokes.");

        try{
            updateIndexTypeResponse = proxy.injectCredentialsAndInvokeV2(updateIndexTypeRequest,
                    client::updateIndexType);
        } catch (RuntimeException e) {
            HandlerErrorCode thisErrorCode = Convertor.convertExceptionToErrorCode(e, logger);
            logger.log(String.format("[UPDATE] Error code at UpdateIndexType: %s.", thisErrorCode));
            // If this error is AlreadyExist, it meant that there is an existed aggregator,
            // users need to update that aggregator to be local before updating a new aggregator.
            return ProgressEvent.failed(model, null, thisErrorCode, "Could not update the index type: " + e.getMessage());
        }

        logger.log("[UPDATE] Invoked UpdateIndexType successfully.");
        // Check if the index state is "active", the index is updated.
        if (updateIndexTypeResponse.stateAsString().equalsIgnoreCase(ACTIVE)){
            model.setIndexState(ACTIVE);
            return updateTagsHelper(proxy, model, request, logger);
        }
        model.setIndexState(UPDATING);

        CallbackContext newCallbackContext = CallbackContext.builder()
                .updateInProgress(true)
                .retryCount(1)
                .build();
        return ProgressEvent.defaultInProgressHandler(newCallbackContext, DELAY_CONSTANT, model);
    }

    private ProgressEvent<ResourceModel, CallbackContext> updateTagsHelper (
            AmazonWebServicesClientProxy proxy, ResourceModel model,
            ResourceHandlerRequest<ResourceModel> request, Logger logger){
        logger.log("[UPDATE] updateTagsHelper invokes.");
        try {
            updateTags(proxy, request, logger);
        }catch (RuntimeException e) {
            HandlerErrorCode thisErrorCode = Convertor.convertExceptionToErrorCode(e, logger);
            logger.log(String.format("[UPDATE] Update Tags error code: %s.", thisErrorCode));
            return ProgressEvent.failed(model, null, thisErrorCode, "Could not update tags for the index: " + e.getMessage());
        }
        return ProgressEvent.defaultSuccessHandler(model);
    }

    // Update tags if required. This requires to access both TagResource and UntagResource.
    private void updateTags ( AmazonWebServicesClientProxy proxy,
                              ResourceHandlerRequest<ResourceModel> request,
                              Logger logger) {
        logger.log("[UPDATE] UpdateTags invoked.");

        ResourceModel desiredModel = request.getDesiredResourceState();

        // First, we need to get the current tags of this Index by using ListTagsForResource.
        Map<String, String> currentTags = listTags(proxy, desiredModel.getArn(), logger);

        // Generate all types of desired tags into one map.
        Map<String,String> desiredTags = TagTools.combineAllTypesOfTags(desiredModel, request, logger);

        Map<String, String> tagsToAddOrModify = desiredTags.entrySet().stream()
                .filter(entry-> !entry.getKey().toLowerCase().startsWith("aws:") )
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // Users cannot remove tags start with "aws:".
        Set<String> tagsToUntag = currentTags.keySet().stream()
                .filter(tagKey -> !desiredTags.containsKey(tagKey))
                .filter(tagKey -> !tagKey.toLowerCase().startsWith("aws:"))
                .collect(Collectors.toSet());

        if (!tagsToUntag.isEmpty()) {
            UntagResourceRequest untagResourceRequest = UntagResourceRequest.builder()
                    .resourceArn(desiredModel.getArn())
                    .tagKeys(tagsToUntag)
                    .build();
            proxy.injectCredentialsAndInvokeV2(untagResourceRequest, client::untagResource);
            logger.log(String.format("[UPDATE] UntagResource removed some tags for %s.",
                    desiredModel.getArn()));
        }

        // We use TagResource to add/modify all of desiredTags.
        if ( !tagsToAddOrModify.isEmpty()){
            TagResourceRequest tagResourceRequest = TagResourceRequest.builder()
                    .resourceArn(desiredModel.getArn())
                    .tags(tagsToAddOrModify)
                    .build();
            proxy.injectCredentialsAndInvokeV2(tagResourceRequest, client::tagResource);
            logger.log(String.format("[UPDATE] TagResource updated tags for %s.",
                    desiredModel.getArn()));
        }

    }

    @VisibleForTesting
    Map<String, String> listTags(AmazonWebServicesClientProxy proxy,
                                 String indexArn, Logger logger){
        return TagTools.listTagsForIndex(client, proxy, logger, indexArn);
    }
}
