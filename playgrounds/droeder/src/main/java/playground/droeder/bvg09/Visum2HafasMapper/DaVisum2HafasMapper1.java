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
package playground.droeder.bvg09.Visum2HafasMapper;

import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

/**
 * @author droeder
 *
 */
public class DaVisum2HafasMapper1 extends AbstractDaVisum2HafasMapper{
	
	public DaVisum2HafasMapper1(){
		super(150.0);
	}
	
	public static void main(String[] args){
		DaVisum2HafasMapper1 mapper = new DaVisum2HafasMapper1();
		mapper.run();
	}
	
	
	protected Map<Id, Id> tryToMatchRoute(TransitRoute visRoute, TransitRoute hafRoute){
		Map<Integer, Tuple<Id, Id>> sortedPrematched = new HashMap<Integer, Tuple<Id, Id>>();
		
		ListIterator<TransitRouteStop> vIterator = visRoute.getStops().listIterator();
		TransitRouteStop vStop;
		Id vId;
		
		if(hafRoute.getStops().size() < visRoute.getStops().size()) return null;
		
		//search for preMatched and sort
		int i = 0;
		while(vIterator.hasNext()){
			vStop = vIterator.next();
			vId = vStop.getStopFacility().getId();
			if(this.preVisum2HafasMap.containsKey(vId)){
				sortedPrematched.put(vIterator.previousIndex(), new Tuple<Id, Id>(vId, this.preVisum2HafasMap.get(vId)));
				i++;
			}else{
				sortedPrematched.put(vIterator.previousIndex(), null);
			}
		}
		
		// no stop matched, return null
		if(i == 0){
			return null;
		}
		
		// calculate stopOffset
		int j = 0;
		Integer offset = null;
		ListIterator<TransitRouteStop> hIterator = hafRoute.getStops().listIterator();
		TransitRouteStop hStop;
		Id hId;
		
		while (hIterator.hasNext()){
			hStop = hIterator.next();
			hId = hStop.getStopFacility().getId();
			
			for(Entry<Integer,  Tuple<Id, Id>> e : sortedPrematched.entrySet()){
				int temp;
				if((!(e.getValue() == null)) && e.getValue().getSecond().equals(hId)){
					temp = e.getKey() - hIterator.previousIndex();
					if(offset == null){
						offset = temp;
					}else if(offset != temp){
						return null;
					}
					j++;
				}
			}
		}
		
		// if not all prematched stops in hRoute return null
		if (i != j) return null;
		
		//add Stops 2 map
		Map<Id, Id> matchedStops = new HashMap<Id, Id>();
		
		for(int ii = 0; ii < visRoute.getStops().size(); ii++){
			if ((ii-offset) < 0 || ((ii-offset) >= hafRoute.getStops().size())) return null;
			if(sortedPrematched.get(ii) == null){
				matchedStops.put(visRoute.getStops().get(ii).getStopFacility().getId(), 
						hafRoute.getStops().get(ii - offset).getStopFacility().getId());
			}else if (!sortedPrematched.get(ii).getSecond().equals(hafRoute.getStops().get(ii-offset).getStopFacility().getId())){
				return null;
			}else{
				matchedStops.put(sortedPrematched.get(ii).getFirst(), sortedPrematched.get(ii).getSecond());
			}
		}
		
		return matchedStops;
	}
	
}
