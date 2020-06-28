package com.jacey.game.common.manager;

import java.util.ArrayList;
import java.util.List;


/**
 * @Description: 核心管理器--用于统一管理Manager（初始化与关闭）
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
public class CoreManager implements IManager {

	private static CoreManager instance = new CoreManager();
	private List<IManager> allManagers = new ArrayList<IManager>();

	public static CoreManager getInstance() {
		return instance;
	}

	/**
	 * 初始化每一个manager
	 */
	@Override
	public void init() {
		for (IManager m : allManagers) {
			m.init();
		}
	}

	@Override
	public void shutdown() {
		int size = allManagers.size();
		for (int i = size - 1; i >= 0; --i) {
			allManagers.get(i).shutdown();
		}
	}

	public void registManager(IManager m) {
		allManagers.add(m);
	}
}
