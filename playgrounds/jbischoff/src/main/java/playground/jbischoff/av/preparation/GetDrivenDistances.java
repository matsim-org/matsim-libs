/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.jbischoff.av.preparation;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author jbischoff
 *
 */
public class GetDrivenDistances {
	public static void main(String[] args) throws IOException {

		EventsManager events = EventsUtils.createEventsManager();
		String inputFile = "C:/Users/Joschka/Documents/shared-svn/projects/audi_av/runs/xx_with_events/21_jb/nullevents21.out.xml.gz";
		String distances = "C:/Users/Joschka/Documents/shared-svn/projects/audi_av/runs/xx_with_events/21_jb/distances.txt";
		String networkFile = "C:/Users/Joschka/Documents/shared-svn/projects/audi_av/scenario/networkc.xml.gz";

		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);

		final double[] distancesAtTime = { .0, .0, .0, .0, .0, .0, .0, .0, .0, .0, .0, .0, .0, .0, .0, .0, .0, .0, .0,
				.0, .0, .0, .0, .0, .0 };
		events.addHandler(new LinkLeaveEventHandler() {

			@Override
			public void reset(int iteration) {
			}

			@Override
			public void handleEvent(LinkLeaveEvent event) {
				double length = scenario.getNetwork().getLinks().get(event.getLinkId()).getLength();
				int time = (int) Math.floor(event.getTime() / 3600.0);
				distancesAtTime[time] += length;
			}
		});
		new MatsimEventsReader(events).readFile(inputFile);

		BufferedWriter writer = IOUtils.getBufferedWriter(distances);
		for (int i = 0; i < distancesAtTime.length; i++) {
			writer.append(i + "\t" + distancesAtTime[i]/1000);
			writer.newLine();
		}
		writer.flush();
		writer.close();

	}
}
