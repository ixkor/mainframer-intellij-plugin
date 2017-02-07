package com.elpassion.intelijidea.action.configure

import com.elpassion.intelijidea.action.configure.releases.MFConfigureProjectDialog
import com.elpassion.intelijidea.action.configure.releases.api.provideGithubApi
import com.elpassion.intelijidea.action.configure.releases.api.provideGithubRetrofit
import com.elpassion.intelijidea.action.configure.releases.service.MFVersionsReleaseService
import com.elpassion.intelijidea.common.MFDownloader
import com.elpassion.intelijidea.util.getMfToolDownloadUrl
import com.elpassion.intelijidea.util.mfFilename
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.intellij.platform.templates.github.Outcome
import io.reactivex.Observable
import io.reactivex.Scheduler

class MFConfigureProjectAction : AnAction(MF_CONFIGURE_PROJECT) {
    override fun actionPerformed(event: AnActionEvent) {
        event.project?.let {
            MFConfigureProjectActionController(
                    service = MFVersionsReleaseService(provideGithubApi(provideGithubRetrofit())),
                    versionChooser = { versionsList ->
                        Observable.create({ emitter ->
                            MFConfigureProjectDialog(it, versionsList, { version ->
                                emitter.onNext(version)
                                emitter.onComplete()
                            }).show()
                        })
                    },
                    downloadMainframer = { version ->
                        Observable.just(
                                MFDownloader.downloadFileToProject(getMfToolDownloadUrl(version), it, mfFilename)
                        )
                    },
                    progressScheduler = ProgressScheduler(it, "Downloading mainframer versions")).configureMainframer()
        }
    }

    companion object {
        private val MF_CONFIGURE_PROJECT = "Configure Mainframer in Project"
    }

    class MFConfigureProjectActionController(
            val service: MFVersionsReleaseService,
            val versionChooser: (List<String>) -> Observable<String>,
            val downloadMainframer: (String) -> Observable<Outcome<Unit>>,
            val progressScheduler: Scheduler) {

        fun configureMainframer() {
            service.getVersions()
                    .subscribeOn(progressScheduler)
                    .observeOn(UIScheduler)
                    .flatMap(versionChooser)
                    .flatMap(downloadMainframer)
                    .subscribe({
                        Messages.showInfoMessage(it.getMessage(), MFConfigureProjectAction.MF_CONFIGURE_PROJECT)
                    }, {
                        Messages.showInfoMessage(it.message, MFConfigureProjectAction.MF_CONFIGURE_PROJECT)
                    })
        }

        private fun Outcome<Unit>.getMessage() = when {
            isCancelled -> "Mainframer configuration canceled"
            exception != null -> "Error during mainframer configuration"
            else -> "Mainframer configured in your project!"
        }
    }
}
