[English](../en/CHANGELOG.md) | **[简体中文](CHANGELOG.md)**

# 版本日志
## 0.1.2027
- Bugfix：当两次构建之间新增或删除类时，`classObfuscation` 不再因 `Class <name> has already been interned` 而中断。输入 `class-mapping.txt` 中已经分配的类名现在会在生成任何新名字之前全部预留；此前只有生成器处理到该映射类时才预留，导致新增的类可能占用另一个已映射类仍在使用的名字。
- Bugfix：如果 `class-mapping.txt` 将两个类映射到同一个名字，现在会被检测、告警并修复，而不是让整个 bundle 打包失败。
- 当两个类最终会得到相同类名时，类名混淆会以明确的错误信息失败，而不是输出重复的类定义。

## 0.1.6（2020/4/21）
- 适配 `AGP-3.5.0`
- 修复获取 `AGP` 版本号失败的问题

## 0.1.5（2020/4/5）
- 适配 `AGP-4.0.0-alpha09`
- 给插件添加 `enableObfuscate` 参数

## 0.1.3（2020/1/8）
- 适配 `AGP-3.5.2`

## 0.1.2（2020/1/7）
- 适配 `AGP-4.0.0-alpha07`
-  Fix issue [#13](https://github.com/bytedance/AabResGuard/issues/13)

## 0.1.1（2019/11/26）
- 适配 `AGP-3.4.1`
- Fix issue [#4](https://github.com/bytedance/AabResGuard/issues/4)

## 0.1.0（2019/10/16）
- 添加资源混淆功能
- 添加资源去重功能
- 添加文件过滤功能
- 添加字符串过滤功能
- 添加 `gradle plugin` 的支持
- 添加命令行支持
