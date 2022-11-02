package software.amazon.resourceexplorer2.defaultviewassociation;


// CloudFormation package
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.HandlerErrorCode;

//Import Resource Explorer package
import software.amazon.awssdk.services.resourceexplorer2.ResourceExplorer2Client;
import software.amazon.awssdk.services.resourceexplorer2.model.GetDefaultViewRequest;
import software.amazon.awssdk.services.resourceexplorer2.model.GetDefaultViewResponse;


public class ReadHandler extends BaseHandler<CallbackContext> {

    private final ResourceExplorer2Client client;

    public ReadHandler() {
        client = ClientFactory.getClient();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        // to behave like other resources, they need to read using the actual primaryIdentifier, not a random value
        if (!request.getAwsAccountId().equals(model.getAssociatedAwsPrincipal())) {
            final String message = String.format("Default view not found for %s", model.getAssociatedAwsPrincipal());
            logger.log(message);
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.NotFound, message);
        }

        GetDefaultViewRequest getDefaultViewRequest = GetDefaultViewRequest.builder().build();
        GetDefaultViewResponse getDefaultViewResponse;
        try {
            getDefaultViewResponse = proxy.injectCredentialsAndInvokeV2( getDefaultViewRequest, client::getDefaultView );
        } catch (Exception e){
            HandlerErrorCode thisErrorCode = Convertor.convertExceptionToErrorCode(e, logger);
            return ProgressEvent.failed(model, callbackContext, thisErrorCode, "Could not get the default view: " + e.getMessage());
        }

        // If there is no existed default view, return NotFound error
        if (getDefaultViewResponse.viewArn() == null){
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.NotFound, "No default view was found.");
        }

        model.setViewArn(getDefaultViewResponse.viewArn());
        logger.log(String.format("[READ] DefaultView found: %s", model.getViewArn()));

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .status(OperationStatus.SUCCESS)
                .build();
    }
}
