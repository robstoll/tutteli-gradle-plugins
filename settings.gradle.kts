rootProject.name = "tutteli-gradle"

listOf(
    "junitjacoco",
    "kotlin-module-info",
    "kotlin-utils",
    "project-utils",
    "publish",
    "spek"
).forEach { include("${rootProject.name}-$it") }

include("test-utils")
