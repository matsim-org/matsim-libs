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
package playground.droeder.gershensonSignals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.signalsystems.systems.SignalGroupDefinition;

/**
 * Provides Methods to find compliant traffic flows an traffic lights.
 * 
 * @author droeder
 *
 */
public class CalculateSignalGroups{
	
	private static final Logger log = Logger.getLogger(CalculateSignalGroups.class);
	private Map<Id, SignalGroupDefinition> groups;
	private Network net;
	
	private Map<Id, List<SignalGroupDefinition>> corrGroups =  new HashMap<Id, List<SignalGroupDefinition>>();
	private Map<Id, SortedSet<Id>> groupsOnLink = new HashMap<Id, SortedSet<Id>>();
	private Map<Id, Id> corrLinks = new HashMap<Id, Id>();
	
	private double right = (3.0/4)* Math.PI;
	private double left = (5.0/4)*Math.PI;
	private double thetaMain;
	private double thetaTemp;
	private double thetaDiff;
	
	
	public CalculateSignalGroups(Map<Id, SignalGroupDefinition> groups, Network net){
		this.groups = groups;
		this.net = net;
	}
	
	private void groupsOnLink(){
		for (SignalGroupDefinition sd: this.groups.values()){
			if(this.groupsOnLink.containsKey(sd.getLinkRefId())){
				this.groupsOnLink.get(sd.getLinkRefId()).add(sd.getId());
			}else{
				this.groupsOnLink.put(sd.getLinkRefId(), new TreeSet<Id>());
				this.groupsOnLink.get(sd.getLinkRefId()).add(sd.getId());
			}
		}
	}
	
	private void calcCorrLinks(){
		
		for (Id i : groupsOnLink.keySet()){
			for (Id ii :groupsOnLink.keySet()){
				if(!(corrLinks.containsKey(i)) && !(corrLinks.containsValue(i))){
					corrLinks.put(i, new IdImpl("null"));
				}
				if(!(corrLinks.containsKey(ii) && !corrLinks.containsValue(ii))){
					thetaMain = this.calcAngle(net.getLinks().get(i));
					thetaTemp = this.calcAngle(net.getLinks().get(ii));
					thetaDiff = Math.abs(thetaMain-thetaTemp);
					if ((thetaDiff>right) && (thetaDiff<left)){
						corrLinks.put(i, ii);
					}
				}
			}
		}
		
		if (corrLinks.containsKey(new IdImpl("162")) || corrLinks.containsValue(new IdImpl("162"))){
			System.out.println(corrLinks.toString());
		}
	}
	
