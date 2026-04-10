#!/usr/bin/env bash
# 在已安装 JDK 的机器上运行：下载 gradle-wrapper.jar（若缺失），以便执行 ./gradlew
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAR="$ROOT/gradle/wrapper/gradle-wrapper.jar"
mkdir -p "$ROOT/gradle/wrapper"
if [[ ! -f "$JAR" ]] || [[ ! -s "$JAR" ]]; then
  echo "正在下载 gradle-wrapper.jar …"
  curl -fsSL -o "$JAR" "https://github.com/gradle/gradle/raw/v8.2.0/gradle/wrapper/gradle-wrapper.jar"
  echo "已保存: $JAR"
else
  echo "已存在: $JAR"
fi
chmod +x "$ROOT/gradlew"
echo "可执行: cd \"$ROOT\" && ./gradlew assembleDebug"
