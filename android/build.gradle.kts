plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.spotless)
}

spotless {
    kotlin {
        target("app/src/**/*.kt")
        ktlint(libs.versions.ktlint.get())
            .setEditorConfigPath("$rootDir/.editorconfig")
            .editorConfigOverride(
                mapOf(
                    "ktlint_class_signature_rule_force_multiline_when_parameter_count_greater_or_equal_than" to 2,
                    "ktlint_function_signature_rule_force_multiline_when_parameter_count_greater_or_equal_than" to 5,
                    "ktlint_standard_blank-line-between-when-conditions" to "disabled",
                ),
            )
    }
    kotlinGradle {
        target("*.gradle.kts", "app/*.gradle.kts")
        ktlint(libs.versions.ktlint.get()).setEditorConfigPath("$rootDir/.editorconfig")
    }
}
