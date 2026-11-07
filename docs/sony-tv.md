# 索尼电视权限与 ADB 说明

适用范围：本仓库 TV 家庭版，包名 `com.studyontv.tv`。已实测设备为 Sony BRAVIA 4K UR3 / Android 10。其他机型可能不支持部分系统页面或 ADB 授权命令，请在 App 权限页与系统输出中核对结果。

## 连接电视

电视打开开发者选项和 USB／网络调试，电脑与电视位于同一局域网。以下使用 PowerShell；替换为自己的 ADB 路径、电视 IP 和端口，并确认电视弹出的调试授权。

```powershell
$adbPath = 'D:\Codex\platform-tools\adb.exe'
$tvAddress = '192.168.0.180:5555'
& $adbPath connect $tvAddress
& $adbPath devices -l
& $adbPath -s $tvAddress shell getprop ro.product.model
& $adbPath -s $tvAddress shell getprop ro.build.version.release
& $adbPath -s $tvAddress shell pm list packages com.studyontv.tv
```

后续代码块沿用这两个变量。操作结束可执行 `& $adbPath disconnect $tvAddress`，并按需在电视中关闭调试；不要将 ADB 端口暴露到公网。

## 使用情况访问

本机调用标准 `USAGE_ACCESS_SETTINGS` 页面时返回无法解析 Activity，并非用户没有找到开关。已通过以下命令开启并读取到真实权限状态：

```powershell
& $adbPath -s $tvAddress shell appops set com.studyontv.tv GET_USAGE_STATS allow
& $adbPath -s $tvAddress shell appops get com.studyontv.tv GET_USAGE_STATS
```

输出应包含 `allow`。开启后，家庭版优先使用系统使用记录识别前台，避免仅依赖可能过时的无障碍事件。本机已识别到 `com.sony.dtv.airplayapp`，并验证观看余额递减。本家庭版的电视桌面也计时，学习页与家长设置页不扣观看余额。

## 悬浮窗

优先在电视「应用 → 特殊应用权限 → 显示在其他应用上层」中为「题了个题」开启。若系统入口不可用，可尝试 Android AppOps 方式；本机实测时该权限原已开启，未单独复测从关闭到开启的过程。

```powershell
& $adbPath -s $tvAddress shell appops set com.studyontv.tv SYSTEM_ALERT_WINDOW allow
& $adbPath -s $tvAddress shell appops get com.studyontv.tv SYSTEM_ALERT_WINDOW
```

悬浮窗用于显示管控窗口；无障碍也可提供管控窗口。不能只根据命令执行完毕就认定播放管控正常，还应实际播放并检查到期拦截。

## 无障碍服务

优先在电视系统「无障碍 → 已下载的服务」中开启 **StudyOnTV 屏幕时间守护**。本机已确认该服务绑定并运行。以下是无法使用系统入口时的 ADB 备用写法，未在本次实测中单独执行授权切换。

不要直接将 `enabled_accessibility_services` 写成只有 StudyOnTV 的值，否则会关闭电视上其他已启用的无障碍服务。下面的 PowerShell 代码先读取、保留现有列表，再添加本 App：

```powershell
$sotvService = 'com.studyontv.tv/com.studyontv.tv.WatchAccessibilityService'
$rawServices = (& $adbPath -s $tvAddress shell settings get secure enabled_accessibility_services).Trim()
$enabledServices = @($rawServices -split ':' | Where-Object { $_ -and $_ -ne 'null' })
if ($enabledServices -notcontains $sotvService) {
    $enabledServices += $sotvService
}
& $adbPath -s $tvAddress shell settings put secure enabled_accessibility_services ($enabledServices -join ':')
& $adbPath -s $tvAddress shell settings put secure accessibility_enabled 1
& $adbPath -s $tvAddress shell settings get secure enabled_accessibility_services
& $adbPath -s $tvAddress shell dumpsys accessibility
```

