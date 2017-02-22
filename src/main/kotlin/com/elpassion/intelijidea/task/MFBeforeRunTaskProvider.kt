package com.elpassion.intelijidea.task

import com.elpassion.intelijidea.task.edit.MFBeforeRunTaskDialog
import com.elpassion.intelijidea.util.MFIcons
import com.elpassion.intelijidea.util.showError
import com.elpassion.intelijidea.util.showInfo
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import javax.swing.Icon
import javax.swing.SwingUtilities

class MFBeforeRunTaskProvider(private val project: Project) : BeforeRunTaskProvider<MFBeforeRunTask>() {

    override fun getId(): Key<MFBeforeRunTask> = ID

    override fun getDescription(task: MFBeforeRunTask): String = TASK_NAME

    override fun getName(): String = TASK_NAME

    override fun getIcon(): Icon = MFIcons.mainframerIcon

    override fun isConfigurable(): Boolean = true

    override fun configureTask(runConfiguration: RunConfiguration?, task: MFBeforeRunTask): Boolean {
        MFBeforeRunTaskDialog(project).run {
            restoreMainframerTaskData(task.data)
            if (showAndGet()) {
                task.data = createMFTaskDataFromForms()
                return true
            }
        }
        return false
    }

    override fun canExecuteTask(configuration: RunConfiguration?, task: MFBeforeRunTask): Boolean = task.isValid()

    override fun executeTask(context: DataContext, configuration: RunConfiguration?, env: ExecutionEnvironment, task: MFBeforeRunTask): Boolean {
        if (!task.isValid()) {
            configuration?.project?.showInvalidDataError()
            return false
        }
        configuration?.project?.showStartExecutionInfo()
        return MFBeforeRunTaskExecutor(project).executeSync(task, env.executionId)
    }

    override fun createTask(runConfiguration: RunConfiguration?): MFBeforeRunTask? {
        val settingsProvider = MFBeforeTaskDefaultSettingsProvider.INSTANCE
        return MFBeforeRunTask(settingsProvider.taskData)
    }

    companion object {
        val ID = Key.create<MFBeforeRunTask>("MainFrame.BeforeRunTask")
        val TASK_NAME = "Mainframer Make"
    }

    private fun Project.showInvalidDataError() = SwingUtilities.invokeAndWait {
        showError(this, "Cannot execute task with invalid data")
    }

    private fun Project.showStartExecutionInfo() = SwingUtilities.invokeAndWait {
        showInfo(this, "Mainframer is executing task: $name")
    }
}

val Project.mfBeforeRunTaskProvider: MFBeforeRunTaskProvider
    get() = BeforeRunTaskProvider.getProvider(this, MFBeforeRunTaskProvider.ID) as MFBeforeRunTaskProvider