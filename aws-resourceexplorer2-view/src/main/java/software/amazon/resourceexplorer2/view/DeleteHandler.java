package software.amazon.resourceexplorer2.view;

// CloudFormation package
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.HandlerErrorCode;

// Resource Explorer package
import software.amazon.awssdk.services.resourceexplorer2.ResourceExplorer2Client;
import software.amazon.awssdk.services.resourceexplorer2.model.DeleteViewRequest;
import software.amazon.awssdk.services.resourceexplorer2.model.GetViewRequest;


public class DeleteHandler extends BaseHandler<CallbackContext> {

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

        final ResourceModel model = request.getDesiredResourceState();
        String errorMessage = "There are internal failures";

        // We need to check if the View resource exists before deleting by using GetViewRequest.
        GetViewRequest getViewRequest = GetViewRequest.builder()
                .viewArn(model.getViewArn())
                .build();
        try {
            proxy.injectCredentialsAndInvokeV2(getViewRequest, client::getView);
        } catch (RuntimeException e){
            HandlerErrorCode errorCode = Convertor.convertExceptionToErrorCode(e, logger);
            logger.log(String.format("[DELETE] Error Code: %s.", errorCode));
            return ProgressEvent.failed(model, callbackContext, errorCode,
                "Could not get the view to delete: " + e.getMessage());
        }

        logger.log(String.format("[DELETE] The expected view to delete existed."));
        DeleteViewRequest deleteViewRequest = DeleteViewRequest.builder()
                .viewArn(model.getViewArn())
                .build();
        try {
            proxy.injectCredentialsAndInvokeV2(deleteViewRequest, client::deleteView);
        } catch (Exception e) {
            HandlerErrorCode errorCode = Convertor.convertExceptionToErrorCode(e, logger);
            logger.log(String.format("[DELETE] Error Code: %s.", errorCode));
            return ProgressEvent.failed(model, callbackContext, errorCode, "Could not delete the view: " + e.getMessage());
        }

        logger.log(String.format("[DELETE] View is deleted."));

        //The requested ViewArn is deleted, return null with default success status
        return ProgressEvent.defaultSuccessHandler(null);
    }
}
