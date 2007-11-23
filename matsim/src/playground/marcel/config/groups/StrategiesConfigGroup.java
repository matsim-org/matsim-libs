/* *********************************************************************** *
 * project: org.matsim.*
 * StrategiesConfigGroup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.marcel.config.groups;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.xml.sax.Attributes;

import playground.marcel.config.ConfigGroupI;
import playground.marcel.config.ConfigListI;

public class StrategiesConfigGroup implements ConfigGroupI {

	public static final String GROUP_NAME = "strategies";

	private static final String MAX_AGENT_PLAN_MEMORY_SIZE = "maxAgentPlanMemorySize";
	private static final String STRATEGIES = "strategies";

	private int maxAgentPlanMemorySize = 5;

	private final Set<String> paramKeySet = new LinkedHashSet<String>();
	private final Set<String> listKeySet = new LinkedHashSet<String>();

	private final StrategiesList strategiesList = new StrategiesList();

	public StrategiesConfigGroup() {
		this.paramKeySet.add(MAX_AGENT_PLAN_MEMORY_SIZE);
		this.listKeySet.add(STRATEGIES);
	}

	public String getName() {
		return GROUP_NAME;
	}

	public String getValue(final String key) {
		if (MAX_AGENT_PLAN_MEMORY_SIZE.equals(key)) {
			return Integer.toString(getMaxAgentPlanMemorySize());
		}
		throw new IllegalArgumentException(key);
	}

	public void setValue(final String key, final String value) {
		if (MAX_AGENT_PLAN_MEMORY_SIZE.equals(key)) {
			setMaxAgentPlanMemorySize(Integer.parseInt(value));
		}
		throw new IllegalArgumentException(key);
	}

	public ConfigListI getList(final String key) {
		if (STRATEGIES.equals(key)) {
			return this.strategiesList;
		}
		throw new IllegalArgumentException(key);
	}

	public Set<String> listKeySet() {
		return this.listKeySet;
	}

	public Set<String> paramKeySet() {
		return this.paramKeySet;
	}

	/* direct access */

	public void setMaxAgentPlanMemorySize(final int maxAgentPlanMemorySize) {
		this.maxAgentPlanMemorySize = maxAgentPlanMemorySize;
	}

	public int getMaxAgentPlanMemorySize() {
		return this.maxAgentPlanMemorySize;
	}

	public StrategySettings addStrategy(final String name) {
		return this.strategiesList.addStrategy(name);
	}

	public StrategySettings getStrategy(final String name) {
		return this.strategiesList.getStrategy(name);
	}

	/* helper classes */

	public class StrategySettings implements ConfigGroupI {

		private static final String WEIGHT = "weight";
		private static final String PLAN_SELECTOR = "planSelector";
		private static final String MODULES = "modules";
		private static final String CHANGES = "changes";

		private final String name;
		private double weight = 1.0;
		private String planSelector = "RandomPlan";

		private final Set<String> keySet = new LinkedHashSet<String>();
		private final Set<String> listSet = new LinkedHashSet<String>();

		private final ModulesList modules = new ModulesList();
		private final ChangesList changes = new ChangesList();

		public StrategySettings(final String name) {
			this.name = name;
			this.keySet.add(WEIGHT);
			this.keySet.add(PLAN_SELECTOR);
			this.listSet.add(MODULES);
			this.listSet.add(CHANGES);
		}

		public String getName() {
			return this.name;
		}

		public String getValue(final String key) {
			if (WEIGHT.equals(key)) {
				return Double.toString(getWeight());
			} else if (PLAN_SELECTOR.equals(key)) {
				return getPlanSelector();
			} else {
				throw new IllegalArgumentException(key);
			}
		}

		public void setValue(final String key, final String value) {
			if (WEIGHT.equals(key)) {
				setWeight(Double.parseDouble(value));
			} else if (PLAN_SELECTOR.equals(key)) {
				setPlanSelector(value);
			} else {
				throw new IllegalArgumentException(key);
			}
		}

		public ConfigListI getList(final String key) {
			if (MODULES.equals(key)) {
				return this.modules;
			} else if (CHANGES.equals(key)) {
				return this.changes;
			} else {
				throw new IllegalArgumentException(key);
			}
		}

		public Set<String> listKeySet() {
			return this.listSet;
		}

		public Set<String> paramKeySet() {
			return this.keySet;
		}

		public void startTag(final String name, final Attributes atts, final Stack<String> context) {
			throw new UnsupportedOperationException("only the default tags 'param', 'group' and 'entry' are supported, but not " + name + ".");
		}

		public void endTag(final String name, final String content, final Stack<String> context) {
			throw new UnsupportedOperationException("only the default tags 'param', 'group' and 'entry' are supported, but not " + name + ".");
		}

		/* direct access */

		public void setWeight(final double weight) {
			this.weight = weight;
		}

		public double getWeight() {
			return this.weight;
		}

		public void setPlanSelector(final String planSelector) {
			this.planSelector = planSelector;
		}

		public String getPlanSelector() {
			return this.planSelector;
		}

		public ModuleSettings addModule(final String name) {
			return this.modules.addModule(name);
		}

		public ModuleSettings getModule(final String name) {
			return this.modules.getModule(name);
		}

		public ChangeSettings addChange(final String name) {
			return this.changes.addChange(name);
		}

		public ChangeSettings getChange(final String name) {
			return this.changes.getChange(name);
		}



	}

	public class StrategiesList implements ConfigListI {

		private final Map<String, StrategySettings> strategies = new LinkedHashMap<String, StrategySettings>();

		public ConfigGroupI addGroup(final String key) {
			return addStrategy(key);
		}

		public ConfigGroupI getGroup(final String key) {
			return getStrategy(key);
		}

		public Set<String> keySet() {
			return this.strategies.keySet();
		}

		/* direct access */

		public StrategySettings addStrategy(final String name) {
			StrategySettings entry = new StrategySettings(name);
			this.strategies.put(name, entry);
			return entry;
		}

		public StrategySettings getStrategy(final String name) {
			return this.strategies.get(name);
		}
	}

	public class ModuleSettings implements ConfigGroupI {

		private final String name;

		private final Map<String, String> params = new LinkedHashMap<String, String>();

		public ModuleSettings(final String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

		public String getValue(final String key) {
			return this.params.get(key);
		}

		public void setValue(final String key, final String value) {
			this.params.put(key, value);
		}

		public ConfigListI getList(final String key) {
			throw new UnsupportedOperationException();
		}

		public Set<String> listKeySet() {
			return EMPTY_LIST_SET;
		}

		public Set<String> paramKeySet() {
			return this.params.keySet();
		}

	}

	public class ModulesList implements ConfigListI {

		private final Map<String, ModuleSettings> modules = new LinkedHashMap<String, ModuleSettings>();

		public ConfigGroupI addGroup(final String key) {
			return addModule(key);
		}

		public ConfigGroupI getGroup(final String key) {
			return getModule(key);
		}

		public Set<String> keySet() {
			return this.modules.keySet();
		}

		/* direct access */

		public ModuleSettings addModule(final String name) {
			ModuleSettings entry = new ModuleSettings(name);
			this.modules.put(name, entry);
			return entry;
		}

		public ModuleSettings getModule(final String name) {
			return this.modules.get(name);
		}
	}

	public class ChangeSettings implements ConfigGroupI {

		private static final String ITERATION = "iteration";
		private static final String WEIGHT = "weight";

		private final String name;

		private int iteration = 0;
		private double weight = Double.NaN;

		private final Set<String> keySet = new LinkedHashSet<String>();

		public ChangeSettings(final String name) {
			this.name = name;
			this.keySet.add(ITERATION);
			this.keySet.add(WEIGHT);
		}

		public String getName() {
			return this.name;
		}

		public String getValue(final String key) {
			if (ITERATION.equals(key)) {
				return Integer.toString(getIteration());
			} else if (WEIGHT.equals(key)) {
				return Double.toString(getWeight());
			} else {
				throw new IllegalArgumentException(key);
			}
		}

		public void setValue(final String key, final String value) {
			if (ITERATION.equals(key)) {
				setIteration(Integer.parseInt(value));
			} else if (WEIGHT.equals(key)) {
				setWeight(Double.parseDouble(value));
			} else {
				throw new IllegalArgumentException(key);
			}
		}

		public ConfigListI getList(final String key) {
			throw new UnsupportedOperationException();
		}

		public Set<String> listKeySet() {
			return EMPTY_LIST_SET;
		}

		public Set<String> paramKeySet() {
			return this.keySet;
		}

		/* direct access */

		public void setIteration(final int iteration) {
			this.iteration = iteration;
		}

		public int getIteration() {
			return this.iteration;
		}

		public void setWeight(final double weight) {
			this.weight = weight;
		}

		public double getWeight() {
			return this.weight;
		}

	}

	public class ChangesList implements ConfigListI {

		private final Map<String, ChangeSettings> changes = new LinkedHashMap<String, ChangeSettings>();

		public ConfigGroupI addGroup(final String key) {
			return addChange(key);
		}

		public ConfigGroupI getGroup(final String key) {
			return getChange(key);
		}

		public Set<String> keySet() {
			return this.changes.keySet();
		}

		/* direct access */

		public ChangeSettings addChange(final String key) {
			ChangeSettings entry = new ChangeSettings(key);
			this.changes.put(key, entry);
			return entry;
		}

		public ChangeSettings getChange(final String key) {
			return this.changes.get(key);
		}

	}


}
