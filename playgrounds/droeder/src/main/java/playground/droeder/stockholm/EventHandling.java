/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.droeder.stockholm;

import org.apache.log4j.Logger;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

/**
 * @author droeder / Senozon Deutschland GmbH
 *
 */
public class EventHandling {

    @SuppressWarnings("unused")
    private static final Logger log = Logger.getLogger(EventHandling.class);

    public EventHandling() {

    }

    //############################################################
    //				main method
    //############################################################

    public static void main(String[] args) {
	String eventsfile = "E:\\sandbox\\org.matsim\\output\\pt-tutorial\\1\\ITERS\\it.25\\1.25.events.xml.gz";
	String networkfile = "E:\\sandbox\\org.matsim\\output\\pt-tutorial\\1\\1.output_network.xml.gz";
	
	
	Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	new MatsimNetworkReader(sc).readFile(networkfile);
	
	TravelTimeCalculator calculator = new TravelTimeCalculator(sc.getNetwork(), sc.getConfig().travelTimeCalculator());
	VolumesAnalyzer volumes = new VolumesAnalyzer(3600, 24 *3600, sc.getNetwork());
	
	EventsManager manager = EventsUtils.createEventsManager();
	manager.addHandler(calculator);
	manager.addHandler(volumes);
	new EventsReaderXMLv1(manager).parse(eventsfile);
	
	TravelTime tt = calculator.getLinkTravelTimes();
	System.out.println("TT");
	for(Link l: sc.getNetwork().getLinks().values()){
	    System.out.print(l.getId());
	    for(int i = 0; i < 24; i++) System.out.print("\t" + tt.getLinkTravelTime(l, i * 3600, null, null));
	    System.out.println();
	}
	
	System.out.println("");
	System.out.println("volumes");
	for(Link l: sc.getNetwork().getLinks().values()){
	    System.out.print(l.getId());
	    for(int i = 0; i < 24; i++) System.out.print("\t" + volumes.getVolumesForLink(l.getId())[i]);
	    System.out.println();
	}
	
    }

}

