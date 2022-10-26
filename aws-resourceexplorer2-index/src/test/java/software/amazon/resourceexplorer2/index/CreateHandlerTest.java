package software.amazon.resourceexplorer2.index;

// CloudFormation package
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;

// Resource Explorer package
import software.amazon.awssdk.services.resourceexplorer.model.CreateIndexRequest;
import software.amazon.awssdk.services.resourceexplorer.model.CreateIndexResponse;
import software.amazon.awssdk.services.resourceexplorer.model.DeleteIndexRequest;
import software.amazon.awssdk.services.resourceexplorer.model.GetIndexRequest;
import software.amazon.awssdk.services.resourceexplorer.model.GetIndexResponse;
import software.amazon.awssdk.services.resourceexplorer.model.UpdateIndexTypeRequest;
import software.amazon.awssdk.services.resourceexplorer.model.UpdateIndexTypeResponse;
import software.amazon.awssdk.services.resourceexplorer.model.AccessDeniedException;
import software.amazon.awssdk.services.resourceexplorer.model.InternalServerException;
import software.amazon.awssdk.services.resourceexplorer.model.ConflictException;
import software.amazon.awssdk.services.resourceexplorer.model.ResourceExplorerRequest;

// Necessary Constants for testing
import static software.amazon.resourceexplorer2.index.IndexUtils.DELAY_CONSTANT;
import static software.amazon.resourceexplorer2.index.IndexUtils.MAX_RETRIES;
import static software.amazon.resourceexplorer2.index.IndexUtils.ACTIVE;
import static software.amazon.resourceexplorer2.index.IndexUtils.AGGREGATOR;
import static software.amazon.resourceexplorer2.index.IndexUtils.CREATING;
import static software.amazon.resourceexplorer2.index.IndexUtils.LOCAL;
import static software.amazon.resourceexplorer2.index.IndexUtils.UPDATING;
import static software.amazon.resourceexplorer2.index.TestConstants.EMPTY_TAGS;
import static software.amazon.resourceexplorer2.index.TestConstants.INDEX_ARN_1;
import static software.amazon.resourceexplorer2.index.TestConstants.STACK_LEVEL_TAGS;
import static software.amazon.resourceexplorer2.index.TestConstants.RESOURCE_TAGS;
import static software.amazon.resourceexplorer2.index.TestConstants.SYSTEM_TAGS;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import java.util.List;


