/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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
package playground.droeder.sketchPlanning;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkChangeEventFactoryImpl;
import org.matsim.core.network.NetworkChangeEventsWriter;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.droeder.DRPaths;

/**
 * @author droeder
 *
 */
public class ChangeEventProducer {
	
	private static String NETWORK = "D:/VSP/shared-svn/studies/countries/de/berlin/_counts/iv_counts/network-ba16_17_storkower_ext.xml.gz";
	private static String OUTFILE = DRPaths.STUDIESSKETCH + "changeEvents_ba17_storkower_ext.xml";
	private static Set<String> ORIGID2CHANGE = new  HashSet<String>(){{
//		add("ba16");
//		add("ba17");
//		add("ba17storkower");
	}};
	
	public static void main(String[] args){
		
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		NetworkReaderMatsimV1 netReader = new NetworkReaderMatsimV1(sc);
		netReader.parse(NETWORK);
		
		final NetworkChangeEvent e = new NetworkChangeEventFactoryImpl().createNetworkChangeEvent(0.0);
		for(Link l : sc.getNetwork().getLinks().values()){
			LinkImpl ll = (LinkImpl)l;
			if(ORIGID2CHANGE.contains(ll.getOrigId())){
				e.addLink(l);
			}
		}
		e.setFlowCapacityChange(new ChangeValue(ChangeType.ABSOLUTE, 0.0));
		e.setFreespeedChange(new ChangeValue(ChangeType.ABSOLUTE, 0.0));
		Set<NetworkChangeEvent> events = new HashSet<NetworkChangeEvent>(){{
			add(e);
		}};
		
		NetworkChangeEventsWriter writer = new NetworkChangeEventsWriter();
		writer.write(OUTFILE,events);
	}

}
