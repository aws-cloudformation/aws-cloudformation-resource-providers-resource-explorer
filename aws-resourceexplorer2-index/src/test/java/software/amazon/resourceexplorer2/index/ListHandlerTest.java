package software.amazon.resourceexplorer2.index;

// CloudFormation package
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;

// Resource Explorer package
import software.amazon.awssdk.services.resourceexplorer2.model.ListIndexesRequest;
import software.amazon.awssdk.services.resourceexplorer2.model.ListIndexesResponse;
import software.amazon.awssdk.services.resourceexplorer2.model.Index;
import software.amazon.awssdk.services.resourceexplorer2.model.ResourceExplorer2Request;
import software.amazon.awssdk.services.resourceexplorer2.model.ValidationException;

import static software.amazon.resourceexplorer2.index.TestConstants.INDEX_ARN_1;
import static software.amazon.resourceexplorer2.index.TestConstants.INDEX_ARN_2;
import static software.amazon.resourceexplorer2.index.IndexUtils.LOCAL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest {
    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private ListHandler handler;

    private static final Index index1 = Index.builder().arn(INDEX_ARN_1).type(LOCAL).build();
    private static final Index index2 = Index.builder().arn(INDEX_ARN_2).type(LOCAL).build();

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
        handler = new ListHandler();
    }

    @Test
    public void handleRequest_SimpleSuccess() {

        ListIndexesRequest listIndexesRequest = ListIndexesRequest.builder().build();

        ListIndexesResponse listIndexesResponse = ListIndexesResponse.builder()
                .indexes(index1, index2)
                .nextToken(null)
                .build();

        doReturn(listIndexesResponse)
                .when(proxy)
                .injectCredentialsAndInvokeV2(eq(listIndexesRequest), any());

        List<ResourceModel> expectedModels = new ArrayList<ResourceModel>() {{
            add(ResourceModel.builder().arn(INDEX_ARN_1).build());
            add(ResourceModel.builder().arn(INDEX_ARN_2).build());
        }};
        final ResourceModel model = ResourceModel.builder()
                .arn(INDEX_ARN_1)
                .type(LOCAL)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

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
    }

    // This test assumes that the first ListIndexesRequest cannot list all existed indexes in an account.
    // There is a token in the first request, and we use that to list the rest.
    @Test
    public void handleRequest_RepeatLoop_Success() {

        ListIndexesRequest listIndexesRequest1 = ListIndexesRequest.builder()
                .nextToken(null)
                .build();

        ListIndexesResponse listIndexesResponse1 = ListIndexesResponse.builder()
                .indexes(index1)
                .nextToken("nextToken2")
                .build();

        doReturn(listIndexesResponse1)
                .when(proxy)
                .injectCredentialsAndInvokeV2(eq(listIndexesRequest1), any());

        ListIndexesRequest listIndexesRequest2 = ListIndexesRequest.builder()
                .nextToken("nextToken2")
                .build();

        ListIndexesResponse listIndexesResponse2 = ListIndexesResponse.builder()
                .indexes(index2)
                .nextToken(null)
                .build();

        doReturn(listIndexesResponse2)
                .when(proxy)
                .injectCredentialsAndInvokeV2(eq(listIndexesRequest2), any());

        List<ResourceModel> expectedModels = new ArrayList<ResourceModel>() {{
            add(ResourceModel.builder().arn(INDEX_ARN_1).build());
            add(ResourceModel.builder().arn(INDEX_ARN_2).build());
        }};
        final ResourceModel model = ResourceModel.builder()
                .arn(INDEX_ARN_1)
                .type(LOCAL)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

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
        ArgumentCaptor<ResourceExplorer2Request> capturedRequest= ArgumentCaptor.forClass(ListIndexesRequest.class);
        verify(proxy, times(2)).injectCredentialsAndInvokeV2(capturedRequest.capture(), any());

        ListIndexesRequest actualListIndexesRequest1 = (ListIndexesRequest) capturedRequest.getAllValues().get(0);
        assertThat(actualListIndexesRequest1.nextToken()).isNull();

        ListIndexesRequest actualListIndexesRequest2 = (ListIndexesRequest) capturedRequest.getAllValues().get(1);
        assertThat(actualListIndexesRequest2.nextToken()).isEqualTo("nextToken2");
    }

    @Test
    public void handlerRequest_ThrowValidationException(){

        final ResourceModel model = ResourceModel.builder()
                .type(LOCAL)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();
        when(proxy.injectCredentialsAndInvokeV2(any(),any()))
                .thenThrow(ValidationException.builder().build());

        ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, null, logger);

        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
    }
}
