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
package playground.agarwalamit.siouxFalls;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author amit
 */
public class PlansComparison {

	private static final String absolutePath = "/Users/aagarwal/Desktop/ils4/agarwal/siouxFalls/";
	private static final String plansAt0Iter = 
			//			"/Users/aagarwal/Desktop/ils4/agarwal/patnaIndia/patnaOutput/modeChoice/run10/output_plans.xml.gz"; 
			absolutePath+"/outputMC/run33/output_plans.xml.gz";
	private static final String plansAtStopReplannig = absolutePath+"outputMC/run101_2/ITERS/it.80/80.plans.xml.gz";
	private static final String plansAtLastIter =  absolutePath+"outputMC/run101_2/ITERS/it.100/100.plans.xml.gz";

	public static void main(String[] args) {
		PlansComparison pc =  new PlansComparison();
		pc.writeLegsDistribution(plansAt0Iter, absolutePath+"/outputMC/run101_2/analysis/it.0.personId2TravelLegsDistribution.txt");
		pc.writeLegsDistribution(plansAtStopReplannig, absolutePath+"/outputMC/run101_2/analysis/it.80.personId2TravelLegsDistribution.txt");
		pc.writeLegsDistribution(plansAtLastIter, absolutePath+"/outputMC/run101_2/analysis/it.100.personId2TravelLegsDistribution.txt");
		//		Map<Id, List<String>> personId2legs =  pc.getLegsForAllPlansInChoiceSet(plansAtStopReplannig);
		//		pc.writeMap2TxtFile(personId2legs, absolutePath+"/outputMC/run101_2/analysis/personId2TravelLegsForChoiceSet.txt");

		SortedMap<Double, Double> changeInCarLegsInChoiceSetOf0thAnd80thPlansDistribution = pc.plansFile2DifferenceClassCounts(plansAt0Iter, plansAtStopReplannig);
		SortedMap<Double, Double> changeInCarLegsInChoiceSetOf0thAnd100thPlansDistribution = pc.plansFile2DifferenceClassCounts(plansAt0Iter, plansAtLastIter);

		pc.writeDifferenceInCarsDistributionData(changeInCarLegsInChoiceSetOf0thAnd80thPlansDistribution,absolutePath+"/outputMC/run101_2/analysis/changeInPtLegsInChoiceSetOf0thAnd80thPlansDistribution.txt");
		pc.writeDifferenceInCarsDistributionData(changeInCarLegsInChoiceSetOf0thAnd100thPlansDistribution,absolutePath+"/outputMC/run101_2/analysis/changeInPtLegsInChoiceSetOf0thAnd100thPlansDistribution.txt");
	}

	private void writeLegsDistribution(String plansFile, String outputFile){
		Map<Id, List<String>> personId2legs = getLegsForAllPlansInChoiceSet(plansFile);
		Map<Id, double[]> personId2LegsCounts = getPersonId2LegsCountInChoiceSet(personId2legs);

		int carIndex =0;
		int ptIndex =1;

		int legsClassesLength = 8;

		Map<Integer, double[]> legsClass2LegsCounter = new HashMap<Integer, double[]>();
		String [] legs = {"car","pt"};


		for(int i=0;i<legsClassesLength;i++){
			double [] legsCounter ={0.,0.};
			legsClass2LegsCounter.put(i, legsCounter);
		}


		for(Id id:personId2LegsCounts.keySet()){
			double carLegs = personId2LegsCounts.get(id)[carIndex];
			double ptLegs = personId2LegsCounts.get(id)[ptIndex];

			for(Integer j:legsClass2LegsCounter.keySet()){
				if((int)carLegs==j)  {
					int carCounterSoFar = (int) legsClass2LegsCounter.get(j) [carIndex];
					double [] legsCounter = {carCounterSoFar+1,legsClass2LegsCounter.get(j) [ptIndex]};
					legsClass2LegsCounter.put(j, legsCounter);
				}  
				if((int)ptLegs==j) {
					int ptCounterSoFar = (int) legsClass2LegsCounter.get(j) [ptIndex];
					double [] legsCounter = {legsClass2LegsCounter.get(j) [carIndex],ptCounterSoFar+1};
					legsClass2LegsCounter.put(j, legsCounter);
				}
			}
		}

		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);
		try {
			writer.write("numberOfLegs \t"+"car"+"\t"+"pt"+"\n");
			for(Integer i:legsClass2LegsCounter.keySet()){
				writer.write(i+"\t"+legsClass2LegsCounter.get(i)[carIndex]+"\t"+legsClass2LegsCounter.get(i)[ptIndex]+"\n");
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in File. Reason "+e);
		}
		legsClass2LegsCounter.clear();
	}

	private void writeDifferenceInCarsDistributionData(SortedMap<Double, Double> inputMap, String outputFile){
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);
		try {
			for(double d : inputMap.keySet()){
				writer.write(d+"\t"+inputMap.get(d)+"\n");
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason "+e);
		}
	}

	private SortedMap<Double, Double> plansFile2DifferenceClassCounts(String initialPlans, String laterPlans){

		Map<Id, List<String>> personId2legsInitial = getLegsForAllPlansInChoiceSet(initialPlans);
		Map<Id, List<String>> personId2legsLater = getLegsForAllPlansInChoiceSet(laterPlans);


		Map<Id, double[]> personId2LegsCountsInitial = getPersonId2LegsCountInChoiceSet(personId2legsInitial);
		Map<Id, double[]> personId2LegsCountsLater = getPersonId2LegsCountInChoiceSet(personId2legsLater);

		Map<Id, Double>  personId2ChangeInCars = getChangeInLegs(personId2LegsCountsInitial, personId2LegsCountsLater);
		SortedMap<Double, Double> changeInCarsDistribution =  getChangeInCarDistribution(personId2ChangeInCars);

		return changeInCarsDistribution;
	}

