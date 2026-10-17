#!/usr/bin/env python3
"""
Generate a self-contained HTML report from a JUnit XML file.

Used by the run-tests-dood skill (Unix mirror of generate-junit-html-report.ps1).
No external dependencies: only stdlib (xml.etree, html, argparse).
"""
from __future__ import annotations

import argparse
import html
import os
import sys
from datetime import datetime
from xml.etree import ElementTree as ET


def fmt_duration(seconds: float) -> str:
    if seconds >= 1:
        return f"{seconds:.2f} s"
    return f"{int(round(seconds * 1000))} ms"


def to_int(node, name: str, default: int = 0) -> int:
    value = node.get(name) if node is not None else None
    try:
        return int(value) if value else default
    except ValueError:
        return default


def to_float(node, name: str, default: float = 0.0) -> float:
    value = node.get(name) if node is not None else None
    try:
        return float(value) if value else default
    except ValueError:
        return default


CSS = """
:root { color-scheme: light dark; }
* { box-sizing: border-box; }
body { margin: 0; font: 14px/1.45 -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background: #fafafa; color: #1f2328; }
@media (prefers-color-scheme: dark) { body { background: #0d1117; color: #e6edf3; } }
header { padding: 16px 24px; border-bottom: 1px solid rgba(127,127,127,0.25); display: flex; flex-wrap: wrap; align-items: baseline; gap: 16px; }
header h1 { margin: 0; font-size: 18px; }
.summary { display: flex; flex-wrap: wrap; gap: 10px; font-size: 13px; }
.summary span { padding: 2px 8px; border-radius: 999px; background: rgba(127,127,127,0.15); }
.summary .pass { background: rgba(46,160,67,0.18); color: #1a7f37; }
.summary .fail { background: rgba(207,34,46,0.18); color: #cf222e; }
.summary .skip { background: rgba(154,103,0,0.18); color: #9a6700; }
.controls { padding: 12px 24px; display: flex; gap: 8px; align-items: center; flex-wrap: wrap; border-bottom: 1px solid rgba(127,127,127,0.25); }
.controls input[type=search] { flex: 1; min-width: 200px; padding: 6px 10px; border-radius: 6px; border: 1px solid rgba(127,127,127,0.4); background: transparent; color: inherit; }
.controls button { padding: 6px 10px; border-radius: 6px; border: 1px solid rgba(127,127,127,0.4); background: transparent; color: inherit; cursor: pointer; }
.controls button.active { background: rgba(127,127,127,0.2); }
main { padding: 16px 24px 80px; }
details.suite { margin: 0 0 12px; border: 1px solid rgba(127,127,127,0.25); border-radius: 8px; background: rgba(255,255,255,0.6); }
@media (prefers-color-scheme: dark) { details.suite { background: rgba(255,255,255,0.04); } }
details.suite > summary { cursor: pointer; padding: 10px 14px; list-style: none; display: flex; gap: 10px; align-items: center; font-weight: 600; }
details.suite > summary::-webkit-details-marker { display: none; }
details.suite > summary::before { content: '\\25B6'; font-size: 10px; opacity: 0.6; transition: transform .15s ease; }
details.suite[open] > summary::before { transform: rotate(90deg); }
.suite-stats { margin-left: auto; font-weight: 400; font-size: 12px; opacity: 0.8; }
.tests { list-style: none; margin: 0; padding: 0 14px 12px; }
.tests li { padding: 6px 0; border-top: 1px solid rgba(127,127,127,0.12); display: flex; flex-wrap: wrap; gap: 10px; align-items: baseline; }
.tests li:first-child { border-top: none; }
.tests .name { flex: 1; }
.tests .time { font-variant-numeric: tabular-nums; opacity: 0.7; font-size: 12px; }
.tests .status { font-size: 12px; padding: 1px 8px; border-radius: 999px; }
.tests .status.pass { background: rgba(46,160,67,0.18); color: #1a7f37; }
.tests .status.fail { background: rgba(207,34,46,0.18); color: #cf222e; }
.tests .status.skip { background: rgba(154,103,0,0.18); color: #9a6700; }
.failure { width: 100%; margin: 6px 0 4px; padding: 10px 12px; border-radius: 6px; background: rgba(207,34,46,0.08); border-left: 3px solid #cf222e; overflow: auto; }
.failure pre { margin: 4px 0 0; white-space: pre-wrap; word-break: break-word; font-size: 12px; }
footer { padding: 16px 24px; font-size: 12px; opacity: 0.6; }
.hidden { display: none !important; }
"""

