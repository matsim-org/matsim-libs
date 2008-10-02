/* *********************************************************************** *
 * project: org.matsim.*
 * ManipulateChangeEvents.java
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

package playground.gregor.evacuation.changeeventsmanipulation;

import java.util.List;

import org.matsim.network.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkChangeEvent;
import org.matsim.network.NetworkChangeEventsParser;
import org.matsim.network.NetworkChangeEventsWriter;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkChangeEvent.ChangeType;
import org.matsim.network.NetworkChangeEvent.ChangeValue;


/**
 * this class reads a change events file adds some new change change events and writes to a new file.
 * for the time beeing this class generates freespeed change events to get a KOGAMI like network.
 * @author laemmel
 *
 */
public class ManipulateChangeEvents {
	
	final static String [] links = new String [] {"111288","11288", "102570", "2570", "105019", "5019", "108025" , "8025",
		"101236", "1236", "105241", "5241", "106821", "6821", "109894", "9894", "100054", "54", "109755", "9755", "109244", "9244",
		"1001", "101001"};
	
	
	public static void main(final String [] args) {
		
		String input = "../inputs/networks/padang_change_evac_v20080618_of20min.xml";
		String output = "../inputs/networks/padang_change_evac_v20080618_of20min_KOGAMI.xml";
		String netfile = "../inputs/networks/padang_net_v20080618.xml";
		
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netfile);
		
		NetworkChangeEventsParser ncep = new NetworkChangeEventsParser(network);
		List<NetworkChangeEvent> e = ncep.parseEvents(input);
		
		
		ChangeValue v = new ChangeValue(ChangeType.ABSOLUTE,0);
		NetworkChangeEvent ee = new NetworkChangeEvent(0);
		ee.setFreespeedChange(v);
		
		for (int i = 0; i < links.length; i++) {
			Link link = network.getLink(links[i]);
			ee.addLink(link);
			
		}
		e.add(ee);
		new NetworkChangeEventsWriter().write(output, e);
		
	}

}