	private Map<Id, List<String>> getScoresForAllPlansInChoiceSet(String plansFile){
		Map<Id, List<String>> personId2ScoresInChoiceSet = new HashMap<Id, List<String>>();
		Population population = loadPopulation(plansFile);
		for(Person p : population.getPersons().values()){
			List<String> scoresChoiceSet = new ArrayList<String>();

			for(int j=0;j<p.getPlans().size();j++){
				scoresChoiceSet.add("0");
			}
			int count =1;
			for(Plan plan:p.getPlans()){
				if(plan.isSelected()){
					scoresChoiceSet.set(0, String.valueOf(plan.getScore()));
				} else {
					scoresChoiceSet.set(count, String.valueOf(plan.getScore()));
					count++;
				}
			}
			personId2ScoresInChoiceSet.put(p.getId(), scoresChoiceSet);
		}
		return personId2ScoresInChoiceSet;
	}

	private Map<Id, List<String>> getLegsForAllPlansInChoiceSet (String plansFile){
		Map<Id, List<String>> personId2LegsInChoiceSet = new HashMap<Id, List<String>>();
		Population population = loadPopulation(plansFile);
		for(Person p : population.getPersons().values()){
			List<String> legsChoiceSet = new ArrayList<String>();

			for(int j=0;j<p.getPlans().size();j++){
				legsChoiceSet.add("NA");
			}
			// strictly forcing leg for selected plan at 0th position.
			int count =1;
			for(Plan plan:p.getPlans()){
				if (plan.isSelected()){
					PlanElement pe = plan.getPlanElements().get(1);
					String leg = ((Leg) pe).getMode();
					legsChoiceSet.set(0, leg);
				} else {
					PlanElement pe = plan.getPlanElements().get(1);
					String leg = ((Leg) pe).getMode();
					legsChoiceSet.set(count,leg);
					count++;
				}
			}
			personId2LegsInChoiceSet.put(p.getId(), legsChoiceSet);
		}
		return personId2LegsInChoiceSet;
	}

	private void writeMap2TxtFile (Map<Id, List<String>> personId2legs, String outputFile){
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);
		try {
			for(Id id : personId2legs.keySet()){
				writer.write(id.toString()+"\t");
				for(Object o:personId2legs.get(id)){
					writer.write(o.toString()+"\t");
				}
				writer.newLine();
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in a File. Reason "+e);
		}
	}

	private Population loadPopulation (String inputPlansFile){
		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(inputPlansFile);
		Scenario sc = ScenarioUtils.loadScenario(config);
		return sc.getPopulation();
	}

	private Map<Id, double[]> getPersonId2LegsCountInChoiceSet (Map<Id, List<String>> personId2legs){

		Map<Id, double[]> personId2LegsInChoiceSet = new HashMap<Id, double[]>();
		for(Id personId : personId2legs.keySet()){
			List<String> initialLegs = personId2legs.get(personId);
			int carCounter =0;
			int ptCounter =0;
			for(int i=0;i<initialLegs.size();i++){
				if(initialLegs.get(i).equals("car")) carCounter++; 
				else if(initialLegs.get(i).equals("pt")) ptCounter++;
			} 
			personId2LegsInChoiceSet.put(personId, new double []{carCounter, ptCounter});
		}
		return personId2LegsInChoiceSet;
	}

	private Map<Id, Double> getChangeInLegs(Map<Id, double[]> personId2legsInitialPlans, Map<Id, double[]> personId2LegsLaterPlans){
		//  change = number of legs as car in later choice set - number of legs as car in former ChoiceSet
		Map<Id, Double> personId2ChangeInCars = new HashMap<Id, Double>();

		int carIndex=0;
		int ptIndex =1;

		for(Id personId:personId2legsInitialPlans.keySet()){
			if(!personId2LegsLaterPlans.containsKey(personId)) throw new RuntimeException("Person id "+personId+" does not exist in later plans.");
			double diff = personId2LegsLaterPlans.get(personId)[carIndex]- personId2legsInitialPlans.get(personId)[carIndex] ;
			personId2ChangeInCars.put(personId, Double.valueOf(diff));
		}
		return personId2ChangeInCars;
	}

	private SortedMap<Double, Double> getChangeInCarDistribution (Map<Id, Double> personId2ChangeInCars){

		double min=999;
		double max=-999;

		for(Id pId : personId2ChangeInCars.keySet()){
			double d=personId2ChangeInCars.get(pId);
			if (d<min) min =d ;
			else if(d>max) max=d;
		}

		SortedMap<Double, Double> differenceClass2PersonCounter = new TreeMap<Double, Double>();

		double [] diffClasses = new double [(int)(max-min+1)];

		for(int i=0; i<diffClasses.length;i++){
			diffClasses[i]=min+i;
			differenceClass2PersonCounter.put(diffClasses[i], Double.valueOf(0.));
		}

		for(int j=(int) min; j<diffClasses.length;j++){
			for(Id pId:personId2ChangeInCars.keySet()){
				double d = (double) personId2ChangeInCars.get(pId);
				if((double)d==j){
					double classCounter = ((double) differenceClass2PersonCounter.get(d))+1;
					differenceClass2PersonCounter.put(d, classCounter);
				}
			}
		}
		return differenceClass2PersonCounter;
	}
}
