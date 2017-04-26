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
package playground.agarwalamit.analysis.activity.scoring;

import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

import playground.agarwalamit.analysis.activity.ActivityType2ActDurationsAnalyzer;
import playground.agarwalamit.utils.FileUtils;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;

/**
 * @author amit
 */

public class Person2ActivityPerformingWriter {
	private static final Logger LOG = Logger.getLogger(Person2ActivityPerformingWriter.class);
	private static final String outputFilesDir = FileUtils.RUNS_SVN+"/detEval/emissionCongestionInternalization/output/1pct/run0/baseCaseCtd/";
	private final Map<Id<Person>,Map<String, Double>> personId2Act2UtilPerf = new HashMap<>();
	private final Set<String> actTypes = new HashSet<>();
	private final UserGroup userGroup = UserGroup.URBAN;

	public static void main(String[] args) {
		new Person2ActivityPerformingWriter().run();
	}

	private void run(){
		ActivityType2ActDurationsAnalyzer actAnalyzer = new ActivityType2ActDurationsAnalyzer(outputFilesDir);
		actAnalyzer.preProcessData();
		actAnalyzer.postProcessData();

		Map<Id<Person>, List<Tuple<String, Double>>> personId2Act2StartTimes = actAnalyzer.getPersonId2ActStartTimes();
		Map<Id<Person>, List<Tuple<String, Double>>> personId2Act2EndTimes = actAnalyzer.getPersonId2ActEndTimes();

		final Scenario sc = LoadMyScenarios.loadScenarioFromOutputDir(outputFilesDir);


		for(Id<Person> personId : personId2Act2EndTimes.keySet()){
			
			Person p = sc.getPopulation().getPersons().get(personId);
			personId2Act2UtilPerf.put(p.getId(), new HashMap<>());

			List<Tuple<String, Double>> actEndTimes = personId2Act2EndTimes.get(p.getId());
			List<Tuple<String, Double>> actStartTimes = personId2Act2StartTimes.get(p.getId());

			//store endTime of last activity if last activity do not have end time.
			if(actStartTimes.size() != actEndTimes.size()) {
				int noOfActivities = actStartTimes.size();
				String lastActType = actStartTimes.get(noOfActivities-1).getFirst();
				actEndTimes.add(new Tuple<>(lastActType, 24. * 3600.));
			} else {
				LOG.warn("Person "+p.getId()+" do not have any open ended activity and simulation ends."
						+ "Possible explanation must be stuckAndAbort.");
			}

			//check first and last act type
			String firstActType = actEndTimes.get(0).getFirst();
			int lastActIndex = actStartTimes.size()-1;
			String lastActType = actStartTimes.get(lastActIndex).getFirst();

			if(firstActType.equals(lastActType)){ // first act == last act
				Activity act = PopulationUtils.createActivityFromLinkId(firstActType, Id.createLinkId("NONE"));
				act.setStartTime(actStartTimes.get(lastActIndex).getSecond());
				act.setEndTime(actEndTimes.get(0).getSecond()+24*3600);
				storeUtilOfPerforming(sc, p, act);
			} else {	// first act != last act

				Activity act = PopulationUtils.createActivityFromLinkId(firstActType, Id.createLinkId("NONE"));
				act.setStartTime(actStartTimes.get(0).getSecond());
				act.setEndTime(actEndTimes.get(0).getSecond());
				storeUtilOfPerforming(sc, p, act);

				act = PopulationUtils.createActivityFromLinkId(lastActType, Id.createLinkId("NONE"));
				act.setStartTime(actStartTimes.get(lastActIndex).getSecond());
				act.setEndTime(actEndTimes.get(lastActIndex).getSecond());
				storeUtilOfPerforming(sc, p, act);
			}

			actTypes.add(firstActType);
			actTypes.add(lastActType);

			// remaining act
			for (int index = 1; index < actEndTimes.size()-1;index++){
				if(!actStartTimes.get(index).getFirst().equals(actEndTimes.get(index).getFirst())) throw new RuntimeException("Activities are not same. Aborting...");//just for double check
				Activity act = PopulationUtils.createActivityFromLinkId(actEndTimes.get(index).getFirst(), Id.createLinkId("NONE"));
				act.setStartTime(actStartTimes.get(index).getSecond());
				act.setEndTime(actEndTimes.get(index).getSecond());
				storeUtilOfPerforming(sc, p, act);
				actTypes.add(actStartTimes.get(index).getFirst());
			}
		}

		//finally write the data
		writeActType2UtilPerforming(outputFilesDir+"/analysis/");
	}

