package com.jacey.game.gm.manager;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.actor.Props;
import com.jacey.game.common.action.BaseMessageAction;
import com.jacey.game.common.constants.GlobalConstant;
import com.jacey.game.common.manager.IManager;
import com.jacey.game.common.msg.RemoteMessage;
import com.jacey.game.common.utils.StringUtil;
import com.jacey.game.db.service.BattleServerLoadBalanceService;
import com.jacey.game.db.service.ChatServerLoadBalanceService;
import com.jacey.game.db.service.GatewayServerLoadBalanceService;
import com.jacey.game.db.service.LogicServerLoadBalanceService;
import com.jacey.game.db.service.impl.BattleServerLoadBalanceServiceImpl;
import com.jacey.game.db.service.impl.ChatServerLoadBalanceServiceImpl;
import com.jacey.game.db.service.impl.GatewayServerLoadBalanceServiceImpl;
import com.jacey.game.db.service.impl.LogicServerLoadBalanceServiceImpl;
import com.jacey.game.gm.actor.BaseMessageActor;
import com.jacey.game.gm.actor.GmActor;
import com.jacey.game.common.proto3.RemoteServer.*;
import com.jacey.game.common.proto3.CommonEnum.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description: 消息处理器。主要用于协议的初始化预加载与消息的分发处理
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
public class MessageManager implements IManager {

	private static final Logger logger = LoggerFactory.getLogger(MessageManager.class);

	private static MessageManager instance = new MessageManager();

	public static MessageManager getInstance() {
		return instance;
	}

	private ActorSystem system;

	private ActorRef gmActor;
	/**==============================<以下Map为消息处理相关>========================================*/
	/** rpcNum通信协议与对应的消息处理Actor关系缓存   key:rpcNum, value:对应处理这个消息的Actor*/
	private final Map<Integer, ActorRef> rpcNumToHandleActorMap = new HashMap<Integer, ActorRef>();
	/** Actor与其对应下属 Action的关系缓存   key:actor, value:map(key:rpcNum, value:处理这个消息的Action)*/
	private final Map<Class<? extends BaseMessageActor>, Map<Integer, Class<BaseMessageAction>>> actorToHandleActionMap = new HashMap<Class<? extends BaseMessageActor>, Map<Integer, Class<BaseMessageAction>>>();
	/**主逻辑服务器id*/
	private int mainLogicServeId;

	/**==============================<以下Map都为服务器注册相关>========================================*/
	/**=======================key:Integer  values:ActorRef=========================================*/
	/** 在线逻辑服务器列表 key:logicServerId  values:Actor */
	private final Map<Integer, ActorRef> logicServerIdToActorMap = new ConcurrentHashMap<Integer, ActorRef>();
	/** 在线对战服务器列表 key:battleServerId  values:Actor */
	private final Map<Integer, ActorRef> battleServerIdToActorMap = new ConcurrentHashMap<Integer, ActorRef>();
	/** 对战聊天服务器列表 key:gatewayServerId  values:Actor */
	private final Map<Integer, ActorRef> gatewayIdToActorMap = new ConcurrentHashMap<Integer, ActorRef>();
	/** 在线对战服务器列表 key:chatServerId  values:Actor */
	private final Map<Integer, ActorRef> chatServerIdToActorMap = new ConcurrentHashMap<Integer, ActorRef>();

	/**=======================key:ActorRef  values:Integer=========================================*/
	/** 在线逻辑服务器列表 key:Actor  values:logicServerId */
	private final Map<ActorRef, Integer> logicActorToServerIdMap = new ConcurrentHashMap<ActorRef, Integer>();
	/** 在线对战服务器列表 key:Actor  values:logicServerId */
	private final Map<ActorRef, Integer> battleActorToServerIdMap = new ConcurrentHashMap<ActorRef, Integer>();
	/** 在线网关服务器列表 key:Actor  values:logicServerId */
	private final Map<ActorRef, Integer> gatewayActorToIdMap = new ConcurrentHashMap<ActorRef, Integer>();
	/** 对战聊天服务器列表 key:Actor  values:chatServerId */
	private final Map<ActorRef, Integer> chatActorToServerIdMap = new ConcurrentHashMap<ActorRef, Integer>();

