# RaidCompat 1.21.2

把 **Minecraft 1.21.2 的袭击生成点搜索逻辑** 移植到 **1.21.1 + NeoForge 21.1.x**。
目标场景：按 1.21.2 教程建的袭击塔，在 1.21.1 里"袭击条出来了但一只怪都不刷"。

## 它替换了什么

1.21.1 的 `Raid#findSpawnPos()` 用的是一套老算法：以 raid center 为圆心，在 **r≈64 / r≈32 / 中心 0** 三个环上各取 20 个随机点，共 60 次；高度取该列最顶层非空气方块，**没有垂直限制**；60 次全失败就直接结束这一波（表现就是"条出来、没怪、然后胜利"）。

本模组把该方法整个换成 1.21.2 的规则：

| 1.21.2 规则 | 实现位置 |
|---|---|
| 由远及近搜索，默认共 `60 步 × 8 次 = 480` 次检查 | `RaidSpawnCompat#findSpawnPos` |
| 水平半径 96 格 | `Config.maxHorizontalRadius` |
| 与 raid center 的**垂直距离必须 < 96 格** | `Config.maxVerticalDelta` |
| 只取该列**最顶层非空气方块**的上方 | `topmostNonAir()` |
| 必须在**实体 ticking** 的已生成区块 | `isUsable()` |
| 脚下必须是合法的可生成面（玻璃、树叶等不算） | `isUsable()` |
| 上方需要 2 格空气 | `isUsable()` |
| 找不到就整波失败（不判胜利，见"已知差异"） | `RaidMixin` |

## 构建

需要 **JDK 21**；首次构建会从 `maven.neoforged.net` 拉取 NeoForge 和 Minecraft（约 1 GB），离线环境无法构建。

```bash
./gradlew build          # Windows: gradlew.bat build
```

产物：`build/libs/raidcompat-1.0.0.jar`。

**不想在本机装环境**：仓库里带了一个 GitHub Actions 工作流（`.github/workflows/build.yml`），
把整个项目推到 GitHub 后会自动编译，跑完在 Actions 页面底部下载 `raidcompat` 构件就是 jar。

本机构建的话：

> 仓库里没有带 `gradle-wrapper.jar`（生成它需要本机装 Gradle）。两种办法：
> 用 IntelliJ IDEA 直接 "Open" 这个目录（它会用自带的 Gradle 导入），
> 或者装 Gradle 8.9+ 后在本目录执行一次 `gradle wrapper` 再走 `./gradlew build`。

> 我这边没有 JDK、也连不上 neoforged 的 maven，所以**这份源码没有被实际编译验证过**。
> 逻辑和结构是完整的，但请按下面"如果编译不过"先确认两个内部符号。

## 安装

把 jar 丢进 `mods/`。袭击判定完全在服务端，**服务器必须装**；客户端装不装都行。

## 验证是否生效

1. 启动日志应出现：`[raidcompat] 1.21.2 raid spawn search is active on 1.21.1`
2. 打开 `config/raidcompat-common.toml`，把 `logFailedSearches` 设为 `true`。
3. 触发袭击，观察 `logs/latest.log`：
   - 有 `no valid raid spawn position ...` → 你的塔附近 96 格水平 / ±96 格垂直内确实没有合法生成面（继续查平台方块和顶部空气）。
   - 没有该行且怪正常刷出 → 生效。
4. 想直接确认是不是本模组起的作用：在距离 raid center **50~90 格**、且与中心**垂直差 30~90 格**的位置放一小块石头平台（上方 2 格空气）。原版 1.21.1 永远刷不到那里，装了本模组就会刷到。

## 调参

`config/raidcompat-common.toml`：

- `enabled` — 关掉即恢复原版行为，方便对照。
- `maxVerticalDelta` — 默认 96。想更严格（只允许贴近平台的高度）就调小，比如 24。
- `maxHorizontalRadius` — 默认 96。
- `searchSteps` / `attemptsPerStep` — 默认 60 / 8，总次数 480。嫌卡就降到 30 / 4。
- `logFailedSearches` — 排错用。

## 已知差异（相对真正的 1.21.2）

1. **没有实现 24w33a 的"找不到生成点就不开打"**。那条改在 `RaidManager` 创建 raid 之前，需要再挂一个 mixin；本模组只替换波次内的生成点搜索。所以如果整波彻底找不到点，1.21.1 原版的 MC-158389（直接判胜利）仍然会发生 —— 但换成 ±96 垂直 + 480 次检查后，正常建好的 1.21.2 式塔基本不会走到这一步。
2. **没有排除"生成点在村庄子区块内"**（1.21.2 会排除）。对塔的影响：袭击者有可能刷在你自己床/工作站所在的平台面上。如果遇到，把村庄平台面换成玻璃/树叶（不可生成面）即可。
3. **搜索在一次性完成，而不是摊在 300 tick 冷却里**。取到的位置分布与 1.21.2 等价，只是没有"冷却越接近 0、越靠近中心"的时序表现。

## 如果编译不过

只有两个集成点可能对不上你的映射版本，都在 `RaidMixin` 里：

1. **方法名 `findSpawnPos`**
   在 IDE 里打开 `net.minecraft.world.entity.raid.Raid`，找返回 `Optional<BlockPos>` 的那个私有方法。
   如果名字不叫 `findSpawnPos`，只改 `@Inject(method = "...")` 即可，其它不用动。
2. **`ServerLevel level` 字段**
   同一个类里找类型为 `ServerLevel` 的私有字段；名字若不是 `level`，把 `@Shadow` 那行的字段名改成实际的即可。

如果 mixin 应用失败，崩溃报告会明确写出 `@Shadow field level was not found` 或 `method findSpawnPos ... was not found`，按上面两条改一行就能过。

## 配套的塔体建议

换成 1.21.2 逻辑后，塔的"合法生成面"可以出现在：

- 水平：raid center 96 格内任意方向（不再要求恰好 32/64 格环上）；
- 垂直：raid center 上下 96 格内（不再被拉到地表/树顶）。

也就是说 1.21.2 教程里的"平台在塔身上、离村庄中心几十格"的设计，在 1.21.1 里装了这个模组之后就能正常工作了，不需要把塔拆了重建。

## 许可

MIT。非官方作品，与 Mojang / NeoForged 无关。