	private void storeUtilOfPerforming(final Scenario sc, final Person p, final Activity activity) {
		ScoringFunctionFactory sfFactory = new ScoringFunctionFactory() {
			final ScoringParameters params = new ScoringParameters.Builder(sc, p.getId()).build();

			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				return new Person2ActivityScoringFunction(new CharyparNagelActivityScoring(params));
			}
		};

		ScoringFunction sf = sfFactory.createNewScoringFunction(p);
		sf.handleActivity(activity);
		sf.finish();

		Map<String, Double> actType2Util = personId2Act2UtilPerf.get(p.getId());
		if(actType2Util.containsKey(activity.getType())){
			actType2Util.put(activity.getType(), sf.getScore() + actType2Util.get(activity.getType()));
		} else {
			actType2Util.put(activity.getType(), sf.getScore());
		}
	}

	public void writeActType2UtilPerforming(final String outputFolder) {
		String fileName = outputFolder+"/"+userGroup+"_actTyp2TotalUtilityOfPerforming.txt";
		SortedMap<String, Double> act2UtilPerform = getActType2TotalUtilOfPerforming();

		BufferedWriter writer = IOUtils.getBufferedWriter(fileName);
		try {
			writer.write("ActType \t totalUtilPerforming \n");
			double sum =0;
			for(String act : act2UtilPerform.keySet()){
				double util = act2UtilPerform.get(act);
				writer.write(act+"\t"+util+"\n");
				sum +=util;
			}
			writer.write("TotalUtilPerforming \t "+sum+"\n");
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Reason - " + e);
		}
		LOG.info("Data is written to file "+fileName);
	}

	/**
	 * @return activity type to total utility of performing
	 */
	public SortedMap<String, Double> getActType2TotalUtilOfPerforming(){
		PersonFilter pf = new PersonFilter();
		SortedMap<String, Double> act2TotalUtilPerforming = new TreeMap<>();

		for(String actType : actTypes){
			double sum=0;
			for(Id<Person> id : personId2Act2UtilPerf.keySet()){
				if(!pf.isPersonIdFromUserGroup(id, userGroup)) continue; 
					if(personId2Act2UtilPerf.get(id).containsKey(actType)){
						double util = personId2Act2UtilPerf.get(id).get(actType);
						sum +=util;
					}
				
			}
			act2TotalUtilPerforming.put(actType, sum);
		}
		return act2TotalUtilPerforming;
	}

	public class Person2ActivityScoringFunction implements ScoringFunction{
		private final CharyparNagelActivityScoring delegate;
		public Person2ActivityScoringFunction(final CharyparNagelActivityScoring delegate) {
			this.delegate = delegate;
		}
		@Override
		public final void handleActivity(Activity activity) {

			double startTime = activity.getStartTime();
			double endTime = activity.getEndTime();

			if (startTime == Time.UNDEFINED_TIME && endTime != Time.UNDEFINED_TIME) {
				throw new RuntimeException("not implemented yet.");
			} else if (startTime != Time.UNDEFINED_TIME && endTime != Time.UNDEFINED_TIME) {
				this.delegate.handleActivity(activity);
			} else if (startTime != Time.UNDEFINED_TIME && endTime == Time.UNDEFINED_TIME) {
				throw new RuntimeException("not implemented yet.");
			} else {
				throw new RuntimeException("Trying to score an activity without start or end time. Should not happen."); 	
			}
		}

		@Override
		public final void handleLeg(Leg leg) {
			throw new RuntimeException("not yet implemented");
		}

		@Override
		public void addMoney(double amount) {
			throw new RuntimeException("not yet implemented");
		}

		@Override
		public void agentStuck(double time) {
			throw new RuntimeException("not yet implemented");
		}

		@Override
		public void handleEvent(Event event) {
			throw new RuntimeException("not yet implemented");
		}

		@Override
		public void finish() {
			this.delegate.finish();
		}

		@Override
		public double getScore() {
			return this.delegate.getScore();
		}
	}
}