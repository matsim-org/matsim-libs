/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.ev.temperature;
/*
 * created by jbischoff, 15.08.2018
 */

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import jakarta.inject.Inject;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.core.utils.misc.Time;

public class TemperatureManager implements MobsimBeforeSimStepListener, MobsimInitializedListener {
	private final Queue<TemperatureChange> temperatureChanges = new LinkedList<>();
	private final List<TemperatureChange> temperatureChangeList = new ArrayList<>();

	private final EventsManager events;

	@Inject
	TemperatureManager(Config config, EventsManager events) {
		this.events = events;
		TemperatureChangeConfigGroup temperatureChangeConfigGroup = (TemperatureChangeConfigGroup)config.getModules()
				.get(TemperatureChangeConfigGroup.GROUP_NAME);
		readTemperatureFile(
				ConfigGroup.getInputFileURL(config.getContext(), temperatureChangeConfigGroup.temperatureChangeFile),
				temperatureChangeConfigGroup.delimiter);
	}

	private void readTemperatureFile(URL temperatureFileURL, String delimiter) {
		TabularFileParserConfig tabularFileParserConfig = new TabularFileParserConfig();
		tabularFileParserConfig.setUrl(temperatureFileURL);
		tabularFileParserConfig.setDelimiterTags(new String[] { delimiter });
		new TabularFileParser().parse(tabularFileParserConfig, row -> {
			double time = Time.parseTime(row[0]);
			Id<Link> linkId = Id.createLinkId(row[1]);
			double temperature = Double.parseDouble(row[2]);
			TemperatureChange temperatureChange = new TemperatureChange(time, temperature, linkId);
			temperatureChangeList.add(temperatureChange);
		});
		Collections.sort(temperatureChangeList);
	}

	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
		while (!temperatureChanges.isEmpty() && temperatureChanges.peek().time <= e.getSimulationTime()) {
			TemperatureChange temperatureChange = temperatureChanges.poll();
			events.processEvent(new TemperatureChangeEvent(e.getSimulationTime(), temperatureChange.linkId,
					temperatureChange.temperature));
		}
	}

	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		temperatureChanges.addAll(temperatureChangeList);
	}

	static class TemperatureChange implements Comparable<TemperatureChange> {
		final Double time;
		final double temperature;
		final Id<Link> linkId;

		public TemperatureChange(double time, double temperature, Id<Link> linkId) {
			this.time = time;
			this.temperature = temperature;
			this.linkId = linkId;
		}

		@Override
		public int compareTo(TemperatureChange o) {
			return time.compareTo(o.time);
		}
	}
}
