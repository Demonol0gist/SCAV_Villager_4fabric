# -*- coding: utf-8 -*-
"""修两处：
   1) mixins.json 被写成双花括号（模板忘记 .format()）→ 重写成正确的 JSON
   2) 新形态 toast 文件从 common/src/main/java 挪到 common/toast-new/java，
      旧项目不再需要 exclude（exclude 会误伤项目自己的同名文件）
"""
from pathlib import Path
import shutil

ROOT = Path(r"C:\Users\Admin\Downloads\SCAV\ScavVillager-Fabric")
COMMON_JAVA = ROOT / "common/src/main/java/com/arkit/scavvillager"
NEW_JAVA = ROOT / "common/toast-new/java/com/arkit/scavvillager"

# ---------- 1) 挪文件 ----------
NEW_JAVA.mkdir(parents=True, exist_ok=True)
(NEW_JAVA / "mixin").mkdir(parents=True, exist_ok=True)
shutil.move(str(COMMON_JAVA / "IntroToast.java"), str(NEW_JAVA / "IntroToast.java"))
shutil.move(str(COMMON_JAVA / "mixin/ToastManagerMixin.java"), str(NEW_JAVA / "mixin/ToastManagerMixin.java"))
print("已把新形态 IntroToast / ToastManagerMixin 挪到 common/toast-new/java")

# ---------- 2) build.gradle：旧项目去掉 exclude，新项目加 toast-new 目录 ----------
OLD_SRC = """sourceSets {
    main {
        java.srcDir rootProject.file('common/src/main/java')
        resources.srcDir rootProject.file('common/src/main/resources')
    }
}"""

OLD_SRC_WITH_EXCLUDE = """sourceSets {
    main {
        java {
            srcDir rootProject.file('common/src/main/java')
            // common 里的 IntroToast / ToastManagerMixin 是 1.21.2+ 的新形态，
            // 本项目有自己的旧形态版本，这里要排除，否则类重复
            exclude 'com/arkit/scavvillager/IntroToast.java'
            exclude 'com/arkit/scavvillager/mixin/ToastManagerMixin.java'
        }
        resources.srcDir rootProject.file('common/src/main/resources')
    }
}"""

NEW_SRC = """sourceSets {
    main {
        java {
            srcDir rootProject.file('common/src/main/java')
            // 1.21.2+ 的 toast 形态（ToastManager + render(GuiGraphics,Font,long)）
            srcDir rootProject.file('common/toast-new/java')
        }
        resources.srcDir rootProject.file('common/src/main/resources')
    }
}"""

for proj in ["v1_20_1", "v1_21"]:
    f = ROOT / proj / "build.gradle"
    t = f.read_text(encoding="utf-8")
    if OLD_SRC_WITH_EXCLUDE in t:
        f.write_text(t.replace(OLD_SRC_WITH_EXCLUDE, OLD_SRC), encoding="utf-8")
        print(f"{proj}: 去掉 exclude，恢复普通 sourceSets")
    else:
        print(f"{proj}: ！！没找到带 exclude 的块")

for proj in ["v1_21_2", "v1_21_4", "v1_21_5", "v1_21_6", "v1_21_9"]:
    f = ROOT / proj / "build.gradle"
    t = f.read_text(encoding="utf-8")
    if OLD_SRC in t:
        f.write_text(t.replace(OLD_SRC, NEW_SRC), encoding="utf-8")
        print(f"{proj}: 加上 common/toast-new/java 源目录")
    else:
        print(f"{proj}: ！！sourceSets 块不是预期内容，需手动处理")

# ---------- 3) 重写各分组的 mixins.json（正确的 JSON）----------
MIXINS = """{
  "required": false,
  "minVersion": "0.8",
  "package": "com.arkit.scavvillager.mixin",
  "compatibilityLevel": "JAVA_17",
  "client": [
    "ClientPacketListenerMixin",
    "MinecraftMixin",
    "SoundManagerMixin",
    "ToastManagerMixin",
    "AdvancementToastMixin"
  ],
  "injectors": {
    "defaultRequire": 0
  },
  "refmap": "scav_villager.refmap.json"
}
"""

import json
for proj in ["v1_21_2", "v1_21_4", "v1_21_5", "v1_21_6", "v1_21_9"]:
    p = ROOT / proj / "src/main/resources/scav_villager.mixins.json"
    p.write_text(MIXINS, encoding="utf-8")
    json.loads(MIXINS)          # 自校验
    print(f"{proj}: mixins.json 已重写（JSON 合法 ✓）")
