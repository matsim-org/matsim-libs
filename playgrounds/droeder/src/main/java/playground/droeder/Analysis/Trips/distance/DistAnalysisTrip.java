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
package playground.droeder.Analysis.Trips.distance;

import java.util.ArrayList;
import java.util.LinkedList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentEvent;
import org.matsim.core.utils.collections.Tuple;

import playground.droeder.Analysis.Trips.AbstractAnalysisTrip;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * @author droeder
 *
 */
public class DistAnalysisTrip extends AbstractAnalysisTrip implements DistAnalysisTripI{
	
	private static final Logger log = Logger.getLogger(DistAnalysisTrip.class);
	
	private int nrOfExpEvents;
	private boolean finished = false;
	private LinkedList<Coord> actLocations;
	
	private Double tripDist = null; 
	
	private Double inPtDist = null;
	private Integer lineCnt = null;

	private Double accesWalkDist = null;
	private Integer accesWalkCnt = null;

	private Double switchWalkDist = null;
	private Integer switchWalkCnt = null;

	private Double egressWalkDist = null;
	private Integer egressWalkCnt = null;
	
	
	public DistAnalysisTrip(ArrayList<PlanElement> elements){
		this.findMode(elements);
		this.analyzeElements(elements);
		this.init();
	}
	
	private void init() {
		this.tripDist = 0.0;
		if(super.getMode().equals(TransportMode.pt)){
			this.inPtDist = 0.0;
			this.lineCnt = 0;
			this.accesWalkDist = 0.0;
			this.accesWalkCnt = 0;
			this.switchWalkDist = 0.0;
			this.switchWalkCnt = 0;
			this.egressWalkDist = 0.0;
			this.egressWalkCnt = 0;
		}
	}

	private void analyzeElements(ArrayList<PlanElement> elements) {
		//if no zones in TripSet are defined, coords not necessary
		this.nrOfExpEvents = this.findNrOfExpEvents(elements);
		if(!(((Activity) elements.get(0)).getCoord() == null) && !(((Activity) elements.get(elements.size() - 1)).getCoord() == null)){
			super.setStart(new Coordinate(((Activity) elements.get(0)).getCoord().getX(), 
					((Activity) elements.get(0)).getCoord().getY()));
			super.setEnd(new Coordinate(((Activity) elements.get(elements.size() - 1)).getCoord().getX(), 
					((Activity) elements.get(elements.size() - 1)).getCoord().getY()));
		}
		
		this.actLocations = new LinkedList<Coord>();
		for(PlanElement p: elements){
			if(p instanceof Activity){
				this.actLocations.add(((Activity) p).getCoord());
			}
		}
	}
	
	private int findNrOfExpEvents(ArrayList<PlanElement> elements) {
		//every second pe has to be a leg
		return (elements.size() -1 ) ;
	}

	private void findMode(ArrayList<PlanElement> elements) {
		for(PlanElement p : elements){
			if(p instanceof Leg){
				if(((Leg) p).getMode().equals(TransportMode.transit_walk)){
					super.setMode(TransportMode.transit_walk);
				}else{
					super.setMode(((Leg) p).getMode());
					return;
				}
			}
		}
	}
	
	public void processLinkEnterEvent(double length) {
		this.tripDist += length;
	}
	
	private int processedEvents = 0;
	private ArrayList<Tuple<Coord, Coord>> legStartEnd = new ArrayList<Tuple<Coord,Coord>>();
	private Coord start = null;
	public void processAgentEvent(AgentEvent e) {
		this.processedEvents++;
		
		if(e instanceof AgentDepartureEvent){
			this.start = actLocations.removeFirst();
			if(e.getLegMode().equals(TransportMode.pt)){
				this.lineCnt++;
			}
		}else if(e instanceof AgentArrivalEvent){
			if(!e.getLegMode().equals(TransportMode.pt) && !e.getLegMode().equals(TransportMode.car)){
				this.legStartEnd.add(new Tuple<Coord, Coord>(start, actLocations.getFirst()));
			}
		}

		if(this.processedEvents == this.nrOfExpEvents){
			this.finish();
		}
	}
	
	private void finish() {
		if(super.getMode().equals(TransportMode.pt)){
			if(this.legStartEnd.size() == 2){
				this.accesWalkCnt++;
				this.egressWalkCnt++;
				this.accesWalkDist = this.getDist(this.legStartEnd.get(0));
				this.egressWalkDist = this.getDist(this.legStartEnd.get(1));
//				System.out.println(this.accesWalkCnt + " " + this.accesWalkDist + "; " + this.egressWalkCnt + " " + this.egressWalkDist);
			}else if(this.legStartEnd.size() >= 2){
				this.accesWalkCnt++;
				this.egressWalkCnt++;
				this.accesWalkDist = this.getDist(this.legStartEnd.get(0));
				this.egressWalkDist = this.getDist(this.legStartEnd.get(this.legStartEnd.size() - 1));
				
				for(int i = 1; i < this.legStartEnd.size() - 1; i++){
					this.switchWalkCnt++;
					this.switchWalkDist += this.getDist(this.legStartEnd.get(i));
				}
//				System.out.println(this.accesWalkCnt + " " + this.accesWalkDist + "; " + this.egressWalkCnt + " " + this.egressWalkDist + "; " + this.switchWalkCnt + " " + this.switchWalkDist);
			}else{
				log.error("a pt-trip need's at least an acces and an egressWalk!");
			}
		this.tripDist = this.accesWalkDist + this.inPtDist + this.switchWalkDist + this.egressWalkDist;	
		}else if(!super.getMode().equals(TransportMode.car)){
			this.tripDist = this.getDist(this.legStartEnd.get(0));
		}
		this.finished = true;
	}

	private double getDist(Tuple<Coord, Coord> startEnd){
		double a = startEnd.getFirst().getX() - startEnd.getSecond().getX();
		double b = startEnd.getFirst().getY() - startEnd.getSecond().getY();
		return Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2));
	}
	public void passedLinkInPt(double length) {
		this.inPtDist += length;
	}
	/**
	 * @return the nrOfExpEvents
	 */
	public Integer getNrOfExpEvents() {
		return nrOfExpEvents;
	}
	/**
	 * @return if trip is finished
	 */
	public boolean isFinished() {
		return finished;
	}
	/**
	 * @return the inVehDist
	 */
	public Double getInPtDist() {
		return inPtDist;
	}
	/**
	 * @return the accesWalkDist
	 */
	public Double getAccesWalkDist() {
		return accesWalkDist;
	}
	/**
	 * @return the switchWalkDist
	 */
	public Double getSwitchWalkDist() {
		return switchWalkDist;
	}
	/**
	 * @return the egressWalkDist
	 */
	public Double getEgressWalkDist() {
		return egressWalkDist;
	}
	/**
	 * @return the lineCnt
	 */
	public Integer getLineCnt() {
		return lineCnt;
	}
	/**
	 * @return the accesWalkCnt
	 */
	public Integer getAccesWalkCnt() {
		return accesWalkCnt;
	}
	/**
	 * @return the switchWalkCnt
	 */
	public Integer getSwitchWalkCnt() {
		return switchWalkCnt;
	}
	/**
	 * @return the egressWalkCnt
	 */
	public Integer getEgressWalkCnt() {
		return egressWalkCnt;
	}

	public Double getTripDist(){
		return tripDist;
	}
}


