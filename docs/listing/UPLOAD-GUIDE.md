# Listing kit upload guide

Prepared on October 12, 2026. All listing copy and artwork text are in English. Modrinth version 2.0.0 has been uploaded and submitted for moderation with unlisted visibility requested. Its last verified status is recorded in `publication.json`; submission is not public approval. The user reported that CurseForge was already published separately. This workflow made no CurseForge changes or Spigot submission.

## Files

- `DESCRIPTION.md`: the complete Markdown description, including the banner.
- `CURSEFORGE.md`: the same description for CurseForge's Markdown editor.
- `TEXT-ONLY.md`: the description without generated artwork for text-only listings.
- `MODRINTH.md`: the description submitted to Modrinth, with existing artist-created artwork, credits, and AI and derivative-work disclosure for the code and text.
- `publication.json`: stable Modrinth project/version IDs, artifact hashes and the last verified submission state.
- `SPIGOT-BBCODE.txt`: the description converted to Spigot BBCode.
- `METADATA.json`: the name, summary, tags, license and project links.
- `../../assets/listing/banner.png`: landscape promotional banner.
- `../../assets/listing/icon.png`: square project icon.
- `../../assets/listing/PROMPTS.md`: exact generation prompts and provenance.
- `../../assets/listing/modrinth/`: existing artist-created icon and featured illustration uploaded to Modrinth, with their separate CC BY 3.0 credits and manifest.

## Project fields

**Name:** DragonTravel Reforged

**Short summary:** Ride real Ender Dragons between stations, homes and scenic routes. One server plugin, no client mods.

**Project type:** Java server plugin. On CurseForge, select the applicable Bukkit/server-plugin project class; do not submit this JAR as a Forge or Fabric client mod.

**License:** BSD 2-Clause. Preserve the bundled license and original attribution.

**Version:** 2.0.0

**Search terms:** dragon travel, Ender Dragon, transport, stations, scenic routes, homes, server plugin.

Version labels should match the exact versions in `../reforged/compatibility-results.json`. Paper 26.3 build 174 was tested as BETA. Do not turn the 1.7.10-26.3 range into a claim that every intervening patch or server distribution was tested.

## CurseForge

Upload `icon.png` as the project logo. Paste `CURSEFORGE.md` into the Markdown description editor. The banner uses a public URL in the fork's Reforged branch. If the editor does not load it, upload `banner.png` through the platform's image controls and replace the image URL with the resulting hosted URL.

The generated icon should be square and at least 400 by 400 pixels. This follows the [official submission guide](https://support.curseforge.com/support/solutions/articles/9000199552-project-submission-guide-and-tips). The logo is original artwork rather than an existing project avatar or official Minecraft logo.

Upload the tested plugin JAR to the Files section. The local build is `../../release/DragonTravel-Reforged-2.0.0.jar`; the listing kit does not create a GitHub release or a CurseForge download page. Select only version tags offered by the platform that correspond to the tested matrix. Review the rendered description before submission.

## Spigot and other editors

For Spigot's resource description, use `SPIGOT-BBCODE.txt`. Its formatting follows the [official BBCode reference](https://www.spigotmc.org/help/bb-codes). Upload the icon separately. If the resource image proxy does not load the remote banner, use an image host accepted by the platform and update the `[IMG]` URL.

For other editors that support Markdown, start with `DESCRIPTION.md` or `TEXT-ONLY.md` and preview it. Image hosting and formatting support vary by platform; this kit does not claim approval or universal rendering support.

## Modrinth

Modrinth supports [GitHub Flavored Markdown](https://support.modrinth.com/en/articles/8801962-advanced-markdown-formatting), but its [AI policy](https://support.modrinth.com/en/articles/16551575-disclosure-and-usage-of-ai) prohibits AI-generated images anywhere on a project page, including branding. **Do not upload the generated `assets/listing/banner.png` or `assets/listing/icon.png` to Modrinth.** The separate `assets/listing/modrinth/` images are existing artist-created artwork under CC BY 3.0; they have been uploaded as the project icon and featured gallery image. Keep the credits in `MODRINTH.md` and `CREDITS.md` when reusing them.

`TEXT-ONLY.md` provides copy without generated artwork, but it is not a guarantee of listing eligibility. This Reforged implementation and description substantially used generative AI. Modrinth requires the relevant content disclosure and may restrict primarily AI-generated projects, including new work in forks, to unlisted status or reject public publication. Removing the images does not remove the code/content eligibility issue. Disclose the Reforged contribution accurately; this kit makes no claim that the original DragonTravel authors used AI.

## Screenshots and provenance

The banner is promotional artwork. For a screenshot gallery or a gameplay demonstration, capture the actual running plugin in Minecraft. Do not label the generated art as a screenshot.

The plugin runtime and local test server configuration have not been changed by generating these assets. You can continue gameplay checks on the local test server if you have left it running.
