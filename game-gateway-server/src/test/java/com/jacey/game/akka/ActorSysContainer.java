package com.jacey.game.akka;

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;

/**
 * @Description:
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
public class ActorSysContainer {

    private ActorSystem sys;

    private String chatAkkaPath = "akka://chatServer_1@192.168.56.1:50000/user/chatServerActor";
    private String battleAkkaPath = "akka://battleServer_1@192.168.2.193:40000/user/battleServerActor";

    /** Remote Actor */
    private ActorSelection chatRemoteActor;
    private ActorSelection battleRemoteActor;

    private ActorSysContainer() {
        sys = ActorSystem.create("MySystem1");
        chatRemoteActor = sys.actorSelection(chatAkkaPath);
        battleRemoteActor = sys.actorSelection(battleAkkaPath);
    }

    public ActorSystem getSystem() {
        return sys;
    }

    public ActorSelection getChatRemoteActor() {
        return chatRemoteActor;
    }

    public ActorSelection getBattleRemoteActor() {
        return battleRemoteActor;
    }

    private static ActorSysContainer instance = null;

    public static synchronized ActorSysContainer getInstance() {
        if (instance == null) {
            instance = new ActorSysContainer();
        }
        return instance;
    }
}