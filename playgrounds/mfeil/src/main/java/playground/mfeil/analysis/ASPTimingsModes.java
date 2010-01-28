/* *********************************************************************** *
 * project: org.matsim.*
 * AnalysisSelectedPlansTimingsModes.java
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

package playground.mfeil.analysis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.knowledges.Knowledges;



/**
 * Simple class to analyze the activity timings of selected plans. Calculates the average timings and 
 * number of modes used.
 * @author mfeil
 */
public class ASPTimingsModes extends ASPActivityChains {

	private final static Logger log = Logger.getLogger(ASPTimingsModes.class);
	
	public ASPTimingsModes(final PopulationImpl population, final Knowledges knowledges, final String outputDir) {
		super (population, null, knowledges, outputDir);
	}
	
	@Override
	protected void analyze(){
		
		ArrayList<List<PlanElement>> legs = new ArrayList<List<PlanElement>>();
		ArrayList<Integer> numberOccurrence = new ArrayList<Integer>();
	
		PrintStream stream1;
		try {
			stream1 = new PrintStream (new File(this.outputDir + "/analysis.xls"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		/* Analysis of activity chains */
		double averageACLength=0;
		stream1.println("Number of occurrences\tAverage activity timings");
		for (int i=0; i<this.activityChains.size();i++){
			legs.clear();
			numberOccurrence.clear();
			
			double weight = this.plans.get(i).size();
			stream1.print(weight+"\t");
			double length = this.activityChains.get(i).size();
			averageACLength+=weight*(java.lang.Math.ceil(length/2));
			for (int j=0; j<length;j=j+2){
				stream1.print(((ActivityImpl)(this.activityChains.get(i).get(j))).getType()+"\t");
			}
			stream1.println();
			stream1.print("\t");
			for (int k=0;k<this.plans.get(i).get(0).getPlanElements().size();k+=2){
				double duration=0;
				for (int j=0;j<this.plans.get(i).size();j++){
					if (k==0) duration += ((LegImpl)(this.plans.get(i).get(j).getPlanElements().get(1))).getDepartureTime();
					else if (k==this.plans.get(i).get(j).getPlanElements().size()-1) duration += 86400 - ((LegImpl)(this.plans.get(i).get(j).getPlanElements().get(this.plans.get(i).get(j).getPlanElements().size()-2))).getArrivalTime();
					else duration += ((LegImpl)(this.plans.get(i).get(j).getPlanElements().get(k+1))).getDepartureTime() - ((LegImpl)(this.plans.get(i).get(j).getPlanElements().get(k-1))).getArrivalTime();
				}
				stream1.print((duration/this.plans.get(i).size())+"\t");
			}
			stream1.println();
			for (int j=0;j<this.plans.get(i).size();j++){
				boolean isEqual = false;
				for (int k=0;k<legs.size();k++)	{
					if (checkLegsForEquality(legs.get(k), this.plans.get(i).get(j).getPlanElements())){
						numberOccurrence.set(k, numberOccurrence.get(k).intValue()+1);
						isEqual = true;
						break;
					}
				}
				if (!isEqual){
					legs.add(this.plans.get(i).get(j).getPlanElements());
					numberOccurrence.add(1);
				}
			}
			for (int j=0;j<legs.size();j++){
				stream1.print(numberOccurrence.get(j).intValue()+"\t");
				for (int k=1;k<legs.get(j).size();k+=2){
					stream1.print(((LegImpl)(legs.get(j).get(k))).getMode()+"\t");
				}		
				stream1.println();
			}
			stream1.println();
			
			
		}
		stream1.println((averageACLength/this.populationMATSim.getPersons().size())+"\tAverage number of activities");
		stream1.println();
		stream1.close();
	}
	
	private boolean checkLegsForEquality (List<PlanElement> in, List<PlanElement> out){
		
		if (in.size()!=out.size()){
		
			return false;
		}
		else{
			ArrayList<String> in1 = new ArrayList<String> ();
			ArrayList<String> out1 = new ArrayList<String> ();
			for (int i = 1;i<in.size();i=i+2){
				in1.add(((LegImpl)(in.get(i))).getMode().toString());				
			}
			for (int i = 1;i<out.size();i=i+2){
				out1.add(((LegImpl)(out.get(i))).getMode().toString());				
			}		
			return (in1.equals(out1));
		}
	}	
		

	public static void main(final String [] args) {
//		final String populationFilename = "./examples/equil/plans100.xml";
//		final String networkFilename = "./examples/equil/network.xml";
		final String populationFilename = "./plans/output_plans.xml.gz";
//		final String populationFilename = "./output/Test1/ITERS/it.0/0.plans.xml.gz";
		final String networkFilename = "./test/scenarios/chessboard/network.xml";
		final String facilitiesFilename = "./test/scenarios/chessboard/facilities.xml";

		final String outputDir = "./plans/";

		ScenarioImpl scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario).readFile(networkFilename);
		new MatsimFacilitiesReader(scenario).readFile(facilitiesFilename);
		new MatsimPopulationReader(scenario).readFile(populationFilename);

		ASPTimingsModes sp = new ASPTimingsModes(scenario.getPopulation(), scenario.getKnowledges(), outputDir);
		sp.analyze();
		sp.checkCorrectness();
		
		log.info("Analysis of plan finished.");
	}

}

