package software.amazon.resourceexplorer2.view;

//CloudFormation package
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;

import static software.amazon.resourceexplorer2.view.TestConstants.EXAMPLE_ARN;
import static software.amazon.resourceexplorer2.view.TestConstants.VIEW_NAME;
import static software.amazon.resourceexplorer2.view.TestConstants.RESOURCE_TAGS;
import static software.amazon.resourceexplorer2.view.TestConstants.STACK_LEVEL_TAGS;
import static software.amazon.resourceexplorer2.view.TestConstants.SYSTEM_TAGS;
import static software.amazon.resourceexplorer2.view.TestConstants.CLIENT_INCLUDED_PROPERTY_LIST;
import static software.amazon.resourceexplorer2.view.TestConstants.CLIENT_SEARCH_FILTER;
import static software.amazon.resourceexplorer2.view.TestConstants.MODEL_INCLUDED_PROPERTY_LIST;
import static software.amazon.resourceexplorer2.view.TestConstants.MODEL_FILTERS;

//Resource Explorer package
import software.amazon.awssdk.services.resourceexplorer.model.CreateViewRequest;
import software.amazon.awssdk.services.resourceexplorer.model.CreateViewResponse;
import software.amazon.awssdk.services.resourceexplorer.model.View;
import software.amazon.awssdk.services.resourceexplorer.model.InternalServerException;
import software.amazon.awssdk.services.resourceexplorer.model.ConflictException;
import software.amazon.awssdk.services.resourceexplorer.model.ServiceQuotaExceededException;

//Mockito package
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.HashMap;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private CreateHandler handler;

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
        handler = new CreateHandler();
    }

    @Test
    public void handleRequest_SimpleCreateSuccess() {

        CreateViewResponse createViewResponse = CreateViewResponse.builder()
                .view( View.builder()
                        .viewArn(EXAMPLE_ARN)
                        .includedProperties(CLIENT_INCLUDED_PROPERTY_LIST)
                        .filters(CLIENT_SEARCH_FILTER)
                        .build())
                .build();

        when(proxy.injectCredentialsAndInvokeV2( any(), any()))
                .thenReturn(createViewResponse);

        //build Resource Model for the CREATE handler
        final ResourceModel model = ResourceModel.builder()
                .viewName(VIEW_NAME)
                .includedProperties(MODEL_INCLUDED_PROPERTY_LIST)
                .filters(MODEL_FILTERS)
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
        assertThat(response.getResourceModel()).isEqualTo(model);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        //Capture the actual CreateViewRequest inside the Create Handler
        ArgumentCaptor<CreateViewRequest> capturedRequest = ArgumentCaptor.forClass(CreateViewRequest.class);
        verify(proxy).injectCredentialsAndInvokeV2(capturedRequest.capture(), any());
        CreateViewRequest capturedRequestValue = capturedRequest.getValue();
        assertEquals(VIEW_NAME, capturedRequestValue.viewName());
        assertEquals(MODEL_FILTERS.getFilterString(), capturedRequestValue.filters().filterString());
        assertEquals(MODEL_INCLUDED_PROPERTY_LIST.get(0).getName(),
                capturedRequestValue.includedProperties().get(0).name());

        // Verify that response's tags includes all types of tags
        Map<String, String> expectedTags = new HashMap<String, String>();
        expectedTags.putAll(RESOURCE_TAGS);
        expectedTags.putAll(STACK_LEVEL_TAGS);

        expectedTags.putAll(SYSTEM_TAGS);
        assertThat(capturedRequestValue.tags()).containsExactlyEntriesOf(expectedTags);

    }

    @Test
    public void handleRequest_CreateSuccess_MissingFilterFields() {

        CreateViewResponse createViewResponse = CreateViewResponse.builder()
                .view( View.builder()
                        .viewArn(EXAMPLE_ARN)
                        .includedProperties(CLIENT_INCLUDED_PROPERTY_LIST)
                        .build())
                .build();

        when(proxy.injectCredentialsAndInvokeV2( any(), any()))
                .thenReturn(createViewResponse);

        //build Resource Model for the CREATE handler
        final ResourceModel model = ResourceModel.builder()
                .tags(RESOURCE_TAGS)
                .viewName(VIEW_NAME)
                .includedProperties(MODEL_INCLUDED_PROPERTY_LIST)
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

    // This test verifies that ViewArn cannot be created by users.
    @Test
    public void handleRequest_ViewArnReadOnlyTest(){

        final ResourceModel model = ResourceModel.builder()
                .viewName(VIEW_NAME)
                .viewArn(EXAMPLE_ARN)
                .includedProperties(MODEL_INCLUDED_PROPERTY_LIST)
                .filters(MODEL_FILTERS)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, null, logger);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
        assertThat(response.getResourceModel()).isNotNull();
    }

    @Test
    public void handleRequest_ThrowInternalServerException(){

        final ResourceModel model = ResourceModel.builder()
                .viewName(VIEW_NAME)
                .includedProperties(MODEL_INCLUDED_PROPERTY_LIST)
                .filters(MODEL_FILTERS)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenThrow(InternalServerException.builder().build());

        ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, null, logger);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InternalFailure);
        assertThat(response.getResourceModel()).isNotNull();
    }

    @Test
    public void handleRequest_ThrowServiceQuotaExceededException(){

        final ResourceModel model = ResourceModel.builder()
                .viewName(VIEW_NAME)
                .includedProperties(MODEL_INCLUDED_PROPERTY_LIST)
                .filters(MODEL_FILTERS)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenThrow(ServiceQuotaExceededException.builder().build());
        ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, null, logger);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.ServiceLimitExceeded);
        assertThat(response.getResourceModel()).isNotNull();
    }

    @Test
    public void handleRequest_ThrowAlreadyExist(){

        final ResourceModel model = ResourceModel.builder()
                .viewName(VIEW_NAME)
                .includedProperties(MODEL_INCLUDED_PROPERTY_LIST)
                .filters(MODEL_FILTERS)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenThrow(ConflictException.builder().build());
        ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, null, logger);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.AlreadyExists);
        assertThat(response.getResourceModel()).isNotNull();
    }

    @Test
    public void handleRequest_SystemTagsInModel(){

        final ResourceModel model = ResourceModel.builder()
                .viewName(VIEW_NAME)
                .viewArn(EXAMPLE_ARN)
                .includedProperties(MODEL_INCLUDED_PROPERTY_LIST)
                .filters(MODEL_FILTERS)
                .tags(SYSTEM_TAGS)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, null, logger);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
        assertThat(response.getResourceModel()).isNotNull();
    }
}
