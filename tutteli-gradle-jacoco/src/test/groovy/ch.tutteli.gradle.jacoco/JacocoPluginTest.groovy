package ch.tutteli.gradle.jacoco

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class JacocoPluginTest {

    @Test
    void smokeTest(){
        Project project = ProjectBuilder.builder().build()
        project.plugins.apply(JacocoPlugin)
    }
}
