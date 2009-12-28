/* *********************************************************************** *
 * project: org.matsim.*
 * IncomeCalculatorGesamtschweiz
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.benjamin.income;

import java.util.Random;

import org.apache.log4j.Logger;


/**
 * @author dgrether
 *
 */
public class IncomeCalculatorGesamtschweiz {
	
	private static final Logger log = Logger.getLogger(IncomeCalculatorGesamtschweiz.class);
	
	private Random random;

	public IncomeCalculatorGesamtschweiz() {
		long seed = 984521478;
		this.random = new Random(seed);
	}
	
	
	public double calculateIncome(double median){
//		double medianLorenz = calculateLorenzValue(0.5);
//	  double totalIncome =  median / medianLorenz;
		
		double rnd = this.random.nextDouble();
		double lorenzDerivative = calculateLorenzDerivativeValue(rnd);

		double income = lorenzDerivative * median;
		
		double scale = calculateLorenzDerivativeValue(0.5);
		income /= scale;
		return income;
	}
	
	
	private double calculateLorenzValue(double x){
		return 0.3178 * Math.pow(x, 3) + 0.2259 * Math.pow(x, 2) + 0.4467 * x;
	}
	

	private double calculateLorenzDerivativeValue(double x){
		return 0.9534 * Math.pow(x, 2.0) + 0.4518 * x + 0.4467;
	}

}
