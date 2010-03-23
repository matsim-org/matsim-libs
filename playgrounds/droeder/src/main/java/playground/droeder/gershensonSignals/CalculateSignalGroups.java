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
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.ptproject.qsim.QNetwork;
import org.matsim.signalsystems.systems.SignalGroupDefinition;

/**
 * @author Daniel
 *
 */
public class CalculateSignalGroups{
	
	private static final Logger log = Logger.getLogger(CalculateSignalGroups.class);
	private Map<Id, SignalGroupDefinition> groups;
	private Network net;
	
	private Map<Id, DaSgDirection> dir =  new HashMap<Id, DaSgDirection>();
	private Map<Id, List<Id>> corrGroups =  new HashMap<Id, List<Id>>();
	
	public CalculateSignalGroups(Map<Id, SignalGroupDefinition> groups, Network net){
		this.groups = groups;
		this.net = net;
	}
	
	private void calcGroupDir(){
		Link mainLink ;
		Link temp;
		double thetaMain;
		double thetaTemp;
		double thetaDiff;
		double right = (3/4) * Math.PI ;
		double left = (5/4) * Math.PI;
		
		for (SignalGroupDefinition sg : groups.values()){
			mainLink =  net.getLinks().get(sg.getLinkRefId());
			thetaMain = calcAngle(mainLink);
			dir.put(sg.getId(), null);
			for (Id i : sg.getToLinkIds()){
				temp =  net.getLinks().get(i);
				thetaTemp = calcAngle(temp);
				thetaDiff = thetaTemp - thetaMain;
				if (thetaDiff < right){
					if (dir.get(sg.getId()).equals(null)){
						dir.put(sg.getId(), DaSgDirection.R);
					}else if(dir.get(sg.getId()).equals(DaSgDirection.L)){
						dir.put(sg.getId(), DaSgDirection.LR);
					}else if(dir.get(sg.getId()).equals(DaSgDirection.S)){
						dir.put(sg.getId(), DaSgDirection.RS);
					}else if(dir.get(sg.getId()).equals(DaSgDirection.LS)){
						dir.put(sg.getId(), DaSgDirection.LRS);
						break;
					}
				}else if (thetaDiff > left){
					if (dir.get(sg.getId()).equals(null)){
						dir.put(sg.getId(), DaSgDirection.L);
					}else if(dir.get(sg.getId()).equals(DaSgDirection.R)){
						dir.put(sg.getId(), DaSgDirection.LR);
					}else if(dir.get(sg.getId()).equals(DaSgDirection.S)){
						dir.put(sg.getId(), DaSgDirection.LS);
					}else if(dir.get(sg.getId()).equals(DaSgDirection.RS)){
						dir.put(sg.getId(), DaSgDirection.LRS);
						break;
					}
				}else{
					if (dir.get(sg.getId()).equals(null)){
						dir.put(sg.getId(), DaSgDirection.S);
					}else if(dir.get(sg.getId()).equals(DaSgDirection.L)){
						dir.put(sg.getId(), DaSgDirection.LS);
					}else if(dir.get(sg.getId()).equals(DaSgDirection.S)){
						dir.put(sg.getId(), DaSgDirection.RS);
					}else if(dir.get(sg.getId()).equals(DaSgDirection.LR)){
						dir.put(sg.getId(), DaSgDirection.LRS);
						break;
					}
				}
			}
		}
		
	}
	
	public Map<Id, List<Id>> calcCorrGroups(){
		

		Link mainLink ;
		Link temp;
		List <Id> corrList;
		boolean leftGroup;
		double thetaMain;
		Double thetaTemp;
		double thetaDiff;
		final double delta = Math.PI /4;

		
		for (SignalGroupDefinition sg : groups.values()){
			corrGroups.put(sg.getId(), new LinkedList<Id>());
			corrList = corrGroups.get(sg.getId());
			for (SignalGroupDefinition sg2 : groups.values()){
				if(!(sg2.getId().equals(sg.getId())) && sg2.getLinkRefId().equals(sg.getLinkRefId())){
					
				}
			}
		}
		
		for (Entry<Id, DaSgDirection> e : dir.entrySet()){
			;
		}
		
		return null;
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
				
				//new code
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
	
	private static Link calculateLink(Link inLink){
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
	
	private static Coord getVector(Link link){
		double x = link.getToNode().getCoord().getX() - link.getFromNode().getCoord().getX();
		double y = link.getToNode().getCoord().getY() - link.getFromNode().getCoord().getY();		
		return new CoordImpl(x, y);
	}
	
}