	/**负载均衡处理service*/
	LogicServerLoadBalanceService logicServerLoadBalanceService = SpringManager.getInstance().getBean(LogicServerLoadBalanceServiceImpl.class);
	GatewayServerLoadBalanceService gatewayServerLoadBalanceService = SpringManager.getInstance().getBean(GatewayServerLoadBalanceServiceImpl.class);
	BattleServerLoadBalanceService battleServerLoadBalanceService = SpringManager.getInstance().getBean(BattleServerLoadBalanceServiceImpl.class);
	ChatServerLoadBalanceService chatServerLoadBalanceService = SpringManager.getInstance().getBean(ChatServerLoadBalanceServiceImpl.class);

	@Override
	public void init() {
		logicServerLoadBalanceService = SpringManager.getInstance().getBean(LogicServerLoadBalanceServiceImpl.class);
		gatewayServerLoadBalanceService = SpringManager.getInstance().getBean(GatewayServerLoadBalanceServiceImpl.class);
		battleServerLoadBalanceService = SpringManager.getInstance().getBean(BattleServerLoadBalanceServiceImpl.class);
		chatServerLoadBalanceService = SpringManager.getInstance().getBean(ChatServerLoadBalanceServiceImpl.class);

		// 生成当前系统的akka system actor 对象，当前系统名称：gm
		system = ActorSystem.create(GlobalConstant.GM_SYSTEM_NAME);
		// 生成并注册 Gm Actor 代理对象，ActorRef。名称：gmActor
		gmActor = system.actorOf(Props.create(GmActor.class), GlobalConstant.GM_ACTOR_NAME);

	}

	@Override
	public void shutdown() {}

	/**
	 * 添加RpcNum通信协议号与其对应的消息处理Actor到map缓存中
	 * @param rpcNum   通信协议号
	 * @param actor	   处理该消息的actor代理
	 */
	public void addRpcNumToHandleActorMap(int rpcNum, ActorRef actor) {
		if (rpcNumToHandleActorMap.containsKey(rpcNum) == true) {
			logger.error(
					"addRpcNumToHandleActorMap error, multiple actor to handle same rpcNum = {}, actorName = {} and {}",
					rpcNum, rpcNumToHandleActorMap.get(rpcNum).getClass().getName(), actor.getClass().getName());
		}
		rpcNumToHandleActorMap.put(rpcNum, actor);
	}


	/**
	 * 获取 Actor 下属的所有 Action 对象
	 * @param clazz  Actor代理对象Class
	 * @return
	 */
	public Map<Integer, Class<BaseMessageAction>> getActionClassByActor(Class<? extends BaseMessageActor> clazz) {
		return actorToHandleActionMap.get(clazz);
	}

	/**
	 * 添加 actor 与其所属下属 action Map（一对多）
	 * @param clazz  actor代理对象
	 * @param map    actionMap
	 */
	public void addActorToHandleAction(Class<? extends BaseMessageActor> clazz,
									   Map<Integer, Class<BaseMessageAction>> map) {
		actorToHandleActionMap.put(clazz, map);
	}

	/**
	 * 逻辑服务器注册
	 * @param logicServerId             逻辑服务器id
	 * @param akkaPath					akka远程访问地址
	 * @param isMainLogicServer			是否是主节点
	 * @param actor						对应的代理actor
	 * @return
	 */
	public boolean addLogicServer(int logicServerId, String akkaPath, boolean isMainLogicServer, ActorRef actor) {
		// 判断是否注册过
		if (logicServerIdToActorMap.containsKey(logicServerId) == true) {
			return false;
		}
		// 判断是否是主逻辑服务器
		if (isMainLogicServer == true) {
			// 如果已经注册了其他主逻辑服务器，则注册失败
			if (this.mainLogicServeId > 0) {
				return false;
			}
			this.mainLogicServeId = logicServerId;
			logicServerLoadBalanceService.setMainLogicServerId(logicServerId);
		}
		// 将逻辑服务器id与其代理actor对应关系，存储带map中
		logicServerIdToActorMap.put(logicServerId, actor);
		// 将代理actor与逻辑服务器id对应关系，存储带map中
		logicActorToServerIdMap.put(actor, logicServerId);
		// 设置该逻辑服务器id与其akkaPath的对应关系（负载均衡相关）
		logicServerLoadBalanceService.setOneLogicServerIdToAkkaPath(logicServerId, akkaPath);
		return true;
	}

