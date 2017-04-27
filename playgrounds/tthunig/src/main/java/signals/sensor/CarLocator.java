/* *********************************************************************** *
 * project: org.matsim.*
 * CarLocator
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package signals.sensor;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.lanes.data.Lane;

/**
 * This class provides methods to check, if a vehicle is approximately in a certain distance from a link's end.
 * 
 * Note: Each instance of this class is valid for only one specific distance.
 * 
 * @author droeder
 * @author dgrether
 *
 */
public class CarLocator {
	
	/** time when the vehicle will be at the given distance in front of the links to node */
	private double earliestTimeInDistance;
	
	private static final Logger log = Logger.getLogger(CarLocator.class);

	public CarLocator(Link link, double enterTime, double distance){
		double dist = this.checkDistance(link.getLength(), distance);
		this.calculateEarliestTimeInDistance(enterTime, dist, link);
	}

	public CarLocator(Lane lane, Link link, double enterTime, double distance){
		double dist = this.checkDistance(lane.getStartsAtMeterFromLinkEnd(), distance);
		this.calculateEarliestTimeInDistance(enterTime, dist, link, lane);
	}

	public void setEarliestTimeInDistance(double time){
		this.earliestTimeInDistance = time;
	}

	public double getEarliestTimeInDistance(){
		return this.earliestTimeInDistance;
	}
	
	/**
	 * Checks whether the car is in the specific distance (given in the controller) from a links end at the given time.
	 * 
	 * @param time
	 * @return true, if the position of the car on the link at the given time is within the specific distance from its end. false, if it is further afar.
	 */
	public boolean isCarinDistance(double time){
		if ((this.earliestTimeInDistance < time)){
			return true;
		}
		return false;
	}

	private void calculateEarliestTimeInDistance (double enterTime, double dist, Link link){
		this.earliestTimeInDistance = enterTime + ((link.getLength() - dist) / link.getFreespeed(enterTime));
//		log.debug("link " + link.getId() + " enterTime: " + enterTime + " earliest time " + this.earliestTimeInDistance +  " distance " + d);
	}

	private void calculateEarliestTimeInDistance (double enterTime, double dist, Link link, Lane lane){
		this.earliestTimeInDistance = enterTime + ((lane.getStartsAtMeterFromLinkEnd() - dist) / link.getFreespeed(enterTime));
//		log.debug("link " + link.getId() + " enterTime: " + enterTime + " earliest time " + this.earliestTimeInDistance +  " distance " + d);
	}
	
	/**
	 * Checks whether the given distance is within the link length.
	 * 
	 * @return the minimum of given distance and link length.
	 */
	private double checkDistance(double length, double distance) {
		if (length < distance){
			log.warn("distance to measure " + distance + " m was longer than link / lane " + length
					 + " m . using link/lane length as distance");
			return length;
		}
		return distance;
//		if (this.link instanceof QLinkLanesImpl){
//			for (QLane ql : ((QLinkLanesImpl)link).getQueueLanes()){
//				if(!((ql.equals(((QLinkLanesImpl)link).getOriginalLane()))) && (ql.getLength()>this.d)){
//					this.d = ql.getLength();
//					log.info("d was shorter than lane. Set to " + this.d);
//					break;
//				}
//			}
//		}
	}
	
	

}