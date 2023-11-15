package software.amazon.resourceexplorer2.defaultviewassociation;

import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public abstract class REBaseHandler<T> extends BaseHandler<T> {
    protected void logRequestInfo(
            final ResourceHandlerRequest<ResourceModel> request,
            final Logger logger) {

        logger.log(String.format("Starting request for %s with token %s, stack %s", request.getAwsAccountId(), request.getClientRequestToken(), request.getStackId()));
    }

}
