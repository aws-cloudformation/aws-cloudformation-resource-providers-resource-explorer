package software.amazon.resourceexplorer2.view;

// CloudFormation package
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;

// Resource Explorer package
import software.amazon.awssdk.services.resourceexplorer2.model.UpdateViewRequest;
import software.amazon.awssdk.services.resourceexplorer2.model.ResourceExplorer2Request;
import software.amazon.awssdk.services.resourceexplorer2.model.TagResourceRequest;
import software.amazon.awssdk.services.resourceexplorer2.model.UntagResourceRequest;
import software.amazon.awssdk.services.resourceexplorer2.model.ResourceNotFoundException;


import static software.amazon.resourceexplorer2.view.TestConstants.EXAMPLE_ARN;
import static software.amazon.resourceexplorer2.view.TestConstants.RESOURCE_TAGS;
import static software.amazon.resourceexplorer2.view.TestConstants.STACK_LEVEL_TAGS;
import static software.amazon.resourceexplorer2.view.TestConstants.SYSTEM_TAGS;
import static software.amazon.resourceexplorer2.view.TestConstants.PRE_STACK_LEVEL_TAGS;
import static software.amazon.resourceexplorer2.view.TestConstants.MODEL_INCLUDED_PROPERTY_LIST;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @Spy
    private UpdateHandler handler;
    private static SearchFilter thisFilters = new SearchFilter("Service:s3 Region:us-west-2");
    private static SearchFilter newFilters = new SearchFilter("Service:s3 Region:us-east-1");

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
        handler = spy( new UpdateHandler());
    }

    // This test verifies the success status when updating full fields,
    // and removing the previous stack level tags. It also verifies all the requests.
    @Test
    public void handleRequest_SimpleUpdateSuccess_FullFields_RemoveTags_VerifyRequest() {
        // Create the previous tags in the current view
        Map<String, String> previousListTags = new HashMap<String, String>() {{
            putAll(PRE_STACK_LEVEL_TAGS);
            putAll(SYSTEM_TAGS);
        }};

        doReturn(previousListTags)
                .when(handler)
                .listTags(proxy, EXAMPLE_ARN, logger);

        final ResourceModel previousModel = ResourceModel.builder()
                .viewArn(EXAMPLE_ARN)
                .includedProperties(MODEL_INCLUDED_PROPERTY_LIST)
                .filters(thisFilters)
                .build();

        final ResourceModel desiredModel = ResourceModel.builder()
                .viewArn(EXAMPLE_ARN)
                .includedProperties(MODEL_INCLUDED_PROPERTY_LIST)
                .filters(newFilters)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(previousModel)
                .desiredResourceState(desiredModel)
                .systemTags(SYSTEM_TAGS)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        assertThat(response.getResourceModel()).isEqualTo(desiredModel);

        // Capture the actual UpdateViewRequest, and UntagResourceRequest in the Update handler.
        ArgumentCaptor<ResourceExplorer2Request> capturedRequest = ArgumentCaptor.forClass(UpdateViewRequest.class);
        verify(proxy, times(2)).injectCredentialsAndInvokeV2(capturedRequest.capture(), any());
        List<ResourceExplorer2Request> invokedResourceExplorer2Request = capturedRequest.getAllValues();

        UpdateViewRequest invokedUpdateViewRequest = (UpdateViewRequest) invokedResourceExplorer2Request.get(0);
        assertThat(invokedUpdateViewRequest.viewArn()).isEqualTo(desiredModel.getViewArn());
        assertThat(invokedUpdateViewRequest.filters().filterString()).isEqualTo(newFilters.getFilterString());

        UntagResourceRequest invokedUntagResourceRequest = (UntagResourceRequest) invokedResourceExplorer2Request.get(1);
        assertThat(invokedUntagResourceRequest.tagKeys().size()).isEqualTo(1);
        assertThat(invokedUntagResourceRequest.tagKeys().get(0)).isEqualTo("StackLevelTag");

    }

    // This test verifies the success status when updating full fields, adding new resource tags,
    // and modifying the stack level tags. It also verifies all the requests.
    @Test
    public void handleRequest_SimpleUpdateSuccess_MissingFields_AddAndModifyTags_VerifyRequests() {
        // Create the previous tags including only system tags in the current view
        Map<String, String> previousListTags = new HashMap<String, String>() {{
            putAll(PRE_STACK_LEVEL_TAGS);
            putAll(SYSTEM_TAGS);
        }};

        doReturn(previousListTags)
                .when(handler)
                .listTags(proxy, EXAMPLE_ARN, logger);

        final ResourceModel previousModel = ResourceModel.builder()
                .viewArn(EXAMPLE_ARN)
                .filters(thisFilters)
                .build();

        final ResourceModel desiredModel = ResourceModel.builder()
                .tags(RESOURCE_TAGS)
                .viewArn(EXAMPLE_ARN)
                .filters(newFilters)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(previousModel)
                .desiredResourceState(desiredModel)
                .desiredResourceTags(STACK_LEVEL_TAGS)
                .systemTags(SYSTEM_TAGS)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        // Capture the actual UpdateViewRequest and TagResourceRequest in the Update handler.
        // There is no UntagResourceRequest since no tag is removed.
        ArgumentCaptor<ResourceExplorer2Request> capturedRequest = ArgumentCaptor.forClass(UpdateViewRequest.class);
        verify(proxy, times(2)).injectCredentialsAndInvokeV2(capturedRequest.capture(), any());
        List<ResourceExplorer2Request> invokedResourceExplorer2Request = capturedRequest.getAllValues();

        UpdateViewRequest invokedUpdateViewRequest = (UpdateViewRequest) invokedResourceExplorer2Request.get(0);
        assertThat(invokedUpdateViewRequest.viewArn()).isEqualTo(desiredModel.getViewArn());
        assertThat(invokedUpdateViewRequest.filters().filterString()).isEqualTo(newFilters.getFilterString());

        TagResourceRequest invokedTagResourceRequest = (TagResourceRequest) invokedResourceExplorer2Request.get(1);
        Map<String, String> expectedAddAndModifyTags = new HashMap<String, String>() {{
            putAll(RESOURCE_TAGS);
            putAll(STACK_LEVEL_TAGS);
        }};
        assertThat(invokedTagResourceRequest.tags()).isEqualTo(expectedAddAndModifyTags);

    }

    @Test
    public void handleRequest_ResourceAlreadyDeleted_NotFoundException() {

        final ResourceModel previousModel = ResourceModel.builder()
                .viewArn(EXAMPLE_ARN)
                .includedProperties(MODEL_INCLUDED_PROPERTY_LIST)
                .filters(thisFilters)
                .build();

        final ResourceModel desiredModel = ResourceModel.builder()
                .viewArn(EXAMPLE_ARN)
                .includedProperties(MODEL_INCLUDED_PROPERTY_LIST)
                .filters(newFilters)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(previousModel)
                .desiredResourceState(desiredModel)
                .build();

        //If the resource is already deleted, the update API throws ResourceNotFoundException.
        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenThrow(ResourceNotFoundException.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);

    }

    @Test
    public void handleRequest_SystemTagsInModel() {

        final ResourceModel previousModel = ResourceModel.builder()
                .viewArn(EXAMPLE_ARN)
                .includedProperties(MODEL_INCLUDED_PROPERTY_LIST)
                .filters(thisFilters)
                .tags(PRE_STACK_LEVEL_TAGS)
                .build();

        final ResourceModel desiredModel = ResourceModel.builder()
                .viewArn(EXAMPLE_ARN)
                .includedProperties(MODEL_INCLUDED_PROPERTY_LIST)
                .filters(newFilters)
                .tags(SYSTEM_TAGS)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(previousModel)
                .desiredResourceState(desiredModel)
                .systemTags(SYSTEM_TAGS)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
        assertThat(response.getResourceModel()).isNotNull();

    }
}
