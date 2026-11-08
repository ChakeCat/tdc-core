# Thor Core

Thor Core 是雷神的锤使用的 Java 核心模块。它提供触控板手势、按键状态、输入状态判断，以及 Linux UHID 输出。编译不需要 Android SDK 或第三方库。

源码按 [PolyForm Noncommercial 1.0.0](LICENSE) 提供。该许可证包含非商业用途限制，不属于 OSI 定义的开源许可证。

## 模块范围

| 源码 | 作用 |
| --- | --- |
| `TouchpadGesture` | 识别移动、轻点、双指滚动和长按拖动，通过回调输出事件。 |
| `KeyboardInput`、`ModifierKeys` | 管理字母大小写、修饰键、组合键和按键释放。按键编号沿用 Android 键码。 |
| `InputRoutePolicy`、`DockVisibility`、`SetupState` | 根据调用方传入的状态，判断输入法选择、下屏背景显示和引导条件。 |
| `ImeWindowState`、`ImeSurfaceState`、`ImeConnectionState` | 解析调用方提供的 Android 系统状态文本，判断输入法窗口与编辑器连接。 |
| `RemoteLaunchPolicy` | 根据前台应用变化和绑定列表，决定是否触发一次远控联动。它不读取系统任务，也不启动应用。 |
| `UhidMouse`、`UhidKeyboard` | 向 Linux `/dev/uhid` 写入虚拟鼠标和键盘报告。 |
| `CommandRunner` | 运行调用方指定的程序，限制等待时间和保留的输出大小。 |

这里没有可安装的 Android 应用。官方界面、系统服务接入、Shizuku 授权、设备适配、品牌素材、更新服务和签名凭据属于官方客户端，不在本仓库内。

## 编译与完整测试

安装 JDK 11 或更高版本，确认 `javac` 和 `java` 来自同一套 JDK。以下命令适用于 macOS、Linux 或其他提供 POSIX shell 的环境。先进入本仓库根目录。若本模块位于官方客户端工作区内，先运行 `cd core`。

只编译核心类：

```sh
mkdir -p build/classes
javac --release 11 -encoding UTF-8 -d build/classes \
  src/main/java/io/github/thorpanel/*.java
```

运行全部四个测试入口：

```sh
(
  set -eu
  core_classes="$(mktemp -d)"
  trap 'rm -rf "$core_classes"' EXIT
  javac --release 11 -encoding UTF-8 -d "$core_classes" \
    src/main/java/io/github/thorpanel/*.java tests/*.java
  for core_test in InteractionTest KeyboardInputTest CommandRunnerTest RemoteLaunchPolicyTest; do
    java -cp "$core_classes" "io.github.thorpanel.$core_test"
  done
)
```

每个入口通过后会打印 `Passed` 和检查数量。任一失败会返回非零退出码。临时编译目录在结束时删除。

测试覆盖手势、按键状态、输入状态文本解析、远控触发策略，以及子进程输出、超时和中断。测试不打开 `/dev/uhid`，也不连接 Android 设备。它们不能替代真实设备上的输入、固件和权限验证。

`build.gradle` 声明 Java 11 源码与字节码兼容性。仓库没有 Gradle Wrapper。现有测试位于 `tests/`，通过 `main` 方法执行，默认 `gradle test` 不会运行这些测试。完整回归请使用上面的命令。

## 集成与权限边界

核心类及多数方法采用 `io.github.thorpanel` 包级可见性。集成代码需放在同一包。跨包调用需由集成方增加公开封装。当前代码没有稳定的公共 API 承诺，升级时应一并检查调用方和测试。

手势和键盘类只维护状态并调用输出接口。调用方负责事件顺序、时间来源、长按定时回调和取消处理。长按回调应携带该次触摸的 `token()`，并使用同一时间基准。页面切换或输入中断时，调用方需取消手势或重置按键，避免留下按下状态。

输入法状态解析器面向现有 Android 状态文本格式。部分判断固定使用显示屏编号 4 和官方客户端包名。移植到其他设备或系统版本时，需检查这些假设和解析样本。核心不会自动获取系统状态或完成通用设备适配。

UHID 类的构造函数会立即打开 `/dev/uhid`。运行环境必须提供该设备节点，并允许当前进程写入。Android 上还受系统与 SELinux 策略限制。普通应用不能通过引入本模块获得这些权限。集成方负责权限获取、错误处理和设备关闭。

`CommandRunner` 使用当前进程身份运行程序，不提升权限。调用方负责选择可执行文件与参数，并控制命令用途。本模块不包含授权界面或权限绕过功能。

## 许可与贡献

使用、修改和分发须符合 [LICENSE](LICENSE) 规定的用途与条件。许可证未覆盖的用途需要另行获得授权。分发时须提供许可证文本或其链接，并保留 [NOTICE](NOTICE) 中的 `Required Notice:` 声明。

本仓库不授予官方客户端、品牌、商标或更新签名的使用权。贡献者须有权按本仓库许可证授权所提交的代码。提交贡献不代表获得官方发布权限。
