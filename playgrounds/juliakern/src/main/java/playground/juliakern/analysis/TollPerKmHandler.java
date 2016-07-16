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

package playground.juliakern.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;



public class TollPerKmHandler implements PersonMoneyEventHandler,
		LinkLeaveEventHandler {

	Map<Id, List<LinkLeaveEvent>> personId2linkLeaveEvents;
	Map<Id, Set<PersonMoneyEvent>> personId2personMoneyEvent;
	Network network;
	private boolean considerOnlyMunich = true;
	Collection<SimpleFeature> featuresInVisBoundary;
	private HashMap<Id, List<Double>> personId2ListOfTollPerKm;
	private HashMap<Id, List<Double>> personId2ListOfToll;
	private HashMap<Id, List<Double>> personId2ListOfKm;
	
	public TollPerKmHandler(Network network, String visBoundaryShapeFile){
		this.personId2linkLeaveEvents = new HashMap<Id, List<LinkLeaveEvent>>();
		this.personId2personMoneyEvent = new HashMap<Id, Set<PersonMoneyEvent>>();
		this.network = network;
		this.featuresInVisBoundary = ShapeFileReader.getAllFeatures(visBoundaryShapeFile);
	}
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id personId = event.getDriverId();
//		Id personId = event.getVehicleId();
		// TODO event.getVehicleId() vs event.getDriverId()
		if(!(personId2linkLeaveEvents.containsKey(personId))){
			personId2linkLeaveEvents.put(personId, new ArrayList<LinkLeaveEvent>());
		}
		 List<LinkLeaveEvent> oldEvents = personId2linkLeaveEvents.get(personId);
		oldEvents.add(event);
		personId2linkLeaveEvents.put(personId, oldEvents);
	}

	@Override
	public void handleEvent(PersonMoneyEvent event) {
		Id personId = event.getPersonId();
		// TODO event.getVehicleId() vs event.getDriverId()
		if(!(personId2personMoneyEvent.containsKey(personId))){
			personId2personMoneyEvent.put(personId, new HashSet<PersonMoneyEvent>());
		}
		personId2personMoneyEvent.get(personId).add(event);
	}

	public void calculateAverages(){
		
		personId2ListOfTollPerKm = new HashMap<Id, List<Double>>();
		personId2ListOfToll = new HashMap<Id, List<Double>>();
		personId2ListOfKm = new HashMap<Id, List<Double>>();
		HashMap<Id, List<LinkLeaveEvent>> relevantPersonId2linkLeaveEvents = new HashMap<Id, List<LinkLeaveEvent>>();
		
		//if(considerOnlyMunich ){
			System.out.println("-------------------------cleaning");
			for(Id personId: personId2linkLeaveEvents.keySet()){
				List<LinkLeaveEvent> eventsInBoundary = new ArrayList<LinkLeaveEvent>();
				
				for(LinkLeaveEvent lle: personId2linkLeaveEvents.get(personId)){
					Coord lleCoord = network.getLinks().get(lle.getLinkId()).getCoord();
					if(isInVisBoundary(lleCoord)){
						eventsInBoundary.add(lle);
					}
				}
				personId2linkLeaveEvents.put(personId, eventsInBoundary);
				relevantPersonId2linkLeaveEvents.put(personId, eventsInBoundary);
				
			}	
			
		//}
		
		personId2linkLeaveEvents = relevantPersonId2linkLeaveEvents;
			
		for(Id personId : personId2personMoneyEvent.keySet()){
			List<Double> tollsperkm = new ArrayList<Double>();
			List<Double> tolls = new ArrayList<Double>();
			List<Double> kms = new ArrayList<Double>();
			
			// calculate toll per km
			for(PersonMoneyEvent pme: personId2personMoneyEvent.get(personId)){
				
				LinkLeaveEvent corrEvent = findCorrespondingLinkLeaveEvent(pme, relevantPersonId2linkLeaveEvents.get(personId));
				if(corrEvent!=null){
				Double tollAmount = - pme.getAmount();
				Double linkLength = network.getLinks().get(corrEvent.getLinkId()).getLength();
				Double tollPerKm = new Double(tollAmount/linkLength*1000.); // per kilometer (*1000) in EUR 
				tollsperkm.add(tollPerKm);
				tolls.add(tollAmount);
				kms.add(linkLength/1000.);
				}
			}
			personId2ListOfTollPerKm.put(personId, tollsperkm);
			personId2ListOfKm.put(personId, kms);
			personId2ListOfToll.put(personId, tolls);
		}
		
		
	}
	public Map<Id, List<Double>> getPersonId2ListOfTollPerKM() {
		return personId2ListOfTollPerKm;
		}
	
	private LinkLeaveEvent findCorrespondingLinkLeaveEvent(
			PersonMoneyEvent pme, List<LinkLeaveEvent> set) {
		if (set!=null) {
			if(!set.isEmpty()){
			Double moneyTime = pme.getTime();
			LinkLeaveEvent lle = set.get(0);
			for (LinkLeaveEvent potential : set) {
				if (Math.abs(potential.getTime() - moneyTime) < Math.abs(lle
						.getTime() - moneyTime)) {
					lle = potential;
				}
			}
			
			if(Math.abs(lle.getTime()-moneyTime )<2.){
				return lle;
			}
		}
		}
		return null;
	}

	private boolean isInVisBoundary(Coord cellCentroid) {
		boolean isInMunichShape = false;
		GeometryFactory factory = new GeometryFactory();
		Geometry geo = factory.createPoint(new Coordinate(cellCentroid.getX(), cellCentroid.getY()));
		for(SimpleFeature feature : this.featuresInVisBoundary){
			if(((Geometry) feature.getDefaultGeometry()).contains(geo)){
				isInMunichShape = true;
				break;
			}
		}
		return isInMunichShape;
	}
	public Map<Id, List<Double>> getPersonId2ListOfToll() {
		return personId2ListOfToll;
	}
	public Map<Id, List<Double>> getPersonId2ListOfKm() {
		return personId2ListOfKm;
	}
}
