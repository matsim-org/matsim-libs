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


import org.apache.log4j.Logger;
import org.matsim.ptproject.qsim.netsimengine.QLane;
import org.matsim.ptproject.qsim.netsimengine.QLinkInternalI;
import org.matsim.ptproject.qsim.netsimengine.QLinkLanesImpl;

/**
 * This class provides methods to check, if a car is in distance d.
 * 
 * @author droeder
 *
 */
public class CarLocator {
	private QLinkInternalI link;
	private double enterTime;
	private double earliestInD;
	private double d;
	
	private static final Logger log = Logger.getLogger(CarLocator.class);

	public CarLocator(QLinkInternalI link, double enterTime, double d){
		this.link = link;
		this.enterTime = enterTime;
		this.d = d;
		this.checkD();
		this.earliestD();
	}
	private void checkD(){
		if (this.link instanceof QLinkLanesImpl){
			for (QLane ql : ((QLinkLanesImpl)link).getQueueLanes()){
				if(!((ql.equals(((QLinkLanesImpl)link).getOriginalLane()))) && (ql.getLength()>this.d)){
					this.d = ql.getLength();
					log.info("d was shorter than lane. Set to " + this.d);
					break;
				}
			}
		}
	}
	private void earliestD (){
		this.earliestInD = enterTime+((this.link.getLink().getLength()-this.d)/this.link.getLink().getFreespeed(this.enterTime));		
	}
	
	public void setEarliestD(double time){
		this.earliestInD = time;
	}
	
	public boolean agentIsInD(double time){
		if ((this.earliestInD<time)){
			return true;
		}else{
			return false;
		}
	}
	
}
