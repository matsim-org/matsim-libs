/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.polettif.crossings;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.misc.Time;
import playground.polettif.crossings.lib.Crossing;
import playground.polettif.crossings.lib.LinkChangeEvent;
import playground.polettif.crossings.lib.LinkChangeEventImpl;
import playground.polettif.crossings.lib.PtLink;

import java.util.*;

/**
 * Handles when a train enters a rail link and stores the
 * closing and reopening time of the crossing link.
 *
 * @author polettif
 */
public class CrossingEventHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {
	
	private static final Logger log = Logger.getLogger(CrossingsFileParser.class);
	
	private double preBuffer = 80;
	private double postBuffer = 40;
	
	private List<LinkChangeEvent> linkChangeEvents = new ArrayList<>();
	private Network network;

	private Map<List<Object>, Double> enterEvents = new HashMap<>();
	private Map<Id<Link>, PtLink> ptLinks = new HashMap<>() ;

	public void reset(int iteration) {
		System.out.println("reset...");
	}

	public void loadCrossings(String filename) {
		CrossingsFileParser parser = new CrossingsFileParser( this.ptLinks );
		parser.readFile(filename);
//		this.ptLinks = parser.getPtLinks();
		}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {	
		if(ptLinks.keySet().contains(event.getLinkId())) {
			List<Object> key = new ArrayList<>();
			key.add(event.getVehicleId());
			key.add(event.getLinkId());
		
			enterEvents.put(key, event.getTime());
			log.info("enterLink: "+event.getLinkId()+", time: "+event.getTime());
		}
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if(ptLinks.containsKey(event.getLinkId())) {
			
			// get corresponding enterEvent
			List<Object> key = new ArrayList<>();
			key.add(event.getVehicleId());
			key.add(event.getLinkId());
			double enterTime = enterEvents.get(key);
			double leaveTime = event.getTime();
			double linkTravelTime = leaveTime-enterTime;
			enterEvents.remove(key);
			
			// add new changeEvents for all crossings on rail link
			// todo combine change events with the same time
			Id<Link> railId = event.getLinkId();
			
			PtLink railLink = ptLinks.get(railId);

			int id=0;
			for(Crossing crossing : railLink.getCrossings()) {
				Id<Link> crossId = crossing.getRefLinkId();

				// calculate time(coordinates of crossing, coordinates of fromNode, train speed, linkEnterTime)
				// -> time(distance, train speed, linkEnterTime
				double timeOfCrossing = enterTime+getTimeToCrossing(railId, crossId, linkTravelTime);

				String starttime = Time.writeTime(timeOfCrossing-preBuffer, "HH:mm:ss");
				String stoptime = Time.writeTime(timeOfCrossing+postBuffer, "HH:mm:ss");
				String capacity = Double.toString( network.getLinks().get(crossId).getCapacity() );
				
				LinkChangeEvent tmpChangeEvent = new LinkChangeEventImpl(crossId, starttime, stoptime, capacity);
								
				linkChangeEvents.add(tmpChangeEvent);
				}
			}
	}

	public void setNetwork(Network network) {
		this.network = network;
	}
	
	public void setBuffer(double preBuffer, double postBuffer) {
		this.preBuffer = preBuffer;
		this.postBuffer = postBuffer;
	}
	
	public List<LinkChangeEvent> getLinkChangeEvents(){
		return this.linkChangeEvents;
	}	
	
	private double getTimeToCrossing(Id<Link> railId, Id<Link> crossId, double linkTravelTime) {
		Map<Id<Link>, ? extends Link> links = this.network.getLinks();
		
		double railLinkLength = links.get(railId).getLength();
		Coord railOrigin = links.get(railId).getFromNode().getCoord(); 
		Coord crossOrigin = links.get(crossId).getFromNode().getCoord(); 
		
		double speed = railLinkLength / linkTravelTime;	

		//return timeToCrossing
		return getDistance(railOrigin, crossOrigin) / speed;
	}
	
	private double getDistance(Coord A, Coord B) {
		return Math.sqrt( Math.pow((A.getX()-B.getX()), 2) + Math.pow((A.getY()-B.getY()), 2) );
	}
	


}
