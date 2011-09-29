package hudson.plugins.android_emulator;

import hudson.Extension;
import hudson.matrix.MatrixConfiguration;
import hudson.model.BuildableItemWithBuildWrappers;
import hudson.model.Executor;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.Queue.BuildableItem;
import hudson.model.Queue.Executable;
import hudson.model.Queue.Task;
import hudson.model.queue.CauseOfBlockage;
import hudson.model.queue.QueueTaskDispatcher;
import hudson.model.queue.SubTask;

/**
 * This QueueTaskDispatcher prevents any one Android emulator instance from being executed more than
 * once concurrently on any one build machine.
 * <p>
 * From the given {@link hudson.model.Queue.Task Task}, we form a hash of the emulator configuration
 * and check whether any other build currently running on the given {@link hudeon.model.Node Node}
 * is already using this configuration. If so, we veto execution of the given {@code Task}.
 * </p>
 * As Android emulator attributes will quite often be parameterised (especially for matrix builds),
 * we attempt to expand as many variables as possible, i.e. from the environment of the {@code Node}
 * and the axis combination for matrix builds. Because we are evaluating these parameters before the
 * build has actually started, it's possible that the variable expansions made aren't 100% accurate,
 * for example if there are earlier {@code BuildWrapper} instances contributing to the environment.
 */
@Extension
public class TaskDispatcher extends QueueTaskDispatcher {

    @Override
    public CauseOfBlockage canTake(Node node, Task task) {
        // If the given task doesn't use the AndroidEmulator BuildWrapper, we don't care
        String desiredHash = getEmulatorConfigHashForTask(node, task);
        if (desiredHash == null) {
            return null;
        }

        // Check for builds in the queue which have the same emulator config as this task
        Queue queue = Hudson.getInstance().getQueue();
        for (BuildableItem item : queue.getBuildableItems()) {
            Task queuedTask = item.task;
            if (task == queuedTask) {
                continue;
            }

            // If build with matching config is about to start (is "pending"), hold off for a moment
            if (queue.isPending(queuedTask)) {
                String queuedTaskHash = getEmulatorConfigHashForTask(node, queuedTask);
                if (desiredHash.equals(queuedTaskHash)) {
                    return CauseOfBlockage.fromMessage(Messages._WAITING_FOR_EMULATOR());
                }
            }
        }

        // Check whether a build with this emulator config is already running on this machine
        for (Executor e : node.toComputer().getExecutors()) {
            Executable executable = e.getCurrentExecutable();
            if (executable == null) {
                continue;
            }

            String hash = getEmulatorConfigHashForTask(node, executable.getParent());
            if (desiredHash.equals(hash)) {
                return CauseOfBlockage.fromMessage(Messages._WAITING_FOR_EMULATOR());
            }
        }

        // Nope, no conflicting builds on this node
        return null;
    }

    /**
     * Determines the Android emulator configuration for the given task, if any.
     *
     * @param node The node on which the task should be executed, so we can retrieve its environment.
     * @param task The task whose Android emulator configuration should be determined.
     * @return A hash representing the Android emulator configuration for the task, or {@code null}
     *         if the given task is not configured to start an Android emulator.
     */
    private static String getEmulatorConfigHashForTask(Node node, SubTask task) {
        // If the job doesn't use any BuildWrappers, we don't care
        if (!(task instanceof BuildableItemWithBuildWrappers)) {
            return null;
        }

        // Fetch the item that actually contains the BuildWrapper config and downcast it
        BuildableItemWithBuildWrappers job;
        MatrixConfiguration matrixBuild = null;
        if (task instanceof MatrixConfiguration) {
            matrixBuild = (MatrixConfiguration) task;
            job = matrixBuild.getParent();
        } else {
            job = (BuildableItemWithBuildWrappers) task;
        }

        // If we aren't one of the wrappers for this build, we don't care
        AndroidEmulator androidWrapper = job.getBuildWrappersList().get(AndroidEmulator.class);
        if (androidWrapper == null) {
            return null;
        }

        if (matrixBuild != null) {
            // If this is a matrix sub-build, substitute in the build variables
            return androidWrapper.getConfigHash(node, matrixBuild.getCombination());
        }
        return androidWrapper.getConfigHash(node);
    }

}
