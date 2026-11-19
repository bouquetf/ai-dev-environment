# Usage - AI Assistant Improver

## Open the settings page

Open **Settings / Preferences** (`Ctrl+Alt+S`), then the **AI Assistant Improver** section.

## Assistants

The page shows the fixed catalog: **Cursor**, **Claude**, **OpenCode**, **Junie**. No assistant is active until it is checked.

- Checking an assistant enables it for the open project.
- Unchecking an assistant disables it and leaves files already installed for that assistant in place.
- Only these four identifiers are offered. An identifier outside the catalog is not shown and is refused by the model.

## Language

The language choice offers **English** (the starting value) and **French**. This setting applies to the whole IDE, for every project.

The language label and the list are on the same line, aligned on the left. The list stays the size of its selected value: as wide as that value and as tall as the label. It does not stretch across the page.

Enabled assistants are a setting of the open project: each project has its own list of active assistants.

Both settings are changed on this same page. Displayed labels (titles, assistant names, messages) come from the bundle of the chosen language.

## Repositories

On this same page, enter a local directory or a URL and choose **Add**. **Browse** always opens a directory chooser. It does not accept a URL.

**Add** accepts either:

- a local directory that contains `.git`, or
- a `git`, `http`, or `https` URL with a host and without a user name or password in the URL, or
- an SCP address `git@host:path`, for example `git@github.com:owner/name.git`.

The scheme is stored in lowercase. `HTTPS://Host/Repo.git` is kept as `https://Host/Repo.git`. `git@host:path` is kept as written. A different scheme, `ssh://`, `file://`, a URL or SCP address that contains a password, an empty field, a file, a directory without `.git`, or an address already listed is refused. The message is shown on the page and the repository is not added. An empty field asks for a directory that contains `.git`, or a `git`, `http`, or `https` URL.

The plugin clones an accepted URL into the IDE project cache, outside the open project. The list shows the URL, not the cache path. A local directory stays where it is and is not copied into that cache. The same retained URL a second time is a duplicate and is not cloned again.

The initial mode is download only. The user can switch that repository to synchronisation, then back to download only. The mode applies to that repository.

Several repositories can appear in the list.

Remove takes the repository off the list. Removing a URL also deletes the plugin's clone and does not delete files already installed in the open project. Removing a local directory does not delete that directory. Files already installed in the open project stay in place either way.

Enabled assistants and repositories are settings of the open project. Language remains a setting of the whole IDE.

## Components

For each added repository, the page lists the skills, agents, and rules it finds.

- A skill is a folder that contains `SKILL.md`, under `.cursor/skills`, `.agents/skills`, `.claude/skills`, `.opencode/skills`, `.junie/skills`, or `ia/skills`.
- An agent is a `.md` file in an `agents` folder of `.cursor`, `.claude`, `.opencode`, or `.junie`.
- A rule is a `.md` or `.mdc` file in a `rules` folder of those same four roots, or `CLAUDE.md` at the repository root, or `.junie/guidelines.md`.
- Two skills with the same folder name at two paths stay two entries.
- Every component starts selected. Unchecking one is kept the next time the repository is read. A component that appears later starts selected.
- A repository with nothing recognized shows an empty list and no error.
- The selection is a setting of the open project, together with the enabled assistants and the repositories.

## Import

**Import** copies each selected component into the open project, for every assistant that is checked on the page.

- A skill is copied to `<assistant>/skills/<folder>/`. The folder name is the directory that contains `SKILL.md`. `ia/skills/specification-fr/` becomes `.cursor/skills/specification-fr/` when Cursor is checked, and the same folder under `.claude`, `.opencode`, and `.junie` when those assistants are checked.
- An agent is copied to `<assistant>/agents/` under its file name only. A rule that comes from a `rules` directory is copied to `<assistant>/rules/` and keeps its path under `rules/`.
- `CLAUDE.md` is copied to the project root only when Claude is checked. `.junie/guidelines.md` is copied to `.junie/guidelines.md` only when Junie is checked.
- An enabled assistant with no target for a component is skipped for that component. The page says so, and the other targets are still copied.
- The first time a destination is missing, the copy runs without a question. The plugin records a snapshot of the installed files.
- When the destination still matches that snapshot, the source is copied again and the snapshot is updated.
- When the destination differs and the repository is download only, the page asks before overwriting. Cancel leaves the destination and the snapshot unchanged. Confirm overwrites and updates the snapshot.
- When the destination differs and the repository is in synchronisation, the local files stay, the page says they were kept, and the snapshot is not updated.
- For a URL, import reads the plugin clone. The snapshot records the URL. If that clone is missing, the plugin clones it again. It then runs `git fetch origin`. The clone moves forward with a fast-forward only when the branch is not detached, the work tree is clean, and there is no local commit that has not been pushed. Otherwise the current files stay and the page says so. A failed fetch still imports the current files.
- Import saves the snapshot immediately. It does not replace **Apply** for assistants, language, repositories, or the selection. Removing an assistant, a repository, or a component does not delete files already copied.

## Publish

**Publish** lists installed files whose project copy differs from the snapshot, for repositories in synchronisation only. A download-only repository is skipped.

- The page shows those files and a commit message. The message starts as `Publish local changes` and can be edited. Cancel copies nothing back and creates no commit.
- Confirm copies the project files to their original paths when those source files still match the snapshot. Only those paths are staged and committed. The current branch is pushed to `origin`.
- The plugin runs `git` in the source repository. For a URL or an SCP address, that repository is the plugin clone, and publish pushes `origin` without fetching. A commit that stayed local is not erased by the next import. The plugin does not store a token and does not accept `ssh://`. Git credentials and SSH keys stay those already available to the user.
- After a successful push, the snapshot matches the published content. Publish saves the snapshot immediately. It does not replace **Apply**.
- A file already changed in the source repository, and outside the published components, stays out of the commit.
- If the source file and the project file both changed since the snapshot, and their contents differ, nothing is overwritten and no commit is created. The page reports the divergence.
- If `origin` is missing, or the repository is not on a branch, nothing is committed and the page reports the error.
- If the commit succeeds and the push fails, the page states that the commit stays local and was not pushed. Publish does not create a second commit. A later publish pushes that commit once `origin` accepts it.
