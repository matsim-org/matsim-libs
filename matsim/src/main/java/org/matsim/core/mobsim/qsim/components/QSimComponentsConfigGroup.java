
/* *********************************************************************** *
 * project: org.matsim.*
 * QSimComponentsConfigGroup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.core.mobsim.qsim.components;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup.StringGetter;
import org.matsim.core.config.ReflectiveConfigGroup.StringSetter;
import org.matsim.core.mobsim.qsim.ActivityEngineModule;
import org.matsim.core.mobsim.qsim.PopulationModule;
import org.matsim.core.mobsim.qsim.TeleportationModule;
import org.matsim.core.mobsim.qsim.messagequeueengine.MessageQueueModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineModule;

public class QSimComponentsConfigGroup extends ConfigGroup {
	public static final String GROUP_NAME = "qsim_components";

	public static final String ACTIVE_COMPONENTS = "activeComponents";

	public static final List<String> DEFAULT_COMPONENTS = Arrays.asList(ActivityEngineModule.COMPONENT_NAME,
			QNetsimEngineModule.COMPONENT_NAME, TeleportationModule.COMPONENT_NAME, PopulationModule.COMPONENT_NAME,
			MessageQueueModule.COMPONENT_NAME);

	private List<String> activeComponents = new LinkedList<>(DEFAULT_COMPONENTS);

	public QSimComponentsConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public final Map<String, String> getComments() {
		Map<String, String> map = new HashMap<>();

		map.put(ACTIVE_COMPONENTS,
				"Defines which components are active and in which order they are registered. Depending on which extensions and contribs you use, it may be necessary to define additional components here. Default is: "
						+ String.join(", ", DEFAULT_COMPONENTS));

		return map;
	}

	public List<String> getActiveComponents() {
		return activeComponents;
	}

	public void setActiveComponents(List<String> activeComponents) {
		this.activeComponents = activeComponents;
	}

	@StringGetter(ACTIVE_COMPONENTS)
	public String getActiveComponentsAsString() {
		return String.join(", ", activeComponents);
	}

	@StringSetter(ACTIVE_COMPONENTS)
	public void setActiveComponentsAsString(String activeComponents) {
		this.activeComponents = interpretQSimComponents(activeComponents);
	}

	private List<String> interpretQSimComponents(String config) {
		List<String> elements = Arrays.asList(config.split(",")).stream().map(String::trim)
				.collect(Collectors.toList());

		if (elements.size() == 1 && elements.get(0).length() == 0) {
			return new LinkedList<>();
		}

		return elements;
	}
}
