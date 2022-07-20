package software.amazon.resourceexplorer2.index;

// CloudFormation package
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;

// Resource Explorer package
import software.amazon.awssdk.services.resourceexplorer.model.GetIndexRequest;
import software.amazon.awssdk.services.resourceexplorer.model.GetIndexResponse;
import software.amazon.awssdk.services.resourceexplorer.model.UpdateIndexTypeRequest;
import software.amazon.awssdk.services.resourceexplorer.model.UpdateIndexTypeResponse;
import software.amazon.awssdk.services.resourceexplorer.model.TagResourceRequest;
import software.amazon.awssdk.services.resourceexplorer.model.UntagResourceRequest;
import software.amazon.awssdk.services.resourceexplorer.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.resourceexplorer.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.resourceexplorer.model.ResourceExplorerRequest;
import software.amazon.awssdk.services.resourceexplorer.model.InternalServerException;
import software.amazon.awssdk.services.resourceexplorer.model.ResourceNotFoundException;

import static software.amazon.resourceexplorer2.index.TestConstants.INDEX_ARN_1;
import static software.amazon.resourceexplorer2.index.TestConstants.INDEX_ARN_2;
import static software.amazon.resourceexplorer2.index.TestConstants.RESOURCE_TAGS;
import static software.amazon.resourceexplorer2.index.TestConstants.STACK_LEVEL_TAGS;
import static software.amazon.resourceexplorer2.index.TestConstants.SYSTEM_TAGS;
import static software.amazon.resourceexplorer2.index.TestConstants.PRE_STACK_LEVEL_TAGS;
import static software.amazon.resourceexplorer2.index.IndexUtils.ACTIVE;
import static software.amazon.resourceexplorer2.index.IndexUtils.AGGREGATOR;
import static software.amazon.resourceexplorer2.index.IndexUtils.CREATING;
import static software.amazon.resourceexplorer2.index.IndexUtils.DELETING;
import static software.amazon.resourceexplorer2.index.IndexUtils.LOCAL;
import static software.amazon.resourceexplorer2.index.IndexUtils.UPDATING;
import static software.amazon.resourceexplorer2.index.IndexUtils.DELAY_CONSTANT;
import static software.amazon.resourceexplorer2.index.IndexUtils.MAX_RETRIES;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Spy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

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

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
        handler = spy( new UpdateHandler());
    }

    // This test verifies the success status when updating an index from AGGREGATOR to LOCAL.
    // Assume there is no wait time in updating, so the handler succeeds at the firs try.
    @Test
    public void handleRequest_FirstTimeInvokeUpdate_RemoveTags_Success() {

        // Create the previous tags including only system tags in the current index
        Map<String, String> previousListTags = new HashMap<String, String>() {{
            putAll(PRE_STACK_LEVEL_TAGS);
            putAll(SYSTEM_TAGS);
        }};

        ListTagsForResourceRequest listTagsForResourceRequest = ListTagsForResourceRequest.builder()
                .resourceArn(INDEX_ARN_1)
                .build();

        doReturn(ListTagsForResourceResponse.builder().tags(previousListTags).build())
                .when(proxy)
                .injectCredentialsAndInvokeV2(eq(listTagsForResourceRequest), any());

        // Build GetIndexRequest and GetIndexResponse
        GetIndexRequest getIndexRequest = GetIndexRequest.builder().build();
        GetIndexResponse getIndexResponse = GetIndexResponse.builder()
                .arn(INDEX_ARN_1)
                .type(AGGREGATOR)
                .state(ACTIVE)
                .build();

        doReturn(getIndexResponse)
                .when(proxy)
                .injectCredentialsAndInvokeV2(eq(getIndexRequest), any());

        // Build UpdateIndexTypeRequest and UpdateIndexTypeResponse
        UpdateIndexTypeRequest updateIndexTypeRequest = UpdateIndexTypeRequest.builder()
                .arn(INDEX_ARN_1)
                .type(LOCAL)
                .build();

        UpdateIndexTypeResponse updateIndexTypeResponse = UpdateIndexTypeResponse.builder()
                .arn(INDEX_ARN_1)
                .type(LOCAL)
                .state(ACTIVE)
                .build();

        doReturn(updateIndexTypeResponse)
                .when(proxy)
                .injectCredentialsAndInvokeV2(eq(updateIndexTypeRequest), any());

        final ResourceModel desiredModel = ResourceModel.builder()
                .arn(INDEX_ARN_1)
                .type(LOCAL)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(desiredModel)
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

        // Capture the actual GetIndexRequest, UpdateIndexTypeRequest and UntagResourceRequest in the Update handler.
        ArgumentCaptor<ResourceExplorerRequest> capturedRequest = ArgumentCaptor.forClass(UpdateIndexTypeRequest.class);
        verify(proxy, times(4)).injectCredentialsAndInvokeV2(capturedRequest.capture(), any());
        List<ResourceExplorerRequest> invokedResourceExplorerRequest = capturedRequest.getAllValues();

        UpdateIndexTypeRequest invokedUpdateIndexTypeRequest = (UpdateIndexTypeRequest) invokedResourceExplorerRequest.get(1);
        assertThat(invokedUpdateIndexTypeRequest.arn()).isEqualTo(desiredModel.getArn());
        assertThat(invokedUpdateIndexTypeRequest.typeAsString()).isEqualTo(desiredModel.getType());

        ListTagsForResourceRequest invokedListTagsForResourceRequest = (ListTagsForResourceRequest) invokedResourceExplorerRequest.get(2);
        assertThat(invokedListTagsForResourceRequest.resourceArn()).isEqualTo(desiredModel.getArn());

        UntagResourceRequest invokedUntagResourceRequest = (UntagResourceRequest) invokedResourceExplorerRequest.get(3);
        assertThat(invokedUntagResourceRequest.resourceArn()).isEqualTo(desiredModel.getArn());
        assertThat(invokedUntagResourceRequest.tagKeys().size()).isEqualTo(1);
        assertThat(invokedUntagResourceRequest.tagKeys().get(0)).isEqualTo("StackLevelTag");
    }

    // This test verifies the IN_PROGRESS status when updating an index from LOCAL to AGGREGATOR,
    // there is short wait time, then the UPDATE handler returns callBackContext that with "updating"
    // status.
    @Test
    public void handleRequest_FirstTimeInvokeUpdate_InProgressStatus() {
        // Build GetIndexRequest and GetIndexResponse
        GetIndexRequest getIndexRequest = GetIndexRequest.builder().build();
        GetIndexResponse getIndexResponse = GetIndexResponse.builder()
                .arn(INDEX_ARN_1)
                .type(LOCAL)
                .state(ACTIVE)
                .build();

        doReturn(getIndexResponse)
                .when(proxy)
                .injectCredentialsAndInvokeV2(eq(getIndexRequest), any());

        // Build UpdateIndexTypeRequest and UpdateIndexTypeResponse
        UpdateIndexTypeRequest updateIndexTypeRequest = UpdateIndexTypeRequest.builder()
                .arn(INDEX_ARN_1)
                .type(AGGREGATOR)
                .build();

        UpdateIndexTypeResponse updateIndexTypeResponse = UpdateIndexTypeResponse.builder()
                .arn(INDEX_ARN_1)
                .type(AGGREGATOR)
                .state(UPDATING)
                .build();

        doReturn(updateIndexTypeResponse)
                .when(proxy)
                .injectCredentialsAndInvokeV2(eq(updateIndexTypeRequest), any());

        final ResourceModel desiredModel = ResourceModel.builder()
                .arn(INDEX_ARN_1)
                .type(AGGREGATOR)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(desiredModel)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);

        // Verify CallBackContext after the first try
        assertThat(response.getCallbackContext()).isNotNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(DELAY_CONSTANT);
        assertThat(response.getCallbackContext().getRetryCount()).isEqualTo(1);
        assertThat(response.getCallbackContext().isUpdateInProgress()).isTrue();
        assertThat(response.getCallbackContext().isCreateInProgress()).isFalse();

        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        // Verify there is only one time invoke Resource Explorer request (GetIndexRequest) here.
        ArgumentCaptor<ResourceExplorerRequest> capturedRequest = ArgumentCaptor.forClass(UpdateIndexTypeRequest.class);
        verify(proxy, times(2)).injectCredentialsAndInvokeV2(
                capturedRequest.capture(), any());

    }

    // This test verifies the SUCCESS status when updating an index from AGGREGATOR to LOCAL,
    // after the first try.
    @Test
    public void handleRequest_CallbackUpdateAfterFirstTry_AddAndModifyTags_SuccessStatus() {

        // Create the previous tags including only system tags in the current index
        Map<String, String> previousListTags = new HashMap<String, String>() {{
            putAll(PRE_STACK_LEVEL_TAGS);
            putAll(SYSTEM_TAGS);
        }};

        doReturn(previousListTags)
                .when(handler)
                .listTags(proxy, INDEX_ARN_1, logger);

        doReturn(GetIndexResponse.builder().arn(INDEX_ARN_1).type(AGGREGATOR)
                .state(ACTIVE)
                .build())
                .when(proxy)
                .injectCredentialsAndInvokeV2(eq(GetIndexRequest.builder().build()), any());

        final ResourceModel desiredModel = ResourceModel.builder()
                .tags(RESOURCE_TAGS)
                .arn(INDEX_ARN_1)
                .type(AGGREGATOR)
                .build();

        CallbackContext callbackContext = CallbackContext.builder()
                .updateInProgress(true)
                .retryCount(1)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(desiredModel)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, callbackContext, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        // Capture the actual GetIndexRequest, TagResourceRequest and UntagResourceRequest in the Update handler.
        ArgumentCaptor<ResourceExplorerRequest> capturedRequest = ArgumentCaptor.forClass(UpdateIndexTypeRequest.class);
        verify(proxy, times(3)).injectCredentialsAndInvokeV2(capturedRequest.capture(), any());
        List<ResourceExplorerRequest> invokedResourceExplorerRequest = capturedRequest.getAllValues();

        UntagResourceRequest invokedUntagResourceRequest = (UntagResourceRequest) invokedResourceExplorerRequest.get(1);
        assertThat(invokedUntagResourceRequest.resourceArn()).isEqualTo(desiredModel.getArn());
        assertThat(invokedUntagResourceRequest.tagKeys().size()).isEqualTo(1);
        assertThat(invokedUntagResourceRequest.tagKeys().get(0)).isEqualTo("StackLevelTag");

        TagResourceRequest invokedTagResourceRequest = (TagResourceRequest) invokedResourceExplorerRequest.get(2);
        assertThat(invokedTagResourceRequest.resourceArn()).isEqualTo(desiredModel.getArn());
        assertThat(invokedTagResourceRequest.tags().size()).isEqualTo(2);

    }

    // This test verifies the IN_PROGRESS status again when updating an index from AGGREGATOR to LOCAL,
    // after the first try.
    @Test
    public void handleRequest_CallbackUpdateAfterFirstTry_InProgress() {
        doReturn(GetIndexResponse.builder()
                .arn(INDEX_ARN_1)
                .type(AGGREGATOR)
                .state(UPDATING)
                .build())
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        final ResourceModel desiredModel = ResourceModel.builder()
                .arn(INDEX_ARN_1)
                .type(AGGREGATOR)
                .build();

        CallbackContext callbackContext = CallbackContext.builder()
                .updateInProgress(true)
                .retryCount(1)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(desiredModel)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, callbackContext, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);

        // Verify CallBackContext after the second
        assertThat(response.getCallbackContext()).isNotNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(DELAY_CONSTANT);
        assertThat(response.getCallbackContext().getRetryCount()).isEqualTo(2);
        assertThat(response.getCallbackContext().isUpdateInProgress()).isTrue();
        assertThat(response.getCallbackContext().isCreateInProgress()).isFalse();

        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        // Verify there is only one time invoke Resource Explorer request (GetIndexRequest) here.
        verify(proxy, times(1)).injectCredentialsAndInvokeV2(
                any(GetIndexRequest.class), any());
    }

    // This test verifies the IN_PROGRESS status when updating an index from LOCAL to AGGREGATOR.
    // However, the index is not "active" to be updated
    @Test
    public void handleRequest_FirstTimeInvokeUpdate_IndexNotReady_InProgressStatus() {
        // Build GetIndexRequest and GetIndexResponse
        GetIndexRequest getIndexRequest = GetIndexRequest.builder().build();
        GetIndexResponse getIndexResponse = GetIndexResponse.builder()
                .arn(INDEX_ARN_1)
                .type(LOCAL)
                .state(CREATING)
                .build();

        doReturn(getIndexResponse)
                .when(proxy)
                .injectCredentialsAndInvokeV2(eq(getIndexRequest), any());

        final ResourceModel desiredModel = ResourceModel.builder()
                .arn(INDEX_ARN_1)
                .type(AGGREGATOR)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(desiredModel)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isNull();

        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        // Verify there is only one time invoke Resource Explorer request (GetIndexRequest) here.
        verify(proxy, times(1)).injectCredentialsAndInvokeV2(
                any(GetIndexRequest.class), any());

    }

    // This test verifies the SUCCESS when the UPDATE handler only modifies Tags without updating
    // Index Type at the first try.
    @Test
    public void handleRequest_AddAndModifyTagsOnly() {
        // Create the previous tags including only system tags in the current index
        Map<String, String> previousListTags = new HashMap<String, String>() {{
            putAll(PRE_STACK_LEVEL_TAGS);
            putAll(SYSTEM_TAGS);
        }};

        doReturn(previousListTags)
                .when(handler)
                .listTags(proxy, INDEX_ARN_1, logger);

        // Build GetIndexRequest and GetIndexResponse
        GetIndexRequest getIndexRequest = GetIndexRequest.builder().build();
        GetIndexResponse getIndexResponse = GetIndexResponse.builder()
                .arn(INDEX_ARN_1)
                .type(LOCAL)
                .state(ACTIVE)
                .build();

        doReturn(getIndexResponse)
                .when(proxy)
                .injectCredentialsAndInvokeV2(eq(getIndexRequest), any());


        final ResourceModel desiredModel = ResourceModel.builder()
                .tags(RESOURCE_TAGS)
                .arn(INDEX_ARN_1)
                .type(LOCAL)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(desiredModel)
                .desiredResourceTags(STACK_LEVEL_TAGS)
                .systemTags(SYSTEM_TAGS)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        // Capture the actual GetIndexRequest, TagResourceRequest and UntagResourceRequest in the Update handler.
        ArgumentCaptor<ResourceExplorerRequest> capturedRequest = ArgumentCaptor.forClass(UpdateIndexTypeRequest.class);
        verify(proxy, times(2)).injectCredentialsAndInvokeV2(capturedRequest.capture(), any());
        List<ResourceExplorerRequest> invokedResourceExplorerRequest = capturedRequest.getAllValues();

        TagResourceRequest invokedTagResourceRequest = (TagResourceRequest) invokedResourceExplorerRequest.get(1);
        assertThat(invokedTagResourceRequest.resourceArn()).isEqualTo(desiredModel.getArn());
        assertThat(invokedTagResourceRequest.tags().size()).isEqualTo(3);

    }

    // This test verifies the FAILED status when trying to update many times.
    @Test
    public void handleRequest_CallbackUpdateExceedMaxTries_Failed() {

        doReturn(GetIndexResponse.builder().arn(INDEX_ARN_1).type(AGGREGATOR)
                .state(UPDATING)
                .build())
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        final ResourceModel desiredModel = ResourceModel.builder()
                .arn(INDEX_ARN_1)
                .type(AGGREGATOR)
                .build();

        CallbackContext callbackContext = CallbackContext.builder()
                .updateInProgress(true)
                .retryCount(MAX_RETRIES-1)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(desiredModel)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, callbackContext, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);

        // Verify CallBackContext after the second
        assertThat(response.getCallbackContext()).isNotNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getCallbackContext().getRetryCount()).isEqualTo(MAX_RETRIES);
        assertThat(response.getCallbackContext().isUpdateInProgress()).isTrue();
        assertThat(response.getCallbackContext().isCreateInProgress()).isFalse();

        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InternalFailure);

        // Verify there is only one time invoke Resource Explorer request (GetIndexRequest) here.
        verify(proxy, times(1)).injectCredentialsAndInvokeV2(
                any(GetIndexRequest.class), any());
    }

    // This test verifies the FAILED status with NotFound error when the desired-updated
    // Arn does not exist anymore.
    @Test
    public void handleRequest_DesiredUpdateIndexNotExist_NotFoundReturn() {

        doReturn(GetIndexResponse.builder().arn(INDEX_ARN_2).type(AGGREGATOR)
                .state(ACTIVE)
                .build())
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        final ResourceModel desiredModel = ResourceModel.builder()
                .arn(INDEX_ARN_1)
                .type(AGGREGATOR)
                .build();

        CallbackContext callbackContext = CallbackContext.builder()
                .updateInProgress(true)
                .retryCount(MAX_RETRIES-1)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(desiredModel)
                .build();

        final UpdateHandler handler = new UpdateHandler();
        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, callbackContext, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);

        // Verify CallBackContext after the second
        assertThat(response.getCallbackContext()).isNotNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getCallbackContext().getRetryCount()).isEqualTo(2);
        assertThat(response.getCallbackContext().isUpdateInProgress()).isTrue();
        assertThat(response.getCallbackContext().isCreateInProgress()).isFalse();

        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNotNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);

        // Verify there is only one time invoke Resource Explorer request (GetIndexRequest) here.
        verify(proxy, times(1)).injectCredentialsAndInvokeV2(
                any(GetIndexRequest.class), any());
    }


    @Test
    public void handleRequest_UpdateDeletingIndex_Failed_NotFoundError() {

        doReturn(GetIndexResponse.builder().arn(INDEX_ARN_1).type(LOCAL)
                .state(DELETING)
                .build())
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        final ResourceModel desiredModel = ResourceModel.builder()
                .arn(INDEX_ARN_1)
                .type(AGGREGATOR)
                .build();

        CallbackContext callbackContext = CallbackContext.builder()
                .updateInProgress(true)
                .retryCount(1)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(desiredModel)
                .build();

        final UpdateHandler handler = new UpdateHandler();
        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, callbackContext, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);

        // Verify CallBackContext after the second
        assertThat(response.getCallbackContext()).isNotNull();

        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNotNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);

        // Verify there is only one time invoke Resource Explorer request (GetIndexRequest) here.
        verify(proxy, times(1)).injectCredentialsAndInvokeV2(
                any(GetIndexRequest.class), any());
    }

    // This test throws NotFound when invoking GetIndex.
    @Test
    public void handleRequest_ThrowNotFound_GetIndex() {

        doThrow(ResourceNotFoundException.builder().build())
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        final ResourceModel model = ResourceModel.builder()
                .arn(INDEX_ARN_1)
                .type(LOCAL)
                .tags(RESOURCE_TAGS)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .desiredResourceTags(STACK_LEVEL_TAGS)
                .systemTags(SYSTEM_TAGS)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
        assertThat(response.getResourceModel()).isNotNull();

        verify(proxy, times(1)).injectCredentialsAndInvokeV2(
                any(GetIndexRequest.class), any());

    }

    // This test throws NotFound when invoking GetIndex.
    @Test
    public void handleRequest_ThrowInternalServer_UpdateIndex() {

        doReturn(GetIndexResponse.builder().arn(INDEX_ARN_1).type(LOCAL)
                .state(ACTIVE)
                .build())
                .when(proxy)
                .injectCredentialsAndInvokeV2(eq(GetIndexRequest.builder().build()), any());

        UpdateIndexTypeRequest updateIndexTypeRequest = UpdateIndexTypeRequest.builder()
                .arn(INDEX_ARN_1)
                .type(AGGREGATOR)
                .build();

        doThrow(InternalServerException.builder().build())
                .when(proxy)
                .injectCredentialsAndInvokeV2(eq(updateIndexTypeRequest), any());

        final ResourceModel model = ResourceModel.builder()
                .arn(INDEX_ARN_1)
                .type(AGGREGATOR)
                .tags(RESOURCE_TAGS)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final UpdateHandler handler = new UpdateHandler();
        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InternalFailure);
        assertThat(response.getResourceModel()).isNotNull();

        // Verify there is only one time invoke Resource Explorer request (GetIndexRequest) here.
        ArgumentCaptor<ResourceExplorerRequest> capturedRequest = ArgumentCaptor.forClass(UpdateIndexTypeRequest.class);
        verify(proxy, times(2)).injectCredentialsAndInvokeV2(
                capturedRequest.capture(), any());

    }
}
