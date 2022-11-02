package software.amazon.resourceexplorer2.index;

// CloudFormation package
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;

// Resource Explorer package
import software.amazon.awssdk.services.resourceexplorer2.model.AccessDeniedException;
import software.amazon.awssdk.services.resourceexplorer2.model.DeleteIndexRequest;
import software.amazon.awssdk.services.resourceexplorer2.model.GetIndexRequest;
import software.amazon.awssdk.services.resourceexplorer2.model.GetIndexResponse;
import software.amazon.awssdk.services.resourceexplorer2.model.UpdateIndexTypeRequest;
import software.amazon.awssdk.services.resourceexplorer2.model.ResourceNotFoundException;
import software.amazon.awssdk.services.resourceexplorer2.model.ResourceExplorer2Request;

import static software.amazon.resourceexplorer2.index.IndexUtils.ACTIVE;
import static software.amazon.resourceexplorer2.index.IndexUtils.AGGREGATOR;
import static software.amazon.resourceexplorer2.index.IndexUtils.DELAY_CONSTANT;
import static software.amazon.resourceexplorer2.index.IndexUtils.DELETING;
import static software.amazon.resourceexplorer2.index.IndexUtils.LOCAL;
import static software.amazon.resourceexplorer2.index.IndexUtils.MAX_RETRIES;
import static software.amazon.resourceexplorer2.index.IndexUtils.UPDATING;
import static software.amazon.resourceexplorer2.index.TestConstants.INDEX_ARN_1;
import static software.amazon.resourceexplorer2.index.TestConstants.INDEX_ARN_2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private DeleteHandler handler;

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
        handler = new DeleteHandler();
    }

    // This tests the success status when calling DELETE handler on an existed "ACTIVE" index.
    @Test
    public void handleRequest_SimpleSuccess() {

        GetIndexRequest getIndexRequest = GetIndexRequest.builder().build();
        GetIndexResponse getIndexResponse = GetIndexResponse.builder().arn(INDEX_ARN_1)
                .state(ACTIVE)
                .type(LOCAL)
                .build();

        doReturn(getIndexResponse)
                .when(proxy)
                .injectCredentialsAndInvokeV2(eq(getIndexRequest), any());

        final ResourceModel model = ResourceModel.builder()
                .arn(INDEX_ARN_1)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        // Verify that Resource Explorer request are invoked two times: GetIndexRequest and DeleteIndexRequest.
        ArgumentCaptor<ResourceExplorer2Request> capturedRequest = ArgumentCaptor.forClass(DeleteIndexRequest.class);
        verify(proxy, times(2)).injectCredentialsAndInvokeV2(capturedRequest.capture(), any());

    }

    // This tests the IN_PROGRESS status when calling DELETE handler on an UPDATING index.
    @Test
    public void handleRequest_DeleteUpdatingIndex_InProgressStatus() {

        GetIndexRequest getIndexRequest = GetIndexRequest.builder().build();
        GetIndexResponse getIndexResponse = GetIndexResponse.builder().arn(INDEX_ARN_1)
                .state(UPDATING)
                .type(LOCAL)
                .build();

        doReturn(getIndexResponse)
                .when(proxy)
                .injectCredentialsAndInvokeV2(eq(getIndexRequest), any());

        final ResourceModel model = ResourceModel.builder()
                .arn(INDEX_ARN_1)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isNotNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(DELAY_CONSTANT);
        assertThat(response.getResourceModel()).isNotNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        // Verify that only GetIndexRequest is invoked.
        verify(proxy, times(1)).injectCredentialsAndInvokeV2(
                any(GetIndexRequest.class), any());

    }

    // This tests the IN_PROGRESS status while waiting for an index to become ACTIVE
    // before we delete it.
    @Test
    public void handleRequest_Update_InProgressStatus() {

        GetIndexRequest getIndexRequest = GetIndexRequest.builder().build();
        GetIndexResponse getIndexResponse = GetIndexResponse.builder().arn(INDEX_ARN_1)
                .state(UPDATING)
                .type(LOCAL)
                .build();

        doReturn(getIndexResponse)
                .when(proxy)
                .injectCredentialsAndInvokeV2(eq(getIndexRequest), any());

        final ResourceModel model = ResourceModel.builder()
                .arn(INDEX_ARN_1)
                .type(AGGREGATOR)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        CallbackContext callbackContext = CallbackContext.builder()
                .retryCount(1)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, callbackContext, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isNotNull();
        assertThat(response.getCallbackContext().getRetryCount()).isEqualTo(2);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(DELAY_CONSTANT);
        assertThat(response.getResourceModel()).isNotNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        // Verify that only GetIndexRequest is invoked.
        verify(proxy, times(1)).injectCredentialsAndInvokeV2(
                any(GetIndexRequest.class), any());

    }

    // This tests the FAILED status when DELETE handler exceeds MAX_RETRIES.
    @Test
    public void handleRequest_ExceedMaxRetries_FailedStatus() {

        GetIndexRequest getIndexRequest = GetIndexRequest.builder().build();
        GetIndexResponse getIndexResponse = GetIndexResponse.builder().arn(INDEX_ARN_1)
                .state(UPDATING)
                .type(LOCAL)
                .build();

        doReturn(getIndexResponse)
                .when(proxy)
                .injectCredentialsAndInvokeV2(eq(getIndexRequest), any());

        final ResourceModel model = ResourceModel.builder()
                .arn(INDEX_ARN_1)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        CallbackContext callbackContext = CallbackContext.builder()
                .retryCount(MAX_RETRIES-1)
                .build();
        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, callbackContext, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isNotNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNotNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNotNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InternalFailure);

        // Verify that only GetIndexRequest is invoked.
        verify(proxy, times(1)).injectCredentialsAndInvokeV2(
                any(GetIndexRequest.class), any());

    }
    // This tests the failed status when the existing index (no matter which state it is)
    // is not the one we want to delete.
    @Test
    public void handleRequest_DesiredIndexNotExist_FailedStatus() {

        GetIndexRequest getIndexRequest = GetIndexRequest.builder().build();
        GetIndexResponse getIndexResponse = GetIndexResponse.builder().arn(INDEX_ARN_2)
                .state(ACTIVE)
                .type(LOCAL)
                .build();

        doReturn(getIndexResponse)
                .when(proxy)
                .injectCredentialsAndInvokeV2(eq(getIndexRequest), any());

        final ResourceModel model = ResourceModel.builder()
                .arn(INDEX_ARN_1)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNotNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNotNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);

        // Verify that only GetIndexRequest is invoked.
        verify(proxy, times(1)).injectCredentialsAndInvokeV2(
                any(GetIndexRequest.class), any());
    }

    // This tests the failed status when the existing index is the one we want to delete, but
    // its state is "DELETING" or "DELETED". It means that it is already being deleted before DELETE
    // handler is called. Return NotFound.
    @Test
    public void handleRequest_IndexAlreadyDeletedBefore_FailedStatus_NotFound() {

        GetIndexRequest getIndexRequest = GetIndexRequest.builder().build();
        GetIndexResponse getIndexResponse = GetIndexResponse.builder().arn(INDEX_ARN_1)
                .state(DELETING)
                .type(LOCAL)
                .build();

        doReturn(getIndexResponse)
                .when(proxy)
                .injectCredentialsAndInvokeV2(eq(getIndexRequest), any());

        final ResourceModel model = ResourceModel.builder()
                .arn(INDEX_ARN_1)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNotNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNotNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);

        // Verify that only GetIndexRequest is invoked.
        verify(proxy, times(1)).injectCredentialsAndInvokeV2(
                any(GetIndexRequest.class), any());
    }

    // This test verifies the exception convertor.
    @Test
    public void handleRequest_throwAccessDeniedExceptionAtTheFirstTry() {

        final ResourceModel model = ResourceModel.builder()
                .arn(INDEX_ARN_1)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when (proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenThrow(AccessDeniedException.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response).isNotNull();
        assertThat(response.getResourceModel()).isNotNull();

    }

    @Test
    public void handleRequest_ThrowResourceNotFoundAtDeleteIndexRequest() {

        GetIndexRequest getIndexRequest = GetIndexRequest.builder().build();
        GetIndexResponse getIndexResponse = GetIndexResponse.builder().arn(INDEX_ARN_1)
                .state(ACTIVE)
                .type(LOCAL)
                .build();

        doReturn(getIndexResponse)
                .when(proxy)
                .injectCredentialsAndInvokeV2(eq(getIndexRequest), any());

        doThrow(ResourceNotFoundException.builder().build())
                .when(proxy)
                .injectCredentialsAndInvokeV2(
                        eq(DeleteIndexRequest.builder().arn(INDEX_ARN_1).build()), any());

        final ResourceModel model = ResourceModel.builder()
                .arn(INDEX_ARN_1)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response).isNotNull();
        assertThat(response.getResourceModel()).isNotNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }
}
