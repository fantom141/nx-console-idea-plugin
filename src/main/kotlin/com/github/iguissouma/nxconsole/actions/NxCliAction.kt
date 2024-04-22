package com.github.iguissouma.nxconsole.actions

import com.github.iguissouma.nxconsole.cli.config.NxProject
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.vfs.VirtualFile
import java.util.*
import javax.swing.Icon

class NxCliAction(
    val command: String,
    val target: String,
    val architect: NxProject.Architect,
    val virtualFile: VirtualFile,
    text: String?,
    description: String?,
    icon: Icon?
) : AnAction(text, description, icon) {

    override fun actionPerformed(event: AnActionEvent) {
    }
}
