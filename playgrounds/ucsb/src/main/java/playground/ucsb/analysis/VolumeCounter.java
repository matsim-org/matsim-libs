/* *********************************************************************** *
 * project: org.matsim.*
 * UCSBSingleTripsConverter.java
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
package playground.ucsb.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.io.IOUtils;


/**
 * @author balmermi
 *
 */
public class VolumeCounter implements LinkLeaveEventHandler {
	
	private Map<Id,List<Integer>> linkVolumes = new HashMap<Id, List<Integer>>();

	public VolumeCounter(Network network) {
		for (Id lid : network.getLinks().keySet()) {
			List<Integer> vols = new ArrayList<Integer>(25);
			for (int i=0; i<25; i++) { vols.add(0); }
			linkVolumes.put(lid,vols);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.matsim.core.events.handler.EventHandler#reset(int)
	 */
	@Override
	public void reset(int iteration) {
		for (List<Integer> vols : linkVolumes.values()) {
			for (int i=0; i<vols.size(); i++) { vols.set(i,0); }
		}
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler#handleEvent(org.matsim.core.api.experimental.events.LinkLeaveEvent)
	 */
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		List<Integer> vols = linkVolumes.get(event.getLinkId());
		if (vols == null) { throw new RuntimeException("at LinkLeaveEvent [t"+event.getTime()+";p"+event.getDriverId()+";l"+event.getLinkId()+";v"+event.getVehicleId()+"]: link id not part of the network."); }
		int hour = (int)(event.getTime()/3600.0);
		if (hour < 24) { vols.set(hour,vols.get(hour)+1); }
		else { vols.set(24,vols.get(24)+1); }
	}
	
	public void writeVolumes(String filename) {
		BufferedWriter out = null;
		try {
			out = IOUtils.getBufferedWriter(filename);
			// write header
			out.write("linkId\tv0\tv1\tv2\tv3\tv4\tv5\tv6\tv7\tv8\tv9\tv10\tv11\tv12\tv13\tv14\tv15\tv16\tv17\tv18\tv19\tv20\tv21\tv22\tv23\tvge24\n");
			for (Entry<Id,List<Integer>> e : linkVolumes.entrySet()) {
				out.write(e.getKey().toString());
				for (Integer v : e.getValue()) { out.write("\t"); out.write(v.toString()); }
				out.write("\n");
			}
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
