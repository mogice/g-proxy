# This is an free proxy based on Google App Engine. #

  * 已经把代码安装在，可以直接使用：https://g-proxy.appspot.com/
  * 源代码放在Google Code：http://code.google.com/p/g-proxy/
  * 发布说明(**_功能讨论_**) ：http://blog.liuhongwei.cn/my-opensource/g-proxy-free-https/

# 欢迎访问 #

  * Blog: http://blog.liuhongwei.cn/
  * Twitter: http://twitter.com/harryempire

# G-Proxy具有的功能： #

  * 完整的代理服务器功能；
  * 自动把获取的HTML文件中的图片和链接，加上代理服务器的网址；
  * HTTPS功能，这样可以保护您的数据不被任何人看到；
  * 代理服务器不保存任何访问记录，充分保护您的隐私；
  * **(新功能)** 支持中文、日文等双字节编码；
  * **(新功能)** 支持ajax，javascript，css等所有外围文件；
  * **(新功能)** 支持href、src、action等属性的链接转换；

TODO
  * referer受到限制，导致个别图片无法显示（被认为是盗链）；
  * HTTP文件中的 http:// 或 https:// ，都被误转换；