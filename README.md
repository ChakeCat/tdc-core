# Thor Core

用于双屏输入工具的手势、按键、输入状态策略及 Linux UHID 核心。源码按 PolyForm Noncommercial 1.0.0 提供，限许可证允许的用途；不属于 OSI 开源。

JDK 11+ 可编译：`javac -d build/classes src/main/java/io/github/thorpanel/*.java tests/*.java`。
运行测试：`java -cp build/classes io.github.thorpanel.InteractionTest`（其他测试同理）。
Android 系统接入、官方界面、品牌图片、更新服务和签名凭据不在此仓库。
当前接口沿用 io.github.thorpanel 包级可见性，集成代码需使用相同包；UHID 运行需要 Linux 设备权限。
贡献必须有权按本仓库许可证授权。贡献代码不自动获得官方发布权限。
