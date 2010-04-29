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

import playground.jhackney.optimization.Objective;
import playground.jhackney.optimization.ParamPoint;
import playground.jhackney.optimization.SimplexOptimization;


public class SimplexRunnerN {

	private final static Logger log = Logger.getLogger(SimplexRunnerN.class);

	public static void main(String[] args) {
//		findSolution("E:/_out/parameter/", 5, 0.001);
		findSolution(args[0], Integer.parseInt(args[1]), Double.parseDouble(args[2]));
	}
	
	public static void findSolution(String tmpDir, int numberOfslots, double epsilon) {
		
		BufferedWriter writer;
		try {
			
			Gbl.startMeasurement();
			int iteration = 0;
			int dimension = numberOfslots * 2;
			
			new File(tmpDir + "results/").mkdir();
			
			writer = new BufferedWriter(new FileWriter(new File(tmpDir + "results/"  + "iteration_results.txt")));
			StringBuffer stringbuffer = new StringBuffer();
			for (int i = 0; i < dimension / 2; i++) {
				stringbuffer.append(", time " + i);
				stringbuffer.append(", factor " + i);
			}
			writer.write("Iteration, Score" + stringbuffer);
			writer.newLine();
			writer.flush();

			Objective objFunc = new PTNetFitObjectiveN(tmpDir, dimension, iteration);		
			ParamPoint basePoint = objFunc.getNewParamPoint();
			
			// set initial points
			for (int i = 0; i < dimension; i += 2) {
				basePoint.setValue(i, 4 * 3600 + i * 20.0 / dimension * 3600);
				basePoint.setValue(i + 1, 1.5 + 1.0 / dimension * i);
			}
			
			double bestIterationScore = Double.NaN;
			boolean keepRunning = true;

			while(keepRunning){

				for (int i = 0; i < dimension + 1; i++) {
					
					ParamPoint p;
					boolean searchNewOne = true;
					
					do {
						p = objFunc.getNewParamPoint();		

						for (int j = 0; j < dimension; j += 2) {
							p.setValue(j, basePoint.getValue(j));
							p.setValue(j + 1, basePoint.getValue(j + 1));
						}					

						if(i < dimension){
						
							if(Math.random() < 0.5){
								p.setValue(i, p.getValue(i) + p.getValue(i) * (-1 * 5.0 / 20.0 * Math.random()));
							} else {
								p.setValue(i, p.getValue(i) + p.getValue(i) * (+1 * 5.0 / 20.0 * Math.random()));
							}

						}
						
						searchNewOne = !objFunc.isValidParamPoint(p);
						
					} while (searchNewOne);

					objFunc.setInitParamPoint(p,i);
				}

				basePoint = SimplexOptimization.getBestParams(objFunc);
				
				if(bestIterationScore - objFunc.getResponse(basePoint) < epsilon){
					keepRunning = false;
				}
				
				bestIterationScore = objFunc.getResponse(basePoint);
				
				writer.write(iteration + ", " + SimplexRunnerN.write(objFunc, basePoint));
				writer.newLine();
				writer.flush();

				iteration++;
				objFunc = new PTNetFitObjectiveN(tmpDir, dimension, iteration);	
			}
			
			log.info("Aborting in Iteration " + iteration + " with best score " + bestIterationScore);
			log.info(SimplexRunnerN.write(objFunc, basePoint));
			Gbl.printElapsedTime();
		
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
