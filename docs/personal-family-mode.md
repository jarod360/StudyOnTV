# 家庭版扩展（仅个人使用）

## 家长免答题
手机同 Wi-Fi 访问 http://电视IP:8099/admin/ ，在「规则」页面的「家长免答题」卡片内直接选择时长。
完成后台家长验证后，可选择暂停答题 1、2、4 小时或随时恢复；不扣孩子观看余额。\n在电视答题或拦截页，遥控器连续短按「下」五次，输入管理密码后也可选择时长；连续「上」五次仍进入原家长设置。
免答题开启时，家长从电视桌面直接打开 App，会出现密码验证与免答题设置，可以提前恢复监管，而不会被自动送回桌面。
截止时间写入本地数据库，到期自动恢复，重启不清零。

## 权限状态与系统限制
- 「使用情况访问」读取 Android AppOps 的真实状态；已授权时不再重复进入设置页。
- 「后台运行」只显示前台管理服务是否在运行，而不是虚假的权限开关；允许尝试启动服务。系统后台限制需在电视设置或通过 ADB 检查。
- 「防卸载」需要设备管理员授权。索尼部分 Android TV 的授权界面可能灰屏；普通 APK 无权直接静默授权，可用 ADB `dpm set-active-admin --user 0 com.studyontv.tv/.StudyDeviceAdminReceiver` 在自有设备上操作。
- Android TV 系统待机清理是厂商行为，即使前台服务正在运行，也不能保证永久存活。

## 防卸载
电视上家长验证进入「权限设置 → 防卸载」，使用 Android 系统设备管理员授权。
进入系统授权页前，暂停答题拦截并移除悬浮窗锚点；返回后恢复监控。这用于排除索尼 TV 系统授权界面灰屏的潜在遮挡原因，尚须实机验证。
如系统仍显示灰屏，普通 APK 无权静默授权；可在自己设备上使用 ADB `dpm set-active-admin --user 0 com.studyontv.tv/.StudyDeviceAdminReceiver` 激活，授权状态会由 App 自动检测。
再次点击并确认可解除；若系统不提供授权界面则该功能不可用。
不会 Root 电视，也不是设备所有者级不可卸载。

## 构建与安装
GitHub Actions 固定使用仓库内 `signing/studyontv.jks` 的 PKCS12 签名密钥，别名 `studyontv`，密钥库和密钥密码均为 `android`；后续构建不会再生成新签名。此仓库为公开仓库，密钥与密码也随源码公开，使用者已明确接受此发布方式。
成功构建后，将当前 versionName 对应的 `StudyOnTV-TV-v<versionName>.apk` 发布到本 fork 的 GitHub Releases。新版本必须同时递增 versionCode 和 versionName，且版本标签不能重复；TV 的版本检查读取 jarod360/StudyOnTV/releases/latest。
首次迁移到固定签名仍须卸载旧临时签名版，之后可直接覆盖安装保留数据。签名文件随仓库版本管理，拉取仓库即可取得相同密钥。
先从官方版家长后台导出题库并记录设置，因为卸载会清除本地数据。

卸载原版（索尼电视）：
```powershell
& "D:\Codex\platform-tools\adb.exe" -s "192.168.0.180:5555" uninstall com.studyontv.tv
```

安装新包（下载 APK 到 D 盘）：
```powershell
& "D:\Codex\platform-tools\adb.exe" -s "192.168.0.180:5555" install "D:\Codex\StudyOnTV-TV-family-test.apk"
```

本分支仅用于个人家庭，原项目 LICENSE 禁止向第三方二次分发源码或 APK。
