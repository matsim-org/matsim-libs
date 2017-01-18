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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * @author amit
 */

public class PlansChoiceSetDistributions {

	private static final String ABS_PATH = "/Users/aagarwal/Desktop/ils4/agarwal/siouxFalls/";
	private static final String PLANS_0_ITS = ABS_PATH+"/outputMCOff/run33/output_plans.xml.gz";
	private static final String PLANS_STOP_REPLANNING = ABS_PATH+"outputMCOff/run105_2/ITERS/it.80/80.plans.xml.gz";
	private static final String PLANS_LAST_IT =  ABS_PATH+"outputMCOff/run105_2/ITERS/it.100/100.plans.xml.gz";
	private final Logger log = Logger.getLogger(PlansChoiceSetDistributions.class);
	
	public static void main(String[] args) {
		PlansChoiceSetDistributions pc =  new PlansChoiceSetDistributions();
		pc.writeLegsDistributionWRTSelectedPlan(PLANS_0_ITS, ABS_PATH+"/outputMCOff/run105_2/analysis/it.0.selectedLeg2OtherLegsDistribution.txt");
		pc.writeLegsDistributionWRTSelectedPlan(PLANS_STOP_REPLANNING, ABS_PATH+"/outputMCOff/run105_2/analysis/it.80.selectedLeg2OtherLegsDistribution.txt");
		pc.writeLegsDistributionWRTSelectedPlan(PLANS_LAST_IT, ABS_PATH+"/outputMCOff/run105_2/analysis/it.100.selectedLeg2OtherLegsDistribution.txt");

		pc.writeLegsDistribution(PLANS_0_ITS, ABS_PATH+"/outputMCOff/run105_2/analysis/it.0.personId2TravelLegsDistribution.txt");
		pc.writeLegsDistribution(PLANS_STOP_REPLANNING, ABS_PATH+"/outputMCOff/run105_2/analysis/it.80.personId2TravelLegsDistribution.txt");
		pc.writeLegsDistribution(PLANS_LAST_IT, ABS_PATH+"/outputMCOff/run105_2/analysis/it.100.personId2TravelLegsDistribution.txt");
		//		Map<Id, List<String>> personId2legs =  pc.getLegsForAllPlansInChoiceSet(plansAtStopReplannig);
		//		pc.writeMap2TxtFile(personId2legs, absolutePath+"/outputMC/run101_2/analysis/personId2TravelLegsForChoiceSet.txt");

		//		SortedMap<Double, Double> changeInCarLegsInChoiceSetOf0thAnd80thPlansDistribution = pc.plansFile2DifferenceClassCounts(plansAt0Iter, plansAtStopReplannig);
		//		SortedMap<Double, Double> changeInCarLegsInChoiceSetOf0thAnd100thPlansDistribution = pc.plansFile2DifferenceClassCounts(plansAt0Iter, plansAtLastIter);
		//
		//		pc.writeDifferenceInCarsDistributionData(changeInCarLegsInChoiceSetOf0thAnd80thPlansDistribution,absolutePath+"/outputMC/run101_2/analysis/changeInPtLegsInChoiceSetOf0thAnd80thPlansDistribution.txt");
		//		pc.writeDifferenceInCarsDistributionData(changeInCarLegsInChoiceSetOf0thAnd100thPlansDistribution,absolutePath+"/outputMC/run101_2/analysis/changeInPtLegsInChoiceSetOf0thAnd100thPlansDistribution.txt");
	}

