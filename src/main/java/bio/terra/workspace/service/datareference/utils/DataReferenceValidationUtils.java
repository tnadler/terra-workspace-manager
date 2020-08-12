package bio.terra.workspace.service.datareference.utils;

import static bio.terra.workspace.generated.model.ReferenceTypeEnum.DATA_REPO_SNAPSHOT;

import bio.terra.workspace.generated.model.DataRepoSnapshot;
import bio.terra.workspace.generated.model.ReferenceTypeEnum;
import bio.terra.workspace.generated.model.UncontrolledReferenceDescription;
import bio.terra.workspace.service.datareference.exception.InvalidDataReferenceException;
import bio.terra.workspace.service.datarepo.DataRepoService;
import bio.terra.workspace.service.iam.AuthenticatedUserRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class DataReferenceValidationUtils {

  private DataRepoService dataRepoService;

  public DataReferenceValidationUtils(DataRepoService dataRepoService) {
    this.dataRepoService = dataRepoService;
  }

  public ReferenceTypeEnum validateReference(
      UncontrolledReferenceDescription reference, AuthenticatedUserRequest userReq) {

    switch (reference.getType()) {
      case DATA_REPO_SNAPSHOT:
        validateDataRepoReference((DataRepoSnapshot) reference, userReq);
        return DATA_REPO_SNAPSHOT;
      default:
        throw new IllegalStateException("Unexpected value: " + reference.getType());
    }
  }

  private void validateDataRepoReference(
      DataRepoSnapshot reference, AuthenticatedUserRequest userReq) {
    if (StringUtils.isEmpty(reference.getInstanceName())
        || StringUtils.isEmpty(reference.getSnapshot())) {
      throw new InvalidDataReferenceException(
          "Both instanceName and snapshot are required fields.");
    }

    if (!dataRepoService.snapshotExists(
        reference.getInstanceName(), reference.getSnapshot(), userReq)) {
      throw new InvalidDataReferenceException(
          "The given snapshot could not be found in the Data Repo instance provided."
              + " Verify that your reference was correctly defined and the instance is correct");
    }
  }
}
