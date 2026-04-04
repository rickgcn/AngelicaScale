
plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

configurations.configureEach {
    resolutionStrategy.force("com.github.GTNewHorizons:GTNHLib:0.7.10")
}
