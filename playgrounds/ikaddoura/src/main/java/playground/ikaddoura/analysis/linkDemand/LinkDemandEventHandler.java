/* *********************************************************************** *
 * project: org.matsim.*
 * LinksEventHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.ikaddoura.analysis.linkDemand;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

/**
 * @author Ihab
 *
 */
public class LinkDemandEventHandler implements  LinkLeaveEventHandler {
	private static final Logger log = Logger.getLogger(LinkDemandEventHandler.class);
	private Network network;
	
	private Map<Id<Link>,Integer> linkId2demand = new HashMap<Id<Link>, Integer>();

	public LinkDemandEventHandler(Network network) {
		this.network = network;
	}

	@Override
	public void reset(int iteration) {
		this.linkId2demand.clear();
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		
		if (this.linkId2demand.containsKey(event.getLinkId())) {
			int agents = this.linkId2demand.get(event.getLinkId());
			this.linkId2demand.put(event.getLinkId(), agents + 1);
			
		} else {
			this.linkId2demand.put(event.getLinkId(), 1);
		}
	}

	public void printResults(String fileName) {
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("link;agents");
			bw.newLine();
			
			for (Id<Link> linkId : this.network.getLinks().keySet()){
				
				bw.write(linkId + ";" + this.linkId2demand.get(linkId));
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Map<Id<Link>, Integer> getLinkId2demand() {
		return linkId2demand;
	}

}
