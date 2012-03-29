package org.matsim.core.controler;

import java.util.HashMap;
import java.util.Map;

import org.matsim.core.mobsim.framework.MobsimFactory;

public class MobsimFactoryRegister {
	
	private Map<String, MobsimFactory> factoryMap = new HashMap<String, MobsimFactory>();
	
	public MobsimFactory getInstance(String mobsimType) {
		if (!factoryMap.containsKey(mobsimType)) {
			throw new IllegalArgumentException("Mobsim type " + mobsimType
					+ " doesn't exist.");
		}
		return factoryMap.get(mobsimType);
	}

	public void register(String string, MobsimFactory mobsimFactory) {
		factoryMap.put(string, mobsimFactory);
	}

}
