package software.amazon.resourceexplorer2.view;

import software.amazon.awssdk.services.resourceexplorer.model.IncludedProperty;
import software.amazon.awssdk.services.resourceexplorer.model.SearchFilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TestConstants {

    protected static final String EXAMPLE_ARN =
            "arn:aws:resource-explorer-2:us-west-2:view/exampleView/2b1ae2fd-5c32-428f-92e3-ac8a2fd50f52";

    protected static final String VIEW_NAME = "exampleView";
    protected static final Map <String, String> RESOURCE_TAGS = new HashMap<String, String>(){{
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

    public static IncludedProperty CLIENT_INCLUDED_PROPERTY = software.amazon.awssdk.services.resourceexplorer.model.IncludedProperty.builder()
            .name("tags")
            .build();
    public static List<IncludedProperty> CLIENT_INCLUDED_PROPERTY_LIST =
            new ArrayList<IncludedProperty>(){{
                add(CLIENT_INCLUDED_PROPERTY);
            }};

    public static SearchFilter CLIENT_SEARCH_FILTER = SearchFilter.builder()
            .filterString("Service:s3 Region:us-west-2")
            .build();

    public static software.amazon.resourceexplorer2.view.IncludedProperty MODEL_INCLUDED_PROPERTY =
            software.amazon.resourceexplorer2.view.IncludedProperty.builder()
                    .name("tags")
                    .build();

    public static List<software.amazon.resourceexplorer2.view.IncludedProperty> MODEL_INCLUDED_PROPERTY_LIST =
            new ArrayList<software.amazon.resourceexplorer2.view.IncludedProperty>(){{
                add(MODEL_INCLUDED_PROPERTY);
            }};

    public static software.amazon.resourceexplorer2.view.Filters MODEL_FILTERS =
            software.amazon.resourceexplorer2.view.Filters.builder()
                    .filterString("Service:s3 Region:us-west-2")
                    .build();
}
