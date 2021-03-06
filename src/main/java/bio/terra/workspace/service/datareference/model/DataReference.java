package bio.terra.workspace.service.datareference.model;

import bio.terra.workspace.generated.model.DataReferenceDescription;
import com.google.auto.value.AutoValue;
import java.util.UUID;

/**
 * Internal representation of an uncontrolled data reference.
 *
 * <p>"Uncontrolled" here means that WM does not own the lifecycle of the underlying data.
 */
@AutoValue
public abstract class DataReference {

  /** ID of the workspace this reference belongs to. */
  public abstract UUID workspaceId();

  /** ID of the reference itself. */
  public abstract UUID referenceId();

  /** Name of the reference. Names are unique per workspace, per reference type. */
  public abstract String name();

  /** Type of this data reference. */
  public abstract DataReferenceType referenceType();

  /** Instructions for how to clone this reference (if at all). */
  public abstract CloningInstructions cloningInstructions();

  /** The actual object being referenced. */
  public abstract ReferenceObject referenceObject();

  /** Convenience method for translating to an API model DataReferenceDescription object. */
  public DataReferenceDescription toApiModel() {
    return new DataReferenceDescription()
        .referenceId(referenceId())
        .name(name())
        .workspaceId(workspaceId())
        .referenceType(referenceType().toApiModel())
        .reference(((SnapshotReference) referenceObject()).toApiModel())
        .cloningInstructions(cloningInstructions().toApiModel());
  }

  public static Builder builder() {
    return new AutoValue_DataReference.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract DataReference.Builder workspaceId(UUID value);

    public abstract DataReference.Builder referenceId(UUID value);

    public abstract DataReference.Builder name(String value);

    public abstract DataReference.Builder referenceType(DataReferenceType value);

    public abstract DataReference.Builder cloningInstructions(CloningInstructions value);

    public abstract DataReference.Builder referenceObject(ReferenceObject value);

    public abstract DataReference build();
  }
}
