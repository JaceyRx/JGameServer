package com.jacey.game.akka;

import akka.actor.UntypedAbstractActor;
import com.jacey.game.common.msg.NetMessage;
import com.jacey.game.common.msg.RemoteMessage;
import com.jacey.game.common.proto3.BaseBattle;
import com.jacey.game.common.proto3.CommonMsg;
import com.jacey.game.common.proto3.RemoteServer;
import com.jacey.game.common.proto3.Rpc;
import lombok.extern.slf4j.Slf4j;

/**
 * @Description:
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Slf4j
public class ResponseActor1 extends UntypedAbstractActor {
    @Override
    public void onReceive(Object o) throws Throwable {
        if (o instanceof RemoteMessage) {
            RemoteMessage remoteMessage = (RemoteMessage) o;
            switch (remoteMessage.getRpcNum()) {
                case RemoteServer.RemoteRpcNameEnum.RemoteRpcNoticeChatServerCreateNewBattleChatRoom_VALUE: {
                    if (remoteMessage.getErrorCode() == RemoteServer.RemoteRpcErrorCodeEnum.RemoteRpcOk_VALUE) {
                        log.info("Remote 响应 >> 创建对战聊天室成功");
                    } else {
                        log.error("Remote 响应 >> 创建对战聊天室失败 errorCode = {}", remoteMessage.getErrorCode());
                    }
                    break;
                } default: {
                    log.error("Remote 响应 >> 未知协议类型 rpcNum = {}", remoteMessage.getRpcNum());
                }
            }
        } else if (o instanceof NetMessage) {
            NetMessage netMessage = (NetMessage) o;
            switch (netMessage.getRpcNum()) {
                case Rpc.RpcNameEnum.JoinChatRoom_VALUE: {
                    if (netMessage.getErrorCode() == Rpc.RpcErrorCodeEnum.Ok_VALUE) {
                        log.info("netMsg 响应 >> 加入聊天室成功");
                    } else {
                        log.error("netMsg 响应 >> 加入聊天室失败 errorCode = {}", netMessage.getErrorCode());
                    }
                    break;
                } case Rpc.RpcNameEnum.BattleChatText_VALUE: {
                    if (netMessage.getErrorCode() == Rpc.RpcErrorCodeEnum.Ok_VALUE) {
                        log.info("netMsg 响应 >> 文本发送成功");
                    } else {
                        log.error("netMsg 响应 >> 文本发送失败 errorCode = {}", netMessage.getErrorCode());
                    }
                    break;
                } case Rpc.RpcNameEnum.BattleChatTextPush_VALUE: {
                    CommonMsg.BattleChatTextPush battleChatTextPush = netMessage.getLite(CommonMsg.BattleChatTextPush.class);
                    int senderUserId = battleChatTextPush.getSenderUserId();
                    log.info("netMsg 响应 >> sender = {} : text = {}", senderUserId, battleChatTextPush.getText());
                    break;
                } case Rpc.RpcNameEnum.GetBattleInfo_VALUE: {
                    BaseBattle.GetBattleInfoResponse response = netMessage.getLite(BaseBattle.GetBattleInfoResponse.class);
                    if (netMessage.getErrorCode() == Rpc.RpcErrorCodeEnum.Ok_VALUE) {
                        log.info("netMsg 响应 >> 获取对战服务器信息 {}",  netMessage.getProtobufText(BaseBattle.GetBattleInfoResponse.class));
                    } else {
                        log.error("netMsg 响应 >> 获取对战服务器信息失败 errorCode = {}", netMessage.getErrorCode());
                    }
                    break;
                } case Rpc.RpcNameEnum.ReadyToStartGame_VALUE: {
                    BaseBattle.ReadyToStartGameResponse response = netMessage.getLite(BaseBattle.ReadyToStartGameResponse.class);
                    if (netMessage.getErrorCode() == Rpc.RpcErrorCodeEnum.Ok_VALUE) {
                        log.info("netMsg 响应 >> 已做好准备");
                    } else {
                        log.error("netMsg 响应 >> 准备失败 errorCode = {}", netMessage.getErrorCode());
                    }
                    break;
                } case Rpc.RpcNameEnum.PlacePieces_VALUE: {
                    BaseBattle.PlacePiecesResponse response = netMessage.getLite(BaseBattle.PlacePiecesResponse.class);
                    if (netMessage.getErrorCode() == Rpc.RpcErrorCodeEnum.Ok_VALUE) {
                        log.info("netMsg 响应 >> 落子成功 {}", netMessage.getProtobufText(BaseBattle.PlacePiecesResponse.class));
                    } else {
                        log.error("netMsg 响应 >> 落子失败 errorCode = {}", netMessage.getErrorCode());
                    }
                    break;
                } default: {
                    log.error("netMsg 响应 >> 未知协议类型 rpcNum = {}", netMessage.getRpcNum());
                    break;
                }
            }
        }
    }
}
