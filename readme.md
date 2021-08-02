# adventure-webui

![GitHub Workflow Status (branch)](https://img.shields.io/github/workflow/status/KyoriPowered/adventure-webui/build/main) [![MIT License](https://img.shields.io/badge/license-MIT-blue)](license.txt)

A web interface for [MiniMessage](https://github.com/KyoriPowered/adventure-text-minimessage).

You can access it here: https://webui.adventure.kyori.net/

---

### Development Usage

This project uses a Kotlin Multiplatform stack with three areas; a Kotlin/JS front-end, a Kotlin/JVM server, and a pure Kotlin common source set.

To run the server, type `./gradlew run -PisDevelopment`.
This will create a server running at `https://localhost:8080`.

For production usage, simply remove the development flag from the run task.
Alternatively, the `distribution` tasks (for example, `distTar`) can be used to create or install archives that contain scripts to run the server.
A [Dockerfile](Dockerfile) has also been provided which can be used to create a Docker image.

### Contributing

We appreciate contributions of any type. For any new features or typo-fix/style changes, please open an issue or come talk to us in our [Discord] first, so we make sure you're going in the right direction for the project.

### Credits

This project is based on MiniDigger's [MiniMessageViewer](https://github.com/MiniDigger/MiniMessageViewer).

The font used can be found [here](https://fonts2u.com/minecraft-regular.font).

---

`adventure-webui` is released under the terms of the [MIT License](license.txt)

[Discord]: https://discord.gg/MMfhJ8F
