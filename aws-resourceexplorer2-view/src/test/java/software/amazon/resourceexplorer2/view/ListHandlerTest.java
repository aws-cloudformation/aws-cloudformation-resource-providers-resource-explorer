package software.amazon.resourceexplorer2.view;

// CloudFormation package
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;

// Resource Explorer package
import software.amazon.awssdk.services.resourceexplorer2.model.ListViewsRequest;
import software.amazon.awssdk.services.resourceexplorer2.model.ListViewsResponse;
import software.amazon.awssdk.services.resourceexplorer2.model.AccessDeniedException;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private ListHandler handler;

    private static final String exampleArn1 = "arn:aws:resource-explorer-2:us-west-2:123456789012:view/exampleView1/2b1ae2fd-5c32-428f-92e3-ac8a2fd50f52";
    private static final String exampleArn2 = "arn:aws:resource-explorer-2:us-west-2:123456789012:view/exampleView2/2b1ae2fd-5c32-428f-92e3-ac8a2fd50f42";

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
        handler = new ListHandler();
    }

    @Test
    public void handleRequest_NoRepeatLoop_Success() {

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .build();

        ListViewsRequest listViewsRequest = ListViewsRequest.builder()
                .nextToken(null)
                .build();

        ListViewsResponse listViewsResponse = ListViewsResponse.builder()
                .views(exampleArn1, exampleArn2)
                .nextToken(null)
                .build();

        List<ResourceModel> expectedModels = new ArrayList<ResourceModel>() {{
            add(ResourceModel.builder().viewArn(exampleArn1).build());
            add(ResourceModel.builder().viewArn(exampleArn2).build());
        }};

        when(proxy.injectCredentialsAndInvokeV2(eq(listViewsRequest), any()))
                .thenReturn(listViewsResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isEqualTo(expectedModels);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getNextToken()).isEqualTo(null);
    }

    @Test
    public void handleRequest_RepeatLoop_Success() {
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .build();

        ListViewsRequest listViewsRequest1 = ListViewsRequest.builder()
                .nextToken(null)
                .build();

        ListViewsResponse listViewsResponse1 = ListViewsResponse.builder()
                .views(exampleArn1)
                .nextToken("nextToken2")
                .build();

        when(proxy.injectCredentialsAndInvokeV2(eq(listViewsRequest1), any()))
                .thenReturn(listViewsResponse1);

        ListViewsRequest listViewsRequest2 = ListViewsRequest.builder()
                .nextToken("nextToken2")
                .build();

        ListViewsResponse listViewsResponse2 = ListViewsResponse.builder()
                .views(exampleArn2)
                .nextToken(null)
                .build();


        when(proxy.injectCredentialsAndInvokeV2(eq(listViewsRequest2), any()))
                .thenReturn(listViewsResponse2);

        List<ResourceModel> expectedModels = new ArrayList<ResourceModel>() {{
            add(ResourceModel.builder().viewArn(exampleArn1).build());
            add(ResourceModel.builder().viewArn(exampleArn2).build());
        }};

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isEqualTo(expectedModels);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getNextToken()).isEqualTo(null);


        //Capture the actual CreateViewRequest inside the Create Handler
        ArgumentCaptor<ListViewsRequest> capturedRequest= ArgumentCaptor.forClass(ListViewsRequest.class);
        verify(proxy, times(2)).injectCredentialsAndInvokeV2(capturedRequest.capture(), any());

        ListViewsRequest actualListViewRequest1 = capturedRequest.getAllValues().get(0);
        assertThat(actualListViewRequest1.nextToken()).isNull();

        ListViewsRequest actualListViewRequest2 = capturedRequest.getAllValues().get(1);
        assertThat(actualListViewRequest2.nextToken()).isEqualTo("nextToken2");
    }

    @Test
    public void handlerRequest_ThrowAccessDeniedException(){

        final ResourceModel model = ResourceModel.builder()
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();
        when(proxy.injectCredentialsAndInvokeV2(any(),any()))
                .thenThrow(AccessDeniedException.builder().build());

        ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, null, logger);

        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.AccessDenied);
    }
}
