package software.amazon.resourceexplorer2.defaultviewassociation;

// CloudFormation package
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.HandlerErrorCode;

// Resource Explorer package
import software.amazon.awssdk.services.resourceexplorer2.ResourceExplorer2Client;
import software.amazon.awssdk.services.resourceexplorer2.model.DisassociateDefaultViewRequest;
import software.amazon.awssdk.services.resourceexplorer2.model.DisassociateDefaultViewResponse;
import software.amazon.awssdk.services.resourceexplorer2.model.GetDefaultViewRequest;
import software.amazon.awssdk.services.resourceexplorer2.model.GetDefaultViewResponse;


public class DeleteHandler extends REBaseHandler<CallbackContext> {
    private final ResourceExplorer2Client client;
    public DeleteHandler() {
        client = ClientFactory.getClient();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        logRequestInfo(request, logger);

        final ResourceModel model = request.getDesiredResourceState();

        // to behave like other resources, they need to delete using the actual primaryIdentifier, not a random value
        if (!request.getAwsAccountId().equals(model.getAssociatedAwsPrincipal())) {
            final String message = String.format("Default view not found for %s", model.getAssociatedAwsPrincipal());
            logger.log(message);
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.NotFound, message);
        }

        // We need to check if there is a default view to delete.
        GetDefaultViewRequest getDefaultViewRequest = GetDefaultViewRequest.builder().build();
        GetDefaultViewResponse getDefaultViewResponse;
        try {
            getDefaultViewResponse = proxy.injectCredentialsAndInvokeV2( getDefaultViewRequest, client::getDefaultView );
        } catch (Exception e){
            logger.log(String.format("[DELETE] Error occurred in GetDefaultView."));
            HandlerErrorCode thisErrorCode = Convertor.convertExceptionToErrorCode(e, logger);
            return ProgressEvent.failed(model, callbackContext, thisErrorCode, "Could not get default view to be deleted: " + e.getMessage());
        }

        // If there is no default view, return NotFound error.
        if (getDefaultViewResponse.viewArn() == null){
            logger.log(String.format("[DELETE] Default View not found."));
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.NotFound, "Could not find the default view to disassociate.");
        }

        DisassociateDefaultViewRequest disassociateDefaultViewRequest = DisassociateDefaultViewRequest.builder().build();
        DisassociateDefaultViewResponse disassociateDefaultViewResponse;

        try {
            disassociateDefaultViewResponse = proxy.injectCredentialsAndInvokeV2(disassociateDefaultViewRequest, client::disassociateDefaultView);
        } catch (RuntimeException e){
            HandlerErrorCode thisErrorCode = Convertor.convertExceptionToErrorCode(e, logger);
            return ProgressEvent.failed(model, callbackContext, thisErrorCode, "Could not disassociate the default view: " + e.getMessage());
        }

        return ProgressEvent.defaultSuccessHandler(null);
    }
}
