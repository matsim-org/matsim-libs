/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.mmoyo.taste_variations;

import java.io.File;

import org.apache.commons.math.linear.Array2DRowRealMatrix;
import org.apache.commons.math.linear.ArrayRealVector;
import org.apache.commons.math.linear.DecompositionSolver;
import org.apache.commons.math.linear.RealMatrix;
import org.apache.commons.math.linear.RealVector;
import org.apache.commons.math.linear.SingularValueDecompositionImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.mmoyo.analysis.tools.PtPlanAnalyzer;
import playground.mmoyo.analysis.tools.PtPlanAnalyzer.PtPlanAnalysisValues;
import playground.mmoyo.io.PopSecReader;
import playground.mmoyo.io.TextFileWriter;
import playground.mmoyo.utils.DataLoader;


public class SingularValueDecomposition implements PersonAlgorithm{
	PtPlanAnalyzer planAnalizer;
	private StringBuffer sBuff = new StringBuffer("IdAgent\tBetawalk\tBetainvehTime\tBetaDistance\tBetaTransfer");
	final String TB = "\t";
	final String NL = "\n";
	
	public SingularValueDecomposition(final Network net, final TransitSchedule schedule){
		planAnalizer = new PtPlanAnalyzer(net, schedule);
	}
	
	@Override
	public void run(final Person person) {
		//objective: to create this matrices
		//						A						X			B
		//         walk time dist chng     	betas		UtlCorr 	
		//plan1 ┌ w1   t1   d1   c1 ¬  	┌ ßw¬   	┌λ1¬ 
		//plan2 | w2   t2 	  d2  	c2  |  	| ßt  |   	| λ2 |
		//plan3 | w3   t3	  d3 	c3  |  	| ßd  |	| λ3 |
		//plan4 └ w4   t4	  d4   c4 ┘		└ ßc ┘	└λ4 ┘

		double[][] arrayA = new double [person.getPlans().size()][4]; 
		double[] arrayB = new double [4];
		int i = 0;
		for (Plan plan: person.getPlans()){
			PtPlanAnalysisValues v = planAnalizer.run(plan);
			arrayA[i][0] = v.getTransitWalkTime_secs();  //[row][col] 
			arrayA[i][1] = v.getInVehTravTime_secs();
			arrayA[i][2] = v.getInVehDist_mts();
			arrayA[i][3] = v.getTransfers_num();
			arrayB[i]= plan.getScore();
			i++;
		}

		//invoke SVD
		RealMatrix matrixA = new Array2DRowRealMatrix(arrayA, false);   //"coefficients" matrix
		DecompositionSolver svd = new SingularValueDecompositionImpl(matrixA).getSolver(); 
		RealVector utlCorrections = new ArrayRealVector(arrayB, false); //"constants" matrix
		RealVector solution = svd.solve(utlCorrections);
		
		sBuff.append(NL + person.getId() + TB + solution.getEntry(0) + TB + solution.getEntry(1) + TB + solution.getEntry(2) + TB + solution.getEntry(3));
	}
	
	private void writeSolution(final String outFile){
		new TextFileWriter().write(sBuff.toString(), outFile, false);
	}
	
	public static void main(String[] args) {
		String netFilePath;
		String popFilePath;
		String schdFilePath;
		if (args.length>0){
			popFilePath = args[0];
			netFilePath = args[1];
			schdFilePath = args[2];
		}else{
			popFilePath = "../../runs_manuel/CalibLineM44/automCalib10xTimeMutated/10xrun/it.500/500.plans.xml.gz";
			netFilePath = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/pt_network.xml.gz";
			schdFilePath = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/pt_transitSchedule.xml.gz";
		}

		DataLoader dataLoader = new DataLoader ();
		ScenarioImpl scn = (ScenarioImpl) dataLoader.createScenario(); 
		MatsimNetworkReader matsimNetReader = new MatsimNetworkReader(scn);
		matsimNetReader.readFile(netFilePath);
		
		final Network net = scn.getNetwork();
		final TransitSchedule schedule = dataLoader.readTransitSchedule(schdFilePath);
		SingularValueDecomposition solver = new SingularValueDecomposition(net, schedule);
		new PopSecReader(scn, solver).readFile(popFilePath);
		
		//write solutions file
		File file = new File(popFilePath);
		solver.writeSolution(file.getPath() + "SVDSolutions.xls");
		
	}

}
