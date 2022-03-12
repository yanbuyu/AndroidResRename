# AndroidResRename
- 使用apktool.jar编译后的MIUI系统应用资源会与原包有很大出入，如果直接将应用资源压缩进原apk中，无疑会加大原apk体积。该项目旨在还原编译后的MIUI系统应用资源文件夹名称，使得两者资源几乎一致，此时将应用资源压缩回原apk，便不会增大原apk体积。

- 使用方法
```shell
java -jar arr.jar 原apk aoktool编译的apk 输出的apk
```
打开输出的apk，解压res和resources.arsc，将它们压缩回原apk

- 核心代码来自[AndroidResProguard](https://github.com/s562218/AndroidResProguard)
