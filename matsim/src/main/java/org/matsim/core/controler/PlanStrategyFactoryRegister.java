package org.matsim.core.controler;

import java.util.HashMap;
import java.util.Map;

import org.matsim.core.replanning.PlanStrategyFactory;

public class PlanStrategyFactoryRegister {
	
	private Map<String, PlanStrategyFactory> factoryMap = new HashMap<String, PlanStrategyFactory>();
	
	public PlanStrategyFactory getInstance(String mobsimType) {
		if (!factoryMap.containsKey(mobsimType)) {
			throw new IllegalArgumentException("Plan strategy " + mobsimType
					+ " doesn't exist.");
		}
		return factoryMap.get(mobsimType);
	}

	public void register(String string, PlanStrategyFactory mobsimFactory) {
		if (string.contains(".")) {
			throw new IllegalArgumentException("Plan strategy names with a '.' are reserved for direct class loading.");
		}
		factoryMap.put(string, mobsimFactory);
	}

}
