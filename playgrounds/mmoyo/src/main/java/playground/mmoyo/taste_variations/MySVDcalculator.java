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
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.mmoyo.analysis.tools.PtPlanAnalyzer;
import playground.mmoyo.analysis.tools.PtPlanAnalyzer.PtPlanAnalysisValues;
import playground.mmoyo.io.PopSecReader;
import playground.mmoyo.io.TextFileWriter;
import playground.mmoyo.utils.DataLoader;

public class MySVDcalculator implements PersonAlgorithm{
	PtPlanAnalyzer planAnalizer;
	private StringBuffer sBuff = new StringBuffer("IdAgent\tBetawalk\tBetainvehTime\tBetaDistance\tBetaTransfer");
	private ObjectAttributes attrs = new ObjectAttributes() ;
	final String TB = "\t";
	final String NL = "\n";
	
	public MySVDcalculator(final Network net, final TransitSchedule schedule){
		planAnalizer = new PtPlanAnalyzer(net, schedule);
	}
	
	public SVDvalues getSVDvalues(final Person person, double [] utilCorrection) {
		//objective: to create these matrices
		//						A						X			b
		//         walk time dist chng     	betas		UtlCorr 	
		//plan1 ┌ w1   t1   d1   c1 ¬  	┌ ßw¬   	┌λ1¬ 
		//plan2 | w2   t2 	  d2  	c2  |  	| ßt  |   	| λ2 |
		//plan3 | w3   t3	  d3 	c3  |  	| ßd  |	| λ3 |
		//plan4 └ w4   t4	  d4   c4 ┘		└ ßc ┘	└λ4 ┘
		
		int plansNum = person.getPlans().size();
		if (utilCorrection.length != plansNum){
			throw new RuntimeException(" Number of plans utility corrections is not the same as the number of agent plans");
		}
		
		//up to now it is hard coded to 4 beta values in matrix X: trWalkTime_sec, trTravelTime_sec, InVehDist_mts, transfers_num  
		double[][] arrayA = new double [plansNum][4]; 
		double[] arrayB = new double [utilCorrection.length]; //oder [plansNum]
		int i = 0;
		for (Plan plan: person.getPlans()){
			PtPlanAnalysisValues v = planAnalizer.run(plan);
			arrayA[i][0] = v.getTransitWalkTime_secs();  //[row][col] 
			arrayA[i][1] = v.trTravelTime_secs();
			arrayA[i][2] = v.getInVehDist_mts();
			arrayA[i][3] = v.getTransfers_num();
			arrayB[i]= utilCorrection[i];
			i++;
		}

		//invoke SVD
		RealMatrix matrixA = new Array2DRowRealMatrix(arrayA, false);   //"coefficients" matrix
		DecompositionSolver svd = new SingularValueDecompositionImpl(matrixA).getSolver(); 
		RealVector utlCorrections = new ArrayRealVector(arrayB, false); //"constants" matrix
		RealVector solution = svd.solve(utlCorrections);
		
		return new SVDvalues (person.getId(), solution.getEntry(0), solution.getEntry(1), solution.getEntry(2), solution.getEntry(3));
	}
	
	
	final String strWwalk = "wWalk";
	final String strWtime = "wTime";
	final String strWdist = "wDista";
	final String strWchng = "wChng";
	
	@Override
	public void run(final Person person) {   //it assumes that the score is the utility correction!
		double[] scores = new double[person.getPlans().size()];
		for (int i=0; i<person.getPlans().size() ; i++ ){
			scores[i] = person.getPlans().get(i).getScore();
		}
		SVDvalues svdValues = this.getSVDvalues(person, scores );
		sBuff.append(NL + svdValues.getIdAgent() + TB + svdValues.getWeight_trWalkTime() + TB + svdValues.getWeight_trTime() + TB + svdValues.getWeight_trDistance() + TB + svdValues.getWeight_changes());
		attrs.putAttribute( person.getId().toString() , strWwalk  , svdValues.getWeight_trWalkTime()) ;
		attrs.putAttribute( person.getId().toString() , strWtime  , svdValues.getWeight_trTime()) ;
		attrs.putAttribute( person.getId().toString() , strWdist  , svdValues.getWeight_trDistance()) ;
		attrs.putAttribute( person.getId().toString() , strWchng  , svdValues.getWeight_changes()) ;
	}
	
	protected void run (final Population pop){
		for(Person person: pop.getPersons().values()){
			run(person);	
		}
	}

	protected void writeSolutionTXT(final String outFile){
		new TextFileWriter().write(sBuff.toString(), outFile, false);
	}
	
	protected void writeSolutionObjAttr(final String outFile){
		new ObjectAttributesXmlWriter(attrs).writeFile(outFile);
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
			popFilePath = "../../";   //assumes that the scores are the utility correction
			netFilePath = "../../";
			schdFilePath = "../../";
		}

		DataLoader dataLoader = new DataLoader ();
		ScenarioImpl scn = (ScenarioImpl) dataLoader.createScenario(); 
		MatsimNetworkReader matsimNetReader = new MatsimNetworkReader(scn);
		matsimNetReader.readFile(netFilePath);
		
		final Network net = scn.getNetwork();
		final TransitSchedule schedule = dataLoader.readTransitSchedule(schdFilePath);
		MySVDcalculator solver = new MySVDcalculator(net, schedule);
		new PopSecReader(scn, solver).readFile(popFilePath);
		
		//write solutions file
		File file = new File(popFilePath);
		solver.writeSolutionObjAttr(file.getPath() + "SVDSolutions.xml.gz");
	}

}