	/**
	 * for each (car/pt) leg in selected plan, it counts number of cars in remaining plans in choice set of each person and then
	 * write the distribution for predefined intervals. For e.g. ==0.2	37.0	153.0== indicates that
	 *  37 persons have more than 0% and less than or equal to 20% cars in their choice set and car is leg in selected plan and
	 *  similarly 153 persons have more than 0% and less than or equal to 20% cars in their choice set and pt is leg in selected plan.
	 */
	private void writeLegsDistributionWRTSelectedPlan(final String plansFile, final String outputFile){
		SortedMap<Id<Person>, List<String>> personId2legs = getLegsForAllPlansInChoiceSet(plansFile);
		SortedMap<Double, Double> selectedCar2OtherLegs = new TreeMap<>();
		SortedMap<Double, Double> selectedPt2OtherLegs = new TreeMap<>();

        double [] intervals = {0.0,.20,.40,.60,.80,.90,1.00};

		for(double d:intervals){
			selectedCar2OtherLegs.put(d, 0.);
			selectedPt2OtherLegs.put(d, 0.);
		}

		for(Id<Person> pId: personId2legs.keySet()){
			List<String> legs = personId2legs.get(pId);
			double carCounter = 0;
			if(legs.get(0).equals("car")){

				for(int i=1;i<legs.size();i++){
					if(legs.get(i).equals("car")) carCounter++; 
				}
				double carShare = carCounter/(legs.size()-1); 

				// find interval
				double interval = 0;

				for(int i=0;i<intervals.length-1;i++){
					if(carShare>intervals[i]&&carShare<=intervals[i+1]) interval=intervals[i+1];
				}

				selectedCar2OtherLegs.put(interval, selectedCar2OtherLegs.get(interval)+1);

			} else if(legs.get(0).equals("pt")){
				for(int i=1;i<legs.size();i++){
					if(legs.get(i).equals("car")) carCounter++; 
				}
				double carShare = carCounter/(legs.size()-1); 

				// find interval
				double interval = 0;

				for(int i=0;i<intervals.length-1;i++){
					if(carShare>intervals[i]&&carShare<=intervals[i+1]) interval=intervals[i+1];
				}

				selectedPt2OtherLegs.put(interval, selectedPt2OtherLegs.get(interval)+1);
			}			
		}

		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);
		try {
			writer.write(" interval \t carCounts \t ptCounts \n");
			for(Double d:selectedCar2OtherLegs.keySet()){
				writer.write(d+"\t"+selectedCar2OtherLegs.get(d)+"\t"+selectedPt2OtherLegs.get(d)+"\n");
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in File. Reason "+e);
		}
		log.info("Data has written to "+ outputFile);
	}

