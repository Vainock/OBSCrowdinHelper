## OBSCrowdinHelper

OBSCrowdinHelper is an easy-to-use Java tool to help keep [OBS Studio](https://github.com/obsproject/obs-studio)'s [translations](https://crowdin.com/project/obs-studio) and [contributors](https://github.com/obsproject/obs-studio/blob/master/AUTHORS) up-to-date.

![screenshot of the simple GUI](https://raw.githubusercontent.com/Vainock/OBSCrowdinHelper/main/GUI.png "The simple GUI of the program.")

### Features

- Build the project.
- Download the newest translations.
- Sort out empty files.
- Generate a formatted and sorted `AUTHORS` file containing all Git contributors and translators.
- Supported command line arguments:
  - `--skip-build`: Skips the build creation, but still downloads the newest build.
  - `--skip-authors`: Skips the AUTHORS file.
    - `--skip-git-contributors`: Skips the Git contributors.
    - `--skip-translators`: Skips the translators.

### Requirements

- [Crowdin API v2 Personal Access Token](https://crowdin.com/settings#api-key) from a project manager or the owner
- Java SE 8 or greater
- Git
- a local clone of the [OBS Studio Repository](https://github.com/obsproject/obs-studio) (up-to-date!)

### Build Instructions

1. Download the repository.
2. Run `./gradlew generateJar` or `gradlew.bat generateJar` on Windows.
3. Go to `/build/libs/`.
