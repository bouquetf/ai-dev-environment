// Structural SOLID smells for JavaScript / JSX sources (CRBPM UI).
//
// The IntelliJ `inspection.kts` API used by the Java lane only sees Java/Kotlin
// PSI, so frontend structure is measured with core ESLint metric rules instead.
// This config intentionally contains no imports so it resolves nothing outside
// the ESLint installation already present in `crbpm/ui/node_modules`.
//
// Run from `crbpm/ui`, review-only (never with `--fix`):
//
//   npx eslint --no-config-lookup \
//     --config ../../.cursor/skills/solid-review/scripts/frontend-structure.eslint.config.mjs \
//     src/pages/SomePage.jsx src/utils/someHelper.js
//
// Every rule is a warning: the command exits 0 and the agent reads the report
// rather than a failure code. Thresholds are heuristics meant to surface
// candidates for a human decision, not hard rules.
//
// TypeScript: `.ts` / `.tsx` need a TypeScript-aware parser, which is not a
// `crbpm/ui` dependency today. Add `typescript-eslint` to the UI devDependencies
// and set `languageOptions.parser` here before extending `files` to those
// extensions; the duplication script is line-based and already handles them.

export default [
  {
    files: ['**/*.{js,jsx,mjs,cjs}'],
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      parserOptions: {
        ecmaFeatures: { jsx: true },
      },
    },
    linterOptions: {
      reportUnusedDisableDirectives: false,
    },
    rules: {
      // A component or helper doing too much: split it or lift logic into a hook.
      'max-lines-per-function': [
        'warn',
        { max: 120, skipBlankLines: true, skipComments: true, IIFEs: true },
      ],
      'max-statements': ['warn', 25],
      // JSX conditionals count here, so the budget is wider than for plain logic.
      'complexity': ['warn', { max: 15 }],

      // Readability: deep nesting, callback pyramids and stacked ternaries.
      'max-depth': ['warn', 3],
      'max-nested-callbacks': ['warn', 3],
      'no-nested-ternary': 'warn',

      // Wide signatures: introduce an options object or a dedicated type.
      'max-params': ['warn', 5],

      // A module accumulating unrelated exports (SRP at file level).
      'max-lines': ['warn', { max: 400, skipBlankLines: true, skipComments: true }],
    },
  },
]
