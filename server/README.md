# PvZ2 Server (Phase 3)

Standalone multiplayer/back-end server for Plant vs. Zombies 2. Runs **completely
separately from the game client** and is built with **plain `javac`/`java` — no
Gradle**. The client and server live in the same repository but never share a
build.

## Requirements
- A JDK on `PATH` (`javac` / `java`). JDK 17+ works; the repo is developed on 26.
- `lib/gson-2.13.1.jar` (already included).

## Build
```
build.bat      (Windows)      ./build.sh     (bash / git-bash)
```
Compiles `../shared/src` (protocol) + `src` into `out/`.

## Run
```
run.bat        (Windows)      ./run.sh       (bash / git-bash)
```
Opens the **Admin dashboard** window and listens on TCP **5599** by default.
Double-clicking `run.bat` works too (it builds first if needed).

### Options (JVM system properties)
| Property        | Default        | Meaning                                  |
|-----------------|----------------|------------------------------------------|
| `-Dpvz.port=`   | `5599`         | Listen port                              |
| `-Dpvz.name=`   | `PvZ2 Server`  | Server name in the HELLO greeting        |
| `-Dpvz.data=`   | `data`         | Data dir (users store + logs)            |
| `-Dpvz.adminUi=`| `true`         | Open the Swing admin window              |

Example: `run.bat -Dpvz.port=6000`

### Console commands (type in the terminal)
`list` · `stats` · `stop` · `help`

## Architecture
- **Protocol** (`../shared`): length-prefixed JSON frames (4-byte big-endian
  length + UTF-8 JSON `Packet{v,type,id,data}`). Versioned (`MessageType.VERSION`).
- **Concurrency**: one accept loop + one reader thread per client + one heartbeat
  task. Writes per connection are serialized by a lock.
- **Dispatcher**: feature modules register handlers by message type (auth, data
  sync, matchmaking, match relay, admin) — no monolithic switch.
- **Admin panel**: Swing dashboard (status, connections, broadcast, live log,
  graceful shutdown) + file logs under `data/logs/`.

## Status
Step 1 (protocol + server skeleton) complete. Next: accounts/auth + user store.
