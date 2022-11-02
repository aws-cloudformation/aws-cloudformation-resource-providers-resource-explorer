package software.amazon.resourceexplorer2.index;

// CloudFormation package
import software.amazon.awssdk.services.resourceexplorer2.ResourceExplorer2Client;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;

// Resource Explorer package
import software.amazon.awssdk.services.resourceexplorer2.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.resourceexplorer2.model.ListTagsForResourceResponse;

import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

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

        // Add Resource Level Tags (Tags that users put in JSON schema to create resource)
        if (resourceModel.getTags() != null){
            tagMap.putAll(resourceModel.getTags());
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

    // Get a list of tags of an index with ListTagsForResource
    public static Map<String, String> listTagsForIndex (ResourceExplorer2Client client,
                                                        AmazonWebServicesClientProxy proxy,
                                                        Logger logger, String IndexArn){
        Map<String, String> result = new HashMap<String, String>();
        ListTagsForResourceRequest listTagsForResourceRequest = ListTagsForResourceRequest.builder()
                .resourceArn(IndexArn)
                .build();
        ListTagsForResourceResponse listTagsForResourceResponse;
        listTagsForResourceResponse = proxy.injectCredentialsAndInvokeV2(listTagsForResourceRequest, client::listTagsForResource);
        if (listTagsForResourceResponse.tags() != null){
            result.putAll(listTagsForResourceResponse.tags());
        }
        logger.log("[ListTagsForIndex] Invoked to list tags of " + IndexArn);
        return result;

    }
}