	public Map<Id, List<SignalGroupDefinition>> calcCorrGroups(){
		int i;
		Id id;
		List<SignalGroupDefinition> temp;
		List<SignalGroupDefinition> left;
		List<SignalGroupDefinition> other;
		this.groupsOnLink();
		this.calcCorrLinks();
		
		
		i = 1;
		// iteration over all corresponding Links
		for (Entry<Id, Id> e : corrLinks.entrySet()){
			// do only if there's a corresponding Link
			if(!(e.getValue().equals(new IdImpl("null")))){
				//do if both corresponding Links have only one sg
				if ((groupsOnLink.get(e.getKey()).size() == 1) && (groupsOnLink.get(e.getValue()).size() == 1)){
					temp = new LinkedList<SignalGroupDefinition>();
					temp.add(groups.get(groupsOnLink.get(e.getKey()).first()));
					temp.add(groups.get(groupsOnLink.get(e.getValue()).first()));
					id = new IdImpl(i);
					corrGroups.put(id, temp);
					i++;
				}
				// else, sort out the groups for left Turns
				else{
					left = new LinkedList<SignalGroupDefinition>();
					other = new LinkedList<SignalGroupDefinition>();
					for (Id ii : groupsOnLink.get(e.getKey())){
						thetaMain = calcAngle(net.getLinks().get(groups.get(ii).getLinkRefId()));
						thetaTemp = 0;
						for(Id outLink: groups.get(ii).getToLinkIds()){
							thetaTemp = calcAngle(net.getLinks().get(outLink));
						}
						if (corrLinks.containsKey(new IdImpl("162")) || corrLinks.containsValue(new IdImpl("162"))){
							log.error(ii + " " + thetaMain + "-" + thetaTemp );
						}
						if(thetaMain > (Math.PI)){
							if (thetaTemp >= (thetaMain + (Math.PI/4)) || thetaTemp <= (thetaMain - Math.PI)){
								left.add(groups.get(ii));
							}else{
								other.add(groups.get(ii));
							}
						}else{
							if (thetaTemp >= (thetaMain + (Math.PI/4)) && thetaTemp <= (thetaMain + Math.PI)){
								left.add(groups.get(ii));
							}else{
								other.add(groups.get(ii));
							}
						}
					}
					for (Id ii : groupsOnLink.get(e.getValue())){
						thetaMain = calcAngle(net.getLinks().get(groups.get(ii).getLinkRefId()));
						thetaTemp = 0;
						for(Id outLink: groups.get(ii).getToLinkIds()){
							thetaTemp = calcAngle(net.getLinks().get(outLink));
						}
						if (corrLinks.containsKey(new IdImpl("162")) || corrLinks.containsValue(new IdImpl("162"))){
							log.error(ii + " " + thetaMain + "-" + thetaTemp );
						}
						if(thetaMain > (Math.PI)){
							if (thetaTemp >= (thetaMain + (Math.PI/4)) || thetaTemp <= (thetaMain - Math.PI)){
								left.add(groups.get(ii));
							}else{
								other.add(groups.get(ii));
							}
						}else{
							if (thetaTemp >= (thetaMain + (Math.PI/4)) && thetaTemp <= (thetaMain + Math.PI)){
								left.add(groups.get(ii));
							}else{
								other.add(groups.get(ii));
							}
						}
							
					}
					id = new IdImpl(i);
					corrGroups.put(id, left);
					i++;
					id = new IdImpl(i);
					corrGroups.put(id, other);
					i++;
				}
			}
			// do if there is no corresponding Link
			else{
				temp = new LinkedList<SignalGroupDefinition>();
				for (Id ii : groupsOnLink.get(e.getKey())){
					temp.add(groups.get(ii));
				}
				id = new IdImpl(i);
				corrGroups.put(id, temp);
				i++;
			}
		}
		
		if (corrLinks.containsKey(new IdImpl("381")) || corrLinks.containsValue(new IdImpl("381"))){
			for(Entry<Id, List<SignalGroupDefinition>> ee : corrGroups.entrySet()){
				System.out.println(ee.getKey());
				for (SignalGroupDefinition sd :  ee.getValue()){
					System.out.println(sd.getId());
				}
				System.out.println("---");
			}
			System.out.println("xxxx");
		}
		// check if a group is missed
		temp =  new LinkedList<SignalGroupDefinition>();
		for (List<SignalGroupDefinition> l  : corrGroups.values()){
			temp.addAll(l);
		}
		for (SignalGroupDefinition sd : groups.values()){
			if(!(temp.contains(sd))) {
				throw new MissingResourceException("sg is missing", CalculateSignalGroups.class.toString(), sd.getId().toString());
			}
		}
		
		return this.corrGroups;
	}
	private double calcAngle(Link l){
		Coord c = this.getVector(l);
		double theta = Math.atan2(c.getY(), c.getX());
		
		if (theta < 0) theta += 2*Math.PI;
		
		return theta;
	}
	
