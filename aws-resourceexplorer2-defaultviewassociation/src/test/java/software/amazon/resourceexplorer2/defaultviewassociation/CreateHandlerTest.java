package software.amazon.resourceexplorer2.defaultviewassociation;

//CloudFormation package
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;

//Resource Explorer package
import software.amazon.awssdk.services.resourceexplorer.model.AssociateDefaultViewRequest;
import software.amazon.awssdk.services.resourceexplorer.model.AssociateDefaultViewResponse;
import software.amazon.awssdk.services.resourceexplorer.model.GetDefaultViewRequest;
import software.amazon.awssdk.services.resourceexplorer.model.GetDefaultViewResponse;
import software.amazon.awssdk.services.resourceexplorer.model.InternalServerException;
import software.amazon.awssdk.services.resourceexplorer.model.AccessDeniedException;
import software.amazon.awssdk.services.resourceexplorer.model.ResourceNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private CreateHandler handler;
    private static String exampleArn1 = "arn:aws:resource-explorer-2:us-west-2:123456789012:view/exampleView/2b1ae2fd-5c32-428f-92e3-ac8a2fd50f52";
    private static String exampleArn2 = "arn:aws:resource-explorer-2:us-west-2:123456789012:view/exampleView2/2b1ae2fd-5c32-428f-92e3-ac8a2fd50f52";

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
        handler = new CreateHandler();
    }

    // This test verifies the success status of setting up a default view while there is
    // no existed default view in an account.
    @Test
    public void handleRequest_NonExistedDefaultView_Success() {

        GetDefaultViewRequest getDefaultViewRequest = GetDefaultViewRequest.builder().build();

        GetDefaultViewResponse getDefaultViewResponse = GetDefaultViewResponse.builder().build();

        when(proxy.injectCredentialsAndInvokeV2(eq(getDefaultViewRequest), any()))
                .thenReturn(getDefaultViewResponse);

        AssociateDefaultViewRequest associateDefaultViewRequest = AssociateDefaultViewRequest.builder()
                .viewArn(exampleArn1)
                .build();

        AssociateDefaultViewResponse associateDefaultViewResponse = AssociateDefaultViewResponse.builder()
                .viewArn(exampleArn1)
                .build();

        when(proxy.injectCredentialsAndInvokeV2( eq(associateDefaultViewRequest), any()))
                .thenReturn(associateDefaultViewResponse);

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
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    // This test verifies the success status of setting up a default view while there is
    // an existed default view in an account, but it is not the view we want to set up.
    @Test
    public void handleRequest_ExistedNotEqualDefaultView_Success() {

        GetDefaultViewRequest getDefaultViewRequest = GetDefaultViewRequest.builder().build();

        GetDefaultViewResponse getDefaultViewResponse = GetDefaultViewResponse.builder()
                .viewArn(exampleArn2)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(eq(getDefaultViewRequest), any()))
                .thenReturn(getDefaultViewResponse);

        AssociateDefaultViewRequest associateDefaultViewRequest = AssociateDefaultViewRequest.builder()
                .viewArn(exampleArn1)
                .build();

        AssociateDefaultViewResponse associateDefaultViewResponse = AssociateDefaultViewResponse.builder()
                .viewArn(exampleArn1)
                .build();

        when(proxy.injectCredentialsAndInvokeV2( eq(associateDefaultViewRequest), any()))
                .thenReturn(associateDefaultViewResponse);

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
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    // This test verifies the failed status of setting up the same default view already associated.
    @Test
    public void handleRequest_defaultViewAlreadyExisted() {

        GetDefaultViewRequest getDefaultViewRequest = GetDefaultViewRequest.builder().build();

        GetDefaultViewResponse getDefaultViewResponse = GetDefaultViewResponse.builder()
                .viewArn(exampleArn1)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(eq(getDefaultViewRequest), any()))
                .thenReturn(getDefaultViewResponse);

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
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.AlreadyExists);
    }

    @Test
    public void handleRequest_throwAccessDeniedException() {

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenThrow(AccessDeniedException.builder().build());

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
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.AccessDenied);
    }

    // This test verifies the failed status of setting up a non existed view to be a default view.
    @Test
    public void handleRequest_throwNotFoundException() {

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenThrow(ResourceNotFoundException.builder().build());

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
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }

    // This test throws InternalServerException when it invokes AssociateDefaultView.
    @Test
    public void handleRequest_throwInternalServerException() {

        GetDefaultViewRequest getDefaultViewRequest = GetDefaultViewRequest.builder().build();

        GetDefaultViewResponse getDefaultViewResponse = GetDefaultViewResponse.builder().build();

        when(proxy.injectCredentialsAndInvokeV2(eq(getDefaultViewRequest), any()))
                .thenReturn(getDefaultViewResponse);

        AssociateDefaultViewRequest associateDefaultViewRequest = AssociateDefaultViewRequest.builder()
                .viewArn(exampleArn1)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(eq(associateDefaultViewRequest), any()))
                .thenThrow(InternalServerException.builder().build());

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
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InternalFailure);
    }
}
