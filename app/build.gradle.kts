plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.zleeper.sleepapp"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.zleeper.sleepapp"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes { release { isMinifyEnabled = false; proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro") } }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    testOptions { unitTests.isIncludeAndroidResources = true }
    sourceSets { getByName("androidTest").assets.srcDir("$projectDir/schemas") }
}

ksp { arg("room.schemaLocation", "$projectDir/schemas") }

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.serialization.json)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    testImplementation(libs.junit)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.androidx.navigation.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

val validateGameAssets by tasks.registering {
    group = "verification"
    description = "Validates required production content and runtime assets."
    val gameRoot = layout.projectDirectory.dir("src/main/assets/game")
    inputs.dir(gameRoot)
    doLast {
        val required = listOf(
            "content/content_manifest.json", "content/progression_rules_v1.json", "content/progression_rules_v2.json",
            "content/pet_species.json", "content/pet_forms.json", "content/items.json", "content/equipment_effects.json",
            "content/quests.json", "content/regions.json", "content/scenes.json", "content/expedition_nodes.json",
            "content/loot_tables.json", "content/dialogue/npcs.json", "content/specializations.json", "content/hearth_stages.json",
            "content/crafting_recipes.json", "content/equipment_infusions.json", "content/equipment_traits.json", "content/titles.json",
            "content/expedition_narrative.json",
            "atlas/hub/hub_moonmoth_hearth.webp",
            "atlas/pet/pet_moonmoth_glimmerling_atlas.webp", "atlas/pet/pet_moonmoth_glimmerling_atlas.json",
            "atlas/pet/pet_moonmoth_lanternwing_atlas.webp", "atlas/pet/pet_moonmoth_lanternwing_atlas.json",
            "atlas/npc/npc_keeper_orin_atlas.webp", "atlas/npc/npc_keeper_orin_atlas.json",
            "atlas/npc/npc_pip_atlas.webp", "atlas/npc/npc_pip_atlas.json",
            "atlas/npc/npc_mara_atlas.webp", "atlas/npc/npc_mara_atlas.json",
            "audio/music/mus_whispering_grove_explore_loop.ogg", "audio/music/mus_bramble_rise_explore_loop.ogg",
            "audio/ambience/amb_whispering_grove_night_loop.ogg", "audio/ambience/amb_bramble_rise_night_loop.ogg",
            "audio/sfx/sfx_pet_wake_01.ogg",
        )
        required.forEach { path ->
            val file = gameRoot.file(path).asFile
            check(file.isFile && file.length() > 0L) { "Missing or empty production asset: $path" }
        }
        val minimumBytes = mapOf(
            Regex("atlas/pet/.+\\.webp") to 1_000_000L,
            Regex("atlas/hub/.+\\.webp") to 100_000L,
            Regex("atlas/npc/.+\\.webp") to 30_000L,
            Regex("atlas/region/.+_bg_(far|mid|near)\\.webp") to 70_000L,
            Regex("atlas/region/.+_(tiles|props_static|foreground)\\.webp") to 40_000L,
            Regex("item/icon/.+\\.webp") to 10_000L,
        )
        minimumBytes.forEach { (pattern, minimum) ->
            gameRoot.asFile.walkTopDown().filter { it.isFile }.forEach { file ->
                val relative = file.relativeTo(gameRoot.asFile).invariantSeparatorsPath
                if (pattern.matches(relative)) check(file.length() >= minimum) { "Production asset is below its quality floor ($minimum bytes): $relative" }
            }
        }
        gameRoot.asFile.walkTopDown().filter { it.isFile }.forEach { file ->
            val relative = file.relativeTo(gameRoot.asFile).invariantSeparatorsPath
            check(relative.matches(Regex("[a-z0-9_/]+\\.(json|webp|ogg)"))) { "Invalid runtime asset filename: $relative" }
        }
    }
}

tasks.named("preBuild").configure { dependsOn(validateGameAssets) }