	/**
	 * write "how many persons have 0/1/2/...6/7 car/pt in their plans choice set?". For e.g. ==0	16699.0		66564.0== 
	 * indicates that 16699 persons have 0 car in their choice set and 66564 persons have 0 pt in their choice set.
	 */
	private void writeLegsDistribution(final String plansFile, final String outputFile){
		Map<Id<Person>, List<String>> personId2legs = getLegsForAllPlansInChoiceSet(plansFile);
		Map<Id<Person>, double[]> personId2LegsCounts = getPersonId2LegsCountInChoiceSet(personId2legs);

		int carIndex =0;
		int ptIndex =1;

		int legsClassesLength = 8;

		Map<Integer, double[]> legsClass2LegsCounter = new HashMap<>();

		for(int i=0;i<legsClassesLength;i++){
			double [] legsCounter ={0.,0.};
			legsClass2LegsCounter.put(i, legsCounter);
		}


		for(Id<Person> id:personId2LegsCounts.keySet()){
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
		log.info("Data has written to "+ outputFile);
	}

	private void writeDifferenceInCarsDistributionData(final SortedMap<Double, Double> inputMap, final String outputFile){
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

	private SortedMap<Double, Double> plansFile2DifferenceClassCounts(final String initialPlans, final String laterPlans){

		Map<Id<Person>, List<String>> personId2legsInitial = getLegsForAllPlansInChoiceSet(initialPlans);
		Map<Id<Person>, List<String>> personId2legsLater = getLegsForAllPlansInChoiceSet(laterPlans);


		Map<Id<Person>, double[]> personId2LegsCountsInitial = getPersonId2LegsCountInChoiceSet(personId2legsInitial);
		Map<Id<Person>, double[]> personId2LegsCountsLater = getPersonId2LegsCountInChoiceSet(personId2legsLater);

		Map<Id<Person>, Double>  personId2ChangeInCars = getChangeInLegs(personId2LegsCountsInitial, personId2LegsCountsLater);

		return getChangeInCarDistribution(personId2ChangeInCars);
	}

	private Map<Id<Person>, List<String>> getScoresForAllPlansInChoiceSet(final String plansFile){
		Map<Id<Person>, List<String>> personId2ScoresInChoiceSet = new HashMap<>();
		Population population = LoadMyScenarios.loadScenarioFromPlans(plansFile).getPopulation();
		for(Person p : population.getPersons().values()){
			List<String> scoresChoiceSet = new ArrayList<>();

			for(int j=0;j<p.getPlans().size();j++){
				scoresChoiceSet.add("0");
			}
			int count =1;
			for(Plan plan:p.getPlans()){
				if(PersonUtils.isSelected(plan)){
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

	/**
	 * read plan file and return list of travel legs for complete choice set for each person
	 */
	private SortedMap<Id<Person>, List<String>> getLegsForAllPlansInChoiceSet (final String plansFile){
		SortedMap<Id<Person>, List<String>> personId2LegsInChoiceSet = new TreeMap<>();
		Population population = LoadMyScenarios.loadScenarioFromPlans(plansFile).getPopulation();
		for(Person p : population.getPersons().values()){
			List<String> legsChoiceSet = new ArrayList<>();

			for(int j=0;j<p.getPlans().size();j++){
				legsChoiceSet.add("NA");
			}
			// strictly forcing leg for selected plan at 0th position.
			int count =1;
			for(Plan plan:p.getPlans()){
				if (PersonUtils.isSelected(plan)){
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

	private void writeMap2TxtFile (final Map<Id<Person>, List<String>> personId2legs, final String outputFile){
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);
		try {
			for(Id<Person> id : personId2legs.keySet()){
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

	/**
	 * return number of car/pt in choice set for each person
	 */
	private Map<Id<Person>, double[]> getPersonId2LegsCountInChoiceSet (final Map<Id<Person>, List<String>> personId2legs){

		Map<Id<Person>, double[]> personId2LegsInChoiceSet = new HashMap<>();
		for(Id<Person> personId : personId2legs.keySet()){
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

	private Map<Id<Person>, Double> getChangeInLegs(final Map<Id<Person>, double[]> personId2legsInitialPlans, final Map<Id<Person>, double[]> personId2LegsLaterPlans){
		//  change = number of legs as car in later choice set - number of legs as car in former ChoiceSet
		Map<Id<Person>, Double> personId2ChangeInCars = new HashMap<>();

		int carIndex=0;
		int ptIndex =1;

		for(Id<Person> personId:personId2legsInitialPlans.keySet()){
			if(!personId2LegsLaterPlans.containsKey(personId)) throw new RuntimeException("Person id "+personId+" does not exist in later plans.");
			double diff = personId2LegsLaterPlans.get(personId)[carIndex]- personId2legsInitialPlans.get(personId)[carIndex] ;
			personId2ChangeInCars.put(personId, Double.valueOf(diff));
		}
		return personId2ChangeInCars;
	}

	private SortedMap<Double, Double> getChangeInCarDistribution (final Map<Id<Person>, Double> personId2ChangeInCars){

		double min=999;
		double max=-999;

		for(Id<Person> pId : personId2ChangeInCars.keySet()){
			double d=personId2ChangeInCars.get(pId);
			if (d<min) min =d ;
			else if(d>max) max=d;
		}

		SortedMap<Double, Double> differenceClass2PersonCounter = new TreeMap<>();

		double [] diffClasses = new double [(int)(max-min+1)];

		for(int i=0; i<diffClasses.length;i++){
			diffClasses[i]=min+i;
			differenceClass2PersonCounter.put(diffClasses[i], Double.valueOf(0.));
		}

		for(int j=(int) min; j<diffClasses.length;j++){
			for(Id<Person> pId:personId2ChangeInCars.keySet()){
				double d = personId2ChangeInCars.get(pId);
				if(d ==j){
					double classCounter = differenceClass2PersonCounter.get(d) +1;
					differenceClass2PersonCounter.put(d, classCounter);
				}
			}
		}
		return differenceClass2PersonCounter;
	}
}
