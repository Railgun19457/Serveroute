# Serveroute

![:Serveroute](https://count.getloli.com/@railgun19457_Serveroute?name=railgun19457_Serveroute&theme=minecraft&padding=6&offset=0&align=top&scale=1&pixelated=1&darkmode=auto)

Serveroute 是一个运行在 Velocity 上的路由插件，用一份配置接管 `/server`，并提供 `/line` 切换同一代理的公网入口。

`/server` 决定去哪玩；`/line` 决定从哪进。

## 功能特性

- 用配置列表取代 Velocity 原版 `/server`，只展示有权限且未隐藏的逻辑服
- 同时支持内部切服（`velocity.toml` 已注册后端）和外部 Transfer
- `/line` 切换同一套 Velocity 的多个公网入口（多 FRP / 多域名），切线后送回原来的子服
- 命令参数同时接受配置 `id` 与展示名 `display`
- 按入站域名识别当前线路，并可把首次连接分流到对应内部服
- Transfer 目标按配置域名发出，不在代理侧解析成 IP
- 配置端口为 `25565` 时查询 `_minecraft._tcp` SRV，避免漏掉非默认端口
- MiniMessage 文案，独立 `message.toml`
- `/serveroute reload` 热重载；配置损坏时保留内存中的旧配置

## 运行环境

- Java 21+
- Velocity 3.3+（推荐 3.5.1）

内部服必须写在 `velocity.toml`。本插件不会注册 Velocity 后端。

## 安装

1. 从 Release 下载插件 Jar（或本地构建）。
2. 放入 Velocity 的 `plugins` 目录。
3. 启动代理端，首次启动会自动生成配置文件：
   - `plugins/serveroute/config.toml`
   - `plugins/serveroute/message.toml`

切线路或接收 Transfer 时，请在 `velocity.toml` 中开启：

```toml
[advanced]
accepts-transfers = true
```

## 命令

- `/server` 列出可加入的逻辑服，并标记当前内部服
- `/server <id|display>` 加入指定逻辑服（补全只出展示名）
- `/line` 列出可切换的线路，并标记当前线路
- `/线路` `/node` `/line` 的别名
- `/line <id|display>` 切换到指定线路
- `/serveroute reload` 重载 `config.toml` 与 `message.toml`

控制台可以使用 `/serveroute reload`，不能执行 `/server` 和 `/line`。

## 权限

- `serveroute.command.server`：使用 `/server`（不设即为允许，显式 `false` 才拒绝）
- `velocity.command.server`：兼容原版权限，显式 `true` 时直接放行
- `serveroute.command.line`：使用 `/line`（不设即为允许，显式 `false` 才拒绝）
- `serveroute.admin.reload`：重载配置；控制台默认可用，玩家必须显式授予
- `serveroute.server.<id>`：进入指定逻辑服（仅当配置写了 `permission` 时生效）
- `serveroute.line.<id>`：使用指定线路（仅当配置写了 `permission` 时生效）

命令级权限与原版 Velocity 一致：没有权限插件或未设置节点时视为允许，只有显式设为 `false` 才拒绝。
条目级 `permission` 相反，必须显式授予，未设置视为不允许。未写 `permission` 的条目只需命令权限即可使用。
`hidden = true` 的条目不出现在列表和补全中，但仍可用命令进入。

## 配置概览

`config.toml` 主要分区：

- `[meta]`：配置版本
- `[command]`：是否接管原版 `/server`，以及 `/line` 别名
- `[transfer]`：Transfer 最低协议号、切线后送回原子服的超时
- `[servers.<id>]`：逻辑服
  - `type = "internal"`：切到 `velocity.toml` 里的 `target`
  - `type = "transfer"`：向客户端发送 Transfer 包，玩家离开当前代理
- `[lines.nodes.<id>]`：同一代理的公网入口，永远走 Transfer

`message.toml` 用于自定义插件提示文本，支持 MiniMessage。

## 注意事项

- Transfer 需要 Minecraft 1.20.5 或更高版本- 对端必须开启 Transfer 接收，否则客户端会提示「此服务器不接受转移」或直接断线
  - Velocity：`[advanced] accepts-transfers = true`
  - Paper / 原版：`server.properties` 里 `accepts-transfers=true`
- 配置端口为 `25565` 时会查询 `_minecraft._tcp` SRV；其它端口按配置直发
- 识别线路依赖连接时的域名，不依赖玩家 IP
- v1 不做测速、GUI、跨代理 Redis 同步

## 升级说明

### 0.1.2

`domain` 字段已合并进 `host`，两者语义本来就相同（都是入站匹配 + Transfer 发出目标）

- 原先只写 `host`：无需改动
- 原先 `host` 与 `domain` 都写：删掉 `domain` 即可，旧字段只会打一条 warn
- 原先**只写 `domain`**：必须改名为 `host`，否则该条目会被跳过

## 本地构建

```bash
./gradlew.bat clean build
```

构建产物位于：

- `build/libs/Serveroute-<version>.jar`

## License

MIT License. See [LICENSE](LICENSE).
