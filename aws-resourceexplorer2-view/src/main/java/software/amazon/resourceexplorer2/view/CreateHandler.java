package software.amazon.resourceexplorer2.view;

import com.amazonaws.util.StringUtils;

// CloudFormation package
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;

// Resource Explorer package
import software.amazon.awssdk.services.resourceexplorer.ResourceExplorerClient;
import software.amazon.awssdk.services.resourceexplorer.model.CreateViewRequest;
import software.amazon.awssdk.services.resourceexplorer.model.CreateViewResponse;

import java.util.ArrayList;
import java.util.List;


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

        // A client cannot create ViewArn, so if there is input in the ViewArn field,
        // the request is invalid. We expect ViewArn of model to be null.
        String modelViewArn = model.getViewArn();
        if (!StringUtils.isNullOrEmpty(modelViewArn)){
            logger.log("[CREATE] ViewArn cannot be set by the caller.");
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest,
                    "ViewArn cannot be set by the caller." );
        }

        CreateViewRequest createViewRequest = translateToCreateViewRequest(model, logger, request);
        CreateViewResponse createViewResponse;
        try {
            createViewResponse = proxy.injectCredentialsAndInvokeV2(createViewRequest, client::createView);
        } catch (Exception e) {
            logger.log("[CREATE] Error at CreateView.");
            HandlerErrorCode thisErrorCode = Convertor.convertExceptionToErrorCode(e, logger);
            return ProgressEvent.failed(model, callbackContext, thisErrorCode,
                "Could not create the view: " + e.getMessage());
        }

        model.setViewArn(createViewResponse.view().viewArn());
        model.setViewName(model.getViewName());

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .status(OperationStatus.SUCCESS)
                .build();

    }

    private CreateViewRequest translateToCreateViewRequest
            (ResourceModel model, Logger logger, final ResourceHandlerRequest<ResourceModel> request){

        List <software.amazon.awssdk.services.resourceexplorer.model.IncludedProperty>
                thisIncludedProperties = new ArrayList<>();

        if (model.getIncludedProperties() != null) {
            for (IncludedProperty modelIncludedProperty: model.getIncludedProperties()) {
                software.amazon.awssdk.services.resourceexplorer.model.IncludedProperty thisIncludedProperty = software.amazon.awssdk.services.resourceexplorer.model.IncludedProperty.builder()
                        .name(modelIncludedProperty.getName())
                        .build();
                thisIncludedProperties.add(thisIncludedProperty);
            }
        }

        if (model.getFilters() == null){
            return CreateViewRequest.builder()
                    .tags(TagTools.combineAllTypesOfTags(model, request, logger))
                    .viewName(model.getViewName())
                    .includedProperties(thisIncludedProperties)
                    .clientToken(request.getClientRequestToken())
                    .build();
        }
        software.amazon.awssdk.services.resourceexplorer.model.SearchFilter thisSearchFilter = software.amazon.awssdk.services.resourceexplorer.model.SearchFilter.builder()
                .filterString(model.getFilters().getFilterString())
                .build();

        return CreateViewRequest.builder()
                .tags(TagTools.combineAllTypesOfTags(model, request, logger))
                .viewName(model.getViewName())
                .includedProperties(thisIncludedProperties)
                .filters(thisSearchFilter)
                .clientToken(request.getClientRequestToken())
                .build();
    }
}
