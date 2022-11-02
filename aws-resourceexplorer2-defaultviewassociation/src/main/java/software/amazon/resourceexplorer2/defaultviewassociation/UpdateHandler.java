package software.amazon.resourceexplorer2.defaultviewassociation;

import software.amazon.awssdk.services.resourceexplorer2.ResourceExplorer2Client;
import software.amazon.awssdk.services.resourceexplorer2.model.AssociateDefaultViewRequest;
import software.amazon.awssdk.services.resourceexplorer2.model.AssociateDefaultViewResponse;
import software.amazon.awssdk.services.resourceexplorer2.model.GetDefaultViewRequest;
import software.amazon.awssdk.services.resourceexplorer2.model.GetDefaultViewResponse;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class UpdateHandler extends BaseHandler<CallbackContext> {
    private final ResourceExplorer2Client client;
    public UpdateHandler() {
        client = ClientFactory.getClient();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        // to behave like other resources, they need to update using the actual primaryIdentifier, not a random value
        if (!request.getAwsAccountId().equals(model.getAssociatedAwsPrincipal())) {
            final String message = String.format("Default view not found for %s", model.getAssociatedAwsPrincipal());
            logger.log(message);
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.NotFound, message);
        }

        // We need to check if there already exists a default view by using GetDefaultView operation.
        GetDefaultViewRequest getDefaultViewRequest = GetDefaultViewRequest.builder().build();
        GetDefaultViewResponse getDefaultViewResponse;
        try {
            getDefaultViewResponse = proxy.injectCredentialsAndInvokeV2( getDefaultViewRequest, client::getDefaultView );
        } catch (Exception e){
            logger.log(String.format("[UPDATE] Error occurred in GetDefaultView."));
            HandlerErrorCode thisErrorCode = Convertor.convertExceptionToErrorCode(e, logger);
            return ProgressEvent.failed(model, callbackContext, thisErrorCode, "Could not check default view: " + e.getMessage());
        }

        logger.log(String.format("[UPDATE] Default view arn: " + getDefaultViewResponse.viewArn()));

        // If a default view does not exist, return NotFound Error.
        if (getDefaultViewResponse.viewArn() == null || getDefaultViewResponse.viewArn().length() == 0){
            logger.log(String.format("[UPDATE] A default view was not found to update."));
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.NotFound, "Default view not found.");
        }

        // if the default view is already set to what we want, avoid making extra API call.
        if (getDefaultViewResponse.viewArn().equals(model.getViewArn())) {
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .status(OperationStatus.SUCCESS)
                .build();
        }

        AssociateDefaultViewRequest associateDefaultViewRequest = AssociateDefaultViewRequest.builder()
            .viewArn(model.getViewArn())
            .build();
        AssociateDefaultViewResponse associateDefaultViewResponse;
        try {
            associateDefaultViewResponse = proxy.injectCredentialsAndInvokeV2( associateDefaultViewRequest, client::associateDefaultView );
            logger.log(String.format("[UPDATE] DefaultView updated successfully."));
        } catch (Exception e){
            HandlerErrorCode thisErrorCode = Convertor.convertExceptionToErrorCode(e, logger);
            logger.log(String.format("[UPDATE] Updating DefaultView failed: %s", thisErrorCode));
            return ProgressEvent.failed(model, callbackContext, thisErrorCode, "Could not associate a default view: " + e.getMessage());
        }

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
            .resourceModel(model)
            .status(OperationStatus.SUCCESS)
            .build();
    }
}
