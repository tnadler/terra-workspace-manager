package bio.terra.workspace.service.workspace;

import com.google.auto.value.AutoValue;
import java.util.Optional;

/**
 * The cloud contexts associated with a resource.
 *
 * <p>A workspace can have resources with each cloud provider, its "cloud context."
 * <li>For GCP, a Google Project is associated with every resource.
 */
@AutoValue
public abstract class WorkspaceCloudContext {
  /** The Google Project id for workspaces with Google context. */
  public abstract Optional<String> googleProjectId();

  public static WorkspaceCloudContext createGoogleContext(String projectId) {
    return new AutoValue_WorkspaceCloudContext(Optional.of(projectId));
  }

  public static WorkspaceCloudContext none() {
    return new AutoValue_WorkspaceCloudContext(Optional.empty());
  }
}
