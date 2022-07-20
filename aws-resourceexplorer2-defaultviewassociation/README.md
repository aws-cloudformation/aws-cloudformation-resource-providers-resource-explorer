# AWS::ResourceExplorer2::DefaultViewAssociation

Welcome to Default View Association resource provider and Congratulations on starting development! Next steps:

1. Write/Modify the JSON schema describing your resource, `aws-resourceexplorer2-defaultviewassociation.json`
2. The RPDK will automatically generate the correct resource model from the schema whenever the project
   is built via Maven. You can also do this manually with the following command: `cfn generate`.
3. Implement/Modify your resource handlers.
4. You can test individual handlers locally by using **SAM CLI Local Test**. The test inputs are located in
   `sam-tests` directory. Remember to include your AWS credentials in the Json inputs.
    You can invoke SAM Test with the following command:

    `sam local invoke TestEntrypoint --event sam-tests/create.json`
5. You can also run the contract tests locally with **SAM CLI Local Test**. The test inputs are located in
   `inputs` directory. The contracts require two terminals opening at a time with the following commands:
   ```
   sam local start-lambda
   cfn test --enforce-timeout 480
   ```

> Please don't modify files under `target/generated-sources/rpdk`, as they will be automatically overwritten.

The code uses [Lombok](https://projectlombok.org/), and [you may have to install IDE integrations](https://projectlombok.org/setup/overview) to enable auto-complete for Lombok-annotated classes.
