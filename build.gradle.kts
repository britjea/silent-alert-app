// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}

// Force a single javapoet version across all configurations to avoid
// the ClassName.canonicalName() NoSuchMethodError caused by mixed javapoet 1.x/2.x on classpath.
allprojects {
    configurations.all {
        resolutionStrategy.force("com.squareup:javapoet:1.13.0")
    }
}