@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
    }

    // This test verifies the SUCCESS status when creating a local index at the first try.
    @Test
    public void handleRequest_SimpleSuccess_AtTheFirstTry() {

        doReturn(CreateIndexResponse.builder()
                .arn(INDEX_ARN_1)
                .state(ACTIVE)
                .build())
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        final CreateHandler handler = new CreateHandler();

        final ResourceModel model = ResourceModel.builder()
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
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    // This test verifies the IN_PROGRESS status when creating a local index at the first try.
    @Test
    public void handleRequest_InProgress_AtTheFirstTry() {

        doReturn(CreateIndexResponse.builder()
                .arn(INDEX_ARN_1)
                .state(CREATING)
                .build())
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        final CreateHandler handler = new CreateHandler();

        final ResourceModel model = ResourceModel.builder()
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
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);

        assertThat(response.getCallbackContext()).isNotNull();
        assertThat(response.getCallbackContext().getRetryCount()).isEqualTo(1);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(DELAY_CONSTANT);

        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxy, times(1)).injectCredentialsAndInvokeV2(
                any(CreateIndexRequest.class), any());

    }

    // This test verifies the SUCCESS status when creating an aggregator index at the first try.
    @Test
    public void handleRequest_SuccessCreateAggregator_AtTheFirstTry() {

        doReturn(CreateIndexResponse.builder().arn(INDEX_ARN_1)
                .state(ACTIVE)
                .build())
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        // Build UpdateIndexTypeRequest and UpdateIndexTypeResponse
        UpdateIndexTypeRequest updateIndexTypeRequest = UpdateIndexTypeRequest.builder()
                .arn(INDEX_ARN_1)
                .type(AGGREGATOR)
                .build();

        UpdateIndexTypeResponse updateIndexTypeResponse = UpdateIndexTypeResponse.builder()
                .arn(INDEX_ARN_1)
                .type(AGGREGATOR)
                .state(ACTIVE)
                .build();

        doReturn(updateIndexTypeResponse)
                .when(proxy)
                .injectCredentialsAndInvokeV2(eq(updateIndexTypeRequest), any());

        final ResourceModel model = ResourceModel.builder()
                .type(AGGREGATOR)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .systemTags(SYSTEM_TAGS)
                .build();

        final CreateHandler handler = new CreateHandler();

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

        // Capture the actual CreateIndexRequest, GetIndexRequest, UpdateIndexTypeRequest
        // and ListTagsForResource in the process.
        ArgumentCaptor<ResourceExplorerRequest> capturedRequest = ArgumentCaptor.forClass(UpdateIndexTypeRequest.class);
        verify(proxy, times(2)).injectCredentialsAndInvokeV2(capturedRequest.capture(), any());
        List<ResourceExplorerRequest> invokedResourceExplorerRequest = capturedRequest.getAllValues();

        CreateIndexRequest invokedCreateIndexRequest = (CreateIndexRequest) invokedResourceExplorerRequest.get(0);
        assertThat(invokedCreateIndexRequest.tags()).isEqualTo(EMPTY_TAGS);

        UpdateIndexTypeRequest invokedUpdateIndexTypeRequest = (UpdateIndexTypeRequest) invokedResourceExplorerRequest.get(1);
        assertThat(invokedUpdateIndexTypeRequest.arn()).isEqualTo(INDEX_ARN_1);
        assertThat(invokedUpdateIndexTypeRequest.typeAsString()).isEqualTo(AGGREGATOR);


    }

    // This test verifies the IN_PROGRESS status when creating a local index at the second try.
    @Test
    public void handleRequest_InProgress_LOCALIndex_AtTheSecond() {

        doReturn(GetIndexResponse.builder()
                .arn(INDEX_ARN_1)
                .state(CREATING)
                .build())
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        final CreateHandler handler = new CreateHandler();

        final ResourceModel model = ResourceModel.builder()
                .type(LOCAL)
                .tags(RESOURCE_TAGS)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        CallbackContext callbackContext = CallbackContext.builder()
                .createInProgress(true)
                .updateInProgress(false)
                .retryCount(1)
                .build();
        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, callbackContext, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);

        assertThat(response.getCallbackContext()).isNotNull();
        assertThat(response.getCallbackContext().getRetryCount()).isEqualTo(2);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(IndexUtils.DELAY_CONSTANT);

        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxy, times(1)).injectCredentialsAndInvokeV2(
                any(GetIndexRequest.class), any());

    }

    // This test verifies the IN_PROGRESS status when updating a new created index to AGGREGATOR.
    @Test
    public void handleRequest_UpdateNewIndexToAGGREGATOR_InProgress() {

        final CreateHandler handler = new CreateHandler();

        // Build GetIndexRequest and GetIndexResponse
        GetIndexRequest getIndexRequest = GetIndexRequest.builder().build();
        GetIndexResponse getIndexResponse = GetIndexResponse.builder().arn(INDEX_ARN_1)
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

        final ResourceModel model = ResourceModel.builder()
                .type(AGGREGATOR)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .systemTags(SYSTEM_TAGS)
                .build();

        CallbackContext callbackContext = CallbackContext.builder()
                .createInProgress(true)
                .updateInProgress(false)
                .retryCount(1)
                .build();
        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, callbackContext, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isNotNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(DELAY_CONSTANT);
        assertThat(response.getResourceModel()).isNotNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        // Capture the actual CreateIndexRequest, GetIndexRequest, UpdateIndexTypeRequest
        // and ListTagsForResource in the process.
        ArgumentCaptor<ResourceExplorerRequest> capturedRequest = ArgumentCaptor.forClass(UpdateIndexTypeRequest.class);
        verify(proxy, times(2)).injectCredentialsAndInvokeV2(capturedRequest.capture(), any());
        List<ResourceExplorerRequest> invokedResourceExplorerRequest = capturedRequest.getAllValues();

        GetIndexRequest invokedGetIndexRequest = (GetIndexRequest) invokedResourceExplorerRequest.get(0);
        assertThat(invokedGetIndexRequest).isEqualTo(getIndexRequest);

        UpdateIndexTypeRequest invokedUpdateIndexTypeRequest = (UpdateIndexTypeRequest) invokedResourceExplorerRequest.get(1);
        assertThat(invokedUpdateIndexTypeRequest.arn()).isEqualTo(INDEX_ARN_1);
        assertThat(invokedUpdateIndexTypeRequest.typeAsString()).isEqualTo(AGGREGATOR);


    }

    // This test verifies the SUCCESS status when callback happens after updating index to
    // AGGREGATOR.
    @Test
    public void handleRequest_UpdateIndexSuccessAtCallback() {

        doReturn(GetIndexResponse.builder().arn(INDEX_ARN_1)
                .state(ACTIVE)
                .type(AGGREGATOR)
                .build())
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        final CreateHandler handler = new CreateHandler();

        final ResourceModel model = ResourceModel.builder()
                .type(AGGREGATOR)
                .tags(RESOURCE_TAGS)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        CallbackContext callbackContext = CallbackContext.builder()
                .createInProgress(false)
                .updateInProgress(true)
                .retryCount(1)
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

        verify(proxy, times(1)).injectCredentialsAndInvokeV2(
                any(GetIndexRequest.class), any());

    }


    // This test verifies the FAILED status when exceeding the maximum retries.
    @Test
    public void handleRequest_CallbackExceedMaxTries_Failed() {

        doReturn(GetIndexResponse.builder()
                .arn(INDEX_ARN_1)
                .state(CREATING)
                .build())
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        final ResourceModel model = ResourceModel.builder()
                .type(LOCAL)
                .tags(RESOURCE_TAGS)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final CreateHandler handler = new CreateHandler();
        CallbackContext callbackContext = CallbackContext.builder()
                .createInProgress(true)
                .updateInProgress(false)
                .retryCount(MAX_RETRIES-1)
                .build();
        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, callbackContext, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);

        // Verify CallBackContext after the second
        assertThat(response.getCallbackContext()).isNotNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getCallbackContext().getRetryCount()).isEqualTo(MAX_RETRIES);
        assertThat(response.getCallbackContext().isUpdateInProgress()).isFalse();
        assertThat(response.getCallbackContext().isCreateInProgress()).isTrue();

        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNotNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InternalFailure);

        // Verify there is only one time invoke Resource Explorer request (GetIndexRequest) here.
        verify(proxy, times(1)).injectCredentialsAndInvokeV2(
                any(GetIndexRequest.class), any());
    }

    // This test verifies that index is deleted when UpdateIndexType fails.
    @Test
    public void handleRequest_UpdateIndexTypeFailed_DeleteIndex_FailedStatus() {

        doReturn(CreateIndexResponse.builder().arn(INDEX_ARN_1)
                .state(ACTIVE)
                .build())
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        // Build UpdateIndexTypeRequest and UpdateIndexTypeResponse
        UpdateIndexTypeRequest updateIndexTypeRequest = UpdateIndexTypeRequest.builder()
                .arn(INDEX_ARN_1)
                .type(AGGREGATOR)
                .build();

        doThrow(InternalServerException.builder().build())
                .when(proxy)
                .injectCredentialsAndInvokeV2(eq(updateIndexTypeRequest), any());

        final ResourceModel model = ResourceModel.builder()
                .type(AGGREGATOR)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .systemTags(SYSTEM_TAGS)
                .build();

        final CreateHandler handler = new CreateHandler();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNotNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InternalFailure);

        // Capture the actual CreateIndexRequest,UpdateIndexTypeRequest and DeleteIndexRequest
        // in the process.
        ArgumentCaptor<ResourceExplorerRequest> capturedRequest = ArgumentCaptor.forClass(UpdateIndexTypeRequest.class);
        verify(proxy, times(3)).injectCredentialsAndInvokeV2(capturedRequest.capture(), any());
        List<ResourceExplorerRequest> invokedResourceExplorerRequest = capturedRequest.getAllValues();

        CreateIndexRequest invokedCreateIndexRequest = (CreateIndexRequest) invokedResourceExplorerRequest.get(0);
        assertThat(invokedCreateIndexRequest.tags()).isEqualTo(EMPTY_TAGS);

        UpdateIndexTypeRequest invokedUpdateIndexTypeRequest = (UpdateIndexTypeRequest) invokedResourceExplorerRequest.get(1);
        assertThat(invokedUpdateIndexTypeRequest.arn()).isEqualTo(INDEX_ARN_1);
        assertThat(invokedUpdateIndexTypeRequest.typeAsString()).isEqualTo(AGGREGATOR);

        DeleteIndexRequest invokedDeleteIndexRequest = (DeleteIndexRequest) invokedResourceExplorerRequest.get(2);
        assertThat(invokedDeleteIndexRequest.arn()).isEqualTo(INDEX_ARN_1);
    }

    // This test throws AlreadyExist when invoking CreateIndex at the first try.
    @Test
    public void handleRequest_ThrowAlreadyExist_AtTheFirstTry() {

        doThrow(ConflictException.builder().build())
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        final CreateHandler handler = new CreateHandler();

        final ResourceModel model = ResourceModel.builder()
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
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.AlreadyExists);
        assertThat(response.getResourceModel()).isNotNull();

        verify(proxy, times(1)).injectCredentialsAndInvokeV2(
                any(CreateIndexRequest.class), any());

    }

    // This test throws AccessDenied when invoking GetIndex at the second try.
    @Test
    public void handleRequest_ThrowAccessDenied_AtTheFirstTry() {

        doThrow(AccessDeniedException.builder().build())
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        final CreateHandler handler = new CreateHandler();

        final ResourceModel model = ResourceModel.builder()
                .type(LOCAL)
                .tags(RESOURCE_TAGS)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        CallbackContext callbackContext = CallbackContext.builder()
                .createInProgress(true)
                .updateInProgress(false)
                .retryCount(1)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, callbackContext, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.AccessDenied);
        assertThat(response.getResourceModel()).isNotNull();

        verify(proxy, times(1)).injectCredentialsAndInvokeV2(
                any(GetIndexRequest.class), any());

    }

    @Test
    public void handleRequest_SystemTagsInModel() {

        final CreateHandler handler = new CreateHandler();

        final ResourceModel model = ResourceModel.builder()
                .type(LOCAL)
                .tags(SYSTEM_TAGS)
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
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
        assertThat(response.getResourceModel()).isNotNull();
    }
}
