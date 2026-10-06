# -*- coding: utf-8 -*-
"""为 1.21.x 各分组生成子项目（骨架来自已验证的 v1_21）。

新的分组用宽容版 mixin 配置：某个小版本万一目标方法变了，游戏照常能开、只在该版本上少个功能，
不会崩游戏。
"""
from pathlib import Path

ROOT = Path(r"C:\Users\Admin\Downloads\SCAV\ScavVillager-Fabric")
BASE = ROOT / "v1_21"

# 目录名 -> (编译用的 MC 版本, fabric.mod.json 里的版本范围)
GROUPS = {
    "v1_21_2": ("1.21.2", ">=1.21.2 <1.21.4"),
    "v1_21_4": ("1.21.4", ">=1.21.4 <1.21.5"),
    "v1_21_5": ("1.21.5", ">=1.21.5 <1.21.6"),
    "v1_21_6": ("1.21.6", ">=1.21.6 <1.21.9"),
    "v1_21_9": ("1.21.9", ">=1.21.9 <1.22"),
}

# 宽容版 mixin 配置：目标找不到时只告警，不崩游戏
TOLERANT_MIXINS = """{
  "required": false,
  "minVersion": "0.8",
  "package": "com.arkit.scavvillager.mixin",
  "compatibilityLevel": "JAVA_17",
  "client": [
    "ClientPacketListenerMixin",
    "MinecraftMixin",
    "SoundManagerMixin",
    "ToastComponentMixin",
    "AdvancementToastMixin"
  ],
  "injectors": {
    "defaultRequire": 0
  },
  "refmap": "scav_villager.refmap.json"
}
"""

base_build = (BASE / "build.gradle").read_text(encoding="utf-8")
base_compat = (BASE / "src/main/java/com/arkit/scavvillager/Compat.java").read_text(encoding="utf-8")
base_mod = (BASE / "src/main/resources/fabric.mod.json").read_text(encoding="utf-8")

for name, (mc_ver, range_) in GROUPS.items():
    proj = ROOT / name
    (proj / "src/main/java/com/arkit/scavvillager").mkdir(parents=True, exist_ok=True)
    (proj / "src/main/resources").mkdir(parents=True, exist_ok=True)

    (proj / "build.gradle").write_text(base_build, encoding="utf-8")
    (proj / "gradle.properties").write_text(
        f"# 按分组里最低的版本编译，向上声明到 {range_}\nminecraft_version={mc_ver}\njava_release=21\n",
        encoding="utf-8")
    (proj / "src/main/java/com/arkit/scavvillager/Compat.java").write_text(base_compat, encoding="utf-8")
    (proj / "src/main/resources/scav_villager.mixins.json").write_text(TOLERANT_MIXINS, encoding="utf-8")

    # fabric.mod.json：只改 minecraft 依赖范围
    import re
    mod = re.sub(r'"minecraft": "[^"]*"', f'"minecraft": "{range_}"', base_mod)
    (proj / "src/main/resources/fabric.mod.json").write_text(mod, encoding="utf-8")
    print(f"已生成 {name}: 编译={mc_ver} 范围={range_}")

# settings.gradle
settings = ROOT / "settings.gradle"
text = settings.read_text(encoding="utf-8")
lines = [l for l in text.splitlines() if not l.strip().startswith("include ")]
lines.append("# 一套源码，按 Minecraft 版本分组各出一个 jar")
lines.append("include 'v1_20_1'      // 1.20.1")
lines.append("include 'v1_21'        // 1.21 / 1.21.1")
for name, (mc_ver, range_) in GROUPS.items():
    lines.append(f"include '{name}'" + " " * max(1, 10 - len(name)) + f"// {mc_ver}（{range_}）")
settings.write_text("\n".join(lines) + "\n", encoding="utf-8")
print("\n=== settings.gradle ===")
print(settings.read_text(encoding="utf-8"))
