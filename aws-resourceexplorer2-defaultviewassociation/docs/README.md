# AWS::ResourceExplorer2::DefaultViewAssociation

Definition of AWS::ResourceExplorer2::DefaultViewAssociation Resource Type

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::ResourceExplorer2::DefaultViewAssociation",
    "Properties" : {
        "<a href="#viewarn" title="ViewArn">ViewArn</a>" : <i>String</i>
    }
}
</pre>

### YAML

<pre>
Type: AWS::ResourceExplorer2::DefaultViewAssociation
Properties:
    <a href="#viewarn" title="ViewArn">ViewArn</a>: <i>String</i>
</pre>

## Properties

#### ViewArn

_Required_: Yes

_Type_: String

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the ViewArn.
