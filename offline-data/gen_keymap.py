#!/usr/bin/env python3
"""
keymap 自动生成脚本

扫描 offline-data/ 下所有 RM1201.json，
提取 key，如果 inputIndexContent 为空（即内容分析类接口），
则注册到 keymap.json 中。

用法：
    python gen_keymap.py                   # 扫描当前目录的 offline-data/
    python gen_keymap.py /path/to/offline-data  # 指定数据包路径
"""

import json
import os
import sys
from pathlib import Path


def generate_keymap(data_path: str) -> dict:
    """扫描数据目录，生成 key → 路径映射"""
    data_root = Path(data_path)
    keymap = {}

    if not data_root.exists():
        print(f"[错误] 目录不存在: {data_root.absolute()}")
        return keymap

    for rm1201_file in data_root.rglob("RM1201.json"):
        # 跳过不在三级结构内的文件
        relative = rm1201_file.relative_to(data_root)
        parts = relative.parts

        if len(parts) != 3:
            print(f"[跳过] 路径层级不对: {relative}")
            continue

        ent_name = parts[0]
        module_code = parts[1]
        relative_dir = f"{ent_name}/{module_code}"

        with open(rm1201_file, encoding="utf-8") as f:
            try:
                data = json.load(f)
            except json.JSONDecodeError as e:
                print(f"[跳过] JSON 解析失败 {relative}: {e}")
                continue

        key = data.get("key", "").strip()
        input_index = data.get("inputIndexContent", {})

        if not key:
            print(f"[跳过] 无 key: {relative}")
            continue

        # 判断是否为内容分析类（inputIndexContent 为空对象即不是指标类）
        is_indicator = bool(input_index)

        if is_indicator:
            print(f"[跳过] 指标类，不注册 keymap: {relative} (key={key})")
            continue

        # 内容分析类：注册 keymap
        if key in keymap and keymap[key] != relative_dir:
            print(f"[警告] key 冲突: {key} 先映射到 {keymap[key]}，现覆盖为 {relative_dir}")

        keymap[key] = relative_dir
        print(f"[注册] {key} → {relative_dir}")

    return keymap


def main():
    if len(sys.argv) > 1:
        data_path = sys.argv[1]
    else:
        data_path = "./offline-data"

    print(f"扫描目录: {os.path.abspath(data_path)}")
    print("=" * 60)

    keymap = generate_keymap(data_path)

    print("=" * 60)
    print(f"共注册 {len(keymap)} 条 key 映射")

    # 写入 keymap.json
    output_path = Path(data_path) / "keymap.json"
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(keymap, f, ensure_ascii=False, indent=2)

    print(f"已写入: {output_path.absolute()}")


if __name__ == "__main__":
    main()
