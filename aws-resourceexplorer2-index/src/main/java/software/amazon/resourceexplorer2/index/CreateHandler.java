package software.amazon.resourceexplorer2.index;

// CloudFormation package
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

// Resource Explorer package
import software.amazon.awssdk.services.resourceexplorer2.model.CreateIndexRequest;
import software.amazon.awssdk.services.resourceexplorer2.model.CreateIndexResponse;
import software.amazon.awssdk.services.resourceexplorer2.model.DeleteIndexRequest;
import software.amazon.awssdk.services.resourceexplorer2.model.GetIndexRequest;
import software.amazon.awssdk.services.resourceexplorer2.model.GetIndexResponse;
import software.amazon.awssdk.services.resourceexplorer2.model.UpdateIndexTypeRequest;
import software.amazon.awssdk.services.resourceexplorer2.model.UpdateIndexTypeResponse;
import software.amazon.awssdk.services.resourceexplorer2.ResourceExplorer2Client;

import static software.amazon.resourceexplorer2.index.IndexUtils.DELAY_CONSTANT;
import static software.amazon.resourceexplorer2.index.IndexUtils.MAX_RETRIES;
import static software.amazon.resourceexplorer2.index.IndexUtils.ACTIVE;
import static software.amazon.resourceexplorer2.index.IndexUtils.AGGREGATOR;
import static software.amazon.resourceexplorer2.index.IndexUtils.LOCAL;

public class CreateHandler extends BaseHandler<CallbackContext> {

    private final ResourceExplorer2Client client;

    public CreateHandler() {
        client = ClientFactory.getClient();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        if (TagTools.containsSystemTags(model)) {
            return ProgressEvent.failed(model, null, HandlerErrorCode.InvalidRequest, TagTools.INVALID_SYSTEM_TAG);
        }

        // First, we check if this is the first time CREATE handler invoked.
        // If it is the first time, we go invokeCreateIndex().
        if (callbackContext == null) {
            return invokeCreateIndex(model, logger, request, proxy);
        }

        // If there is callbackContext, CREATE handler is IN_PROGRESS.
        // We use GetIndex to check the index state.
        logger.log("[CREATE] Create in progress, invoking GetIndex.");
        GetIndexRequest getIndexRequest = GetIndexRequest.builder().build();
        GetIndexResponse getIndexResponse;
        try{
            getIndexResponse = proxy.injectCredentialsAndInvokeV2(getIndexRequest, client::getIndex);
        } catch (RuntimeException e){
            HandlerErrorCode thisErrorCode = Convertor.convertExceptionToErrorCode(e, logger);
            logger.log(String.format("[CREATE] Error code: %s.", thisErrorCode));
            return ProgressEvent.failed(model, callbackContext, thisErrorCode, "Could not get the index being created: " + e.getMessage());
        }

        logger.log("[CREATE] GetIndex invoked successfully.");
        // Check if the new created index is ACTIVE, then we reset retryCount and start
        // update index type if required.
        if (getIndexResponse.stateAsString().equalsIgnoreCase(ACTIVE)){

            // Check if CreateInProgress is true, it meant an index is created successfully and
            // its state is ACTIVE. We need to update index type if required.
            if (callbackContext.isCreateInProgress()){
                callbackContext.setCreateInProgress(false);
                callbackContext.setUpdateInProgress(true);
                // We reset retryCount because UpdateIndexType requires some time to finish.
                callbackContext.setRetryCount(1);

                model.setArn(getIndexResponse.arn());
                model.setIndexState(getIndexResponse.stateAsString());
                return updateIndexTypeHelper(model, logger, proxy, callbackContext);
            }

            // Check if UpdateInProgress is true, it meant the new created index is updated successfully
            // and its state is ACTIVE. We return succeed.
           if (callbackContext.isUpdateInProgress() && getIndexResponse.typeAsString().equalsIgnoreCase(model.getType())){
               model.setIndexState(ACTIVE);
               return ProgressEvent.defaultSuccessHandler(model);
           }
        }

        // If the new index is still CREATING, we increment retryCount.
        callbackContext.setRetryCount(callbackContext.getRetryCount() + 1);
        // If retryCount exceeds MAX_RETRIES, we stop waiting and start deleting the
        // created index before returning failed.
        if (callbackContext.getRetryCount() >= MAX_RETRIES && callbackContext.isCreateInProgress()){
            DeleteIndexRequest deleteIndexRequest = DeleteIndexRequest.builder()
                    .arn(getIndexResponse.arn())
                    .build();
            try {
                proxy.injectCredentialsAndInvokeV2(deleteIndexRequest, client::deleteIndex);
            } catch (RuntimeException e){
                HandlerErrorCode thisErrorCode = Convertor.convertExceptionToErrorCode(e, logger);
                return ProgressEvent.failed(model, null, thisErrorCode,
                    "Exceeded the max retry count while creating the index, then could not clean up the index: " + e.getMessage());
            }
            logger.log("[CREATE] DeleteIndex invoked.");
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InternalFailure,
                    "Exceeded the max retry count while creating the index.");
        }

