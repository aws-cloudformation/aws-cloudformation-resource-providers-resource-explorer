package software.amazon.resourceexplorer2.defaultviewassociation;

//CloudFormation package
import software.amazon.awssdk.services.resourceexplorer.model.*;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;

//Resource Explorer package

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest {
    @Mock
    private AmazonWebServicesClientProxy proxy;
    @Mock
    private Logger logger;
    private DeleteHandler handler;
    private static final String exampleArn1 = "arn:aws:resource-explorer-2:us-west-2:123456789012:view/exampleView/2b1ae2fd-5c32-428f-92e3-ac8a2fd50f52";
    private static final String exampleArn2 = "arn:aws:resource-explorer-2:us-west-2:123456789012:view/exampleView2/2b1ae2fd-5c32-428f-92e3-ac8a2fd50f52";

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
        handler = new DeleteHandler();
    }

    @Test
    public void handleRequest_SimpleSuccess() {

        GetDefaultViewRequest getDefaultViewRequest = GetDefaultViewRequest.builder().build();

        GetDefaultViewResponse getDefaultViewResponse = GetDefaultViewResponse.builder()
                .viewArn(exampleArn1)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(eq(getDefaultViewRequest), any()))
                .thenReturn(getDefaultViewResponse);

        DisassociateDefaultViewRequest disassociateDefaultViewRequest = DisassociateDefaultViewRequest.builder().build();

        DisassociateDefaultViewResponse disassociateDefaultViewResponse = DisassociateDefaultViewResponse.builder().build();

        when(proxy.injectCredentialsAndInvokeV2(eq(disassociateDefaultViewRequest), any()))
                .thenReturn(disassociateDefaultViewResponse);

        final ResourceModel model = ResourceModel.builder()
                .viewArn(exampleArn1)
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
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_NoDefaultViewToDelete() {

        GetDefaultViewResponse getDefaultViewResponse = GetDefaultViewResponse.builder().build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenReturn(getDefaultViewResponse);

        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }

    // This test verifies that DELETE handler only deletes the desired ViewArn.
    @Test
    public void handleRequest_NotTheRightDefaultViewToDelete() {

        GetDefaultViewRequest getDefaultViewRequest = GetDefaultViewRequest.builder().build();

        GetDefaultViewResponse getDefaultViewResponse = GetDefaultViewResponse.builder()
                .viewArn(exampleArn1)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(eq(getDefaultViewRequest), any()))
                .thenReturn(getDefaultViewResponse);

        final ResourceModel model = ResourceModel.builder()
                .viewArn(exampleArn2)
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
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }

    @Test
    public void handleRequest_ThrowAccessDeniedException() {

        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenThrow(AccessDeniedException.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.AccessDenied);
    }

    // This test throws ValidationException when it invokes DisassociateDefaultView.
    @Test
    public void handleRequest_throwValidationException() {

        GetDefaultViewRequest getDefaultViewRequest = GetDefaultViewRequest.builder().build();
        GetDefaultViewResponse getDefaultViewResponse = GetDefaultViewResponse.builder()
                .viewArn(exampleArn1)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(eq(getDefaultViewRequest), any()))
                .thenReturn(getDefaultViewResponse);

        DisassociateDefaultViewRequest disassociateDefaultViewRequest = DisassociateDefaultViewRequest.builder().build();

        when(proxy.injectCredentialsAndInvokeV2(eq(disassociateDefaultViewRequest), any()))
                .thenThrow(ValidationException.builder().build());

        final ResourceModel model = ResourceModel.builder()
                .viewArn(exampleArn1)
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
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
    }
}
