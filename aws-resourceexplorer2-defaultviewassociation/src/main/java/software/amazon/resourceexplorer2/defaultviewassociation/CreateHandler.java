package software.amazon.resourceexplorer2.defaultviewassociation;

// CloudFormation package
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.HandlerErrorCode;

// Resource Explorer package
import software.amazon.awssdk.services.resourceexplorer.ResourceExplorerClient;
import software.amazon.awssdk.services.resourceexplorer.model.AssociateDefaultViewRequest;
import software.amazon.awssdk.services.resourceexplorer.model.AssociateDefaultViewResponse;
import software.amazon.awssdk.services.resourceexplorer.model.GetDefaultViewRequest;
import software.amazon.awssdk.services.resourceexplorer.model.GetDefaultViewResponse;

// TODO: Delete this import before publishing.
import software.amazon.awssdk.regions.Region;

import java.net.URI;

public class CreateHandler extends BaseHandler<CallbackContext> {

    private final ResourceExplorerClient client;

    public CreateHandler() {

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

        // We need to check if there already exists a default view by using GetDefaultView operation.
        GetDefaultViewRequest getDefaultViewRequest = GetDefaultViewRequest.builder().build();
        GetDefaultViewResponse getDefaultViewResponse;
        try {
            getDefaultViewResponse = proxy.injectCredentialsAndInvokeV2( getDefaultViewRequest, client::getDefaultView );
        } catch (Exception e){
            logger.log(String.format("[CREATE] Error occurred in GetDefaultView."));
            HandlerErrorCode thisErrorCode = Convertor.convertExceptionToErrorCode(e, logger);
            return ProgressEvent.failed(model, callbackContext, thisErrorCode, null);
        }

        // If a default view existed, and it is the desired default view, return AlreadyExist Error.
        if (getDefaultViewResponse.viewArn() != null && getDefaultViewResponse.viewArn().equals(model.getViewArn())){
            logger.log(String.format("[CREATE] Default View already existed."));
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.AlreadyExists, null);
        }

        AssociateDefaultViewRequest associateDefaultViewRequest = AssociateDefaultViewRequest.builder()
                .viewArn(model.getViewArn())
                .build();
        AssociateDefaultViewResponse associateDefaultViewResponse;
        try {
            associateDefaultViewResponse = proxy.injectCredentialsAndInvokeV2( associateDefaultViewRequest, client::associateDefaultView );
            logger.log(String.format("[CREATE] DefaultView created successfully."));
        } catch (Exception e){
            HandlerErrorCode thisErrorCode = Convertor.convertExceptionToErrorCode(e, logger);
            logger.log(String.format("[CREATE] DefaultView failed: %s", thisErrorCode));
            return ProgressEvent.failed(model, callbackContext, thisErrorCode, null);
        }

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .status(OperationStatus.SUCCESS)
                .build();
    }
}
