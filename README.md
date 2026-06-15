# ChallengeTracker 🏆

A lightweight, asynchronous global event tracker and leaderboard add-on for the LMBishop Quests plugin. Originally developed for the SnejkSMP community, this plugin allows server admins to take individual player quest data and aggregate it into massive, server-wide global goals.

## Features
* **Asynchronous Data Scanning:** Reads player data files in the background, ensuring zero lag or TPS drops on your server, regardless of player count.
* **Global Milestones:** Automatically tracks percentage milestones (25%, 50%, 75%, 100%) and broadcasts immersive sound effects and titles to the server.
* **Anti-Snipe Protection:** Once a goal is reached or the time expires, the final leaderboard is frozen and saved to the config, preventing late data entries from altering the results.
* **Interactive Admin GUI:** Easily add or remove time from an active event directly in-game without touching configuration files.

## Requirements
* Server Software: PaperMC (1.21+)
* Java: Java 21
* Dependencies: [Quests by LMBishop](https://github.com/LMBishop/Quests)

## Setup Instructions
1. Drop the `ChallengeTracker-1.0.jar` into your `plugins` folder.
2. Start the server to generate the default `config.yml`.
3. Open `plugins/ChallengeTracker/config.yml`.
4. Find the `quest-id` and `task-id` of the quest you want to track from your Quests `playerdata` files.
5. Update the configuration, save the file, and type `/ch reload` in-game.

## Commands & Permissions

| Command | Description | Permission |
| :--- | :--- | :--- |
| `/ch leaderboard` | Displays the live event progress and top 10 players. | *None* |
| `/ch admin` | Opens the interactive settings GUI. | `challengetracker.admin` |
| `/ch reload` | Reloads the configuration file instantly. | `challengetracker.admin` |

*Aliases:* `/ch top`, `/challenge`

## License
This project is licensed under the MIT License.
