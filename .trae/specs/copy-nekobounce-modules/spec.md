# 从NekoBounce复制模块 Spec

## Why
用户需要从NekoBounce项目复制多个渲染模块到当前项目，以增强客户端功能。

## What Changes
- 复制9个模块文件到当前项目
- 复制所需的资源文件（图片、纹理等）
- 在每个kt文件顶部添加来源注释
- 将TargetHUD重命名为TargetHUD2
- 修改ClientUtils.kt以支持ChatPrefix模块
- **BREAKING** 修改displayChatMessage方法以使用ChatPrefix

## Impact
- 新增模块：Cape, DashTrail, ESP2D, Island, BAHalo, ChatPrefix, KillESP, TargetHUD2, Gapple
- 修改文件：ClientUtils.kt
- 需要复制资源文件到resources目录

## ADDED Requirements

### Requirement: 复制模块文件
系统应将以下模块从NekoBounce复制到当前项目：
- Cape.kt - 披风模块
- DashTrail.kt - 拖尾效果模块
- ESP2D.kt - 2D ESP模块
- Island.kt - HUD信息模块
- BAHalo.kt - 头顶光环模块
- ChatPrefix.kt - 聊天前缀模块
- KillESP.kt - Kill ESP模块
- Gapple.kt - 自动吃金苹果模块
- TargetHUD.kt → TargetHUD2.kt - 目标HUD模块（重命名）

### Requirement: 添加来源注释
每个复制的kt文件顶部必须添加注释：
```
// skid neko bounce 
// https://github.com/RouQingNeko1024/NekoBounce
```

### Requirement: 复制资源文件
需要复制以下资源文件夹：
- `liquidbounce/cape/` - 披风图片（8个png文件）
- `liquidbounce/halo/` - 光环图片（12个png/jpg文件）
- `liquidbounce/watermark_images/` - Island模块需要的图片
- `liquidbounce/textures/dashtrail/` - 拖尾纹理

### Requirement: 修改ClientUtils.kt支持ChatPrefix
修改`displayChatMessage`方法：
- 当ChatPrefix模块启用时，使用ChatPrefix.getFormattedPrefix()
- 当ChatPrefix模块禁用时，使用默认前缀
- 添加ChatPrefix的import语句

### Requirement: ChatPrefix显示时机
ChatPrefix模块应该：
- 在发送聊天消息时显示前缀
- 模块启用时使用自定义前缀
- 模块禁用时使用默认客户端前缀

### Requirement: Island模块通知功能
Island模块包含：
- 模块开启/关闭通知
- 方块挖掘进度显示
- Gapple进度显示
- Scaffold方块计数
- TabList自定义显示
- 箱子内容显示

## MODIFIED Requirements

### Requirement: 修改ClientUtils.displayChatMessage
原代码：
```kotlin
fun displayChatMessage(message: String) {
    mc.thePlayer?.addChatMessage(ChatComponentText("§8[§9§l$CLIENT_NAME§8]§r $message"))
        ?: LOGGER.info("(MCChat) $message")
}
```

修改为：
```kotlin
fun displayChatMessage(message: String) {
    val prefix = if (ChatPrefix.state) {
        ChatPrefix.getFormattedPrefix()
    } else {
        "§8[§9§l$CLIENT_NAME§8]§r "
    }
    mc.thePlayer?.addChatMessage(ChatComponentText("$prefix$message"))
        ?: LOGGER.info("(MCChat) $message")
}
```

## REMOVED Requirements
无移除需求
