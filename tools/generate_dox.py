#!/usr/bin/env python3
from __future__ import annotations

import re
import subprocess
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, List, Optional


ROOT = Path(__file__).resolve().parents[1]


def run(args: List[str], cwd: Optional[Path] = None) -> str:
    proc = subprocess.run(
        args,
        cwd=str(cwd) if cwd else None,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        encoding="utf-8",
        errors="replace",
        check=False,
    )
    if proc.returncode != 0:
        raise RuntimeError(f"Command failed ({proc.returncode}): {' '.join(args)}\n{proc.stdout}")
    return proc.stdout


def git_ls_files() -> List[str]:
    out = run(["git", "ls-files"], cwd=ROOT)
    return [line.strip() for line in out.splitlines() if line.strip()]


def dox_name_for_path(relpath: str) -> str:
    p = relpath.replace("\\", "/")
    p = p.replace("/", "__")
    p = p.replace(".", "_")
    return f"dox__{p}_dox.md"


def strip_java_comments(src: str) -> str:
    src = re.sub(r"/\*.*?\*/", "", src, flags=re.DOTALL)
    src = re.sub(r"//.*?$", "", src, flags=re.MULTILINE)
    return src


def _collapse_ws(s: str) -> str:
    return re.sub(r"\s+", " ", s).strip()


@dataclass(frozen=True)
class JavaApi:
    package: Optional[str]
    types: List[str]
    public_methods: List[str]
    protected_methods: List[str]
    public_fields: List[str]


def parse_java_api(path: Path) -> JavaApi:
    raw = path.read_text(encoding="utf-8", errors="replace")
    src = strip_java_comments(raw)

    pkg_m = re.search(r"^\s*package\s+([a-zA-Z0-9_.]+)\s*;\s*$", src, flags=re.MULTILINE)
    package = pkg_m.group(1) if pkg_m else None

    types: List[str] = []
    for m in re.finditer(r"\b(class|interface|enum)\s+([A-Za-z_][A-Za-z0-9_]*)\b", src):
        types.append(m.group(2))
    seen = set()
    types = [t for t in types if not (t in seen or seen.add(t))]

    method_re = re.compile(
        r"\b(?P<vis>public|protected)\s+"
        r"(?P<mods>(?:static|final|synchronized|abstract|native|strictfp)\s+)*"
        r"(?P<ret>[A-Za-z0-9_.$<>,\[\]\s?]+?)\s+"
        r"(?P<name>[A-Za-z_][A-Za-z0-9_]*)\s*"
        r"\((?P<args>[^)]*)\)\s*"
        r"(?P<tail>throws\s+[^;{]+)?\s*"
        r"(?P<end>[;{])",
        flags=re.MULTILINE,
    )

    public_methods: List[str] = []
    protected_methods: List[str] = []
    for m in method_re.finditer(src):
        vis = m.group("vis")
        mods = _collapse_ws(m.group("mods") or "")
        ret = _collapse_ws(m.group("ret"))
        name = m.group("name")
        args = _collapse_ws(m.group("args"))
        tail = _collapse_ws(m.group("tail") or "")
        sig = f"{vis} {((mods + ' ') if mods else '')}{ret} {name}({args})"
        if tail:
            sig += f" {tail}"
        sig = _collapse_ws(sig)
        (public_methods if vis == "public" else protected_methods).append(sig)

    # Public fields/constants (heuristic).
    field_re = re.compile(
        r"^\s*public\s+"
        r"(?P<mods>(?:static|final|transient|volatile)\s+)*"
        r"(?P<type>[A-Za-z0-9_.$<>,\[\]\s?]+?)\s+"
        r"(?P<name>[A-Za-z_][A-Za-z0-9_]*)\s*(?:=|;)",
        flags=re.MULTILINE,
    )
    public_fields: List[str] = []
    for m in field_re.finditer(src):
        mods = _collapse_ws(m.group("mods") or "")
        typ = _collapse_ws(m.group("type"))
        name = m.group("name")
        public_fields.append(_collapse_ws(f"public {((mods + ' ') if mods else '')}{typ} {name}"))

    def dedup(items: Iterable[str]) -> List[str]:
        s = set()
        out: List[str] = []
        for it in items:
            if it in s:
                continue
            s.add(it)
            out.append(it)
        return out

    return JavaApi(
        package=package,
        types=types,
        public_methods=dedup(public_methods),
        protected_methods=dedup(protected_methods),
        public_fields=dedup(public_fields),
    )


def write_text(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8", newline="\n")


def render_dox(relpath: str) -> str:
    path = (ROOT / relpath).resolve()
    ext = path.suffix.lower()
    lines: List[str] = []
    lines.append(f"# DOX: `{relpath}`")
    lines.append("")

    if ext == ".java":
        api = parse_java_api(path)
        lines.append("## 包与类型")
        lines.append(f"- `package`: `{api.package or '(none)'}`")
        if api.types:
            lines.append("- 声明的类型：")
            for t in api.types:
                lines.append(f"  - `{t}`")
        else:
            lines.append("- 声明的类型：无")
        lines.append("")

        lines.append("## 对外接口（public/protected）")
        if api.public_fields:
            lines.append("- `public` 字段：")
            for f in api.public_fields:
                lines.append(f"  - `{f}`")
        if api.public_methods:
            lines.append("- `public` 方法：")
            for m in api.public_methods:
                lines.append(f"  - `{m}`")
        if api.protected_methods:
            lines.append("- `protected` 方法：")
            for m in api.protected_methods:
                lines.append(f"  - `{m}`")
        if not (api.public_fields or api.public_methods or api.protected_methods):
            lines.append("- 无（或全部为包内/私有实现）")
        lines.append("")
        lines.append("## 维护者备注")
        lines.append("- 参考仓库根目录的 `stealthpath_*_dox.md` 获取更高层说明（架构/OverlayUI/发布）。")
        lines.append("")
        return "\n".join(lines).rstrip() + "\n"

    # Non-java: minimal stub.
    lines.append("## 说明")
    if ext in {".properties"}:
        lines.append("- 类型：Mindustry bundle（本地化文本）。")
        lines.append("- 作用：提供设置项/提示/标题等多语言文本。")
    elif ext in {".json"}:
        lines.append("- 类型：JSON 配置/元数据。")
    elif ext in {".gradle"}:
        lines.append("- 类型：Gradle 构建脚本。")
    elif ext in {".md"}:
        lines.append("- 类型：Markdown 文档。")
    else:
        lines.append(f"- 类型：`{ext or '(no ext)'}` 文件。")
    lines.append("")
    return "\n".join(lines).rstrip() + "\n"


def main() -> None:
    files = git_ls_files()
    index_lines: List[str] = []
    index_lines.append("# DOX Index")
    index_lines.append("")
    index_lines.append("本目录下的 `*_dox.md` 为仓库中文档/接口说明索引。")
    index_lines.append("")
    index_lines.append("## 重新生成")
    index_lines.append("```bash")
    index_lines.append("python tools/generate_dox.py")
    index_lines.append("```")
    index_lines.append("")
    index_lines.append("## 文件映射")

    for rel in files:
        dox_name = dox_name_for_path(rel)
        dox_path = ROOT / dox_name
        write_text(dox_path, render_dox(rel))
        index_lines.append(f"- `{rel}` -> `{dox_name}`")

    write_text(ROOT / "INDEX_dox.md", "\n".join(index_lines).rstrip() + "\n")


if __name__ == "__main__":
    main()

