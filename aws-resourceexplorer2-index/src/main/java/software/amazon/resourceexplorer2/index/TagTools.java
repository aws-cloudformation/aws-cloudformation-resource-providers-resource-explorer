package software.amazon.resourceexplorer2.index;

// CloudFormation package
import software.amazon.awssdk.services.resourceexplorer.ResourceExplorerClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;

// Resource Explorer package
import software.amazon.awssdk.services.resourceexplorer.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.resourceexplorer.model.ListTagsForResourceResponse;

import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class TagTools {

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

        // CloudFormation System Tags (SystemTags) are automatically created,
        // but we need to add them separately.
        // TODO: We cannot support CloudFormation system tags now since the reserved "aws"
        //  keyword cannot be added "manually". Uncomment this block of code to enable CFN system tags.
//        if (request.getSystemTags() != null) {
//            tagMap.putAll(request.getSystemTags());
//        }
//        else{
//            logger.log("[generateTagsForCreate] CFN system tags are unexpectedly null for "
//                    + resourceModel.getArn());
//        }

        return tagMap;
    }

    // Get a list of tags of an index with ListTagsForResource
    public static Map<String, String> listTagsForIndex (ResourceExplorerClient client,
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
        logger.log("[listTagsForIndex] Invoked to list tags of " + IndexArn);
        return result;

    }
}
