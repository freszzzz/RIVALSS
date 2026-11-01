plugins {
    java
}

group = "pl.rivals"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

// Wspólne stałe (glify, lista przedmiotów, dźwięki) są kompilowane i do pluginu, i do generatora packa.
sourceSets {
    main {
        java.srcDir("src/shared/java")
    }
    create("packgen") {
        java.srcDirs("src/packgen/java", "src/shared/java")
    }
}

dependencies {
    // Kompilujemy pod najstarszą wspieraną wersję (1.21.4) – plugin działa też na nowszych Paperach.
    // Sprawdzenie zgodności z nowszym API: gradlew compileJava "-PpaperApi=1.21.11-R0.1-SNAPSHOT"
    val paperApi = (findProperty("paperApi") as String?) ?: "1.21.4-R0.1-SNAPSHOT"
    compileOnly("io.papermc.paper:paper-api:$paperApi")

    "packgenImplementation"("com.google.code.gson:gson:2.11.0")
    "packgenImplementation"("org.joml:joml:1.10.5")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
    options.compilerArgs.add("-Xlint:deprecation")
}

// Resource pack

val packDir = layout.buildDirectory.dir("resourcepack")
val packgenSet = sourceSets["packgen"]

val generateResourcePack by tasks.registering(JavaExec::class) {
    group = "rivals"
    description = "Generuje resource pack (modele, tekstury w palecie, czcionki, dźwięki)."
    classpath = packgenSet.runtimeClasspath
    mainClass.set("pl.rivals.packgen.PackGen")
    inputs.files(packgenSet.allSource)
    outputs.dir(packDir)
    doFirst { delete(packDir) }
    argumentProviders.add(CommandLineArgumentProvider { listOf(packDir.get().asFile.absolutePath) })
}

val zipResourcePack by tasks.registering(Zip::class) {
    group = "rivals"
    dependsOn(generateResourcePack)
    from(packDir)
    archiveFileName.set("rivals-pack.zip")
    destinationDirectory.set(layout.buildDirectory.dir("packzip"))
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.processResources {
    filteringCharset = "UTF-8"
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filesMatching("plugin.yml") { expand(props) }
    from(zipResourcePack)
}

tasks.jar {
    archiveFileName.set("Rivals-${project.version}.jar")
}

val desktopDir = file(System.getProperty("user.home") + "/Desktop/RIVALS")

tasks.register<Copy>("exportToDesktop") {
    group = "rivals"
    description = "Buduje plugin i kopiuje go (oraz resource pack) na pulpit: Desktop/RIVALS."
    dependsOn(tasks.jar, zipResourcePack)
    into(desktopDir)
    from(tasks.jar) { into("plugins") }
    from(zipResourcePack) { into("resourcepack") }
    from(packDir) { into("resourcepack/rozpakowany") }
    from("README.md")
}

// Podgląd modeli (narzędzie deweloperskie): gradlew previewModels "-Pvanilla=<katalog z assets/minecraft>"
tasks.register<JavaExec>("previewModels") {
    group = "rivals"
    dependsOn(generateResourcePack)
    classpath = packgenSet.runtimeClasspath
    mainClass.set("pl.rivals.packgen.Preview")
    val vanilla = (findProperty("vanilla") as String?) ?: ""
    val models = (findProperty("models") as String?) ?: ""
    argumentProviders.add(CommandLineArgumentProvider {
        listOf(packDir.get().asFile.absolutePath, vanilla, layout.buildDirectory.dir("preview").get().asFile.absolutePath) +
                models.split(",").filter { it.isNotBlank() }
    })
}
// Makieta HUD (narzędzie deweloperskie): gradlew hudMock "-Pvanilla=<katalog z assets/minecraft>"
tasks.register<JavaExec>("hudMock") {
    group = "rivals"
    dependsOn(generateResourcePack)
    classpath = packgenSet.runtimeClasspath
    mainClass.set("pl.rivals.packgen.HudMock")
    val vanilla = (findProperty("vanilla") as String?) ?: ""
    argumentProviders.add(CommandLineArgumentProvider {
        listOf(packDir.get().asFile.absolutePath, vanilla, layout.buildDirectory.file("preview/hud.png").get().asFile.absolutePath)
    })
}