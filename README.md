## OBSCrowdinHelper

OBSCrowdinHelper is an easy-to-use Java tool to obtain the newest translations and translators from the [OBS Studio Crowdin project](https://crowdin.com/project/obs-studio).

Run the program with a simple double-click.

### Features

- Automatically build the project.
- Download the newest translations.
- Sort out untranslated files.
- Generate a formatted file containing all project members sorted by language and contributions.

### Requirements

- [Crowdin API v2 Personal Access Token](https://crowdin.com/settings#api-key) from a project manager or the owner
- Java 1.8 or greater
- Windows (only tested on there)

### Build Instructions

1. Download or clone the repository.
2. Execute `generateJar.bat`.
3. Go to `/build/libs/`.

### Alternate Usage

This program can easily be used for other Crowdin projects, or even projects hosted on Crowdin Enterprise. Simply replace the two variables at the beginning of `OBSCrowdinHelper.java`, re-compile and run it.

Where to get these values from?

|                    | `PROJECT_ID` (_int_)                                            | `PROJECT_DOMAIN` (_string_)                   |
| ------------------ | --------------------------------------------------------------- | --------------------------------------------- |
| Crowdin            | Third line of every language XLIFF file                         | `crowdin.com`                                 |
| Crowdin Enterprise | Present in the url if you're not in the editor, or method above | `[organization].crowdin.com` (present in url) |
