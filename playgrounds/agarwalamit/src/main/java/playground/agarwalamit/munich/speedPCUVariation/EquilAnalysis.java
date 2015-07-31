/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.munich.speedPCUVariation;

import playground.agarwalamit.analysis.congestion.AbsoluteDelays;
import playground.agarwalamit.analysis.emission.AbsoluteEmissions;

/**
 * @author amit
 */

public class EquilAnalysis {
	
	private String [] runCases =  {"allCar_20","allCar_30","allCar_40","allCar_50","allCar_60","allCar_70","allCar_80","allCar_90","allCar_100"};
	private String outDir = "./equil/output/";
	
	public static void main(String[] args) {
		new EquilAnalysis().run();
	}
	
	private void run(){
		new AbsoluteEmissions(outDir).runAndWrite(runCases);
		new AbsoluteDelays(outDir).runAndWrite(runCases);
	}
}
