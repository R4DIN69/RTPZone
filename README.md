# RTPZone - Minecraft Paper 1.21 Plugin

An immersive, cinematic Random Teleport (RTP) plugin for **Paper 1.21** featuring vortex hole creation, safe Overworld teleportation from 50 blocks in the air, and an automated **3rd-Person to 1st-Person POV transition** upon landing.

---

## 🌟 Key Features

1. **`/rtp zone select` Command**:
   - Creates a physical or particle vortex hole area directly 3 blocks in front of the admin/player.
   - Carves a 3x3 pit with decorative crying obsidian borders (fully customizable in `config.yml`).
   - Plays continuous multi-ring portal vortex particles and ambient sounds.

2. **Hole Entry & Slow Falling**:
   - When a normal player walks or jumps into the hole, they are granted the **Slow Falling** effect.
   - The player gently descends down into the void vortex with portal sound effects.

3. **Safe Random Teleportation**:
   - Teleports to a random location in the Minecraft Overworld (between configured min & max radius).
   - Automatically verifies ground safety using asynchronous chunk loading to prevent server lag.
   - Blacklists water, lava, fire, cactus, sweet berry bushes, and dangerous blocks.

4. **50-Block Sky Drop & 3rd-Person POV Transition**:
   - The player spawns **exactly 50 blocks above the safe ground**.
   - **Slow Falling** allows the player to float gently through the air.
   - **Automatic 3rd Person POV**:
     - **Bedrock / Geyser**: Automatically executes the native Bedrock `/camera <player> set minecraft:third_person` command for seamless crossplay camera control!
     - **Java Edition**: Prompts an on-screen action bar and Title with live altitude counter (`☁ Descending... X blocks to ground | Press F5 for 3rd Person`).

5. **Ground Landing & 1st-Person Restoration**:
   - Detects the exact moment the player touches the ground.
   - Automatically switches the camera POV back to **1st Person** (clears Bedrock `/camera`).
   - Removes Slow Falling and prevents any fall damage.
   - Displays a celebratory `✔ Safe Landing!` Title and plays a level-up sound.

---

## 🚀 How to Build the Jar with GitHub (Step-by-Step)

This project includes a pre-configured **GitHub Actions Workflow** (`.github/workflows/build.yml`) that compiles the `.jar` automatically in the cloud.

### Step 1: Create a GitHub Repository
1. Go to [GitHub](https://github.com/) and click **New Repository**.
2. Name it `RTPZone` and keep it Public or Private.
3. Leave "Initialize with README" unchecked.

### Step 2: Push the Code
In your local terminal inside the `RTPZone` folder, run:
```bash
git init
git add .
git commit -m "Initial commit for Paper 1.21 RTPZone plugin"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/RTPZone.git
git push -u origin main
```

### Step 3: Download your Compiled `.jar`
1. Go to your repository on GitHub and click the **Actions** tab at the top.
2. You will see the **Build RTPZone Plugin** workflow running.
3. Once completed (takes ~30-45 seconds), click on the build run.
4. Under **Artifacts** at the bottom of the page, click **`RTPZone-Plugin-Jar`** to download your compiled `.jar` file!

---

## 🛠️ Local Build (Optional)

If you prefer building locally on your machine with Maven or Gradle:

### Maven:
```bash
mvn clean package
```
The compiled jar will be located in `target/RTPZone-1.0.0.jar`.

### Gradle:
```bash
./gradlew build
```
The compiled jar will be located in `build/libs/RTPZone-1.0.0.jar`.

---

## 🎮 Commands & Permissions

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/rtp zone select` | `rtpzone.admin` | Creates the RTP hole area directly in front of you |
| `/rtp zone remove` | `rtpzone.admin` | Removes the active RTP hole zone |
| `/rtp reload` | `rtpzone.admin` | Reloads `config.yml` |
| `/rtp help` | `rtpzone.use` | Displays command assistance |

---

## ⚙️ Configuration (`config.yml`)

```yaml
zone:
  configured: false
  world: "world"
  x: 0.0
  y: 64.0
  z: 0.0
  radius: 1.8
  depth-range: 8.0
  physical-hole:
    enabled: true
    radius: 1               # 3x3 hole
    depth: 6                # 6 blocks deep
    hollow-block: "AIR"
    border-enabled: true
    border-block: "CRYING_OBSIDIAN"

teleport:
  target-world: "world"
  min-radius: 500
  max-radius: 5000
  fall-height-above-ground: 50 # 50 blocks above safe ground
  hole-fall-delay-ticks: 35

camera:
  bedrock-camera-support: true
  bedrock-third-person-preset: "minecraft:third_person"
  show-f5-prompt: true
  landing-title: "§a§l✔ SAFE LANDING!"
  landing-sound: "ENTITY_PLAYER_LEVELUP"
```

---

## 📌 Technical Notes on Minecraft Camera POV

- In **Minecraft Java Edition**, client camera perspective (`F5`) is client-authoritative for vanilla players. The plugin provides on-screen titles, audio cues, and altitude actionbars to guide the player into F5 mode during the drop.
- In **Minecraft Bedrock / Geyser** crossplay, the server sends native `/camera` commands to physically lock the player's camera to third person during the 50-block descent and cleanly clears it back to first person upon touching ground.
