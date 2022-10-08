package software.amazon.resourceexplorer2.view;

// CloudFormation package
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;

// Resource Explorer package
import software.amazon.awssdk.services.resourceexplorer.model.GetViewRequest;
import software.amazon.awssdk.services.resourceexplorer.model.GetViewResponse;
import software.amazon.awssdk.services.resourceexplorer.model.View;
import software.amazon.awssdk.services.resourceexplorer.model.ResourceNotFoundException;
import software.amazon.awssdk.services.resourceexplorer.model.AccessDeniedException;
import software.amazon.awssdk.services.resourceexplorer.model.UnauthorizedException;

import static software.amazon.resourceexplorer2.view.TestConstants.EXAMPLE_ARN;
import static software.amazon.resourceexplorer2.view.TestConstants.VIEW_NAME;
import static software.amazon.resourceexplorer2.view.TestConstants.RESOURCE_TAGS;
import static software.amazon.resourceexplorer2.view.TestConstants.CLIENT_INCLUDED_PROPERTY_LIST;
import static software.amazon.resourceexplorer2.view.TestConstants.CLIENT_SEARCH_FILTER;
import static software.amazon.resourceexplorer2.view.TestConstants.MODEL_FILTERS;
import static software.amazon.resourceexplorer2.view.TestConstants.MODEL_INCLUDED_PROPERTY_LIST;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;


import java.util.HashMap;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private ReadHandler handler;

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
        handler = new ReadHandler();
    }

    @Test
    public void handleRequest_SimpleSuccess_WithoutTags() {

        Map<String, String> tags = new HashMap<String, String>(){{
            putAll(RESOURCE_TAGS);
        }};

        GetViewRequest expectedGetViewRequest = GetViewRequest.builder()
                .viewArn(EXAMPLE_ARN)
                .build();

        GetViewResponse getViewResponse = GetViewResponse.builder()
                .view( View.builder()
                        .viewArn(EXAMPLE_ARN)
                        .includedProperties(CLIENT_INCLUDED_PROPERTY_LIST)
                        .filters(CLIENT_SEARCH_FILTER)
                        .build())
                .tags(tags)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(eq(expectedGetViewRequest), any()))
                .thenReturn(getViewResponse);

        final ResourceModel model = ResourceModel.builder()
                .viewArn(EXAMPLE_ARN)
                .viewName(VIEW_NAME)
                .filters(MODEL_FILTERS)
                .includedProperties(MODEL_INCLUDED_PROPERTY_LIST)
                .tags(tags)
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
        assertThat(response.getResourceModel()).isEqualTo(model);

    }
    @Test
    public void handleRequest_throwResourceNotFoundException() {

        final ResourceModel model = ResourceModel.builder()
                .viewArn(EXAMPLE_ARN)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenThrow(ResourceNotFoundException.builder().build());

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

        final ResourceModel model = ResourceModel.builder()
                .viewArn(EXAMPLE_ARN)
                .build();

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

    @Test
    public void handleRequest_Throw401Exception(){

        final ResourceModel model = ResourceModel.builder()
                .viewName(VIEW_NAME)
                .includedProperties(MODEL_INCLUDED_PROPERTY_LIST)
                .filters(MODEL_FILTERS)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenThrow(UnauthorizedException.builder().build());

        ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, null, logger);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
        assertThat(response.getResourceModel()).isNotNull();
    }
}
