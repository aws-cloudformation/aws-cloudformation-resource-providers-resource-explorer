package software.amazon.resourceexplorer2.view;

// CloudFormation package
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;

// Resource Explorer package
import software.amazon.awssdk.services.resourceexplorer2.ResourceExplorer2Client;
import software.amazon.awssdk.services.resourceexplorer2.model.GetViewRequest;
import software.amazon.awssdk.services.resourceexplorer2.model.GetViewResponse;

import java.util.ArrayList;
import java.util.List;


public class ReadHandler extends REBaseHandler<CallbackContext> {

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

        logRequestInfo(request, logger);

        final ResourceModel model = request.getDesiredResourceState();

        GetViewRequest getViewRequest = GetViewRequest.builder()
                .viewArn(model.getViewArn())
                .build();
        GetViewResponse getViewResponse;
        try {
            getViewResponse = proxy.injectCredentialsAndInvokeV2(getViewRequest, client::getView);

        } catch (RuntimeException e){
            HandlerErrorCode thisErrorCode = Convertor.convertExceptionToErrorCode(e, logger);
            return ProgressEvent.failed(model, callbackContext, thisErrorCode, "Could not get the view: " + e.getMessage());
        }

        ResourceModel resultModel = translateToResourceModel(getViewResponse);
        logger.log("[READ] View existed.");
        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(resultModel)
                .status(OperationStatus.SUCCESS)
                .build();
    }

    private ResourceModel translateToResourceModel (GetViewResponse getViewResponse){
        List<IncludedProperty> modelIncludedProperties = new ArrayList<>();

        if (getViewResponse.view().includedProperties() != null) {
            for (software.amazon.awssdk.services.resourceexplorer2.model.IncludedProperty
                    getViewIncludedProperty: getViewResponse.view().includedProperties()) {
                IncludedProperty modelIncludedProperty = IncludedProperty.builder()
                        .name(getViewIncludedProperty.name())
                        .build();
                modelIncludedProperties.add(modelIncludedProperty);
            }
        }

        SearchFilter thisSearchFilter = SearchFilter.builder().filterString("").build();
        if (getViewResponse.view().filters().filterString() != null){
            thisSearchFilter.setFilterString(getViewResponse.view().filters().filterString());
        }

        final String[] viewArnSplit = getViewResponse.view().viewArn().split("/", -2);
        final String viewName = viewArnSplit[1];

        ResourceModel resultModel = ResourceModel.builder()
                .viewName(viewName)
                .viewArn(getViewResponse.view().viewArn())
                .includedProperties(modelIncludedProperties)
                .filters(thisSearchFilter)
                .scope(getViewResponse.view().scope())
                .tags(getViewResponse.tags())
                .build();

        return resultModel;
    }
}
