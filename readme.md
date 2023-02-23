# adventure-webui

[![MIT License](https://img.shields.io/badge/license-MIT-blue)](license.txt)

A web interface for [MiniMessage](https://github.com/KyoriPowered/adventure-text-minimessage).

You can access it here: https://webui.advntr.dev/

---

### API

This project contains a developer API which can be used to create a simple "editor" system.
This system allows you to post a MiniMessage string to the server and provides a token which an end user can use to edit the string.
The result can easily be saved back again, allowing for a smooth editing experience.

For more information, see [the wiki](https://github.com/KyoriPowered/adventure-webui/wiki/Editor-API).

### Deployment

To run the server, type `./gradlew run -PisDevelopment`.
This will create a server running at `http://localhost:8080`.

For production usage, simply remove the development flag from the run task.
Alternatively, the `distribution` tasks (for example, `distTar`) can be used to create or install archives that contain scripts to run the server.
A [Dockerfile](Dockerfile) has also been provided which can be used to create a Docker image.

### Contributing

We appreciate contributions of any type. For any new features or typo-fix/style changes, please open an issue or come talk to us in our [Discord] first, so we make sure you're going in the right direction for the project.

### Credits

This project is based on MiniDigger's [MiniMessageViewer](https://github.com/MiniDigger/MiniMessageViewer).

The font used can be found [here](https://fonts2u.com/minecraft-regular.font).

[Discord]: https://discord.gg/MMfhJ8F
