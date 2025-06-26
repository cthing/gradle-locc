rootProject.name = "gradle-locc"

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version ("1.0.0")
    id("com.gradle.develocity") version("4.0.2")
}

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/help/legal-terms-of-use"
        termsOfUseAgree = "yes"

        // Upload scans when built by GitHub actions.
        publishing.onlyIf { !System.getenv("CI").isNullOrEmpty() }
    }
}
