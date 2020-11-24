package bio.terra.workspace.service.job;

import bio.terra.stairway.Flight;
import bio.terra.stairway.FlightMap;
import bio.terra.workspace.service.iam.AuthenticatedUserRequest;
import bio.terra.workspace.service.job.exception.InvalidJobParameterException;
import io.opencensus.contrib.spring.aop.Traced;
import java.util.Optional;
import java.util.UUID;

public class JobBuilder {

  private JobService jobServiceRef;
  private Class<? extends Flight> flightClass;
  private FlightMap jobParameterMap;
  private String jobId;
  // User-provided jobIds and generated jobIds have different behavior for duplicates, so we track
  // whether this jobId was generated or not here.
  private boolean jobIdGenerated;

  /**
   * Constructor for job builder object which takes only required parameters
   *
   * @param description A human readable description of the flight, used for logging and debugging.
   * @param jobId A unique identifier for this job. If not specified, a random UUID is used instead.
   * @param flightClass The class of the Stairway flight to launch
   * @param userReq User credentials
   * @param jobServiceRef Reference to a JobService object to use.
   */
  public JobBuilder(
      String description,
      Optional<String> jobId,
      Class<? extends Flight> flightClass,
      AuthenticatedUserRequest userReq,
      JobService jobServiceRef) {
    this.jobServiceRef = jobServiceRef;
    this.flightClass = flightClass;
    this.jobId = jobId.orElse(UUID.randomUUID().toString());
    this.jobIdGenerated = jobId.isEmpty();

    // initialize with required parameters
    this.jobParameterMap = new FlightMap();
    jobParameterMap.put(JobMapKeys.DESCRIPTION.getKeyName(), description);
    jobParameterMap.put(JobMapKeys.AUTH_USER_INFO.getKeyName(), userReq);
    jobParameterMap.put(JobMapKeys.SUBJECT_ID.getKeyName(), userReq.getSubjectId());
  }

  // use addParameter method for optional parameter
  // returns the JobBuilder object to allow method chaining
  public JobBuilder addParameter(String keyName, Object val) {
    if (keyName == null) {
      throw new InvalidJobParameterException("Parameter name cannot be null.");
    }

    // check that keyName doesn't match one of the required parameter names
    // i.e. disallow overwriting one of the required parameters
    if (JobMapKeys.isRequiredKey(keyName)) {
      throw new InvalidJobParameterException(
          "Required parameters can only be set by the constructor. (" + keyName + ")");
    }

    // note that this call overwrites a parameter if it already exists
    jobParameterMap.put(keyName, val);

    return this;
  }

  /**
   * Submit a job to stairway and return the jobID immediately.
   *
   * @return jobID of submitted flight
   */
  public String submit() {
    return jobServiceRef.submit(flightClass, jobParameterMap, jobId, !jobIdGenerated);
  }

  /**
   * Submit a job to stairway, wait until it's complete, and return the job result.
   *
   * @return Result of the finished job.
   */
  @Traced
  public <T> T submitAndWait(Class<T> resultClass) {
    return jobServiceRef.submitAndWait(
        flightClass, jobParameterMap, resultClass, jobId, !jobIdGenerated);
  }
}
