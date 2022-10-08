package software.amazon.resourceexplorer2.view;

import com.google.common.annotations.VisibleForTesting;

// CloudFormation package
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.HandlerErrorCode;

//Import Resource Explorer package
import software.amazon.awssdk.services.resourceexplorer.ResourceExplorerClient;
import software.amazon.awssdk.services.resourceexplorer.model.UpdateViewRequest;
import software.amazon.awssdk.services.resourceexplorer.model.TagResourceRequest;
import software.amazon.awssdk.services.resourceexplorer.model.UntagResourceRequest;

import java.util.ArrayList;
import java.util.List;
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
            AmazonWebServicesClientProxy proxy,
            ResourceHandlerRequest<ResourceModel> request,
            CallbackContext callbackContext,
            Logger logger) {

        final ResourceModel desiredModel = request.getDesiredResourceState();

        UpdateViewRequest updateViewRequest = translateToUpdateViewRequest(desiredModel);
        try {
           proxy.injectCredentialsAndInvokeV2(updateViewRequest, client::updateView);
        }catch (RuntimeException e){
            HandlerErrorCode thisErrorCode = Convertor.convertExceptionToErrorCode(e, logger);
            logger.log(String.format("[UPDATE handler] Error code: %s.", thisErrorCode));
            return ProgressEvent.failed(desiredModel, callbackContext, thisErrorCode, e.getMessage());
        }

        // Update tags for this view.
        try {
            updateTags(proxy, request, logger);
        } catch (RuntimeException e) {
            HandlerErrorCode thisErrorCode = Convertor.convertExceptionToErrorCode(e, logger);
            logger.log(String.format("[UPDATE handler] Update Tags error code: %s.", thisErrorCode));
            return ProgressEvent.failed(desiredModel, callbackContext, thisErrorCode, e.getMessage());
        }

        return ProgressEvent.defaultSuccessHandler(desiredModel);
    }

     // Generate Tags to put in update request.
     // This requires to access both TagResource and UntagResource
    void updateTags ( AmazonWebServicesClientProxy proxy,
                      ResourceHandlerRequest<ResourceModel> request,
                      Logger logger) {

        ResourceModel desiredModel = request.getDesiredResourceState();

        // First, we need to get the current tags of this View.
        Map<String, String> currentTags = listTags(proxy, desiredModel.getViewArn(), logger);

        // Generate all types of desired tags into one map.
        Map<String,String> desiredTags = TagTools.combineAllTypesOfTags(desiredModel, request, logger);

        Map<String, String> tagsToAddOrModify = desiredTags.entrySet().stream()
                .filter(entry-> !entry.getKey().toLowerCase().startsWith("aws:") )
                .collect(Collectors.toMap(entry-> entry.getKey(), entry-> entry.getValue()));

        // Users cannot remove tags start with "aws:".
        // Note: System tags do not include other AWS tags except AWS:CloudFormation tags.
        Set<String> tagsToUntag = currentTags.keySet().stream()
                .filter(tagKey -> !desiredTags.containsKey(tagKey))
                .filter(tagKey -> !tagKey.toLowerCase().startsWith("aws:"))
                .collect(Collectors.toSet());

        if (!tagsToUntag.isEmpty()) {
            UntagResourceRequest untagResourceRequest = UntagResourceRequest.builder()
                    .resourceArn(desiredModel.getViewArn())
                    .tagKeys(tagsToUntag)
                    .build();
            proxy.injectCredentialsAndInvokeV2(untagResourceRequest, client::untagResource);
            logger.log(String.format("[UPDATE handler]  UntagResource removed some tags for %s.",
                    desiredModel.getViewArn()));
        }

        // We use TagResource to add/modify all of desiredTags.
        if ( !tagsToAddOrModify.isEmpty()){
            TagResourceRequest tagResourceRequest = TagResourceRequest.builder()
                    .resourceArn(desiredModel.getViewArn())
                    .tags(tagsToAddOrModify)
                    .build();
            proxy.injectCredentialsAndInvokeV2(tagResourceRequest, client::tagResource);
            logger.log(String.format("[UPDATE handler]  TagResource updated tags for %s.",
                    desiredModel.getViewArn()));
        }

    }

    private UpdateViewRequest translateToUpdateViewRequest (ResourceModel model){

        List<software.amazon.awssdk.services.resourceexplorer.model.IncludedProperty>
                thisIncludedProperties = new ArrayList<>();

        if (model.getIncludedProperties() != null) {
            for (IncludedProperty modelIncludedProperty: model.getIncludedProperties()) {
                software.amazon.awssdk.services.resourceexplorer.model.IncludedProperty thisIncludedProperty = software.amazon.awssdk.services.resourceexplorer.model.IncludedProperty.builder()
                        .name(modelIncludedProperty.getName())
                        .build();
                thisIncludedProperties.add(thisIncludedProperty);
            }
        }

        if (model.getFilters() == null){
            return UpdateViewRequest.builder()
                    .viewArn(model.getViewArn())
                    .includedProperties(thisIncludedProperties)
                    .build();
        }
        software.amazon.awssdk.services.resourceexplorer.model.SearchFilter thisSearchFilter = software.amazon.awssdk.services.resourceexplorer.model.SearchFilter.builder()
                .filterString(model.getFilters().getFilterString())
                .build();

        return UpdateViewRequest.builder()
                .viewArn(model.getViewArn())
                .includedProperties(thisIncludedProperties)
                .filters(thisSearchFilter)
                .build();

    }

    @VisibleForTesting
    Map<String, String> listTags(AmazonWebServicesClientProxy proxy,
                                 String viewArn, Logger logger){
        Map<String, String> tags = TagTools.listTagsForView(client, proxy, logger, viewArn);
        return tags;
    }
}
