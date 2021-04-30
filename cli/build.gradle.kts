plugins {
    kotlin("multiplatform")
    id("com.github.johnrengelman.shadow")
    application
}

group = "${rootProject.group}"
version = "${rootProject.version}"
val artifactName by extra { "${rootProject.name.toLowerCase()}-${project.name.toLowerCase()}" }
val theMainClass by extra { "com.hoffi.mpp.cli.AppKt" }

kotlin {
    jvm {
        //withJava() // applies the Gradle java plugin to allow the multiplatform project to have both Java and Kotlin source files (src/jvmMain/java/...)

       // tasks.withType<Jar> {
       //     archiveBaseName.set(project.name.toLowerCase())
       // }

        // create an executable java fat jar
        //val jvmJar by tasks.getting(org.gradle.jvm.tasks.Jar::class) {
        //    doFirst {
        //        // configurations.forEach { println(it.name) }
        //        manifest {
        //            attributes["Main-Class"] = theMainClass
        //        }
        //        from(configurations.getByName("jvmRuntimeClasspath").map { if (it.isDirectory) it else zipTree(it) })
        //    }
        //}
    }
    macosX64("mac") { // without name param, creates sourceSet named("macosX64Main") and named("macosX64Main")
        // linuxX64("linux") // on Linux
        // mingwX64("windows") // on Windows
        binaries {
            executable {
                // entry point function = package with non-inside-object main method + ".main" (= name of the main function)
                entryPoint(theMainClass.replaceAfterLast(".", "main"))
                //runTask?.args("")
                // Use the following Gradle tasks to run your application:
                // (<subproject>):runReleaseExecutableMacos - without debug symbols
                // (<subproject>):runDebugExecutableMacos - with debug symbols
            }
        }
    }
    linuxX64("unix") { // without name param, creates sourceSet named("macosX64Main") and named("macosX64Main")
        // linuxX64("linux") // on Linux
        // mingwX64("windows") // on Windows
        binaries {
            executable {
                // entry point function = package with non-inside-object main method + ".main" (= name of the main function)
                entryPoint(theMainClass.replaceAfterLast(".", "main"))
                //runTask?.args("")
                // Use the following Gradle tasks to run your application:
                // (<subproject>):runReleaseExecutableMacos - without debug symbols
                // (<subproject>):runDebugExecutableMacos - with debug symbols
            }
        }
    }

    // https://kotlinlang.org/docs/mpp-share-on-platforms.html#share-code-in-libraries
    // To enable usage of platform-dependent libraries in shared source sets, add the following to your `gradle.properties`
    // kotlin.mpp.enableGranularSourceSetsMetadata=true
    // kotlin.native.enableDependencyPropagation=false
    sourceSets {
        val commonMain by getting  { // predefined by gradle multiplatform plugin
            dependencies {
                implementation(project(":lib"))
                //implementation("io.github.microutils:kotlin-logging:2.0.6")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:${Deps.Misc.DATETIME.VERSION}")
                implementation("com.github.ajalt.clikt:clikt:${Deps.Misc.CLIKT.VERSION}")
            }
        }
        val commonTest by getting {
            dependencies {
            }
        }

        val jvmMain by getting {
            //print("${name} dependsOn: ")
            //println(dependsOn.map { it.name }.joinToString())
            dependencies {
                //implementation("ch.qos.logback:logback-classic:1.2.3")
                //implementation("org.slf4j:slf4j-api:1.7.30")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        val macMain by getting { // named("macMain") {
            dependencies {

            }
        }
        val macTest by getting { // named("macTest") {
            dependencies {

            }
        }
    }
}

application {
    mainClassName = theMainClass // workaround for shadow plugin (might be fixed in shadow gradle plugin > 6.1.0)
    mainClass.set(theMainClass)
}

tasks.getByName<JavaExec>("run") {
    classpath(tasks.getByName<Jar>("jvmJar")) // so that the JS artifacts generated by `jvmJar` can be found and served
}

tasks {
    val shadowCreate by creating(com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
        manifest {
            attributes["Main-Class"] = theMainClass
        }
        mergeServiceFiles()

        archiveClassifier.set("fat")
        from(kotlin.jvm().compilations.getByName("main").output)
        configurations =
            mutableListOf(kotlin.jvm().compilations.getByName("main").compileDependencyFiles as Configuration)
    }
    val build by existing {
        dependsOn(shadowCreate)
    }
}

// ################################################################################################
// #####    pure informational stuff on stdout    #################################################
// ################################################################################################
tasks.register("printClasspath") {
    group = "misc"
    description = "print classpath"
    doLast {
        //project.getConfigurations().filter { it.isCanBeResolved }.forEach {
        //    println(it.name)
        //}
        //println()
        val targets = listOf(
            "metadataCommonMainCompileClasspath",
            "commonMainApiDependenciesMetadata",
            "commonMainImplementationDependenciesMetadata",
            "jvmCompileClasspath",
            "kotlinCompilerClasspath"
        )
        targets.forEach { targetConfiguration ->
            println("$targetConfiguration:")
            println("=".repeat("$targetConfiguration:".length))
            project.getConfigurations()
                .getByName(targetConfiguration).files
                // filters only existing and non-empty dirs
                .filter { (it.isDirectory() && it.listFiles().isNotEmpty()) || it.isFile() }
                .forEach { println(it) }
        }
    }
}
