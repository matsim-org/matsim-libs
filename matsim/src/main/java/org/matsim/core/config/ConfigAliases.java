package org.matsim.core.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ConfigAliases {

	private final static Logger LOG = LogManager.getLogger(ConfigAliases.class);

	private final Map<String, List<ConfigAlias>> aliases = new HashMap<>();

	public ConfigAliases() {
		this.addDefaultAliases();
	}

	public void addDefaultAliases() {
		this.addAlias("ReplanningAnnealer", "replanningAnnealer");
		this.addAlias("TimeAllocationMutator", "timeAllocationMutator");
		this.addAlias("JDEQSim", "jdeqsim");
		this.addAlias("controler", "controller");
		this.addAlias("planCalcScore", "scoring");
		this.addAlias("planscalcroute", "routing");
		this.addAlias("strategy", "replanning");
		this.addAlias("parallelEventHandling", "eventsManager");
		this.addAlias("freight", "freightCarriers");

		this.addAlias("BrainExpBeta", "brainExpBeta", "scoring");
		this.addAlias("PathSizeLogitBeta", "pathSizeLogitBeta", "scoring");
	}

	public void addAlias(String oldName, String newName, String... path) {
		this.aliases.computeIfAbsent(oldName, k -> new ArrayList<>(2)).add(new ConfigAlias(oldName, newName, path));
	}

	public void clearAliases() {
		this.aliases.clear();
	}

	public String resolveAlias(String oldName, Deque<String> pathStack) {
		List<ConfigAlias> definedAliases = this.aliases.get(oldName);
		if (definedAliases == null || definedAliases.isEmpty()) {
			return oldName;
		}
		for (ConfigAlias alias: definedAliases) {
			boolean matches = true;

			if (alias.path.length > pathStack.size()) {
				matches = false;
			} else {
				Iterator<String> iter = pathStack.iterator();
				for (int i = alias.path.length - 1; i >= 0; i--) {
					if (iter.hasNext()) {
						String name = iter.next();
						if (!name.equals(alias.path[i])) {
							matches = false;
							break;
						}
					} else {
						matches = false;
						break;
					}
				}
			}

			if (matches) {
				String stack = pathStack.stream().collect(Collectors.joining(" < ", oldName + " < ", " /"));
				LOG.warn("Config name '{}' is deprecated, please use '{}' instead. Config path: {}", oldName, alias.newName, stack);
				return alias.newName;
			}
		}
		return oldName;
	}

	public record ConfigAlias(String oldName, String newName, String[] path) {
	}

}
