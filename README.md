## OBSCrowdinHelper

OBSCrowdinHelper is an easy-to-use Java tool to retrieve the newest translations and translators from the [OBS Studio Crowdin project](https://crowdin.com/project/obs-studio).

Run the program with a simple double-click which opens a terminal.

### Features

- Automatically build the project.
- Download the newest translations.
- Sort-out untranslated files.
- Generate a formatted file containing all project members sorted by language and contributions.

### Requirements

- [Crowdin API v2 Personal Access Token](https://crowdin.com/settings#api-key) from a project manager or the owner
- Java 1.8 or greater
- Windows (only tested on there)

### Build Instructions

1. Download or clone the repository.
2. Navigate to the directory with the terminal and run `gradlew.bat generateJar`.
3. Go to `/build/libs/`.