	/**
	 * 根据逻辑服务器id,移除该逻辑服务器注册信息
	 * @param logicServerId		逻辑服务器id
	 * @return
	 */
	public boolean removeLogicServer(int logicServerId) {
		if (logicServerIdToActorMap.containsKey(logicServerId) == true) {
			ActorRef actor = logicServerIdToActorMap.remove(logicServerId);
			logicActorToServerIdMap.remove(actor);
			// 移除逻辑服务器与其akkaPath的绑定关系
			logicServerLoadBalanceService.removeOneLogicServerIdToAkkaPath(logicServerId);
			if (logicServerId == this.mainLogicServeId) {
				this.mainLogicServeId = 0;
				logicServerLoadBalanceService.setMainLogicServerId(0);
			}
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 根据逻辑服务器对应的代理Actor信息，移除该逻辑服务器的注册信息
	 * @param actor
	 * @return
	 */
	public boolean removeLogicServer(ActorRef actor) {
		if (logicActorToServerIdMap.containsKey(actor) == true) {
			int logicServerId = logicActorToServerIdMap.remove(actor);
			logicServerIdToActorMap.remove(logicServerId);
			logicServerLoadBalanceService.removeOneLogicServerIdToAkkaPath(logicServerId);
			if (logicServerId == this.mainLogicServeId) {
				this.mainLogicServeId = 0;
				logicServerLoadBalanceService.setMainLogicServerId(0);
			}
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 对战服务器注册
	 * @param battleServerId	对战服务器id
	 * @param akkaPath			远程访问akka地址
	 * @param actor				所属actor代理
	 * @return
	 */
	public boolean addBattleServer(int battleServerId, String akkaPath, ActorRef actor) {
		battleServerIdToActorMap.put(battleServerId, actor);
		battleActorToServerIdMap.put(actor, battleServerId);
		battleServerLoadBalanceService.setOneBattleServerIdToAkkaPath(battleServerId, akkaPath);
		return true;
	}

	/**
	 * 移除已注册对战服务器信息By对战服务器id
	 * @param battleServerId
	 * @return
	 */
	public boolean removeBattleServer(int battleServerId) {
		if (battleServerIdToActorMap.containsKey(battleServerId) == true) {
			ActorRef actor = battleServerIdToActorMap.remove(battleServerId);
			battleActorToServerIdMap.remove(actor);
			battleServerLoadBalanceService.removeOneBattleServerIdToAkkaPath(battleServerId);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 移除已注册对战服务器信息By对ActorRef
	 * @param actor
	 * @return
	 */
	public boolean removeBattleServer(ActorRef actor) {
		if (battleActorToServerIdMap.containsKey(actor) == true) {
			int battleServerId = battleActorToServerIdMap.remove(actor);
			battleServerIdToActorMap.remove(battleServerId);
			battleServerLoadBalanceService.removeOneBattleServerIdToAkkaPath(battleServerId);
			return true;
		} else {
			return false;
		}
	}

	/**==========================================================*/
	/**
	 * 聊天服务器注册
	 * @param chatServerId	聊天服务器id
	 * @param akkaPath			远程访问akka地址
	 * @param actor				所属actor代理
	 * @return
	 */
	public boolean addChatServer(int chatServerId, String akkaPath, ActorRef actor) {
		chatServerIdToActorMap.put(chatServerId, actor);
		chatActorToServerIdMap.put(actor, chatServerId);
		chatServerLoadBalanceService.setOneChatServerIdToAkkaPath(chatServerId, akkaPath);
		return true;
	}

	/**
	 * 移除已注册聊天服务器信息By聊天服务器id
	 * @param chatServerId
	 * @return
	 */
	public boolean removeChatServer(int chatServerId) {
		if (chatServerIdToActorMap.containsKey(chatServerId) == true) {
			ActorRef actor = chatServerIdToActorMap.remove(chatServerId);
			chatActorToServerIdMap.remove(actor);
			chatServerLoadBalanceService.removeOneChatServerIdToAkkaPath(chatServerId);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 移除已注册聊天服务器信息By对ActorRef
	 * @param actor
	 * @return
	 */
	public boolean removeChatServer(ActorRef actor) {
		if (chatActorToServerIdMap.containsKey(actor) == true) {
			int chatServerId = chatActorToServerIdMap.remove(actor);
			chatServerIdToActorMap.remove(chatServerId);
			chatServerLoadBalanceService.removeOneChatServerIdToAkkaPath(chatServerId);
			return true;
		} else {
			return false;
		}
	}

	/**==========================================================*/

	/**
	 * 注册网关服务器
	 * @param gatewayId		网关服务器id
	 * @param akkaPath		远程调用akka地址
	 * @param connectPath	连接地址（ip:port）
	 * @param actor			所属actor代理
	 * @return
	 */
	public boolean addGateway(int gatewayId, String akkaPath, String connectPath, ActorRef actor) {
		if (gatewayIdToActorMap.containsKey(gatewayId) == true) {
			return false;
		} else {
			gatewayIdToActorMap.put(gatewayId, actor);
			gatewayActorToIdMap.put(actor, gatewayId);
			gatewayServerLoadBalanceService.setOneGatewayIdToAkkaPath(gatewayId, akkaPath);
			gatewayServerLoadBalanceService.setOneGatewayIdToConnectPath(gatewayId, connectPath);
			return true;
		}
	}

	/**
	 * 移除网关服务器by网关服务器id
	 * @param gatewayId
	 * @return
	 */
	public boolean removeGateway(int gatewayId) {
		if (gatewayIdToActorMap.containsKey(gatewayId) == true) {
			ActorRef actor = gatewayIdToActorMap.remove(gatewayId);
			gatewayActorToIdMap.remove(actor);
			gatewayServerLoadBalanceService.removeOneGatewayIdToAkkaPath(gatewayId);
			gatewayServerLoadBalanceService.removeOneGatewayIdToConnectPath(gatewayId);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 移除网关服务器byActorRef
	 * @param actor
	 * @return
	 */
	public boolean removeGateway(ActorRef actor) {
		if (gatewayActorToIdMap.containsKey(actor) == true) {
			int gatewayId = gatewayActorToIdMap.remove(actor);
			gatewayIdToActorMap.remove(gatewayId);
			gatewayServerLoadBalanceService.removeOneGatewayIdToAkkaPath(gatewayId);
			gatewayServerLoadBalanceService.removeOneGatewayIdToConnectPath(gatewayId);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 发送远程消息到网关服务器（作用：如停服、刷新等）
	 * @param remoteMsg			远程消息载体
	 * @param serverIds			服务器列表
	 * @param protobufClass		protobuf class
	 */
	public void sendRemoteMsgToGateway(RemoteMessage remoteMsg, List<Integer> serverIds, Class protobufClass) {
		logger.info(
				"sendRemoteMsgToGateway serverIdList = {}, rpcNum = {}, rpcName = {}, errorCode = {}, means {}, protobuf text = {}",
				StringUtil.getCollectionMemberString(serverIds, ","), remoteMsg.getRpcNum(),
				RemoteRpcNameEnum.forNumber(remoteMsg.getRpcNum()), remoteMsg.getErrorCode(),
				RemoteRpcErrorCodeEnum.forNumber(remoteMsg.getErrorCode()),
				protobufClass != null ? remoteMsg.getProtobufText(protobufClass) : "null");
		for (Integer oneServerId : serverIds) {
			ActorRef actor = gatewayIdToActorMap.get(oneServerId);
			if (actor != null) {
				actor.tell(remoteMsg, gmActor);
			}
		}
	}

	/**
	 * 发送远程消息到所有网关服务器（广播）
	 * @param remoteMsg			远程消息载体
	 * @param protobufClass		class
	 */
	public void sendRemoteMsgToAllGateway(RemoteMessage remoteMsg, Class protobufClass) {
		logger.info(
				"sendRemoteMsgToAllGateway serverIdList = {}, rpcNum = {}, rpcName = {}, errorCode = {}, means {}, protobuf text = {}",
				StringUtil.getCollectionMemberString(gatewayIdToActorMap.keySet(), ","), remoteMsg.getRpcNum(),
				RemoteRpcNameEnum.forNumber(remoteMsg.getRpcNum()), remoteMsg.getErrorCode(),
				RemoteRpcErrorCodeEnum.forNumber(remoteMsg.getErrorCode()),
				protobufClass != null ? remoteMsg.getProtobufText(protobufClass) : "null");
		for (ActorRef oneGatewayActor : gatewayActorToIdMap.keySet()) {
			oneGatewayActor.tell(remoteMsg, gmActor);
		}
	}

	/**
	 * 发送远程消息到所有逻辑服务器（广播）
	 * @param remoteMsg
	 * @param protobufClass
	 */
	public void sendRemoteMsgToAllLogicServer(RemoteMessage remoteMsg, Class protobufClass) {
		logger.info(
				"sendRemoteMsgToAllLogicServer serverIdList = {}, rpcNum = {}, rpcName = {}, errorCode = {}, means {}, protobuf text = {}",
				StringUtil.getCollectionMemberString(logicServerIdToActorMap.keySet(), ","), remoteMsg.getRpcNum(),
				RemoteRpcNameEnum.forNumber(remoteMsg.getRpcNum()), remoteMsg.getErrorCode(),
				RemoteRpcErrorCodeEnum.forNumber(remoteMsg.getErrorCode()),
				protobufClass != null ? remoteMsg.getProtobufText(protobufClass) : "null");
		for (ActorRef oneLogicServerActor : logicActorToServerIdMap.keySet()) {
			oneLogicServerActor.tell(remoteMsg, gmActor);
		}
	}

	/**
	 * 发送远程消息到逻辑服务器
	 * @param remoteMsg
	 * @param serverIds
	 * @param protobufClass
	 */
	public void sendRemoteMsgToLogicServer(RemoteMessage remoteMsg, List<Integer> serverIds, Class protobufClass) {
		logger.info(
				"sendRemoteMsgToLogicServer serverIdList = {}, rpcNum = {}, rpcName = {}, errorCode = {}, means {}, protobuf text = {}",
				StringUtil.getCollectionMemberString(serverIds, ","), remoteMsg.getRpcNum(),
				RemoteRpcNameEnum.forNumber(remoteMsg.getRpcNum()), remoteMsg.getErrorCode(),
				RemoteRpcErrorCodeEnum.forNumber(remoteMsg.getErrorCode()),
				protobufClass != null ? remoteMsg.getProtobufText(protobufClass) : "null");
		for (Integer oneServerId : serverIds) {
			ActorRef actor = logicServerIdToActorMap.get(oneServerId);
			if (actor != null) {
				actor.tell(remoteMsg, gmActor);
			}
		}
	}

	/**
	 * 发送远程消息到所有对战服务器（广播）
	 * @param remoteMsg
	 * @param protobufClass
	 */
	public void sendRemoteMsgToAllBattleServer(RemoteMessage remoteMsg, Class protobufClass) {
		logger.info(
				"sendRemoteMsgToAllBattleServer serverIdList = {}, rpcNum = {}, rpcName = {}, errorCode = {}, means {}, protobuf text = {}",
				StringUtil.getCollectionMemberString(battleServerIdToActorMap.keySet(), ","), remoteMsg.getRpcNum(),
				RemoteRpcNameEnum.forNumber(remoteMsg.getRpcNum()), remoteMsg.getErrorCode(),
				RemoteRpcErrorCodeEnum.forNumber(remoteMsg.getErrorCode()),
				protobufClass != null ? remoteMsg.getProtobufText(protobufClass) : "null");
		for (ActorRef oneBattleServerActor : battleActorToServerIdMap.keySet()) {
			oneBattleServerActor.tell(remoteMsg, gmActor);
		}
	}

	/**
	 * 发送远程消息到对战服务器
	 * @param remoteMsg
	 * @param serverIds
	 * @param protobufClass
	 */
	public void sendRemoteMsgToBattleServer(RemoteMessage remoteMsg, List<Integer> serverIds, Class protobufClass) {
		logger.info(
				"sendRemoteMsgToBattleServer serverIdList = {}, rpcNum = {}, rpcName = {}, errorCode = {}, means {}, protobuf text = {}",
				StringUtil.getCollectionMemberString(serverIds, ","), remoteMsg.getRpcNum(),
				RemoteRpcNameEnum.forNumber(remoteMsg.getRpcNum()), remoteMsg.getErrorCode(),
				RemoteRpcErrorCodeEnum.forNumber(remoteMsg.getErrorCode()),
				protobufClass != null ? remoteMsg.getProtobufText(protobufClass) : "null");
		for (Integer oneServerId : serverIds) {
			ActorRef actor = battleServerIdToActorMap.get(oneServerId);
			if (actor != null) {
				actor.tell(remoteMsg, gmActor);
			}
		}
	}

	/**
	 * 发送远程消息到所有聊天服务器
	 * @param remoteMsg
	 * @param protobufClass
	 */
	public void sendRemoteMsgToAllChatServer(RemoteMessage remoteMsg, Class protobufClass) {
		logger.info(
				"sendRemoteMsgToAllChatServer rpcNum = {}, rpcName = {}, errorCode = {}, means {}, protobuf text = {}", remoteMsg.getRpcNum(),
				RemoteRpcNameEnum.forNumber(remoteMsg.getRpcNum()), remoteMsg.getErrorCode(),
				RemoteRpcErrorCodeEnum.forNumber(remoteMsg.getErrorCode()),
				protobufClass != null ? remoteMsg.getProtobufText(protobufClass) : "null");
		for (ActorRef oneChatServerActor  : chatActorToServerIdMap.keySet()) {
			oneChatServerActor.tell(remoteMsg, gmActor);
		}
	}

	/**
	 * 发送远程消息到聊天服务器
	 * @param remoteMsg
	 * @param serverIds
	 * @param protobufClass
	 */
	public void sendRemoteMsgToChatServer(RemoteMessage remoteMsg, List<Integer> serverIds, Class protobufClass) {
		logger.info(
				"sendRemoteMsgToChatServer serverIdList = {}, rpcNum = {}, rpcName = {}, errorCode = {}, means {}, protobuf text = {}",
				StringUtil.getCollectionMemberString(serverIds, ","), remoteMsg.getRpcNum(),
				RemoteRpcNameEnum.forNumber(remoteMsg.getRpcNum()), remoteMsg.getErrorCode(),
				RemoteRpcErrorCodeEnum.forNumber(remoteMsg.getErrorCode()),
				protobufClass != null ? remoteMsg.getProtobufText(protobufClass) : "null");
		for (Integer oneServerId : serverIds) {
			ActorRef actor = chatServerIdToActorMap.get(oneServerId);
			if (actor != null) {
				actor.tell(remoteMsg, gmActor);
			}
		}
	}

	/**
	 * 根据远程服务器类型与服务器id,获取服务器IP与端口
	 * @param serverType
	 * @param serverId
	 * @return
	 */
	public String getServerIpWithPort(RemoteServerTypeEnum serverType, int serverId) {
		ActorRef actor = null;
		if (serverType == RemoteServerTypeEnum.ServerTypeLogic) {
			actor = logicServerIdToActorMap.get(serverId);
		} else if (serverType == RemoteServerTypeEnum.ServerTypeBattle) {
			actor = battleServerIdToActorMap.get(serverId);
		} else if (serverType == RemoteServerTypeEnum.ServerTypeGateway) {
			actor = gatewayIdToActorMap.get(serverId);
		} else {
			logger.error("getServerIp error, not support serverType = {}", serverType);
			return null;
		}

		if (actor != null) {
			Address address = actor.path().address();
			return address.host().get() + ":" + address.port().get();
		} else {
			return null;
		}
	}
}
