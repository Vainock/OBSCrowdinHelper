## OBSCrowdinHelper

OBSCrowdinHelper is an easy-to-use Java tool to obtain the newest translations and translators from Crowdin projects, mainly used for the [OBS Studio project](https://crowdin.com/project/obs-studio).

### Features

- Automatically build the project.
- Download the newest translations.
- Sort out empty files.
- Generate a formatted file containing all project members sorted by language names and contributions.

### Requirements

- [Crowdin API v2 Personal Access Token](https://crowdin.com/settings#api-key) from a project manager or the owner
- Java SE 10 or higher

### Build Instructions

1. Download or clone the repository.
2. Execute `generateJar.bat`.
3. Go to `/build/libs/`.

### Other Projects

This program can easily be used for other Crowdin projects, or even projects hosted on Crowdin Enterprise. Simply replace the two variables at the beginning of `OBSCrowdinHelper.java`, re-compile and run it.

Where to get these values from?

|                    | `PROJECT_ID` (_int_)    | `PROJECT_DOMAIN` (_string_)                   |
| ------------------ | ----------------------- | --------------------------------------------- |
| Crowdin            | Main project page       | `crowdin.com`                                 |
| Crowdin Enterprise | Project "About" section | `[organization].crowdin.com` (present in url) |
