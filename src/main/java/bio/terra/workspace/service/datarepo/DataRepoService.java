package bio.terra.workspace.service.datarepo;

import bio.terra.datarepo.api.RepositoryApi;
import bio.terra.datarepo.api.UnauthenticatedApi;
import bio.terra.datarepo.client.ApiClient;
import bio.terra.datarepo.client.ApiException;
import bio.terra.workspace.app.configuration.external.DataRepoConfiguration;
import bio.terra.workspace.app.configuration.spring.TraceInterceptorConfig;
import bio.terra.workspace.common.exception.ValidationException;
import bio.terra.workspace.generated.model.SystemStatusSystems;
import bio.terra.workspace.service.datareference.exception.DataRepoAuthorizationException;
import bio.terra.workspace.service.datareference.exception.DataRepoInternalServerErrorException;
import bio.terra.workspace.service.iam.AuthenticatedUserRequest;
import io.opencensus.contrib.spring.aop.Traced;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class DataRepoService {

  private final DataRepoConfiguration dataRepoConfiguration;

  @Autowired
  public DataRepoService(DataRepoConfiguration dataRepoConfiguration) {
    this.dataRepoConfiguration = dataRepoConfiguration;
  }

  private Logger logger = LoggerFactory.getLogger(DataRepoService.class);

  private ApiClient getApiClient(String accessToken) {
    ApiClient client = new ApiClient();
    client.addDefaultHeader(
        TraceInterceptorConfig.MDC_REQUEST_ID_HEADER,
        MDC.get(TraceInterceptorConfig.MDC_REQUEST_ID_KEY));
    client.setAccessToken(accessToken);
    return client;
  }

  private RepositoryApi repositoryApi(String instanceName, AuthenticatedUserRequest userReq) {
    String instanceUrl = getInstanceUrl(instanceName);
    return new RepositoryApi(getApiClient(userReq.getRequiredToken()).setBasePath(instanceUrl));
  }

  public String getInstanceUrl(String instanceName) {
    HashMap<String, String> dataRepoInstances = dataRepoConfiguration.getInstances();
    String cleanedInstanceName = instanceName.toLowerCase().trim();

    if (dataRepoInstances.containsKey(cleanedInstanceName)) {
      return dataRepoInstances.get(cleanedInstanceName);
    } else {
      throw new ValidationException(
          "Provided Data repository instance is not allowed. Valid instances are: \""
              + String.join("\", \"", dataRepoInstances.keySet())
              + "\"");
    }
  }

  @Traced
  public boolean snapshotExists(
      String instanceName, String snapshotId, AuthenticatedUserRequest userReq) {
    RepositoryApi repositoryApi = repositoryApi(instanceName, userReq);

    try {
      repositoryApi.retrieveSnapshot(snapshotId);
      logger.info(
          String.format(
              "Retrieved snapshot %s on Data Repo instance %s", snapshotId, instanceName));
      return true;
    } catch (ApiException e) {
      if (e.getCode() == HttpStatus.NOT_FOUND.value()) {
        return false;
      } else if (e.getCode() == HttpStatus.UNAUTHORIZED.value()) {
        throw new DataRepoAuthorizationException(
            "Not authorized to access Data Repo", e.getCause());
      } else {
        throw new DataRepoInternalServerErrorException(
            "Data Repo returned the following error: " + e.getMessage(), e.getCause());
      }
    }
  }

  public SystemStatusSystems status(String instanceUrl) {
    UnauthenticatedApi unauthenticatedApi =
        new UnauthenticatedApi(new ApiClient().setBasePath(instanceUrl));
    try {
      // TDR serviceStatus method returns cleanly on a 200 response and throws an error otherwise,
      // no other information is available through this endpoint.
      unauthenticatedApi.serviceStatus();
      return new SystemStatusSystems().ok(true);
    } catch (ApiException nonOkStatusException) {
      return new SystemStatusSystems()
          .ok(false)
          .addMessagesItem(nonOkStatusException.getResponseBody());
    }
  }
}
