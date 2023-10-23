rootProject.name = "tutteli-gradle"

listOf(
    "dokka",
    "junitjacoco",
    "kotlin-module-info",
    "publish",
    "spek"
).forEach { include("${rootProject.name}-$it") }

include("test-utils")
