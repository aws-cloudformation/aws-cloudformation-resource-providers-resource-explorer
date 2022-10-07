package software.amazon.resourceexplorer2.index;

//CloudFormation package
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.HandlerErrorCode;

//Resource Explorer package
import software.amazon.awssdk.services.resourceexplorer.model.AccessDeniedException;
import software.amazon.awssdk.services.resourceexplorer.model.ConflictException;
import software.amazon.awssdk.services.resourceexplorer.model.InternalServerException;
import software.amazon.awssdk.services.resourceexplorer.model.ResourceNotFoundException;
// TODO: add in this exception
// import software.amazon.awssdk.services.resourceexplorer.model.ServiceQuotaExceededException;
import software.amazon.awssdk.services.resourceexplorer.model.ThrottlingException;
import software.amazon.awssdk.services.resourceexplorer.model.ValidationException;

public class Convertor {

    static HandlerErrorCode convertExceptionToErrorCode(Exception e, Logger logger){
        logger.log(String.format("Exception,\"%s\" , is converted to error code.", e.getMessage()));

        if (e instanceof ConflictException){
            return HandlerErrorCode.AlreadyExists;
        }
        else if (e instanceof ValidationException) {
            return HandlerErrorCode.InvalidRequest;
        }
        else if (e instanceof AccessDeniedException) {
            return HandlerErrorCode.AccessDenied;
        }
        else if (e instanceof ResourceNotFoundException) {
            return HandlerErrorCode.NotFound;
        }
        else if (e instanceof InternalServerException) {
            return HandlerErrorCode.InternalFailure;
        }
        else if (e instanceof ThrottlingException) {
            return HandlerErrorCode.Throttling;
        }
        // else if (e instanceof ServiceQuotaExceededException) {
        //     return HandlerErrorCode.ServiceLimitExceeded;
        // }
        else{
            logger.log(String.format("Unexpected exception \"%s\"", e.getMessage()));
            return HandlerErrorCode.InternalFailure;
        }
    }

}