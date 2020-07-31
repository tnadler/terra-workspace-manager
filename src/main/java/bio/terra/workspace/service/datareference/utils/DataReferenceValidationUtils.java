package bio.terra.workspace.service.datareference.utils;

import static bio.terra.workspace.generated.model.ReferenceTypeEnum.DATA_REPO_SNAPSHOT;

import bio.terra.workspace.generated.model.DataRepoSnapshot;
import bio.terra.workspace.generated.model.ReferenceTypeEnum;
import bio.terra.workspace.generated.model.UncontrolledReferenceDescription;
import bio.terra.workspace.service.datareference.exception.InvalidDataReferenceException;
import bio.terra.workspace.service.datarepo.DataRepoService;
import bio.terra.workspace.service.iam.AuthenticatedUserRequest;
import java.util.ArrayList;
import java.util.List;
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
    List<ReferenceTypeEnum> presentFields = new ArrayList<>();
    if (reference.getDataRepoSnapshot() != null) {
      presentFields.add(DATA_REPO_SNAPSHOT);
    }

    // Check that only one type of reference is provided
    if (presentFields.size() != 1) {
      throw new InvalidDataReferenceException(
          "Invalid reference shape specified. Your request contained all of: "
              + presentFields.toString()
              + ". Specify exactly one of: "
              + DATA_REPO_SNAPSHOT.toString());
    }

    // Check that the provided reference is valid
    switch (presentFields.get(0)) {
      case DATA_REPO_SNAPSHOT:
        validateDataRepoReference(reference.getDataRepoSnapshot(), userReq);
        return DATA_REPO_SNAPSHOT;
      default:
        throw new IllegalStateException("Unexpected value: " + presentFields.get(0));
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
