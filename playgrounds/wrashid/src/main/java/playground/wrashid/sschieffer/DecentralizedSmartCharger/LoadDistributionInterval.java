/* *********************************************************************** *
 * project: org.matsim.*
 * LoadDistributionInterval.java
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
package playground.wrashid.sschieffer.DecentralizedSmartCharger;

import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.jfree.data.xy.XYSeries;
import org.matsim.api.core.v01.Id;

public class LoadDistributionInterval  extends TimeInterval
{
	
	private boolean optimal;
	private Id hubId;
	private PolynomialFunction p;
	private XYSeries xy;
	
	
	
	public LoadDistributionInterval(double start, double end, PolynomialFunction p, boolean isOptimal){
		super(start, end);
		this.p=p;
		this.optimal=isOptimal;
		
		
	}
	
	
	
	
	public void makeXYSeries(){
		
		xy  = new XYSeries("LoadDistributionInterval at Hub: todo...");
		
		for(int i=(int)Math.ceil(super.getStartTime()); i<=(int)Math.floor(super.getEndTime()); i++){
			xy.add(i, p.value(i));
		}
	}
	
	
	
	public XYSeries getXYSeries(){
		return xy;
	}
	
	
	
	@Override
	public void printInterval(){
		System.out.println("Load Distribution Interval \t start: "+ this.getStartTime()+ "\t  end: "+ this.getEndTime()+ "\t  optimalTime: " + optimal);
	}
	
	
	public boolean isOptimal(){
		return optimal;
	}
	
	public PolynomialFunction getPolynomialFunction(){
		return p;
	}
	
}
