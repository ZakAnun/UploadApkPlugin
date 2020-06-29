package com.zakli.task.upload

import com.android.build.gradle.api.BaseVariant
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException

/**
 * 上传 fir 插件
 */
class UploadFirPlugin implements Plugin<Project> {

    public static final String sPluginExtensionName = "uploadFir"

    @Override
    void apply(Project project) {
        if (!project.plugins.hasPlugin("com.android.application")) {
            throw new ProjectConfigurationException("Plugin requires the 'com.android.application' plugin to be configured.", null)
        }

        applyExtension(project)

        applyTask(project)
    }

    static void applyExtension(Project project) {
        project.extensions.create(sPluginExtensionName, UploadExtension, project)
    }

    static void applyTask(Project project) {
        project.afterEvaluate {
            project.android.applicationVariants.all { BaseVariant variant ->
                def variantName = variant.name.capitalize()

                UploadFirTask uploadTask = project.tasks.create("assemble${variantName}UploadFir", UploadFirTask)
                uploadTask.targetProject = project
                uploadTask.variant = variant
                uploadTask.setup()

                if (variant.hasProperty("assembleProvider")) {
                    uploadTask.dependsOn variant.assembleProvider.get()
                } else {
                    uploadTask.dependsOn variant.getAssembleProvider()
                }
            }
        }
    }
}