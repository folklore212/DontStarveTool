#!/usr/bin/env python3
"""
Steam 数据采集脚本
在有 Steam 网络访问的电脑上运行，采集数据后拷回开发机。

用法:
  1. 获取 Steam Web API Key: https://steamcommunity.com/dev/apikey
  2. 运行: python3 collect_steam_data.py --apikey YOUR_STEAM_API_KEY
  3. 将生成的 data/steam_samples/ 目录打包拷回开发机

输出目录结构:
  data/steam_samples/
    summary.json                 # 总览（所有文件的摘要）
    _collect.log                 # 采集日志

    # Workshop 数据
    workshop/hot_mods.json              # 热门 DST mod（前 500 个，分页）
    workshop/hot_mods_page1.json        # 第 1 页（50 条）
    workshop/hot_mods_page2.json        # 第 2 页
    ...
    workshop/search_qol.json            # 搜索 "quality of life"
    workshop/search_boss.json           # 搜索 "boss"
    workshop/search_character.json      # 搜索 "character"
    workshop/search_food.json           # 搜索 "crock pot"
    workshop/search_bag.json            # 搜索 "backpack"
    workshop/mod_378160973.json         # 单个 mod 详情（Global Positions）
    workshop/mod_350811795.json         # 单个 mod 详情（Health Info）
    ...

    # DST 版本 & 新闻
    steam/versions.json           # DST 客户端 + 服务端版本/build ID
    steam/news_dst.json           # DST 新闻/公告
    steam/player_count.json       # 当前在线玩家数
"""

import argparse
import json
import logging
import os
import sys
import time
import urllib.request
import urllib.parse
from datetime import datetime, timezone
from pathlib import Path

STEAM_WEB_API = "https://api.steampowered.com"
DST_CLIENT_APPID = 322330      # Don't Starve Together (game)
DST_SERVER_APPID = 343050      # DST Dedicated Server

OUTPUT_DIR = Path("data/steam_samples")

# ── Logger ──────────────────────────────────────────────────────────

def setup_logging(out_dir: Path):
    out_dir.mkdir(parents=True, exist_ok=True)
    log_path = out_dir / "_collect.log"
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s  %(levelname)-8s  %(message)s",
        datefmt="%Y-%m-%d %H:%M:%S",
        handlers=[
            logging.FileHandler(log_path, encoding="utf-8"),
            logging.StreamHandler(sys.stdout),
        ],
    )


# ── Helpers ─────────────────────────────────────────────────────────

def steam_api_call(apikey: str, interface: str, method: str, version: int, params: dict) -> dict:
    """Call Steam Web API and return JSON."""
    qs = urllib.parse.urlencode(params)
    url = f"{STEAM_WEB_API}/{interface}/{method}/v{version:04d}/?key={apikey}&{qs}"
    req = urllib.request.Request(url)
    t0 = time.time()
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            data = json.loads(resp.read())
            elapsed = time.time() - t0
            logging.info("API  OK  %6.2fs  %s/%s/v%d", elapsed, interface, method, version)
            return data
    except Exception as e:
        elapsed = time.time() - t0
        logging.error("API  ERR %6.2fs  %s/%s/v%d  %s", elapsed, interface, method, version, e)
        raise


def save_json(subdir: str, name: str, data: dict):
    """Save JSON to OUTPUT_DIR/<subdir>/<name>.json."""
    dir_path = OUTPUT_DIR / subdir
    dir_path.mkdir(parents=True, exist_ok=True)
    path = dir_path / f"{name}.json"
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
    logging.info("Saved  %s", path.relative_to(OUTPUT_DIR))


# ══════════════════════════════════════════════════════════════════════
# 1. Workshop 数据
# ══════════════════════════════════════════════════════════════════════

POPULAR_MODS = [
    "378160973",  # Global Positions
    "350811795",  # Health Info
    "356930882",  # Combined Status
    "375859599",  # Wormhole Marks
    "661253977",  # Show Me
    "362175979",  # Extra Equip Slots
    "462434129",  # Simple Health Bar
    "458940297",  # Status Announcements
    "563663595",  # Minimap HUD
    "458587300",  # Snapling Twigs
]

SEARCH_QUERIES = [
    ("", "search_empty"),
    ("quality of life", "search_qol"),
    ("boss", "search_boss"),
    ("character", "search_character"),
    ("crock pot", "search_food"),
    ("backpack", "search_bag"),
    ("base building", "search_base"),
    ("health bar", "search_health"),
    ("farming", "search_farming"),
    ("map", "search_map"),
    ("inventory", "search_inventory"),
    ("skin", "search_skin"),
    ("combined status", "search_status"),
    ("equip slot", "search_equip"),
]


