package software.amazon.resourceexplorer2.index;

public class IndexUtils {

    // Define index types.
    public static final String LOCAL = "LOCAL";
    public static final String AGGREGATOR = "AGGREGATOR";

    // Define index states.
    public static final String ACTIVE = "ACTIVE";
    public static final String CREATING = "CREATING";
    public static final String UPDATING = "UPDATING";
    public static final String DELETING = "DELETING";
    public static final String DELETED = "DELETED";

    // TODO: We may need to change DELAY_CONSTANT and MAX_RETRIES to match the real
    //  functioned time of Index before publishing.

    // A callback is scheduled with an initial delay of no less than the number
    // of seconds of DELAY_CONSTANT.
    public static final int DELAY_CONSTANT = 30;

    // We retry IN_PROGRESS handlers no more than MAX_RETRIES times.
    public static final int MAX_RETRIES = 3;
}
