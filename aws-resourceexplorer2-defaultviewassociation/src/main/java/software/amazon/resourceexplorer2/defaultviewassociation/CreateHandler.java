package software.amazon.resourceexplorer2.defaultviewassociation;

// CloudFormation package
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.HandlerErrorCode;

// Resource Explorer package
import software.amazon.awssdk.services.resourceexplorer2.ResourceExplorer2Client;
import software.amazon.awssdk.services.resourceexplorer2.model.AssociateDefaultViewRequest;
import software.amazon.awssdk.services.resourceexplorer2.model.AssociateDefaultViewResponse;
import software.amazon.awssdk.services.resourceexplorer2.model.GetDefaultViewRequest;
import software.amazon.awssdk.services.resourceexplorer2.model.GetDefaultViewResponse;


public class CreateHandler extends REBaseHandler<CallbackContext> {

    private final ResourceExplorer2Client client;

    public CreateHandler() {
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
        logger.log(String.format("[CREATE] callbackContext: %s", callbackContext));

        return ProgressEvent.progress(model, callbackContext)
            .then(
                progress -> (callbackContext != null && callbackContext.isPreExistenceCheck())
                        ? progress
                        : preExistenceCheck(request, proxy, model, logger))
            .then(progress -> createResource(proxy, model, request, callbackContext, logger));
        }

        private ProgressEvent<ResourceModel, CallbackContext> createResource(
            final AmazonWebServicesClientProxy proxy,
            final ResourceModel model,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger
        ) {
        logger.log(String.format("[CREATE] Inside method createResource, callbackContext: %s", callbackContext));
        AssociateDefaultViewRequest associateDefaultViewRequest = AssociateDefaultViewRequest.builder()
                .viewArn(model.getViewArn())
                .build();
        AssociateDefaultViewResponse associateDefaultViewResponse;
        try {
            associateDefaultViewResponse = proxy.injectCredentialsAndInvokeV2( associateDefaultViewRequest, client::associateDefaultView );
            // only set the AssociatedAwsPrincipal if the request was successful.
            model.setAssociatedAwsPrincipal(request.getAwsAccountId());
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
        
        private ProgressEvent<ResourceModel, CallbackContext> preExistenceCheck(
            final ResourceHandlerRequest<ResourceModel> request,
            final AmazonWebServicesClientProxy proxy,
            final ResourceModel model,
            final Logger logger
        ) {
            
            CallbackContext newCallbackContext = CallbackContext.builder()
                .preExistenceCheck(true)
                .build();
            logger.log(String.format("[CREATE][preExistenceCheck] executing preExistenceCheck, callbackContext: %s", newCallbackContext));
            GetDefaultViewRequest getDefaultViewRequest = GetDefaultViewRequest.builder().build();
            GetDefaultViewResponse getDefaultViewResponse;
            try {
                getDefaultViewResponse = proxy.injectCredentialsAndInvokeV2( getDefaultViewRequest, client::getDefaultView );
                model.setAssociatedAwsPrincipal(request.getAwsAccountId());
            } catch (Exception e){
                logger.log(String.format("[CREATE][preExistenceCheck] Error occurred in GetDefaultView."));
                HandlerErrorCode thisErrorCode = Convertor.convertExceptionToErrorCode(e, logger);
                return ProgressEvent.failed(model, newCallbackContext, thisErrorCode, "Could not check default view: " + e.getMessage());
            }

            if (getDefaultViewResponse.viewArn() != null) {
                logger.log(String.format("[CREATE][preExistenceCheck] A default view is already associated."));
                return ProgressEvent.failed(model, newCallbackContext, HandlerErrorCode.AlreadyExists, "A default view is already associated.");
            }
            
            return ProgressEvent.defaultInProgressHandler(newCallbackContext, 1, model);
        }
    }
