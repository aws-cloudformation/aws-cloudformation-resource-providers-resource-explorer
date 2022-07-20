# AWS::ResourceExplorer2::View

Definition of AWS::ResourceExplorer2::View Resource Type

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::ResourceExplorer2::View",
    "Properties" : {
        "<a href="#includedproperties" title="IncludedProperties">IncludedProperties</a>" : <i>[ <a href="includedproperty.md">IncludedProperty</a>, ... ]</i>,
        "<a href="#filters" title="Filters">Filters</a>" : <i><a href="filters.md">Filters</a></i>,
        "<a href="#tags" title="Tags">Tags</a>" : <i><a href="tags.md">Tags</a></i>,
        "<a href="#viewname" title="ViewName">ViewName</a>" : <i>String</i>
    }
}
</pre>

### YAML

<pre>
Type: AWS::ResourceExplorer2::View
Properties:
    <a href="#includedproperties" title="IncludedProperties">IncludedProperties</a>: <i>
      - <a href="includedproperty.md">IncludedProperty</a></i>
    <a href="#filters" title="Filters">Filters</a>: <i><a href="filters.md">Filters</a></i>
    <a href="#tags" title="Tags">Tags</a>: <i><a href="tags.md">Tags</a></i>
    <a href="#viewname" title="ViewName">ViewName</a>: <i>String</i>
</pre>

## Properties

#### IncludedProperties

_Required_: No

_Type_: List of <a href="includedproperty.md">IncludedProperty</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Filters

_Required_: No

_Type_: <a href="filters.md">Filters</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Tags

_Required_: No

_Type_: <a href="tags.md">Tags</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### ViewName

_Required_: Yes

_Type_: String

_Pattern_: <code>^[a-zA-Z0-9\-]{1,64}$</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the ViewArn.

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### ViewArn

Returns the <code>ViewArn</code> value.

