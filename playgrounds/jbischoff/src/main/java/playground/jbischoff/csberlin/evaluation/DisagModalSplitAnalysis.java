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

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.mutable.MutableInt;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.IOUtils;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class DisagModalSplitAnalysis implements ActivityEndEventHandler, PersonDepartureEventHandler{

	
	
	private Network network;
	private Map<String,Geometry> zones = new HashMap<>();
	private Map<String,Map<String,MutableInt>> modeDeparturesperZone = new HashMap<>();
	private Map<Id<Person>,String> homeZone = new HashMap<>();
	
	/**
	 * 
	 */
	public DisagModalSplitAnalysis(Network network) {
		this.network=network;
		this.modeDeparturesperZone.put("other", new HashMap<String,MutableInt>());

	}
	/* (non-Javadoc)
	 * @see org.matsim.core.events.handler.EventHandler#reset(int)
	 */
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler#handleEvent(org.matsim.api.core.v01.events.PersonDepartureEvent)
	 */
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		String zone = this.homeZone.get(event.getPersonId());
		if (zone!=null){
			String mode = event.getLegMode();
			if (this.modeDeparturesperZone.get(zone).containsKey(mode)){
				modeDeparturesperZone.get(zone).get(mode).increment();
			}else {
				modeDeparturesperZone.get(zone).put(mode, new MutableInt(1));
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.ActivityEndEventHandler#handleEvent(org.matsim.api.core.v01.events.ActivityEndEvent)
	 */
	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getActType().equals("home")){
			if (!homeZone.containsKey(event.getPersonId())){
				String homeZ = findHomeZone(event.getLinkId());
				homeZone.put(event.getPersonId(), homeZ);
			}
		}
	}
	/**
	 * @param linkId
	 * @return
	 */
	private String findHomeZone(Id<Link> linkId) {
		Coord c = network.getLinks().get(linkId).getCoord();
		for (Entry<String, Geometry> e : zones.entrySet()){
			if (e.getValue().contains(MGC.coord2Point(c))){
				return e.getKey();
			}
		}
		return "other";
	}

	public void addZone(String id, Geometry geo){
		this.zones.put(id, geo);
		this.modeDeparturesperZone.put(id, new HashMap<String,MutableInt>());
	}

	public void writeStats(String fileName){
		BufferedWriter bw = IOUtils.getBufferedWriter(fileName);
		try {
			bw.write("Home Zone Modal Split");
			for (Entry<String, Map<String, MutableInt>> e : this.modeDeparturesperZone.entrySet()){
				bw.newLine();
				bw.write("Zone :" + e.getKey());
				for (Entry<String, MutableInt> f: e.getValue().entrySet() ){
					bw.newLine();
					bw.write(f.getKey()+";"+f.getValue());
				}
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
