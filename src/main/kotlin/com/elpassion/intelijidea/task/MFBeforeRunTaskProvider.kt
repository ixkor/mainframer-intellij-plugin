package com.elpassion.intelijidea.task

import com.elpassion.intelijidea.task.edit.MFBeforeRunTaskDialog
import com.elpassion.intelijidea.util.showError
import com.elpassion.intelijidea.util.showInfo
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import javax.swing.SwingUtilities

class MFBeforeRunTaskProvider(private val project: Project) : BeforeRunTaskProvider<MFBeforeRunTask>() {

    override fun getId(): Key<MFBeforeRunTask> = ID

    override fun getDescription(task: MFBeforeRunTask): String = TASK_NAME

    override fun getName(): String = TASK_NAME

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
            configuration?.project?.let {
                SwingUtilities.invokeAndWait {
                    showError(it, "Cannot execute task with invalid data")
                }
            }
            return false
        }
        configuration?.project?.let {
            SwingUtilities.invokeAndWait {
                showInfo(it, "Mainframer is executing task: ${it.name}")
            }
        }
        return MFBeforeRunTaskExecutor(project).executeSync(task, env.executionId)
    }

    override fun createTask(runConfiguration: RunConfiguration?): MFBeforeRunTask? {
        val settingsProvider = MFBeforeTaskDefaultSettingsProvider.INSTANCE
        return MFBeforeRunTask(settingsProvider.taskData)
    }

    companion object {
        val ID = Key.create<MFBeforeRunTask>("MainFrame.BeforeRunTask")
        val TASK_NAME = "MainframerBefore"
    }
}

val Project.mfBeforeRunTaskProvider: MFBeforeRunTaskProvider
    get() = BeforeRunTaskProvider.getProvider(this, MFBeforeRunTaskProvider.ID) as MFBeforeRunTaskProvider