	public Map<Id, Id> calculateCorrespondingGroups(){
		SortedMap<Id, Id> corrGroups = new TreeMap<Id, Id>();
		Link l;
		//Iteration over all Signalgroupdefinitions
		for(SignalGroupDefinition sd : groups.values()) {
			l = net.getLinks().get(sd.getLinkRefId());
			if (l == null){
				corrGroups.put(sd.getId(), null);
			}else{
				l = calculateLink(l);
				if (!(l == null)){
					//compare which Signalgroup fits to the calculated Link
					for(SignalGroupDefinition sd2 : groups.values()){
						if(sd2.getLinkRefId().equals(l.getId())){
							corrGroups.put(sd.getId(), sd2.getId());
						}
					}
				}else{
					corrGroups.put(sd.getId(), null);
				}
			}
		}	
		return corrGroups;
	}
	
	public Map<Id, List<Id>> calculateCompetingGroups(Map<Id, Id> corrIds){
		Map<Id, List<Id>> cg = new TreeMap<Id,List<Id>>();
		
		for (Entry <Id, Id> ee : corrIds.entrySet()){
			List<Id> l = new ArrayList<Id>();
			for (SignalGroupDefinition sd : groups.values()){
				
				if(net.getLinks().get(groups.get(ee.getKey()).getLinkRefId()).getToNode().
						equals(net.getLinks().get(sd.getLinkRefId()).getToNode())){
					if (!(sd.getId().equals(ee.getKey()) || sd.getId().equals(ee.getValue()))){
						l.add(sd.getId());
					}
				}
			
			}
			cg.put(ee.getKey(), l);
		}
		return cg;
	}
	
	private Link calculateLink(Link inLink){
		Coord coordInLink = getVector(inLink);
		double corrAngle = (Math.PI);
		double temp = Math.PI *3/8;
		double thetaInLink = Math.atan2(coordInLink.getY(), coordInLink.getX());
		Link l = null;
		Map<Link, Double> linkAngle = new HashMap<Link, Double>();
				
		for (Link cl : inLink.getToNode().getInLinks().values()){
			
			Coord coordCorrLink = getVector(cl);
			double thetaCorrLink = Math.atan2(coordCorrLink.getY(), coordCorrLink.getX());
			double thetaDiff = thetaCorrLink - thetaInLink;
			if (thetaDiff < -Math.PI){
				thetaDiff += 2 * Math.PI;
			} else if (thetaDiff > Math.PI){
				thetaDiff -= 2 * Math.PI;
			}
			linkAngle.put(cl, (Math.abs(thetaDiff)));
		}
		
		for (Entry<Link, Double> e :  linkAngle.entrySet()) {
			if ((corrAngle - e.getValue()) < temp ){
				temp = corrAngle - e.getValue();
				
				if (inLink.getToNode().getInLinks().size() > 2){
					l = e.getKey();
				}else{
					l = null;
				}
			}
		}
		return l;
	}
	
	public Map<Id, Id> calculateMainOutlinks(){
		Map<Id, Id> mainOutLinks = new HashMap<Id, Id>();
		Coord coordInLink;
		Coord coordOutLink;
		double temp;
		double thetaOutLink;
		double thetaInLink;
		double thetaDiff;
		
		for(SignalGroupDefinition sg: groups.values()){
			coordInLink = getVector(net.getLinks().get(sg.getLinkRefId()));
			thetaInLink = Math.atan2(coordInLink.getY(), coordInLink.getX());
			temp = Math.PI;
			mainOutLinks.put(sg.getLinkRefId(), null);

			for(Id i : sg.getToLinkIds()){
				coordOutLink = getVector(net.getLinks().get(i));
				thetaOutLink = Math.atan2(coordOutLink.getY(), coordOutLink.getX());
				thetaDiff = Math.abs(thetaOutLink-thetaInLink);
			
				if (temp>thetaDiff){
					temp = thetaDiff;
					mainOutLinks.put(sg.getLinkRefId(), i);
				}
			}
		}
		return mainOutLinks;
	}
	
	
	private Coord getVector(Link link){
		double x = link.getToNode().getCoord().getX() - link.getFromNode().getCoord().getX();
		double y = link.getToNode().getCoord().getY() - link.getFromNode().getCoord().getY();		
		return new CoordImpl(x, y);
	}
	
}
