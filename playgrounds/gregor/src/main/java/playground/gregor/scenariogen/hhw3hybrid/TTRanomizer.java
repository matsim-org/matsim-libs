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
package playground.gregor.scenariogen.hhw3hybrid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkChangeEventFactoryImpl;
import org.matsim.core.network.NetworkChangeEventsWriter;
import org.matsim.core.scenario.ScenarioUtils;

public class TTRanomizer {

	public static void main(String [] args) {
		Config c = ConfigUtils.createConfig();
		c.network().setTimeVariantNetwork(false);
		c.network().setChangeEventInputFile(null);
		ConfigUtils.loadConfig(c, "/Users/laemmel/arbeit/papers/2015/trgindia2015/hhwsim/input/config.xml");
		Scenario sc = ScenarioUtils.loadScenario(c);
		List<Link> jps = new ArrayList<>();
		
		
		
		for (Link l : sc.getNetwork().getLinks().values()) {
			if (l.getId().toString().contains("jps_out")){
				jps.add(l);
			}
		}
		NetworkChangeEventFactoryImpl fac = new NetworkChangeEventFactoryImpl();
		Collection<NetworkChangeEvent> events = new ArrayList<>();
		int cnt = 0;
		for (double time = 0; time < 2*7200; time += 30) {
			NetworkChangeEvent e = fac.createNetworkChangeEvent(time);
			ChangeValue val = new NetworkChangeEvent.ChangeValue(ChangeType.ABSOLUTE,0.004);
			e.setFreespeedChange(val);
			events.add(e);
			
			NetworkChangeEvent e2 = fac.createNetworkChangeEvent(time);
			ChangeValue val2 = new NetworkChangeEvent.ChangeValue(ChangeType.ABSOLUTE,2);
			e2.setFreespeedChange(val2);
			events.add(e2);
			
			if (cnt == jps.size()) {
				cnt=0;
			}
			int idx = 0;
			for (Link l : jps) {
				
				if (cnt == idx){
					System.out.println(idx + " " + time);		
					e2.addLink(l);
				} else {
					e.addLink(l);
				}
				idx++;
			}
			cnt++;
		}
		
		NetworkChangeEventsWriter w = new NetworkChangeEventsWriter();
		
		w.write("/Users/laemmel/arbeit/papers/2015/trgindia2015/hhwsim/input/rndChangeEvents.xml.gz", events);
	}
}
