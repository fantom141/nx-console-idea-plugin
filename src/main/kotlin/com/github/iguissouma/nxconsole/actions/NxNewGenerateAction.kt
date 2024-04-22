package com.github.iguissouma.nxconsole.actions

import com.github.iguissouma.nxconsole.schematics.Schematic
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

class NxNewGenerateAction(
    val schematic: Schematic,
    val virtualFile: VirtualFile,
    text: String?,
    description: String?,
    icon: Icon?
) : AnAction(
    text,
    description,
    icon
) {
    override fun actionPerformed(e: AnActionEvent) {
    }
}
