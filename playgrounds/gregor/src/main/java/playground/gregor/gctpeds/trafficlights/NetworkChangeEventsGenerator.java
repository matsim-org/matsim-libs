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
package playground.gregor.gctpeds.trafficlights;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkChangeEventFactoryImpl;
import org.matsim.core.network.NetworkChangeEventsWriter;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.gregor.casim.simulation.physics.AbstractCANetwork;

public class NetworkChangeEventsGenerator {

	//0 2 494

	private final double green = 120;
	private final double red = 120;

	private Scenario sc;

	public NetworkChangeEventsGenerator(Scenario sc) {
		this.sc = sc;
	}

	private void run() {
		List<NetworkChangeEvent> events = new ArrayList<>();
		NetworkChangeEventFactoryImpl fac = new NetworkChangeEventFactoryImpl();
		for (double time = 6*3600; time < 20*3600; time += green) {
			{
				NetworkChangeEvent e0 = fac.createNetworkChangeEvent(time);
				ChangeValue freespeedChange = new ChangeValue(ChangeType.ABSOLUTE, 0.01);
				e0.setFreespeedChange(freespeedChange);
				e0.addLink(this.sc.getNetwork().getLinks().get(Id.createLinkId(0)));
				e0.addLink(this.sc.getNetwork().getLinks().get(Id.createLinkId(2)));
				events.add(e0);
			}
			time += red;
			{
				NetworkChangeEvent e0 = fac.createNetworkChangeEvent(time);
				ChangeValue freespeedChange = new ChangeValue(ChangeType.ABSOLUTE, AbstractCANetwork.V_HAT);
				e0.setFreespeedChange(freespeedChange);
				e0.addLink(this.sc.getNetwork().getLinks().get(Id.createLinkId(0)));
				e0.addLink(this.sc.getNetwork().getLinks().get(Id.createLinkId(2)));
				events.add(e0);
			}
		}

		for (double time = 6*3600; time < 20*3600; time += red) {
			{
				NetworkChangeEvent e0 = fac.createNetworkChangeEvent(time);
				ChangeValue freespeedChange = new ChangeValue(ChangeType.ABSOLUTE,  AbstractCANetwork.V_HAT);
				e0.setFreespeedChange(freespeedChange);
				e0.addLink(this.sc.getNetwork().getLinks().get(Id.createLinkId(494)));
				e0.addLink(this.sc.getNetwork().getLinks().get(Id.createLinkId(470)));
				events.add(e0);
			}
			time += green;
			{
				NetworkChangeEvent e0 = fac.createNetworkChangeEvent(time);
				ChangeValue freespeedChange = new ChangeValue(ChangeType.ABSOLUTE,0.01);
				e0.setFreespeedChange(freespeedChange);
				e0.addLink(this.sc.getNetwork().getLinks().get(Id.createLinkId(494)));
				e0.addLink(this.sc.getNetwork().getLinks().get(Id.createLinkId(470)));
				events.add(e0);
			}
		}
		((NetworkImpl)sc.getNetwork()).setNetworkChangeEvents(events);

	}

	public static void main(String [] args) {
		String configFile = "/Users/laemmel/devel/nyc/gct_vicinity/config.xml.gz";
		Config c = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(c, configFile);
		c.network().setTimeVariantNetwork(true);
		Scenario sc = ScenarioUtils.loadScenario(c);

		new NetworkChangeEventsGenerator(sc).run();

		c.network().setChangeEventsInputFile( "/Users/laemmel/devel/nyc/gct_vicinity/changeevents.xml.gz");
		new NetworkChangeEventsWriter().write(c.network().getChangeEventsInputFile(), ((NetworkImpl)sc.getNetwork()).getNetworkChangeEvents());
		new ConfigWriter(c).write("/Users/laemmel/devel/nyc/gct_vicinity/config.xml.gz");
	}



}
