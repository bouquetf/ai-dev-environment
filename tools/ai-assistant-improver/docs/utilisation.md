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

Enabled assistants are a setting of the open project: each project has its own list of active assistants.

Both settings are changed on this same page. Displayed labels (titles, assistant names, messages) come from the bundle of the chosen language.

## Repositories

On this same page, enter the path of a local directory and choose **Add**. **Browse** opens a directory chooser.

The directory must contain `.git`. An empty path, a file, a directory without `.git`, or a path already listed is refused: the message is shown on the page and the repository is not added.

A URL is not cloned. The added repository is already a local clone.

The initial mode is download only. The user can switch that repository to synchronisation, then back to download only. The mode applies to that repository.

Several repositories can appear in the list.

Remove takes the repository off the list. The clone on disk and the files already installed in the open project stay in place.

Enabled assistants and repositories are settings of the open project. Language remains a setting of the whole IDE.
