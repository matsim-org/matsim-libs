/* *********************************************************************** *
 * project: org.matsim.*
 * PersonCalcActivitySpace.java
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

package playground.andreas.bln.net.simplex;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.misc.Time;

import playground.andreas.optimization.Objective;
import playground.andreas.optimization.ParamPoint;
import playground.andreas.optimization.SimplexOptimization;


public class SimplexRunner {

	private final static Logger log = Logger.getLogger(SimplexRunner.class);

	public static void main(String[] args) {
		findSolution("E:/_out/parameter/");		
	}
	
	public static void findSolution(String tmpDir) {
		
		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(new File(tmpDir + "iteration_results.txt")));

			Gbl.startMeasurement();
			int iteration = 0;

			Objective objFunc = new PTNetFitObjectiveFive(tmpDir, iteration);		
			ParamPoint basePoint = objFunc.getNewParamPoint();

			basePoint.setValue(PTNetFitObjectiveFive.sF_1_idx, 1.1);
			basePoint.setValue(PTNetFitObjectiveFive.sF_2_idx, 1.234);
			basePoint.setValue(PTNetFitObjectiveFive.sF_3_idx, 1.432);
			basePoint.setValue(PTNetFitObjectiveFive.sF_4_idx, 1.5432);
			basePoint.setValue(PTNetFitObjectiveFive.sF_5_idx, 1.321);

			basePoint.setValue(PTNetFitObjectiveFive.sT_1_idx, 1 * 3600);
			basePoint.setValue(PTNetFitObjectiveFive.sT_2_idx, 6 * 3600);
			basePoint.setValue(PTNetFitObjectiveFive.sT_3_idx, 12 * 3600);
			basePoint.setValue(PTNetFitObjectiveFive.sT_4_idx, 16 * 3600);
			basePoint.setValue(PTNetFitObjectiveFive.sT_5_idx, 22 * 3600);

			while(iteration < 10){

				for (int i = 0; i < PTNetFitObjectiveFive.DIMENSION + 1; i++) {
					ParamPoint p = objFunc.getNewParamPoint();		
					p.setValue(PTNetFitObjectiveFive.sF_1_idx, basePoint.getValue(PTNetFitObjectiveFive.sF_1_idx));
					p.setValue(PTNetFitObjectiveFive.sF_2_idx, basePoint.getValue(PTNetFitObjectiveFive.sF_2_idx));
					p.setValue(PTNetFitObjectiveFive.sF_3_idx, basePoint.getValue(PTNetFitObjectiveFive.sF_3_idx));
					p.setValue(PTNetFitObjectiveFive.sF_4_idx, basePoint.getValue(PTNetFitObjectiveFive.sF_4_idx));
					p.setValue(PTNetFitObjectiveFive.sF_5_idx, basePoint.getValue(PTNetFitObjectiveFive.sF_5_idx));

					p.setValue(PTNetFitObjectiveFive.sT_1_idx, basePoint.getValue(PTNetFitObjectiveFive.sT_1_idx));
					p.setValue(PTNetFitObjectiveFive.sT_2_idx, basePoint.getValue(PTNetFitObjectiveFive.sT_2_idx));
					p.setValue(PTNetFitObjectiveFive.sT_3_idx, basePoint.getValue(PTNetFitObjectiveFive.sT_3_idx));
					p.setValue(PTNetFitObjectiveFive.sT_4_idx, basePoint.getValue(PTNetFitObjectiveFive.sT_4_idx));
					p.setValue(PTNetFitObjectiveFive.sT_5_idx, basePoint.getValue(PTNetFitObjectiveFive.sT_5_idx));

					if(i < PTNetFitObjectiveFive.DIMENSION){
						p.setValue(i, p.getValue(i) * 1.123);
					}

					objFunc.setInitParamPoint(p,i);
				}

				basePoint = SimplexOptimization.getBestParams(objFunc);
				writer.write(SimplexRunner.write(objFunc, basePoint));
				writer.flush();

				Gbl.printElapsedTime();

				iteration++;
				objFunc = new PTNetFitObjectiveFive(tmpDir, iteration);	
			}
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static String write(Objective objective, ParamPoint point){
		StringBuffer stringbuffer = new StringBuffer();
		stringbuffer.append(objective.getResponse(point));
		for (int i = 0; i < point.getDimension(); i += 2) {
			stringbuffer.append(", ");
			stringbuffer.append(Time.writeTime(point.getValue(i)));
			stringbuffer.append(", ");
			stringbuffer.append(point.getValue(i + 1));			
		}
		return stringbuffer.toString();
	}
}
