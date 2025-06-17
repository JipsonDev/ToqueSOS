// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    dependencies {
        classpath("com.android.tools.build:gradle:8.1.1")
        classpath(kotlin("gradle-plugin", version = "1.9.10")) // o la versión que uses
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}