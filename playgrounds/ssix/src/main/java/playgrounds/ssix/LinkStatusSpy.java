/* *********************************************************************** *
 * project: org.matsim.*
 * LangeStreckeSzenario													   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playgrounds.ssix;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.utils.collections.Tuple;

/**
 * @author ssix
 */

public class LinkStatusSpy implements LinkEnterEventHandler, LinkLeaveEventHandler {
	
	private Logger log = Logger.getLogger(LinkStatusSpy.class);
	
	//private Scenario scenario;
	private Id linkId;
	private List<Tuple<Double,Id>> EnteringAgents;
	private List<Tuple<Double,Id>> LeavingAgents;
	//private int enteringCount = 0;
	//private int leavingCount = 0;
	
	public LinkStatusSpy (/*Scenario scenario,*/ Id linkId){
		//this.scenario = scenario;
		this.linkId = linkId;
		this.EnteringAgents = new ArrayList<Tuple<Double,Id>>();
		this.LeavingAgents = new ArrayList<Tuple<Double,Id>>(); 
	}

	
	public boolean sameLeavingOrderAsEnteringOrder(){
		//System.out.println(enteringCount+" <> "+leavingCount);
	
		//Comparing two ordered lists in order to find out the wanted result		
		if (LeavingAgents.size() != EnteringAgents.size()){
			log.info("Some Agents have been lost or not yet accounted for. Different numbers of cars have entered and left the link!");
			return false;
		} else {
			for (int i=0; i<EnteringAgents.size(); i++){
				if (!(EnteringAgents.get(i).getSecond().equals(LeavingAgents.get(i).getSecond()))){//Comparing Ids
					log.info("Not the same entering order as leaving order on link "+linkId.toString()+". Some agents have overtaken others.");
					return false;
				}
			}
			log.info("Same entering order as leaving order on link "+linkId.toString()+".");
			return true;
		}
	}
	
	public boolean didXLeaveLinkBeforeY(Id x, Id y){
		//NB: This function is meant for post-processing, once EnteringAgents and LeavingAgents are completely filled.
		boolean containsX = false;
		boolean containsY = false;
		Double xLeavingTime = new Double("0");
		Double yLeavingTime = new Double("0");
		
		for (Tuple<Double, Id> t : LeavingAgents){
			if (t.getSecond().equals(x)){
				containsX = true;
				xLeavingTime = t.getFirst();
			}	
		}
		for (Tuple<Double, Id> t : LeavingAgents){
			if (t.getSecond().equals(y))
				containsY = true;
				yLeavingTime = t.getFirst();
		}
		
		if ((containsX) && (containsY)){
			if (Double.compare(xLeavingTime, yLeavingTime) == -1){
				return true;
			} else {
				return false;
			}
		} else {
			log.info("The specified Identifiers do not exist in this simulation.");
			return false;
		}
	}
	
	public void handleEvent(LinkEnterEvent event){
		if (event.getLinkId().equals(this.linkId)){
			//enteringCount++;
			this.EnteringAgents.add(new Tuple<Double, Id>(event.getTime(), event.getPersonId()));
		}

	}
	
	public void handleEvent(LinkLeaveEvent event){
		if (event.getLinkId().equals(this.linkId)){
			//leavingCount++;
			this.LeavingAgents.add(new Tuple<Double, Id>(event.getTime(), event.getPersonId()));
		}
	}
	
	public void reset(int iteration){
		this.EnteringAgents.clear();
		this.LeavingAgents.clear();
		//this.enteringCount = 0;
		//this.leavingCount = 0;
	}

	public Id getLinkId() {
		return linkId;
	}

}