        return ProgressEvent.defaultInProgressHandler(callbackContext, DELAY_CONSTANT, model);
    }

    private ProgressEvent<ResourceModel, CallbackContext> invokeCreateIndex (
            ResourceModel model, Logger logger,
            final ResourceHandlerRequest<ResourceModel> request,
            final AmazonWebServicesClientProxy proxy){

        CreateIndexRequest createIndexRequest = CreateIndexRequest.builder()
                .tags(TagTools.combineAllTypesOfTags(model, request, logger))
                .build();
        CreateIndexResponse createIndexResponse;
        logger.log("[CREATE] Invoking CreateIndex.");
        try{
            createIndexResponse = proxy.injectCredentialsAndInvokeV2(createIndexRequest, client::createIndex);
        } catch (RuntimeException e){
            HandlerErrorCode thisErrorCode = Convertor.convertExceptionToErrorCode(e, logger);
            logger.log(String.format("[CREATE] Error code: %s.", thisErrorCode));
            return ProgressEvent.failed(model, null, thisErrorCode, "Could not create the index: " + e.getMessage());
        }

        logger.log("[CREATE] CreateIndex invoked successfully.");

        // Set the new index arn and state for the Cfn resource model.
        model.setArn(createIndexResponse.arn());
        model.setIndexState(createIndexResponse.stateAsString());

        CallbackContext newCallbackContext = CallbackContext.builder()
                .createInProgress(true)
                .updateInProgress(false)
                .retryCount(1)
                .build();

        // Check IndexState of the creation
        logger.log("[CREATE] CreateIndexResponseState: "+ createIndexResponse.stateAsString());
        // Since any recent-created index has LOCAL index type as default, we need to make sure whether
        // users want a different index type. We need to check if the index is ACTIVE before staring the
        // updating process.
        if (createIndexResponse.stateAsString().equalsIgnoreCase(ACTIVE)){
            newCallbackContext.setCreateInProgress(false);
            newCallbackContext.setUpdateInProgress(true);
            return updateIndexTypeHelper(model, logger, proxy, newCallbackContext);
        }

        return ProgressEvent.defaultInProgressHandler(newCallbackContext, DELAY_CONSTANT, model);

    }

    // This method checks if users want to create an aggregator index, it will invoke
    // UPDATE handler.
    private ProgressEvent<ResourceModel, CallbackContext> updateIndexTypeHelper (
            ResourceModel model, Logger logger,
            final AmazonWebServicesClientProxy proxy, CallbackContext callbackContext){

        // The new created index is local as default. If users do not specify a desired type or
        // wish to have LOCAL type, we do not need to update. Then, return success.
        if(model.getType() == null || model.getType().equalsIgnoreCase(LOCAL)){
            model.setType(LOCAL);
            model.setIndexState(ACTIVE);
            logger.log("[CREATE] Type is already local. No need to update index type.");
            return ProgressEvent.defaultSuccessHandler(model);
        }

        UpdateIndexTypeRequest updateIndexTypeRequest = UpdateIndexTypeRequest.builder()
                .arn(model.getArn())
                .type(AGGREGATOR)
                .build();
        UpdateIndexTypeResponse updateIndexTypeResponse;
        try{
            updateIndexTypeResponse = proxy.injectCredentialsAndInvokeV2(updateIndexTypeRequest, client::updateIndexType);
        } catch (RuntimeException updateException){
            // If there is exception while invoking UpdateIndexType,
            // we delete the index and return Failed.
            final DeleteIndexRequest deleteIndexRequest = DeleteIndexRequest.builder()
                    .arn(model.getArn())
                    .build();
            try {
                proxy.injectCredentialsAndInvokeV2(deleteIndexRequest, client::deleteIndex);
            } catch (RuntimeException deleteException){
                HandlerErrorCode thisErrorCode = Convertor.convertExceptionToErrorCode(deleteException, logger);
                return ProgressEvent.failed(model, null,
                        thisErrorCode,
                        String.format("Index type could not be updated: %s, then could not delete the index: %",
                            updateException.getMessage(), deleteException.getMessage()));
            }
            model.setArn(null);
            model.setIndexState(null);
            return ProgressEvent.failed(model, null,
                    HandlerErrorCode.InternalFailure,
                    "Index type could not be updated: " + updateException.getMessage());
        }

        model.setIndexState(updateIndexTypeResponse.stateAsString());
        if (updateIndexTypeResponse.stateAsString().equalsIgnoreCase(ACTIVE)){
            return ProgressEvent.defaultSuccessHandler(model);
        }
        logger.log("[CREATE] UpdateIndexType invoked successfully.");
        return ProgressEvent.defaultInProgressHandler(callbackContext, DELAY_CONSTANT, model);

    }
}
