package software.amazon.resourceexplorer2.index;

@lombok.Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Builder

public class CallbackContext {
    private boolean createInProgress;
    private boolean updateInProgress;
    private int retryCount;
}
