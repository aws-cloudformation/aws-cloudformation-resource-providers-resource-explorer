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


public class CreateHandler extends BaseHandler<CallbackContext> {

    private final ResourceExplorerClient client;

    public CreateHandler() {
        client = ClientFactory.getClient();
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
            return ProgressEvent.failed(model, callbackContext, thisErrorCode, "Could not check default view: " + e.getMessage());
        }

        logger.log(String.format("[CREATE] Default view arn: " + getDefaultViewResponse.viewArn()));

        // If a default view exists, and it is the desired default view, return AlreadyExist Error.
        if (getDefaultViewResponse.viewArn() != null){
            logger.log(String.format("[CREATE] A default view is already associated."));
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.AlreadyExists, "A default view is already associated.");
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
            logger.log(String.format("[CREATE] Creating DefaultView failed: %s", thisErrorCode));
            return ProgressEvent.failed(model, callbackContext, thisErrorCode, "Could not associate a default view: " + e.getMessage());
        }

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .status(OperationStatus.SUCCESS)
                .build();
    }
}
