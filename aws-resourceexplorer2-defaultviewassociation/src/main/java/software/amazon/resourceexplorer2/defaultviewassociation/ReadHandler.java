package software.amazon.resourceexplorer2.defaultviewassociation;


// CloudFormation package
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.HandlerErrorCode;

//Import Resource Explorer package
import software.amazon.awssdk.services.resourceexplorer.ResourceExplorerClient;
import software.amazon.awssdk.services.resourceexplorer.model.GetDefaultViewRequest;
import software.amazon.awssdk.services.resourceexplorer.model.GetDefaultViewResponse;

// TODO: Delete this import before publishing.
import software.amazon.awssdk.regions.Region;

import java.net.URI;

public class ReadHandler extends BaseHandler<CallbackContext> {
    private final ResourceExplorerClient client;
    public ReadHandler() {

        // TODO: Delete endpointOverride and region before publishing.
        client = ResourceExplorerClient.builder()
                .endpointOverride(URI.create("https://resource-explorer-2.us-west-2.api.aws"))
                .region(Region.US_WEST_2)
                .build();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        GetDefaultViewRequest getDefaultViewRequest = GetDefaultViewRequest.builder().build();
        GetDefaultViewResponse getDefaultViewResponse;
        try {
            getDefaultViewResponse = proxy.injectCredentialsAndInvokeV2( getDefaultViewRequest, client::getDefaultView );
        } catch (Exception e){
            HandlerErrorCode thisErrorCode = Convertor.convertExceptionToErrorCode(e, logger);
            return ProgressEvent.failed(model, callbackContext, thisErrorCode, null);
        }

        // If there is no existed default view, return NotFound error
        if (getDefaultViewResponse.viewArn() == null){
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.NotFound, null);
        }

        model.setViewArn(getDefaultViewResponse.viewArn());
        logger.log(String.format("[READ] DefaultView found: %s", model.getViewArn()));

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .status(OperationStatus.SUCCESS)
                .build();
    }
}
