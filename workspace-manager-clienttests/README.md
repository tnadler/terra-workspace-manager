# workspace-manager-clienttests
This Gradle project contains Test Runner tests written with the Workspace Manager Service client library.

The Test Runner library [GitHub repository](https://github.com/DataBiosphere/terra-test-runner) has documentation for
how to write and execute tests.

#### Run a test
From the workspace-manager-clienttests directory:
```
./gradlew  runTest --args="configs/BasicAuthenticated.json /tmp/TR"
```

The default server that this test will run against is specified in the resources/configs/BasicAuthenticated.json file.
To override the default server, set an environment variable
```
TEST_RUNNER_SERVER_SPECIFICATION_FILE="workspace-dev.json" ./gradlew  runTest --args="configs/BasicAuthenticated.json /tmp/TR"
```

#### SA keys from Vault
Run the ./render-config.sh script (inside tools directory) before running tests. 
Currently this script targets the dev environment for testing.

From the workspace-manager-clienttests/tools directory:
```
./render-config.sh
```

Each service account JSON files in the resources/serviceaccounts directory of this project specifies a default file
path for the client secret file. This default path should match where the render-config.sh script puts the secret.