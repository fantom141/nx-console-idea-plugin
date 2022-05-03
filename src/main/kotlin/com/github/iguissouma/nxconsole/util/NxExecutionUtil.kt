package com.github.iguissouma.nxconsole.util

import com.intellij.execution.ExecutionException
import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.process.AnsiEscapeDecoder.ColoredTextAcceptor
import com.intellij.execution.process.CapturingProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutput
import com.intellij.javascript.nodejs.NodeCommandLineUtil
import com.intellij.javascript.nodejs.execution.NodeTargetRun
import com.intellij.javascript.nodejs.interpreter.NodeCommandLineConfigurator
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.javascript.nodejs.npm.NpmManager
import com.intellij.javascript.nodejs.npm.NpmNodePackage
import com.intellij.javascript.nodejs.npm.NpmPackageDescriptor
import com.intellij.javascript.nodejs.npm.NpmUtil
import com.intellij.lang.javascript.JavaScriptBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.text.HtmlBuilder
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.util.ThreeState


class NxExecutionUtil(val project: Project) {

    fun execute(command: String, vararg args: String): Boolean {
        val output = executeAndGetOutput(command, *args)
        return output?.exitCode == 0
    }

    fun executeAndGetOutput(command: String, vararg args: String): ProcessOutput? {
        val nodeInterpreter = NodeJsInterpreterManager.getInstance(project).interpreter ?: return null
        val configurator = NodeCommandLineConfigurator.find(nodeInterpreter) ?: return null
        val npmPackageRef = NpmUtil.createProjectPackageManagerPackageRef()
        val npmPkg = NpmUtil.resolveRef(npmPackageRef, project, nodeInterpreter)
        val targetRun = NodeTargetRun(
            nodeInterpreter,
            project,
            null,
            NodeTargetRun.createOptions(
                ThreeState.UNSURE,
                listOf(),
                true
            )
        )

        if (npmPkg == null) {
            if (NpmUtil.isProjectPackageManagerPackageRef(npmPackageRef)) {
                val message = JavaScriptBundle.message(
                    "npm.dialog.message.cannot.resolve.package.manager",
                    NpmManager.getInstance(project).packageRef.identifier
                )
                throw NpmManager.InvalidNpmPackageException(
                    project,
                    HtmlBuilder().append(message).append(HtmlChunk.p())
                        .toString() + JavaScriptBundle.message("please.specify.package.manager", *arrayOfNulls(0))
                ) {} // onNpmPackageRefResolved
            } else {
                throw ExecutionException(
                    JavaScriptBundle.message(
                        "npm.dialog.message.cannot.resolve.package.manager",
                        npmPackageRef.identifier
                    )
                )
            }
        } else {


            val commandLine = targetRun.commandLineBuilder


            targetRun.enableWrappingWithYarnPnpNode = false
            NpmNodePackage.configureNpmPackage(targetRun, npmPkg, *arrayOfNulls(0))
            NodeCommandLineUtil.prependNodeDirToPATH(targetRun)

            val yarn = NpmUtil.isYarnAlikePackage(npmPkg)
            if (NpmUtil.isPnpmPackage(npmPkg)) {
                if (npmPkg.version != null && npmPkg.version!!.major >= 6 && npmPkg.version!!.minor >= 13) {
                    // useExec like vscode extension
                    commandLine.addParameter("exec")
                } else {
                    NpmPackageDescriptor.findBinaryFilePackage(nodeInterpreter, "pnpx")?.configureNpmPackage(targetRun)
                }
            } else if (yarn.not()) {
                NpmPackageDescriptor.findBinaryFilePackage(nodeInterpreter, "npx")?.configureNpmPackage(targetRun)
            }


            commandLine.setWorkingDirectory(project.basePath!!)
            commandLine.addParameter("nx")
            commandLine.addParameter(command)
            commandLine.addParameter(args.joinToString(" "))
            val handler: ProcessHandler = targetRun.startProcess()
            return getOutput(handler)
        }
    }

    fun getOutput(processHandler: ProcessHandler): ProcessOutput? {
        val adapter = AnsiEscapesAwareAdapter(ProcessOutput())
        processHandler.addProcessListener(adapter)
        processHandler.startNotify()
        processHandler.waitFor()
        return adapter.output
    }
}


class AnsiEscapesAwareAdapter(output: ProcessOutput?) :
    CapturingProcessAdapter(output!!), ColoredTextAcceptor {
    private val myAnsiEscapeDecoder: AnsiEscapeDecoder = object : AnsiEscapeDecoder() {
        override fun getCurrentOutputAttributes(outputType: Key<*>): Key<*> {
            return outputType
        }
    }

    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        myAnsiEscapeDecoder.escapeText(event.text, outputType, this)
    }

    override fun coloredTextAvailable(text: String, attributes: Key<*>) {
        addToOutput(text, attributes)
    }
}
