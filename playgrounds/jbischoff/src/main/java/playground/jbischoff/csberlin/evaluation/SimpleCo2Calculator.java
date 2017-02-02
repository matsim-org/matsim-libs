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

/**
 * 
 */
package playground.jbischoff.csberlin.evaluation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

import playground.jbischoff.utils.JbUtils;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class SimpleCo2Calculator {
	final static String EVENTSFILE = "D:/runs-svn/bmw_carsharing/basecase/bc09-nopark/output_events.xml.gz";
	final static String NETWORKFILE = "D:/runs-svn/bmw_carsharing/basecase/bc09-nopark/output_network.xml.gz";
	final static String LINKSFILE = "C:/Users/Joschka/Documents/shared-svn/projects/bmw_carsharing/data/klauslinks.txt";
	
	final static String OUTFILE = "D:/runs-svn/bmw_carsharing/basecase/bc09-nopark/klaus-links-use.csv";
//	final static String EVENTSFILE = "D:/runs-svn/bmw_carsharing/run23/run23.output_events.xml.gz";
//	final static String NETWORKFILE = "D:/runs-svn/bmw_carsharing/run23/run23.output_network.xml.gz";
//	final static String LINKSFILE = "C:/Users/Joschka/Documents/shared-svn/projects/bmw_carsharing/data/parkplaetze.txt";
//	
//	final static String OUTFILE = "D:/runs-svn/bmw_carsharing/run23/emissions_co2.csv";
	final static double KG_PER_KM = 1.0;
	// according to http://www.co2online.de/klima-schuetzen/mobilitaet/auto-co2-ausstoss/
	
	public static void main(String[] args) {
		new SimpleCo2Calculator().run();
	}

	private void run(){
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(NETWORKFILE);
		EventsManager events = EventsUtils.createEventsManager();
		final Map<Id<Link>,MutableDouble> links = new HashMap<>();
		TabularFileParserConfig config = new TabularFileParserConfig();
        config.setDelimiterTags(new String[] {"\t"});
        config.setFileName(LINKSFILE);
        config.setCommentTags(new String[] { "#" });
        new TabularFileParser().parse(config, new TabularFileHandler() {
			@Override
			public void startRow(String[] row) {
				Id<Link> linkId = Id.createLinkId(row[0]);
				links.put(linkId, new MutableDouble(0.0));
			}
		
        });
        events.addHandler(new LinkEnterEventHandler() {
			
			@Override
			public void reset(int iteration) {
			
			}
			
			@Override
			public void handleEvent(LinkEnterEvent event) {
				if (!event.getVehicleId().toString().startsWith("ff")){
				if (links.containsKey(event.getLinkId())){
					double length = network.getLinks().get(event.getLinkId()).getLength() / 1000.0;
					double emmission = KG_PER_KM*length;
					links.get(event.getLinkId()).add(emmission);	
					
				}
				}
			}
		});
        new MatsimEventsReader(events).readFile(EVENTSFILE);
        JbUtils.map2Text(links, OUTFILE, ";", "Link;Co2(kg)");
        
	}
}
