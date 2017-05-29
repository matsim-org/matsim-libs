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
package playground.ikaddoura.analysis.dynamicLinkDemand;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.misc.Time;

/**
 * @author Ihab
 *
 */
public class DynamicLinkDemandEventHandler implements  LinkLeaveEventHandler {
	private static final Logger log = Logger.getLogger(DynamicLinkDemandEventHandler.class);
	
	private double timeBinSize = 3600.;
	private Network network;
	
	private SortedMap<Double, Map<Id<Link>, Integer>> timeBinEndTime2linkId2demand = new TreeMap<Double, Map<Id<Link>, Integer>>();

	public DynamicLinkDemandEventHandler(Network network) {
		this.network = network;
	}

	@Override
	public void reset(int iteration) {
		this.timeBinEndTime2linkId2demand.clear();
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		
		double currentTimeBin = (Math.floor(event.getTime() / this.timeBinSize) + 1) * this.timeBinSize;
		
		if (this.timeBinEndTime2linkId2demand.containsKey(currentTimeBin)) {
			if (this.timeBinEndTime2linkId2demand.get(currentTimeBin).containsKey(event.getLinkId())) {
				int agents = this.timeBinEndTime2linkId2demand.get(currentTimeBin).get(event.getLinkId());
				this.timeBinEndTime2linkId2demand.get(currentTimeBin).put(event.getLinkId(), agents + 1);
			} else {
				this.timeBinEndTime2linkId2demand.get(currentTimeBin).put(event.getLinkId(), 1);
			}
			
		} else {
			Map<Id<Link>, Integer> linkId2demand = new HashMap<Id<Link>, Integer>();
			linkId2demand.put(event.getLinkId(), 1);
			this.timeBinEndTime2linkId2demand.put(currentTimeBin, linkId2demand);
		}
	}

	public void printResults(String path) {
		
		String fileName = path + "dynamicLinkDemand_numberOfLinkLeaveEvents.csv";
		File file1 = new File(fileName);
		File file2 = new File(fileName + "t");
		
		try {
			BufferedWriter bw1 = new BufferedWriter(new FileWriter(file1));
			BufferedWriter bw2 = new BufferedWriter(new FileWriter(file2));

			bw1.write("link");
			bw2.write("\"String\"");
			
			for (Double timeBinEndTime : this.timeBinEndTime2linkId2demand.keySet()) {
				bw1.write(";" + Time.writeTime(timeBinEndTime, Time.TIMEFORMAT_HHMMSS));
//				bw1.write(";" + timeBinEndTime);
				bw2.write(",\"Real\"");
			}
			bw1.newLine();
			
			for (Id<Link> linkId : this.network.getLinks().keySet()){
				
				bw1.write(linkId.toString());
				
				for (Double timeBinEndTime : this.timeBinEndTime2linkId2demand.keySet()) {
					int agents = 0;
					if (this.timeBinEndTime2linkId2demand.get(timeBinEndTime).containsKey(linkId)) {
						agents = this.timeBinEndTime2linkId2demand.get(timeBinEndTime).get(linkId);
					}
					bw1.write(";" + agents);
				}
				bw1.newLine();
			}
			
			bw1.close();
			bw2.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
