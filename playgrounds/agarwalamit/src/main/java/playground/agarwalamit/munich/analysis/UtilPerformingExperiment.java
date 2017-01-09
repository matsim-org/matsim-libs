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
package playground.agarwalamit.munich.analysis;

import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.activity.LegModeActivityEndTimeAndActDurationHandler;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.vsp.analysis.modules.AbstractAnalysisModule;

/**
 * @author amit
 */
public class UtilPerformingExperiment extends AbstractAnalysisModule {

	private final LegModeActivityEndTimeAndActDurationHandler actDurationUtilHandler;
	private final String eventsFile;
	private final String outputDir;
	private static final Logger LOG = Logger.getLogger(UtilPerformingExperiment.class);

	private final Map<String, Double> actType2TypicalDuration ;
	private double marginalUtilPerformingSec ;
	private final SortedMap<String, Double> actType2UnderPerformUtils;
	private final SortedMap<String, Double> actType2OverPerformUtils;
	private final SortedMap<String, Double> actType2EqualPerformUtils;

	public UtilPerformingExperiment(String outputDir, Scenario scenario) {
		super(UtilPerformingExperiment.class.getSimpleName());
		this.outputDir = outputDir;
		this.actDurationUtilHandler = new LegModeActivityEndTimeAndActDurationHandler(scenario);
		int lastIt = scenario.getConfig().controler().getLastIteration();
		eventsFile = outputDir+"/ITERS/it."+lastIt+"/"+lastIt+".events.xml.gz";

		actType2TypicalDuration = new HashMap<>();
		actType2UnderPerformUtils = new TreeMap<>();
		actType2EqualPerformUtils = new TreeMap<>();
		actType2OverPerformUtils = new TreeMap<>();

		initializeActType2DurationsMaps(scenario.getConfig());
	}

	@Override
	public List<EventHandler> getEventHandler() {
		return null;
	}

	@Override
	public void preProcessData() {
		EventsManager manager = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(manager);
		manager.addHandler(actDurationUtilHandler);
		reader.readFile(eventsFile);
	}

	@Override
	public void postProcessData() {
		Map<Id<Person>, SortedMap<String, Double>> personId2ActType2ActDurations = actDurationUtilHandler.getPersonId2ActType2ActDurations();
		for(Id<Person> personId : personId2ActType2ActDurations.keySet()){
			for(String actType:personId2ActType2ActDurations.get(personId).keySet()){
				double typActDuration = actType2TypicalDuration.get(actType);
				double actDur = personId2ActType2ActDurations.get(personId).get(actType);
				if(actDur==typActDuration) {
					double utilSoFar = actType2EqualPerformUtils.get(actType);
					double utilNow = utilSoFar+10*marginalUtilPerformingSec;
					actType2EqualPerformUtils.put(actType,utilNow);
				} else if(actDur<typActDuration) {
					double utilSoFar = actType2UnderPerformUtils.get(actType);
					double utilNow = utilSoFar + calcUtil4Performing(typActDuration, actDur);
					actType2UnderPerformUtils.put(actType, utilNow);
				} else if (actDur>typActDuration){
					double utilSoFar = actType2OverPerformUtils.get(actType);
					double utilNow = utilSoFar + calcUtil4Performing(typActDuration, actDur);
					actType2OverPerformUtils.put(actType, utilNow);
				}
			}
		}
	}

	@Override
	public void writeResults(String outputFolder) {
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder+"/analysis/actPerformDurationData.txt");
		try {
			writer.write("ActType \t typicalDuration \t underPerformDuration \t equalPerformDuration \t overPerformDuration \n");
			for(String actType : actType2TypicalDuration.keySet()){
				writer.write(actType+"\t"+actType2TypicalDuration.get(actType)+
						"\t"+actType2UnderPerformUtils.get(actType)+
						"\t"+actType2EqualPerformUtils.get(actType)+
						"\t"+actType2OverPerformUtils.get(actType)+"\n");
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written into File. Reason "+e);
		}
	}

	private void initializeActType2DurationsMaps(Config config){
		LOG.info("Storing activity type and typical durations.");

		for(String actType :config.planCalcScore().getActivityTypes()){
			actType2TypicalDuration.put(actType, config.planCalcScore().getActivityParams(actType).getTypicalDuration());
			actType2UnderPerformUtils.put(actType, 0.0);
			actType2OverPerformUtils.put(actType, 0.0);
			actType2EqualPerformUtils.put(actType, 0.0);
		}
		marginalUtilPerformingSec = config.planCalcScore().getPerforming_utils_hr() / 3600 ;
	}

	/**
	 * Only true if all activities have same priority
	 */
	private double calcUtil4Performing(final double typDuration, final double actualDuration){
		return 10 * marginalUtilPerformingSec + marginalUtilPerformingSec * typDuration * Math.log(actualDuration/typDuration);
	}

	private void run(){
		preProcessData();
		postProcessData();
		writeResults(outputDir);
	}

	public static void main(String[] args) {
		String outputDir = "/Users/aagarwal/Desktop/ils4/agarwal/munich/output/1pct/ci/";
		String configFile = outputDir+"/output_config.xml";
		String populationFile = outputDir+"/output_plans.xml.gz";
		Scenario sc = LoadMyScenarios.loadScenarioFromPlansAndConfig(populationFile, configFile);
		new UtilPerformingExperiment(outputDir,sc).run();
	}
}
