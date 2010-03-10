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

import loci.formats.Log;

import org.apache.log4j.Logger;
import org.matsim.ptproject.qsim.QLane;
import org.matsim.ptproject.qsim.QLink;
import org.matsim.ptproject.qsim.QLinkLanesImpl;

/**
 * @author droeder
 *
 */
public class CarLocator {
	private QLinkLanesImpl link;
	private boolean parking = false;
	private double enterTime;
	private double earliestInD;
	private double d;
	
	private static final Logger log = Logger.getLogger(CarLocator.class);

	public CarLocator(QLink link, double enterTime, double d){
		this.link = (QLinkLanesImpl) link;
		this.enterTime = enterTime;
		this.d = d;
		this.checkD();
		this.earliestD();
	}
	private void checkD(){
		for (QLane ql : link.getQueueLanes()){
			if(!((ql.equals(link.getOriginalLane()))) && (ql.getLength()>this.d)){
				this.d = ql.getLength();
				log.info("d was shorter than lane. Set to " + this.d);
				break;
			}
		}
		if(this.d>link.getLink().getLength()){
			this.d = link.getLink().getLength();
			log.info("parameter d must not longer then linkLength, d set to " + link.getLink().getLength());
		}
	}
	private void earliestD (){
		this.earliestInD = enterTime+(this.link.getLink().getLength()-this.d)/this.link.getLink().getFreespeed(this.enterTime);		
	}
	
	public void agentStartsActivity(){
		this.parking = true;
	}
	
	public void agentEndsActivity(){
		this.parking = false;
	}
	public void setEarliestD(double time){
		this.earliestInD = time;
	}
	
	public boolean agentIsInD(double time){
		if ((this.earliestInD<time) && (parking==false)){
			return true;
		}else{
			return false;
		}
	}
	
}
