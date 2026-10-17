// Duplicated JavaScript / TypeScript / JSX blocks (CRBPM UI).
//
// The Java lane fingerprints method bodies through IntelliJ PSI, which does not
// see frontend sources. This script is the frontend counterpart: it normalizes
// each file into significant lines (comments, blank lines and lines made only of
// punctuation are dropped, whitespace is collapsed) and reports maximal blocks of
// at least `--min-lines` identical normalized lines, inside a file or across the
// compared files.
//
// Identifiers are kept as written, so only exact copies are reported. Renamed
// variables and reordered statements still need the manual review checklist.
//
// Run from the repo root with the locally modified frontend files plus untouched
// siblings from the same folders as read-only comparison targets:
//
//   node .cursor/skills/solid-review/scripts/duplicated-js-blocks.mjs \
//     crbpm/ui/src/pages/SomePage.jsx crbpm/ui/src/pages/OtherPage.jsx
//
// Options: `--min-lines <n>` (default 6). Always exits 0 when the analysis ran,
// so the agent reads findings rather than an exit code.

import { readFileSync } from 'node:fs'
import path from 'node:path'

const DEFAULT_MIN_LINES = 6
const PUNCTUATION_ONLY = /^[\s{}()[\];,.<>/=+&|?:'"`-]*$/

function parseArguments(argv) {
  let minLines = DEFAULT_MIN_LINES
  const files = []
  for (let index = 0; index < argv.length; index += 1) {
    const argument = argv[index]
    if (argument === '--min-lines') {
      minLines = Number.parseInt(argv[index + 1] ?? '', 10)
      index += 1
      continue
    }
    if (argument.startsWith('--min-lines=')) {
      minLines = Number.parseInt(argument.slice('--min-lines='.length), 10)
      continue
    }
    files.push(argument)
  }
  return { minLines, files }
}

/**
 * Comment stripping is textual: a `//` or `/* *` sequence inside a string or a
 * regex literal is removed as if it were a comment. That can only merge two
 * lines into one normalized line, never invent a duplicate across files.
 */
function normalize(source) {
  const withoutBlockComments = source.replace(/\/\*[\s\S]*?\*\//g, '\n')
  return withoutBlockComments.split(/\r?\n/).map((rawLine) => {
    const withoutLineComment = rawLine.replace(/\/\/.*$/, '')
    return withoutLineComment.trim().replace(/\s+/g, ' ')
  })
}

function significantLinesOf(filePath) {
  const source = readFileSync(filePath, 'utf8')
  const lines = []
  normalize(source).forEach((text, zeroBasedIndex) => {
    if (text.length === 0 || PUNCTUATION_ONLY.test(text)) {
      return
    }
    lines.push({ text, sourceLine: zeroBasedIndex + 1 })
  })
  return lines
}

function indexByText(lines) {
  const index = new Map()
  lines.forEach((line, position) => {
    const positions = index.get(line.text)
    if (positions) {
      positions.push(position)
    } else {
      index.set(line.text, [position])
    }
  })
  return index
}

function findBlocks(left, right, minLines) {
  const sameFile = left === right
  const rightIndex = indexByText(right.lines)
  const reportedDiagonals = new Set()
  const blocks = []

  for (let leftStart = 0; leftStart < left.lines.length; leftStart += 1) {
    const candidates = rightIndex.get(left.lines[leftStart].text) ?? []
    for (const rightStart of candidates) {
      if (sameFile && rightStart <= leftStart) {
        continue
      }
      if (reportedDiagonals.has(`${leftStart},${rightStart}`)) {
        continue
      }
      let length = 0
      while (
        leftStart + length < left.lines.length &&
        rightStart + length < right.lines.length &&
        left.lines[leftStart + length].text === right.lines[rightStart + length].text &&
        !(sameFile && leftStart + length === rightStart)
      ) {
        length += 1
      }
      if (length < minLines) {
        continue
      }
      for (let offset = 0; offset < length; offset += 1) {
        reportedDiagonals.add(`${leftStart + offset},${rightStart + offset}`)
      }
      blocks.push({
        length,
        left: {
          file: left.file,
          from: left.lines[leftStart].sourceLine,
          to: left.lines[leftStart + length - 1].sourceLine,
        },
        right: {
          file: right.file,
          from: right.lines[rightStart].sourceLine,
          to: right.lines[rightStart + length - 1].sourceLine,
        },
      })
    }
  }
  return blocks
}

const { minLines, files } = parseArguments(process.argv.slice(2))

if (files.length === 0 || !Number.isInteger(minLines) || minLines < 2) {
  process.stderr.write(
    'Usage: node duplicated-js-blocks.mjs [--min-lines <n>] <file> [<file> ...]\n',
  )
  process.exit(2)
}

const analyzedFiles = files.map((filePath) => ({
  file: path.relative(process.cwd(), path.resolve(filePath)).split(path.sep).join('/'),
  lines: significantLinesOf(filePath),
}))

const blocks = []
for (let leftIndex = 0; leftIndex < analyzedFiles.length; leftIndex += 1) {
  for (let rightIndex = leftIndex; rightIndex < analyzedFiles.length; rightIndex += 1) {
    blocks.push(...findBlocks(analyzedFiles[leftIndex], analyzedFiles[rightIndex], minLines))
  }
}

blocks.sort((first, second) => second.length - first.length)

process.stdout.write(
  `Compared ${analyzedFiles.length} file(s), minimum block size ${minLines} normalized lines.\n`,
)
if (blocks.length === 0) {
  process.stdout.write('No duplicated block found.\n')
} else {
  process.stdout.write(`${blocks.length} duplicated block(s):\n`)
  blocks.forEach((block) => {
    process.stdout.write(
      `\n  ${block.length} normalized lines\n` +
        `    ${block.left.file}:${block.left.from}-${block.left.to}\n` +
        `    ${block.right.file}:${block.right.from}-${block.right.to}\n`,
    )
  })
  process.stdout.write(
    '\nExact copies only. Report a block when both copies encode the same rule and must change together.\n',
  )
}
