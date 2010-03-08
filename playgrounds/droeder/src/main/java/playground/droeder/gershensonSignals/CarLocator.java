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

import org.matsim.ptproject.qsim.QLink;

/**
 * @author droeder
 *
 */
public class CarLocator {
	private QLink link;
	private boolean parking = false;
	private double enterTime;
	private double earliestD;
	private double d;
	
	public CarLocator(QLink link, double enterTime, double d){
		this.link = link;
		this.enterTime = enterTime;
		this.d = d;
		this.earliestD();
	}
	
	private void earliestD (){
		this.earliestD = enterTime+(this.link.getLink().getLength()-d)/this.link.getLink().getFreespeed(this.enterTime);		
	}
	
	public void agentStartsActivity(){
		parking = true;
	}
	
	public void agentEndsActivity(){
		parking = false;
	}
	
	public boolean agentIsInD(double time){
		if (earliestD<time && parking==false){
			return true;
		}else{
			return false;
		}
	}
	
}
