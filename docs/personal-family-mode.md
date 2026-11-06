# 家庭版扩展（仅个人使用）

## 家长免答题
手机同 Wi-Fi 访问 http://电视IP:8099/admin/ ，点击右下角「家长免答题」。
用管理密码验证，可选择暂停答题 1、2、4 小时或随时恢复；不扣孩子观看余额。
截止时间写入本地数据库，到期自动恢复，重启不清零。

## 防卸载
电视上家长验证进入「权限设置 → 防卸载」，使用 Android 系统设备管理员授权。
再次点击并确认可解除；若系统不提供授权界面则该功能不可用。
不会 Root 电视，也不是设备所有者级不可卸载。

## 构建与安装
GitHub Actions 会构建个人测试 APK，使用一次性的测试签名。
每次构建签名不同，不能覆盖安装上一次构建；要长期稳定更新必须自行配置固定签名。
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
