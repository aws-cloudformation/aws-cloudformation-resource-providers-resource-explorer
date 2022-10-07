# AWS::ResourceExplorer2::DefaultViewAssociation

Definition of AWS::ResourceExplorer2::DefaultViewAssociation Resource Type

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::ResourceExplorer2::DefaultViewAssociation",
    "Properties" : {
        "<a href="#viewarn" title="ViewArn">ViewArn</a>" : <i>String</i>,
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

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the AccountId.

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### AccountId

The accountId of the caller account, which is used as the unique identifier for this resource.

