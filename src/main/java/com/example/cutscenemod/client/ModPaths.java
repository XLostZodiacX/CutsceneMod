package com.example.cutscenemod.client;

import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;

public class ModPaths {

    /**
     * .minecraft/config/cutscenemod/cutscenes/
     */
    public static File cutscenesDir() {
        File dir = FMLPaths.CONFIGDIR.get().resolve("cutscenemod").resolve("cutscenes").toFile();
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    /**
     * .minecraft/config/cutscenemod/songs/
     */
    public static File songsDir() {
        File dir = FMLPaths.CONFIGDIR.get().resolve("cutscenemod").resolve("songs").toFile();
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public static File cutsceneFile(String nameWithoutExtension) {
        return new File(cutscenesDir(), nameWithoutExtension + ".mp4");
    }

    public static File songFile(String fileNameWithExtension) {
        return new File(songsDir(), fileNameWithExtension);
    }
}
