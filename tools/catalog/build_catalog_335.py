#!/usr/bin/env python3
"""Builds catalog-335.json, the addon catalog for the 3.3.5a private server game version.

Most 3.3.5a backports never publish releases, so each entry is a snapshot of a branch: the
download is the commit's source zip and the commit is the file id, so a new commit shows up as
an update. A repo is kept only when the folders the app would install have a TOC for
Interface 303xx.

    python3 tools/catalog/build_catalog_335.py catalog/wotlk335.json out/catalog-335.json
"""
import datetime
import json
import os
import posixpath
import re
import sys
import urllib.error
import urllib.parse
import urllib.request

API = "https://api.github.com"
KEY = "wotlk335"
INTERFACE = re.compile(r"^##\s*Interface:\s*303\d\d\b", re.MULTILINE)


def fetch(url, accept="application/vnd.github+json"):
    req = urllib.request.Request(url, headers={"Accept": accept, "User-Agent": "WoWAndroWhere-catalog"})
    token = os.environ.get("GITHUB_TOKEN")
    if token and url.startswith(API):
        req.add_header("Authorization", f"Bearer {token}")
    with urllib.request.urlopen(req, timeout=30) as resp:
        return resp.read()


def get(url):
    return json.loads(fetch(url))


def addon_tocs(paths):
    """The TOCs the game would load from the zip, following AddonArchive: a TOC at the top makes the
    repo one addon, otherwise each top-level folder is one."""
    tocs = [p for p in paths if p.lower().endswith(".toc")]
    top = [p for p in tocs if "/" not in p]
    if top:
        return [min(top, key=len)]
    by_dir = {}
    for p in tocs:
        parts = p.split("/")
        if len(parts) == 2:
            by_dir.setdefault(parts[0], []).append(p)
    return [min(v, key=len) for v in by_dir.values()]


def build_entry(entry):
    repo = entry["repo"]
    info = get(f"{API}/repos/{repo}")
    branch = entry.get("branch") or info["default_branch"]
    commit = get(f"{API}/repos/{repo}/commits/{urllib.parse.quote(branch, safe='')}")
    sha = commit["sha"]
    tree = get(f"{API}/repos/{repo}/git/trees/{sha}?recursive=1")
    paths = [t["path"] for t in tree.get("tree", []) if t.get("type") == "blob"]
    tocs = addon_tocs(paths)
    if not tocs:
        raise ValueError("no addon TOC at the top or one folder down")
    if not any(INTERFACE.search(fetch(f"https://raw.githubusercontent.com/{repo}/{sha}/{urllib.parse.quote(t)}", accept="*/*").decode("utf-8", "replace")) for t in tocs):
        raise ValueError(f"no Interface 303xx in {', '.join(posixpath.basename(t) for t in tocs)}")
    date = commit["commit"]["committer"]["date"][:10]
    owner = repo.split("/")[0]
    return {
        "repo": repo,
        "name": entry.get("name") or info.get("name"),
        "summary": entry.get("summary") or info.get("description") or "",
        "category": entry.get("category"),
        "author": owner,
        "icon": info.get("owner", {}).get("avatar_url") or f"https://github.com/{owner}.png",
        "website": info.get("html_url") or f"https://github.com/{repo}",
        "stars": info.get("stargazers_count", 0),
        "downloads": 0,
        "updated": commit["commit"]["committer"]["date"],
        "requires": entry.get("requires", []),
        "files": {KEY: {"fileId": sha, "version": f"{date} ({sha[:7]})", "url": f"https://github.com/{repo}/archive/{sha}.zip"}},
    }


def main():
    src, dest = sys.argv[1], sys.argv[2]
    with open(src) as f:
        sources = json.load(f)
    out = []
    for entry in sources["addons"]:
        try:
            out.append(build_entry(entry))
            print(f"ok   {entry['repo']}", file=sys.stderr)
        except (urllib.error.URLError, ValueError, KeyError) as e:
            print(f"skip {entry['repo']}: {e}", file=sys.stderr)
    catalog = {"generated": datetime.datetime.now(datetime.timezone.utc).isoformat(timespec="seconds"), "addons": out}
    os.makedirs(os.path.dirname(dest) or ".", exist_ok=True)
    with open(dest, "w") as f:
        json.dump(catalog, f, indent=1)
        f.write("\n")
    print(f"{len(out)} of {len(sources['addons'])} addons -> {dest}", file=sys.stderr)


if __name__ == "__main__":
    main()
