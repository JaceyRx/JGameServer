## 概览
![概览](https://github.com/JaceyRx/JGameServer/blob/master/doc/img/game.png "概览")
## 模块组织结构
``` 
  JGameServer
  ├── game-common            --基础公共模块
  ├── game-db                --基础数据库操作模块
  ├── game-gm-server         --Gm服务器模块:作为各服务器的注册中心、提供服务器管理功能
  ├── game-gateway-server    --网关服务器模块：用于请求转发与权限控制，支持分布式部署
  ├── game-logic-server      --逻辑服务器模块：主要游戏业务功能所在处，支持分布式部署
  ├── game-chat-server       --聊天服务器模块：用于聊天数据处理，支持分布式部署
  └── game-battle-server     --对战服务器模块：对战相关请求处理，支持分布式部署
  ```
## 执行流程
1.客户端通过 HTTP 请求从GM服务器获取空闲Gateway服务器连接地址
2.客户端连接网关服务器，长连接由gateway网关服务器维护
3.Gateway服务器接收客户端请求后，根据协议的不同通过Akka Remote 发送给其他业务服务器处理 
### 协议说明
```
 ----------------消息协议格式---------------------
  packetLength | rpcNum | errorCode | body
   int            int      int       byte[]

协议由四部分组成，前三部分为协议头，用于描述消息，第四部分为消息主体
第一部分：packetLength  4字节 int 类型 用于描述这个数据包的长度
第二部分：rpcNum  4字节 int 类型 用于描述当前消息的协议类型
第三部分：errorCode 4字节 int 类型 用于描述消息的错误类型
第四部分：body n字节 byte[] 类型 用于存储经protobuf序列化过的消息主体
```

## 快速开始
## 文档
