# Compatibility — DragonTravel Reforged 2.0.0

Tested at (UTC): 2026-10-12T18:43:13.194771+00:00
JAR SHA-256: `9341d0746b6d024d00dacabaa290949a312b07150c2416eb7d2b0317a01df64c`

All rows use this exact JAR. These are real isolated Paper server boots and connected protocol clients. Each run includes mounting, travel, home, scenic route, item fares, cancellation, refund, permission/protection veto, durable data, restart and reconnect. The process is killed during a fourth real flight when indicated.

The client confirms dragon spawn, mount and movement packets; it does not render a Minecraft window. This is not a claim about every third-party plugin, every server distribution, camera comfort or production load. Vault needs a separately installed provider and is outside these item-fare checks.

Some historical Paper builds are ALPHA/BETA because that is what the official archive publishes for those versions. Unsuffixed versions in the official Paper catalog are covered; missing patches and prerelease suffixes are not extrapolated.

Crash recovery returns to the stored departure when safe. If unsaved terrain disappeared during the forced kill, the intended fallback to world spawn is tested instead (shown in the last column).

| Version | Paper build | Channel | Java | Server / player checks | Network dragons | Forced kill | Recovery |
| --- | ---: | --- | ---: | --- | ---: | --- | --- |
| 1.7.10 | 2025 | STABLE | 8 | 129 / 153 | 3 | yes | spawn fallback |
| 1.8.8 | 445 | STABLE | 8 | 129 / 153 | 3 | yes | departure |
| 1.9.4 | 775 | STABLE | 8 | 129 / 153 | 3 | yes | departure |
| 1.10.2 | 918 | STABLE | 8 | 129 / 153 | 3 | yes | departure |
| 1.11.2 | 1106 | STABLE | 8 | 129 / 153 | 3 | yes | departure |
| 1.12 | 1169 | STABLE | 8 | 129 / 153 | 3 | yes | departure |
| 1.12.1 | 1204 | STABLE | 8 | 129 / 153 | 3 | yes | departure |
| 1.12.2 | 1620 | STABLE | 8 | 129 / 153 | 3 | yes | departure |
| 1.13 | 173 | STABLE | 8 | 129 / 153 | 3 | yes | departure |
| 1.13.1 | 386 | STABLE | 8 | 129 / 153 | 3 | yes | departure |
| 1.13.2 | 657 | STABLE | 8 | 129 / 153 | 3 | yes | departure |
| 1.14 | 17 | STABLE | 8 | 129 / 153 | 3 | yes | departure |
| 1.14.1 | 50 | STABLE | 8 | 129 / 153 | 3 | yes | departure |
| 1.14.2 | 107 | STABLE | 8 | 129 / 153 | 3 | yes | spawn fallback |
| 1.14.3 | 134 | STABLE | 8 | 129 / 153 | 3 | yes | spawn fallback |
| 1.14.4 | 245 | STABLE | 8 | 129 / 153 | 3 | yes | departure |
| 1.15 | 21 | STABLE | 8 | 129 / 153 | 3 | yes | departure |
| 1.15.1 | 62 | STABLE | 8 | 129 / 153 | 3 | yes | departure |
| 1.15.2 | 393 | STABLE | 8 | 129 / 153 | 3 | yes | departure |
| 1.16.1 | 138 | STABLE | 8 | 129 / 153 | 3 | yes | departure |
| 1.16.2 | 189 | STABLE | 8 | 129 / 153 | 3 | yes | departure |
| 1.16.3 | 253 | STABLE | 8 | 129 / 153 | 3 | yes | departure |
| 1.16.4 | 416 | STABLE | 8 | 129 / 153 | 3 | yes | departure |
| 1.16.5 | 794 | STABLE | 8 | 129 / 153 | 3 | yes | departure |
| 1.17 | 79 | STABLE | 16 | 129 / 153 | 3 | yes | departure |
| 1.17.1 | 411 | STABLE | 17 | 129 / 153 | 3 | yes | departure |
| 1.18 | 66 | ALPHA | 17 | 129 / 153 | 3 | yes | departure |
| 1.18.1 | 216 | STABLE | 17 | 129 / 153 | 3 | yes | departure |
| 1.18.2 | 388 | STABLE | 17 | 129 / 153 | 3 | yes | departure |
| 1.19 | 81 | STABLE | 17 | 129 / 153 | 3 | yes | departure |
| 1.19.1 | 111 | STABLE | 17 | 129 / 153 | 3 | yes | departure |
| 1.19.2 | 307 | STABLE | 17 | 129 / 153 | 3 | yes | departure |
| 1.19.3 | 448 | STABLE | 17 | 129 / 153 | 3 | yes | departure |
| 1.19.4 | 550 | STABLE | 17 | 129 / 153 | 3 | yes | departure |
| 1.20 | 17 | STABLE | 17 | 129 / 153 | 3 | yes | departure |
| 1.20.1 | 196 | STABLE | 17 | 129 / 153 | 3 | yes | departure |
| 1.20.2 | 318 | STABLE | 17 | 129 / 153 | 3 | yes | departure |
| 1.20.4 | 499 | STABLE | 17 | 129 / 153 | 3 | yes | departure |
| 1.20.5 | 22 | ALPHA | 21 | 129 / 153 | 3 | yes | departure |
| 1.20.6 | 151 | STABLE | 21 | 129 / 153 | 3 | yes | departure |
| 1.21 | 130 | STABLE | 21 | 129 / 153 | 3 | yes | departure |
| 1.21.1 | 133 | STABLE | 21 | 129 / 153 | 3 | yes | departure |
| 1.21.3 | 83 | STABLE | 21 | 129 / 153 | 3 | yes | departure |
| 1.21.4 | 232 | STABLE | 21 | 129 / 153 | 3 | yes | departure |
| 1.21.5 | 114 | ALPHA | 21 | 129 / 153 | 3 | yes | departure |
| 1.21.6 | 48 | STABLE | 21 | 129 / 153 | 3 | yes | departure |
| 1.21.7 | 32 | STABLE | 21 | 129 / 153 | 3 | yes | departure |
| 1.21.8 | 60 | STABLE | 21 | 129 / 153 | 3 | yes | departure |
| 1.21.9 | 59 | ALPHA | 21 | 129 / 153 | 3 | yes | departure |
| 1.21.10 | 130 | STABLE | 21 | 129 / 153 | 3 | yes | departure |
| 1.21.11 | 132 | STABLE | 21 | 129 / 153 | 3 | yes | departure |
| 26.1.1 | 29 | ALPHA | 25 | 129 / 153 | 3 | yes | departure |
| 26.1.2 | 74 | STABLE | 25 | 129 / 153 | 3 | yes | departure |
| 26.2 | 133 | STABLE | 25 | 129 / 153 | 3 | yes | departure |
| 26.3 | 174 | BETA | 25 | 129 / 153 | 3 | yes | departure |

55 versions passed; 15510 repeated server assertions; 8 Maven geometry tests passed.

Server logs: `test-servers/<version>/reforged-test.log`. First client: `client-first-boot.log`; reconnect: `client-restart.log`. The release ZIP includes these logs and the download manifests, without server binaries/worlds.