JS = """
(function () {
  const search = document.getElementById('search');
  const buttons = document.querySelectorAll('.controls button[data-filter]');
  let activeFilter = 'all';

  function applyFilters() {
    const term = (search.value || '').toLowerCase();
    document.querySelectorAll('details.suite').forEach((suite) => {
      let suiteVisible = 0;
      suite.querySelectorAll('.tests > li').forEach((li) => {
        const matchesFilter = activeFilter === 'all'
          || li.dataset.status === activeFilter
          || (activeFilter === 'failed' && (li.dataset.status === 'fail' || li.dataset.status === 'error'));
        const matchesSearch = term === '' || li.dataset.search.indexOf(term) !== -1;
        const visible = matchesFilter && matchesSearch;
        li.classList.toggle('hidden', !visible);
        if (visible) suiteVisible += 1;
      });
      suite.classList.toggle('hidden', suiteVisible === 0);
      if (activeFilter === 'failed' && suiteVisible > 0) suite.open = true;
    });
  }

  search.addEventListener('input', applyFilters);
  buttons.forEach((btn) => {
    btn.addEventListener('click', () => {
      buttons.forEach((b) => b.classList.toggle('active', b === btn));
      activeFilter = btn.dataset.filter;
      applyFilters();
    });
  });

  document.getElementById('expand-all').addEventListener('click', () => {
    document.querySelectorAll('details.suite').forEach((d) => { d.open = true; });
  });
  document.getElementById('collapse-all').addEventListener('click', () => {
    document.querySelectorAll('details.suite').forEach((d) => { d.open = false; });
  });
})();
"""