def collect_hot_mods_paginated(apikey: str, total: int = 500, per_page: int = 50):
    """Fetch top DST mods by subscription count, paginated."""
    logging.info("=== [Workshop] Hot mods (top %d, %d/page) ===", total, per_page)
    all_mods = []
    pages = (total + per_page - 1) // per_page
    for page in range(1, pages + 1):
        data = steam_api_call(apikey,
            interface="IPublishedFileService",
            method="QueryFiles",
            version=1,
            params={
                "appid": DST_CLIENT_APPID,
                "return_vote_data": "true",
                "return_tags": "true",
                "return_metadata": "true",
                "numperpage": per_page,
                "page": page,
                "query_type": 9,  # ranked by subscriptions (all-time)
            })
        published = data.get("response", {}).get("publishedfiledetails", [])
        all_mods.extend(published)
        save_json("workshop", f"hot_mods_page{page}", data)
        logging.info("  Page %d/%d: %d mods (total so far: %d)", page, pages, len(published), len(all_mods))
        if len(published) < per_page:
            break  # no more results

    # Combined file for convenience
    combined = {"response": {"publishedfiledetails": all_mods, "total": len(all_mods)}}
    save_json("workshop", "hot_mods_combined", combined)
    return combined


def collect_individual_mods(apikey: str):
    """Fetch details for specific popular mods."""
    logging.info("=== [Workshop] Individual mod details (%d mods) ===", len(POPULAR_MODS))
    for wid in POPULAR_MODS:
        try:
            data = steam_api_call(apikey,
                interface="IPublishedFileService",
                method="QueryFiles",
                version=1,
                params={
                    "appid": DST_CLIENT_APPID,
                    "return_vote_data": "true",
                    "return_tags": "true",
                    "return_metadata": "true",
                    "numperpage": 1,
                    "page": 1,
                    "query_type": 0,
                    "search_text": wid,
                })
            save_json("workshop", f"mod_{wid}", data)
        except Exception:
            pass


def collect_search_results(apikey: str):
    """Fetch search results for common queries."""
    logging.info("=== [Workshop] Search queries (%d queries) ===", len(SEARCH_QUERIES))
    for query, name in SEARCH_QUERIES:
        logging.info("  Search: '%s'", query)
        try:
            data = steam_api_call(apikey,
                interface="IPublishedFileService",
                method="QueryFiles",
                version=1,
                params={
                    "appid": DST_CLIENT_APPID,
                    "return_vote_data": "true",
                    "return_tags": "true",
                    "return_metadata": "true",
                    "numperpage": 20,
                    "page": 1,
                    "query_type": 0,
                    "search_text": query,
                })
            save_json("workshop", name, data)
        except Exception:
            pass


# ══════════════════════════════════════════════════════════════════════
# 2. DST 版本信息
# ══════════════════════════════════════════════════════════════════════

def collect_dst_versions(apikey: str):
    """Fetch DST client and server version/build IDs from Steam."""
    logging.info("=== [Steam] DST version check ===")

    versions = {
        "collected_at": datetime.now(timezone.utc).isoformat(),
        "apps": {},
    }

    # ISteamApps/GetAppBuilds — returns current buildid
    # Note: this endpoint may require a publisher key for some apps,
    # but works with a regular API key for public apps.
    for label, appid in [("dst_client", DST_CLIENT_APPID), ("dst_server", DST_SERVER_APPID)]:
        try:
            # Try GetAppBuilds first (most accurate, gives buildid)
            data = steam_api_call(apikey,
                interface="ISteamApps",
                method="GetAppBuilds",
                version=1,
                params={"appid": appid, "count": 1})
            versions["apps"][label] = {
                "appid": appid,
                "source": "GetAppBuilds",
                "data": data,
            }
        except Exception:
            # Fallback: UpToDateCheck (less detail but always works)
            try:
                data = steam_api_call(apikey,
                    interface="ISteamApps",
                    method="UpToDateCheck",
                    version=1,
                    params={"appid": appid, "version": 0})
                versions["apps"][label] = {
                    "appid": appid,
                    "source": "UpToDateCheck",
                    "data": data,
                }
            except Exception:
                logging.warning("  Could not fetch version for %s (%d)", label, appid)
                versions["apps"][label] = {"appid": appid, "error": "both endpoints failed"}

    save_json("steam", "versions", versions)
    return versions


# ══════════════════════════════════════════════════════════════════════
# 3. DST 新闻/公告
# ══════════════════════════════════════════════════════════════════════

def collect_dst_news(apikey: str, count: int = 20):
    """Fetch DST news/announcements from Steam."""
    logging.info("=== [Steam] DST news ===")
    try:
        data = steam_api_call(apikey,
            interface="ISteamNews",
            method="GetNewsForApp",
            version=2,
            params={
                "appid": DST_CLIENT_APPID,
                "count": count,
                "maxlength": 500,
                "format": "json",
            })
        save_json("steam", "news_dst", data)
        return data
    except Exception:
        logging.warning("  Could not fetch DST news")
        return {}


