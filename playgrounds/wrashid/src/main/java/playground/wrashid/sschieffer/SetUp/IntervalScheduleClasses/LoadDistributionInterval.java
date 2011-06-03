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
package playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.integration.SimpsonIntegrator;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.jfree.data.xy.XYSeries;


/**
 * extends TimeInterval. its additional parameters are a PolynomialFunction to indicate 
 * i.e. the available free load, or the charging prices for the time interval,
 * 
 * the boolean optimal can indicate whether the time interval has optimal values during the time interval. This is only used for the deterministic hub load in HubLoadDistributionReader.
 * If this variable is not needed, feel free to put a dummy value.
 * 
 * @author Stella
 *
 */
public class LoadDistributionInterval  extends TimeInterval
{
	
	private boolean optimal;	
	private PolynomialFunction p;
	private XYSeries xy;
	
	private SimpsonIntegrator integrator= new SimpsonIntegrator();
	
	
	/**
	 * creates LoadDistributionInterval with certain start, end value and a give PolynomialFunction
	 * @param start
	 * @param end
	 * @param p
	 * @throws MaxIterationsExceededException
	 * @throws FunctionEvaluationException
	 * @throws IllegalArgumentException
	 */
	public LoadDistributionInterval(double start, double end, PolynomialFunction p) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		super(start, end);
		this.p=p;
		if(integrator.integrate(p, start, end)>0){
			optimal=true;
		}else{
			optimal=false;
		}
	}
	
	
	
	/**
	 * creates a LoadDistribution Interval with given start and end value and a constant function over the load interval 
	 * @param start
	 * @param end
	 * @param constantValue
	 * @throws MaxIterationsExceededException
	 * @throws FunctionEvaluationException
	 * @throws IllegalArgumentException
	 */
	public LoadDistributionInterval(double start, double end, double constantValue) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		super(start, end);
		PolynomialFunction pX= new PolynomialFunction(new double[]{constantValue});
		this.p=pX;
		if(constantValue>0){
			optimal=true;
		}else{
			optimal=false;
		}
	}
	
	
	
	/**
	 * 
	 * @param start
	 * @param end
	 * @param p
	 * @param isOptimal
	 */
	public LoadDistributionInterval(double start, double end, PolynomialFunction p, boolean isOptimal){
		super(start, end);
		this.p=p;
		this.optimal=isOptimal;
		
		
	}
	
	public void negatePolynomialFunc(){
		p=p.negate();
	}
	
	
	public LoadDistributionInterval clone(){
		LoadDistributionInterval clone= new LoadDistributionInterval(start, end, p, optimal);
		return clone;
	}
	
	
	private void makeXYSeries(String seriesName){
		
		xy  = new XYSeries(seriesName);
		
		for(double i=super.getStartTime(); i<=super.getEndTime();){
			xy.add(i, p.value(i));
			i+=60;//in one minute bins
		}
	}
	
	
	
	public XYSeries getXYSeries(String seriesName){
		makeXYSeries(seriesName);
		return xy;
	}
	
	
	
	@Override
	public void printInterval(){
		System.out.println("Load Distribution Interval \t start: "+ this.getStartTime()+ "\t  end: "+ this.getEndTime()+ "\t  optimalTime: " + optimal+ "\t  Function: " + p.toString());
	}
	
	
	public boolean isOptimal(){
		return optimal;
	}
	
	public PolynomialFunction getPolynomialFunction(){
		return p;
	}
	
	
	public boolean haveSamePolynomialFuncCoeffs(LoadDistributionInterval second){
		double [] d1= getPolynomialFunction().getCoefficients();
		double [] d2= second.getPolynomialFunction().getCoefficients();
		//compare PolynomialFuncs
		boolean sameFunc=true;
		if(d1.length==d2.length){
			for(int i=0; i< d1.length; i++){
				if(d1[i]!= d2[i]){
					sameFunc=false;
				}
			}
		}else{
			sameFunc= false;
		}
		return sameFunc;
	}
	
}