def render(junit_path: str, output_path: str, title: str) -> None:
    tree = ET.parse(junit_path)
    root = tree.getroot()
    if root.tag == "testsuites":
        suites = list(root.findall("testsuite"))
        root_node = root
    elif root.tag == "testsuite":
        suites = [root]
        root_node = root
    else:
        raise SystemExit(f"Unrecognized JUnit XML root in {junit_path}: <{root.tag}>")

    root_tests = to_int(root_node, "tests")
    root_failures = to_int(root_node, "failures")
    root_errors = to_int(root_node, "errors")
    root_skipped = to_int(root_node, "skipped")
    root_time = to_float(root_node, "time")
    if root_tests == 0:
        for s in suites:
            root_tests += to_int(s, "tests")
            root_failures += to_int(s, "failures")
            root_errors += to_int(s, "errors")
            root_skipped += to_int(s, "skipped")
            root_time += to_float(s, "time")
    root_passed = max(0, root_tests - root_failures - root_errors - root_skipped)
    status_class = "fail" if (root_failures + root_errors) > 0 else "pass"

    parts: list[str] = []
    add = parts.append
    add("<!doctype html>\n<html lang=\"en\"><head><meta charset=\"utf-8\">")
    add(f"<title>{html.escape(title)}</title>")
    add(f"<style>{CSS}</style></head><body>")

    add("<header>")
    add(f"<h1>{html.escape(title)}</h1>")
    add("<div class=\"summary\">")
    add(f"<span class=\"{status_class}\">{root_tests} tests</span>")
    add(f"<span class=\"pass\">{root_passed} passed</span>")
    if root_failures:
        add(f"<span class=\"fail\">{root_failures} failed</span>")
    if root_errors:
        add(f"<span class=\"fail\">{root_errors} errors</span>")
    if root_skipped:
        add(f"<span class=\"skip\">{root_skipped} skipped</span>")
    add(f"<span>{fmt_duration(root_time)}</span>")
    add("</div></header>")

    add("<div class=\"controls\">")
    add("<input id=\"search\" type=\"search\" placeholder=\"Filter by test or suite name...\" autofocus>")
    add("<button type=\"button\" data-filter=\"all\" class=\"active\">All</button>")
    add("<button type=\"button\" data-filter=\"failed\">Failures</button>")
    add("<button type=\"button\" data-filter=\"skip\">Skipped</button>")
    add("<button type=\"button\" id=\"expand-all\">Expand all</button>")
    add("<button type=\"button\" id=\"collapse-all\">Collapse all</button>")
    add("</div>")

    add("<main>")
    for suite in suites:
        s_name = suite.get("name") or "(suite)"
        s_tests = to_int(suite, "tests")
        s_failures = to_int(suite, "failures")
        s_errors = to_int(suite, "errors")
        s_skipped = to_int(suite, "skipped")
        s_time = to_float(suite, "time")
        open_attr = " open" if (s_failures + s_errors) > 0 else ""
        add(f"<details class=\"suite\"{open_attr}>")
        add("<summary>")
        add(html.escape(s_name))
        stats = [f"{s_tests} tests"]
        if s_failures:
            stats.append(f"{s_failures} failed")
        if s_errors:
            stats.append(f"{s_errors} errors")
        if s_skipped:
            stats.append(f"{s_skipped} skipped")
        stats.append(fmt_duration(s_time))
        add("<span class=\"suite-stats\">" + " &middot; ".join(stats) + "</span></summary>")

        add("<ul class=\"tests\">")
        for case in suite.findall("testcase"):
            c_name = case.get("name") or "(unnamed)"
            c_class = case.get("classname") or ""
            c_time = to_float(case, "time")
            failure = case.find("failure")
            error = case.find("error")
            skipped = case.find("skipped")
            status = "pass"
            if failure is not None:
                status = "fail"
            elif error is not None:
                status = "error"
            elif skipped is not None:
                status = "skip"
            search_key = f"{c_class} {c_name}".lower().strip()
            pill_class = "fail" if status == "error" else status
            add(f"<li data-status=\"{status}\" data-search=\"{html.escape(search_key, quote=True)}\">")
            add(f"<span class=\"status {pill_class}\">{status}</span>")
            add(f"<span class=\"name\">{html.escape(c_name)}</span>")
            add(f"<span class=\"time\">{fmt_duration(c_time)}</span>")
            err = failure if failure is not None else error
            if err is not None:
                msg = err.get("message", "")
                etype = err.get("type", "")
                body = (err.text or "").strip()
                add("<div class=\"failure\">")
                if etype or msg:
                    add(f"<strong>{html.escape(etype)}</strong>" + (f": {html.escape(msg)}" if msg else ""))
                if body:
                    add(f"<pre>{html.escape(body)}</pre>")
                add("</div>")
            add("</li>")
        add("</ul></details>")
    add("</main>")
    add(f"<footer>Generated {html.escape(datetime.now().strftime('%Y-%m-%d %H:%M:%S %z') or datetime.now().isoformat())} from <code>{html.escape(os.path.basename(junit_path))}</code></footer>")
    add(f"<script>{JS}</script></body></html>")

    out_dir = os.path.dirname(output_path)
    if out_dir:
        os.makedirs(out_dir, exist_ok=True)
    with open(output_path, "w", encoding="utf-8") as f:
        f.write("".join(parts))
    print(f"Generated {output_path} ({root_tests} tests, {root_failures} failures, {root_errors} errors)")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--junit", required=True, help="Path to the JUnit XML file")
    parser.add_argument("--output", required=True, help="Path to the HTML output")
    parser.add_argument("--title", default="Test report", help="Page title")
    args = parser.parse_args()
    render(args.junit, args.output, args.title)
    return 0


if __name__ == "__main__":
    sys.exit(main())
