package software.amazon.resourceexplorer2.view;

// CloudFormation package
import software.amazon.awssdk.services.resourceexplorer.ResourceExplorerClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;


// Resource Explorer package
import software.amazon.awssdk.services.resourceexplorer.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.resourceexplorer.model.ListTagsForResourceResponse;

import java.util.HashMap;
import java.util.Map;

public class TagTools {

    public static final String INVALID_SYSTEM_TAG = "aws: prefixed tag key names are not allowed for external use.";

    // Combine all types of tags: resource tags, stack level tags, and system tags.
    public static Map<String, String> combineAllTypesOfTags(
            ResourceModel resourceModel,
            ResourceHandlerRequest<ResourceModel> request, Logger logger) {
        Map<String, String> tagMap = new HashMap<>();

        // DesiredResourceTags includes stack-level tags
        if (request.getDesiredResourceTags() != null){
            tagMap.putAll(request.getDesiredResourceTags());
        }

        // Add Resource Level Tags
        if (resourceModel.getTags() != null){
            tagMap.putAll(resourceModel.getTags());
        }

        // CloudFormation System Tags (SystemTags) are automatically created,
        // but we need to add them separately.
        if (request.getSystemTags() != null) {
            tagMap.putAll(request.getSystemTags());
        }
        else{
            logger.log("[GenerateTagsForCreate] CFN system tags are unexpectedly null for "
                    + resourceModel.getViewName());
        }

        return tagMap;
    }

    public static boolean containsSystemTags(ResourceModel model) {
        if (null == model.getTags() || model.getTags().isEmpty()) {
            return false;
        }

        for (String tagKey : model.getTags().keySet()) {
            if (tagKey.startsWith("aws:")) {
                return true;
            }
        }

        return false;
    }

    // Get a list of tags of a view with ListTagsForResource
    public static Map<String, String> listTagsForView (ResourceExplorerClient client,
                                                       AmazonWebServicesClientProxy proxy,
                                                       Logger logger, String ViewArn){
        Map<String, String> result = new HashMap<String, String>();
        ListTagsForResourceRequest listTagsForResourceRequest = ListTagsForResourceRequest.builder()
                .resourceArn(ViewArn)
                .build();

        ListTagsForResourceResponse listTagsForResourceResponse;
        listTagsForResourceResponse = proxy.injectCredentialsAndInvokeV2(listTagsForResourceRequest, client::listTagsForResource);
        if (listTagsForResourceResponse.tags() != null){
            result.putAll(listTagsForResourceResponse.tags());
        }

        logger.log("[ListTagsForView] Invoked to list tags of " + ViewArn);
        return result;

    }
}
