package org.matsim.core.controler;

import java.util.HashMap;
import java.util.Map;

import org.matsim.core.replanning.PlanStrategyFactory;

public class PlanStrategyFactoryRegister {
	
	private Map<String, PlanStrategyFactory> factoryMap = new HashMap<String, PlanStrategyFactory>();
	
	public PlanStrategyFactory getInstance(String strategyType) {
		if (!factoryMap.containsKey(strategyType)) {
			throw new IllegalArgumentException("Plan strategy " + strategyType
					+ " doesn't exist.");
		}
		return factoryMap.get(strategyType);
	}

	public void register(String string, PlanStrategyFactory strategyFactory) {
		if (string.contains(".")) {
			throw new IllegalArgumentException("Plan strategy names with a '.' are reserved for direct class loading.");
		}
		factoryMap.put(string, strategyFactory);
	}

}
