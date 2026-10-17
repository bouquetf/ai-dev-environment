import { mkdir, readFile, rm, writeFile } from 'node:fs/promises'
import { spawnSync } from 'node:child_process'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = dirname(fileURLToPath(import.meta.url))
const sourcePath = join(root, 'slide-deck.md')
const generatedPath = join(root, '.slide-deck.generated.md')
const workDir = join(root, '.talk-deck-mermaid')
const assetDir = join(root, 'assets', 'generated')
const source = await readFile(sourcePath, 'utf8')
const blocks = [...source.matchAll(/```mermaid\s*\r?\n([\s\S]*?)```/g)]

await rm(workDir, { recursive: true, force: true })
await rm(assetDir, { recursive: true, force: true })
await mkdir(workDir, { recursive: true })
await mkdir(assetDir, { recursive: true })

let generated = source
for (let index = blocks.length - 1; index >= 0; index -= 1) {
  const [block, diagram] = blocks[index]
  const number = index + 1
  const input = join(workDir, `diagram-${number}.mmd`)
  const output = join(assetDir, `diagram-${number}.svg`)
  const alt = diagram.match(/^\s*%%\s*alt:\s*(.+)$/m)?.[1]?.trim()
    ?? `Diagram ${number}`

  await writeFile(input, diagram, 'utf8')
  const quote = (value) => `"${value.replaceAll('"', '')}"`
  const command = [
    'npx --yes @mermaid-js/mermaid-cli@latest',
    '-i', quote(input),
    '-o', quote(output),
    '-b transparent',
  ].join(' ')
  const result = spawnSync(command, {
    cwd: root,
    encoding: 'utf8',
    shell: true,
    windowsHide: true,
  })

  if (result.error || result.status !== 0) {
    const details = [result.error?.message, result.stdout, result.stderr]
      .filter(Boolean)
      .join('\n')
    throw new Error(`Mermaid conversion failed for diagram ${number}\n${details}`)
  }

  const image = `![${alt}](assets/generated/diagram-${number}.svg)`
  generated = `${generated.slice(0, blocks[index].index)}${image}${generated.slice(blocks[index].index + block.length)}`
}

await writeFile(generatedPath, generated, 'utf8')
await rm(workDir, { recursive: true, force: true })
process.stdout.write(`${resolve(generatedPath)}\n`)
