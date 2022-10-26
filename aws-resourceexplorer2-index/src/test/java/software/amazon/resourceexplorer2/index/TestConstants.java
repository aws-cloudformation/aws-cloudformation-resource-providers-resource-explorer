package software.amazon.resourceexplorer2.index;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TestConstants {

    protected static final String INDEX_ARN_1 =
            "arn:aws:resource-explorer-2:us-east-2:123456789012:index/e57e3910-33b3-440a-9d51-6cd7e186fd9e";

    protected static final String INDEX_ARN_2 =
            "arn:aws:resource-explorer-2:us-east-2:123456789012:index/ta4e3910-33b3-440a-9d51-6cd7e186ftd7o";

    protected static final Map <String, String> RESOURCE_TAGS = new HashMap<String, String>(){{
        put("Purpose", "TestResourceTag1");
        put("Type", "ResourceTag1");
    }};

    protected static final Map <String, String> RESOURCE_TAGS_MAP = new HashMap<String, String>(){{
        put("Purpose", "TestResourceTag1");
        put("Type", "ResourceTag1");
    }};

    protected static final Map <String, String> STACK_LEVEL_TAGS = new HashMap<String, String>(){{
        put("StackLevelTag", "After");
    }};

    protected static final Map <String, String> PRE_STACK_LEVEL_TAGS = new HashMap<String, String>(){{
        put("StackLevelTag", "Before");
    }};

    protected static final Map<String, String> SYSTEM_TAGS = new HashMap<String, String>(){{
        put("aws:cloudformation:logical-id", "UnitTest");
        put("aws:cloudformation:stack-id", "STACKID");
        put("aws:cloudformation:stack-name", "UnitTesStack");
    }};

    protected static final Map<String, String> EMPTY_TAGS = new HashMap<String, String>();
}