在 `dumpsys accessibility` 中检查 Bound services，而不只是 Enabled services；配置中出现名字不代表服务已绑定。需要撤销时，优先在系统无障碍页面仅关闭本 App，保留其他服务。

## 防卸载：授权灰屏与解除

本机的标准 `com.android.tv.settings/.deviceadmin.DeviceAdminAdd` Activity 可以启动，但内容容器为空，显示灰屏。移除 StudyOnTV 的悬浮窗锚点后仍复现。索尼另一个专用授权页要求系统签名／特权权限，普通 APK 不能直接调用；这不代表所有索尼电视都无法从 App 授权。

在这台设备上，已验证以下普通设备管理员授权方式：

```powershell
& $adbPath -s $tvAddress shell dpm set-active-admin --user 0 com.studyontv.tv/.StudyDeviceAdminReceiver
& $adbPath -s $tvAddress shell dumpsys device_policy
```

成功时会显示 `Success: Active admin set ...`，`Enabled Device Admins` 中应包含 `com.studyontv.tv/.StudyDeviceAdminReceiver`。它不是 Root，也不是设备所有者，不要改用 `set-device-owner`。

解除流程已实测通过：管控页 **↑ 连续短按 5 次 → 输入家长密码 → 权限设置 → 防卸载 → 确认解除**。App 通过 `removeActiveAdmin` 解除自身授权，不必再进入灰屏页。进入家长权限页已验证密码，不重复要求第二次密码。

`dpm remove-active-admin` 对正式签名、非 test-only 管理员通常不可用，不应作为本项目正式版的通用解除命令。没有执行真正卸载来测试防卸载，以免丢失题库与设置；普通设备管理员可被系统管理员解除，不能保证永久不可卸载。

## 在线覆盖更新与后台服务

- 家长页点「检查更新」，下载后由 Android 系统确认覆盖安装。首次安装时，只需为「题了个题」允许安装未知应用；不要求打开所有来源。
- 取消安装或从安装权限设置返回后，可以再次点击「开始安装」。
- 新旧 APK 必须使用相同签名。签名不一致时停止处理，不要为了升级而先卸载、清除应用数据或恢复出厂设置。
- 权限设置中的「后台运行」是管理服务运行状态，不是可以解除所有厂商后台限制的特殊授权。权限授权或电视待机后，可重新打开 App 并核对服务：

```powershell
& $adbPath -s $tvAddress shell am start -n com.studyontv.tv/.MainActivity
& $adbPath -s $tvAddress shell dumpsys activity services com.studyontv.tv
& $adbPath -s $tvAddress shell appops get com.studyontv.tv REQUEST_INSTALL_PACKAGES
```

## 撤销可选权限

仅在希望停用相应能力时执行。`ignore` 表示明确拒绝，会影响计时或管控；以后可用前文的 `allow` 命令重新开启。

```powershell
& $adbPath -s $tvAddress shell appops set com.studyontv.tv GET_USAGE_STATS ignore
& $adbPath -s $tvAddress shell appops set com.studyontv.tv SYSTEM_ALERT_WINDOW ignore
```

设备管理员按上面的 App 内家长解除流程处理，无障碍优先通过系统页面关闭。

## AirPlay 和家长免答题

本机已实测 AirPlay 前台识别、计时、管控止声及解除后的声音恢复。投屏音频来自 Sony／Vewd 服务，没有标准 Android 媒体会话；它可能忽略暂停和音频焦点请求。家庭版对此提供静音兜底，撤掉管控后恢复原静音状态；本次原本未静音的设备恢复到了原音量 16。**静音有效不等于投屏播放进度真正暂停**，后者未得到确认。

达到当天轮次上限后，家长仍可在管控页 **↓ 连续短按 5 次 → 家长密码 → 免答题 1／2／4 小时**。免答题不扣孩子余额，到期自动恢复；提前恢复时重新打开 App、验证后选择「立即恢复管控」。这不会清空孩子当天的学习历史或增加答题轮次。
