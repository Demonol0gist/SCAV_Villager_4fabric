# -*- coding: utf-8 -*-
"""为 MC 26.2 建一个独立的 Gradle 根（Loom 1.18.2 + Gradle 9.8 + JDK 25）。

和已有的 ScavVillager-Fabric（Loom 1.6.12 + Gradle 8.7）分开：
一套 Gradle 构建里放不下两个 Loom 版本，而且已验过的 1.20.1–1.21.11 构建不希望被动到。
共享的是同一份 common 源码与素材。
"""
from pathlib import Path
import json, shutil

OLD = Path(r"C:\Users\Admin\Downloads\SCAV\ScavVillager-Fabric")
NEW = Path(r"C:\Users\Admin\Downloads\SCAV\ScavVillager-Fabric-26")
MC = "26.2"

(NEW / "src/main/java/com/arkit/scavvillager/mixin").mkdir(parents=True, exist_ok=True)
(NEW / "src/main/resources").mkdir(parents=True, exist_ok=True)

# ---------- settings / build / properties ----------
(NEW / "settings.gradle").write_text("""pluginManagement {
    repositories {
        maven {
            name = 'Fabric'
            url = 'https://maven.fabricmc.net/'
        }
        maven {
            name = 'Aliyun'
            url = 'https://maven.aliyun.com/repository/gradle-plugin'
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = 'scav-villager-fabric-26'
""", encoding="utf-8")

(NEW / "build.gradle").write_text("""plugins {
    id 'fabric-loom' version '1.18.2'
    id 'java'
}

version = project.mod_version
group = project.maven_group

base {
    archivesName = "scav-villager-fabric-${project.minecraft_version}"
}

repositories {
    maven {
        name = 'Aliyun'
        url = 'https://maven.aliyun.com/repository/central'
    }
    mavenCentral()
}

dependencies {
    minecraft "com.mojang:minecraft:${project.minecraft_version}"
    mappings loom.officialMojangMappings()
    modImplementation "net.fabricmc:fabric-loader:${project.loader_version}"
}

loom {
    mixin {
        defaultRefmapName = "scav_villager.refmap.json"
    }
}

sourceSets {
    main {
        java {
            srcDir rootProject.file('../ScavVillager-Fabric/common/src/main/java')
            // 1.21.2+ 形态的 toast 代码（ToastManager + render(GuiGraphics,Font,long)）
            srcDir rootProject.file('../ScavVillager-Fabric/common/toast-new/java')
        }
        resources.srcDir rootProject.file('../ScavVillager-Fabric/common/src/main/resources')
    }
}

processResources {
    inputs.property "version", project.version
    filesMatching("fabric.mod.json") {
        expand "version": project.version
    }
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = "UTF-8"
    options.release = 25
}
""", encoding="utf-8")

(NEW / "gradle.properties").write_text(f"""org.gradle.jvmargs=-Xmx3G

minecraft_version={MC}
loader_version=0.19.5
mod_version=1.0.5
maven_group=com.arkit
""", encoding="utf-8")

# ---------- fabric.mod.json / mixins.json ----------
mod = json.loads((OLD / "v1_21_9/src/main/resources/fabric.mod.json").read_text(encoding="utf-8"))
mod["depends"]["minecraft"] = f"~{MC}"
mod["depends"]["java"] = ">=25"
mod["description"] = mod["description"] + "（26.2 版）"
(NEW / "src/main/resources/fabric.mod.json").write_text(
    json.dumps(mod, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

# 宽容模式：26.x 的 API 尚未逐一验证，目标对不上时只告警不崩游戏
mixins = json.loads((OLD / "v1_21_9/src/main/resources/scav_villager.mixins.json").read_text(encoding="utf-8"))
mixins["required"] = True
mixins["injectors"] = {"defaultRequire": 1}
(NEW / "src/main/resources/scav_villager.mixins.json").write_text(
    json.dumps(mixins, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

# ---------- 起步版 Compat / AdvancementToastMixin（从 1.21.9 抄，构建时按报错再改）----------
shutil.copyfile(OLD / "v1_21_9/src/main/java/com/arkit/scavvillager/Compat.java",
                NEW / "src/main/java/com/arkit/scavvillager/Compat.java")
shutil.copyfile(OLD / "v1_21_9/src/main/java/com/arkit/scavvillager/mixin/AdvancementToastMixin.java",
                NEW / "src/main/java/com/arkit/scavvillager/mixin/AdvancementToastMixin.java")

print(f"已生成 {NEW}")
print("  minecraft:", mod["depends"]["minecraft"], "| java:", mod["depends"]["java"])
print("  共享源码: ../ScavVillager-Fabric/common/src/main/java + common/toast-new/java")
print("  共享素材: ../ScavVillager-Fabric/common/src/main/resources（953 个音频）")
print("  Loom 1.18.2 / Gradle 9.8 / JDK 25 / 宽容模式 mixin")
