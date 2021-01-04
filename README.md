## OBSCrowdinHelper

OBSCrowdinHelper is an easy-to-use Java tool to obtain the newest translations and translators from the [OBS Studio Crowdin project](https://crowdin.com/project/obs-studio).

![screenshot of the simple GUI](https://raw.githubusercontent.com/Vainock/OBSCrowdinHelper/main/GUI.png "The simple GUI of the program.")

### Features

- Automatically build the project.
- Download the newest translations.
- Sort out empty files.
- Generate a formatted file containing all project members sorted by language names and contributions.

### Requirements

- [Crowdin API v2 Personal Access Token](https://crowdin.com/settings#api-key) from a project manager or the owner
- Java SE 8 or greater

### Build Instructions

1. Download the repository.
2. Run `./gradlew generateJar` or `gradlew.bat generateJar` on Windows.
3. Go to `/build/libs/`.
