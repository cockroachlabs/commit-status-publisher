package jetbrains.buildServer.commitPublisher.github;

import com.intellij.openapi.diagnostic.Logger;

import java.util.HashMap;
import java.util.Map;

import jetbrains.buildServer.commitPublisher.*;
import jetbrains.buildServer.serverSide.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class GitHubPublisher extends BaseCommitStatusPublisher {

  private static final Logger LOG = Logger.getInstance(GitHubPublisher.class.getName());

  private final ChangeStatusUpdater myUpdater;

  GitHubPublisher(@NotNull CommitStatusPublisherSettings settings,
                  @NotNull SBuildType buildType, @NotNull String buildFeatureId,
                  @NotNull ChangeStatusUpdater updater,
                  @NotNull Map<String, String> params,
                  @NotNull CommitStatusPublisherProblems problems) {
    super(settings, buildType, buildFeatureId, params, problems);
    myUpdater = updater;
  }

  @NotNull
  public String toString() {
    return "github";
  }

  @NotNull
  @Override
  public String getId() {
    return Constants.GITHUB_PUBLISHER_ID;
  }

  @Override
  public boolean buildStarted(@NotNull SBuild build, @NotNull BuildRevision revision) throws PublisherException {
    // Fired when a build is kicked off.
    LOG.debug("buildStarted: " + build.getBuildId());
    updateBuildStatus(build, revision, true);
    return true;
  }

  @Override
  public boolean buildFinished(@NotNull SBuild build, @NotNull BuildRevision revision) throws PublisherException {
    // Fired when a build completes either with success or failure.
    LOG.debug("buildFinished: " + build.getBuildId() + " " + build.getBuildStatus().getText());
    updateBuildStatus(build, revision, false);
    return true;
  }

  @Override
  public boolean buildInterrupted(@NotNull SBuild build, @NotNull BuildRevision revision) throws PublisherException {
    // This is called for an explicit cancellation of a build.
    LOG.debug("buildInterrupted: " + build.getBuildId() + " " + build.getBuildStatus().getText() + " " + build.getBuildStatus().getPriority());
    updateBuildStatus(build, revision, false);
    return true;
  }

  @Override
  public boolean buildFailureDetected(@NotNull SBuild build, @NotNull BuildRevision revision) throws PublisherException {
    // This implements an early-out for composite builds in which one or more components fail. In our case,
    // this gets in the way when a sub-project is running on a machine that gets preempted. We don't seem to have access
    // to the underlying INTERRUPTED_WITH_RETRY status to do a better job of filtering.
    LOG.debug("buildFailureDetected: " + build.getBuildId() + " " + build.getBuildStatus().getText() + " " + build.getBuildStatus().getPriority());
    // updateBuildStatus(build, revision, false);
    return true;
  }

  @Override
  public boolean buildMarkedAsSuccessful(@NotNull final SBuild build, @NotNull final BuildRevision revision, final boolean buildInProgress) throws PublisherException {
    // This gets called if a TC user explicitly overrides a failed build and also when a component build is retried.
    LOG.debug("buildMarkedAsSuccessful: " + build.getBuildId() + " " + build.getBuildStatus().getText() + " " + build.getBuildStatus().getPriority() + " " + buildInProgress);
    updateBuildStatus(build, revision, buildInProgress);
    return true;
  }

  public String getServerUrl() {
    return myParams.get(Constants.GITHUB_SERVER);
  }

  private void updateBuildStatus(@NotNull SBuild build, @NotNull BuildRevision revision, boolean isStarting) throws PublisherException {
    final ChangeStatusUpdater.Handler h = myUpdater.getUpdateHandler(revision.getRoot(), getParams(build), this);

    if (isStarting && !h.shouldReportOnStart()) return;
    if (!isStarting && !h.shouldReportOnFinish()) return;

    if (!revision.getRoot().getVcsName().equals("jetbrains.git")) {
      LOG.warn("No revisions were found to update GitHub status. Please check you have Git VCS roots in the build configuration");
      return;
    }

    if (isStarting) {
      h.scheduleChangeStarted(revision, build);
    } else {
      h.scheduleChangeCompleted(revision, build);
    }
  }


  @NotNull
  private Map<String, String> getParams(@NotNull SBuild build) {
    String context = getCustomContextFromParameter(build);
    if (context == null)
      context = getDefaultContext(build);
    Map<String, String> result = new HashMap<String, String>(myParams);
    result.put(Constants.GITHUB_CONTEXT, context);
    return result;
  }


  @NotNull
  private String getDefaultContext(@NotNull SBuild build) {
    SBuildType buildType = build.getBuildType();
    if (buildType != null) {
      return String.format("%s (%s)", buildType.getName(), buildType.getProject().getName());
    } else {
      return "<Removed build configuration>";
    }
  }


  @Nullable
  private String getCustomContextFromParameter(@NotNull SBuild build) {
    String value = build.getParametersProvider().get(Constants.GITHUB_CUSTOM_CONTEXT_BUILD_PARAM);
    if (value == null) {
      return null;
    } else {
      return build.getValueResolver().resolve(value).getResult();
    }
  }
}