# ══════════════════════════════════════════════════════════════════════
# 4. 当前在线玩家数
# ══════════════════════════════════════════════════════════════════════

def collect_player_count(apikey: str):
    """Fetch current DST player count."""
    logging.info("=== [Steam] Current player count ===")
    try:
        data = steam_api_call(apikey,
            interface="ISteamUserStats",
            method="GetNumberOfCurrentPlayers",
            version=1,
            params={"appid": DST_CLIENT_APPID})
        save_json("steam", "player_count", data)
        return data
    except Exception:
        logging.warning("  Could not fetch player count")
        return {}


# ══════════════════════════════════════════════════════════════════════
# 5. 生成汇总
# ══════════════════════════════════════════════════════════════════════

def generate_summary():
    """Generate summary.json for the developer."""
    logging.info("=== Generating summary ===")
    summary = {
        "collected_at": datetime.now(timezone.utc).isoformat(),
        "directories": {},
    }

    for subdir_name in ["workshop", "steam"]:
        subdir = OUTPUT_DIR / subdir_name
        if not subdir.exists():
            continue
        files_info = {}
        for f in sorted(subdir.glob("*.json")):
            with open(f, encoding="utf-8") as fp:
                data = json.load(fp)
            # Extract preview: first few mod titles or top-level keys
            preview = None
            published = data.get("response", {}).get("publishedfiledetails", [])
            if isinstance(published, list) and published:
                preview = [{"id": m.get("publishedfileid"), "title": (m.get("title") or "")[:80]} for m in published[:5]]
            elif "appnews" in data.get("appnews", {}):
                items = data["appnews"]["newsitems"][:3]
                preview = [{"title": n["title"][:80], "date": datetime.utcfromtimestamp(n["date"]).isoformat() if n.get("date") else None} for n in items]
            files_info[f.name] = {
                "file_size": f.stat().st_size,
                "preview": preview,
            }
        summary["directories"][subdir_name] = {
            "file_count": len(files_info),
            "files": files_info,
        }

    with open(OUTPUT_DIR / "summary.json", "w", encoding="utf-8") as f:
        json.dump(summary, f, indent=2, ensure_ascii=False)
    logging.info("Summary saved: %s", OUTPUT_DIR / "summary.json")


# ══════════════════════════════════════════════════════════════════════

def main():
    parser = argparse.ArgumentParser(description="Steam data collector for DST platform development")
    parser.add_argument("--apikey", required=True, help="Steam Web API Key (from https://steamcommunity.com/dev/apikey)")
    parser.add_argument("--output", default="data/steam_samples", help="Output directory")
    parser.add_argument("--hot-count", type=int, default=500, help="Number of hot mods to fetch (default: 500)")
    parser.add_argument("--skip-workshop", action="store_true", help="Skip Workshop data collection")
    parser.add_argument("--skip-steam", action="store_true", help="Skip Steam metadata (versions, news, players)")
    args = parser.parse_args()

    global OUTPUT_DIR
    OUTPUT_DIR = Path(args.output)
    setup_logging(OUTPUT_DIR)

    logging.info("╔══════════════════════════════════════════════╗")
    logging.info("║     Steam Data Collector for DST Platform    ║")
    logging.info("╠══════════════════════════════════════════════╣")
    logging.info("║  Output: %-35s ║", OUTPUT_DIR)
    logging.info("╚══════════════════════════════════════════════╝")

    t_start = time.time()

    # ── Workshop ────────────────────────────────────────────────
    if not args.skip_workshop:
        logging.info("")
        logging.info("━━━ PHASE 1/2: Workshop Data ━━━")
        collect_hot_mods_paginated(args.apikey, total=args.hot_count)
        collect_individual_mods(args.apikey)
        collect_search_results(args.apikey)
    else:
        logging.info("Workshop phase skipped (--skip-workshop)")

    # ── Steam Metadata ──────────────────────────────────────────
    if not args.skip_steam:
        logging.info("")
        logging.info("━━━ PHASE 2/2: Steam Metadata ━━━")
        collect_dst_versions(args.apikey)
        collect_dst_news(args.apikey)
        collect_player_count(args.apikey)
    else:
        logging.info("Steam metadata phase skipped (--skip-steam)")

    # ── Summary ─────────────────────────────────────────────────
    logging.info("")
    generate_summary()

    elapsed = time.time() - t_start
    logging.info("")
    logging.info("Done in %.1fs! Copy this directory to the dev machine:", elapsed)
    logging.info("  %s", OUTPUT_DIR.absolute())


if __name__ == "__main__":
    main()
