"""Check that local Markdown links and HTML images will exist in a Git checkout."""

import re
import subprocess
from pathlib import Path
from urllib.parse import unquote, urlsplit

ROOT = Path(__file__).resolve().parents[1]
tracked = set(subprocess.check_output(
    ["git", "ls-files", "-z"], cwd=ROOT, text=True
).split("\0"))
errors = []
checked = 0
for name in sorted(tracked):
    if not name.endswith(".md"):
        continue
    document = ROOT / name
    content = document.read_text(encoding="utf-8")
    links = re.findall(r"\]\(([^)]+)\)", content)
    links += re.findall(r'''(?:src|href)=["']([^"']+)["']''', content)
    for link in links:
        url = urlsplit(link)
        if url.scheme or url.netloc or not url.path:
            continue
        target = (document.parent / unquote(url.path)).resolve()
        checked += 1
        try:
            relative = target.relative_to(ROOT).as_posix()
        except ValueError:
            errors.append(f"{name}: link leaves repository: {link}")
            continue
        if not target.exists() or relative not in tracked:
            errors.append(f"{name}: target missing from Git: {link}")

if errors:
    raise SystemExit("\n".join(errors))
print(f"Verified {checked} local documentation links against tracked Git files.")
