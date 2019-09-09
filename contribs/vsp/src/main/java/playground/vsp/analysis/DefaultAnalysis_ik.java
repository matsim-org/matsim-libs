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

package playground.vsp.analysis;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.utils.io.UncheckedIOException;

import playground.vsp.analysis.modules.AbstractAnalysisModule;
import playground.vsp.analysis.modules.waitingTimes.WaitingTimesAnalyzer;
import playground.vsp.analysis.modules.welfareAnalyzer.WelfareAnalyzer;

/**
 * 
 * @author ikaddoura, aneumann
 *
 */
public class DefaultAnalysis_ik {
	
	private final static Logger log = Logger.getLogger(DefaultAnalysis_ik.class);
	
	private final String outputDir;
	private final String eventsFile;
	private final MutableScenario scenario;
	
	private final List<AbstractAnalysisModule> anaModules = new LinkedList<AbstractAnalysisModule>();

	public DefaultAnalysis_ik(MutableScenario scenario, String iterationOutputDir, String eventsFile) {
		this.outputDir = iterationOutputDir + "defaultAnalysis" + "/";
		this.eventsFile = eventsFile;
		this.scenario = scenario;
	}

	public void init(String aasRunnerConfigFile){
		log.info("Configuration through config file is currently not implemented. Initializing all modules with defaults...");
				
		// END of configuration file

//		EmissionEventsWriter emiWriter = new EmissionEventsWriter(ptDriverPrefix, this.outputDir);
//		// additional files, required for this analysis module:
//		String emissionVehicleFile = "/Users/Ihab/Documents/workspace/shared-svn/projects/detailedEval/pop/merged/emissionVehicles_1pct.xml.gz";
//		String emissionInputPath = "/Users/Ihab/Documents/workspace/shared-svn/projects/detailedEval/emissions/hbefaForMatsim/";
//		String roadTypeMappingFile = emissionInputPath + "roadTypeMapping.txt";
//		String averageFleetWarmEmissionFactorsFile = emissionInputPath + "EFA_HOT_vehcat_2005average.txt";
//		String averageFleetColdEmissionFactorsFile = emissionInputPath + "EFA_ColdStart_vehcat_2005average.txt";
//		boolean isUsingDetailedEmissionCalculation = true;
//		String detailedWarmEmissionFactorsFile = emissionInputPath + "EFA_HOT_SubSegm_2005detailed.txt";
//		String detailedColdEmissionFactorsFile = emissionInputPath + "EFA_ColdStart_SubSegm_2005detailed.txt";
//		VspExperimentalConfigGroup vcg = scenario.getConfig().vspExperimental() ;
//		vcg.setEmissionRoadTypeMappingFile(roadTypeMappingFile);
//		vcg.setEmissionVehicleFile(emissionVehicleFile);
//		vcg.setAverageWarmEmissionFactorsFile(averageFleetWarmEmissionFactorsFile);
//		vcg.setAverageColdEmissionFactorsFile(averageFleetColdEmissionFactorsFile);
//		vcg.setIsUsingDetailedEmissionCalculation(isUsingDetailedEmissionCalculation);
//		vcg.setDetailedWarmEmissionFactorsFile(detailedWarmEmissionFactorsFile);
//		vcg.setDetailedColdEmissionFactorsFile(detailedColdEmissionFactorsFile);
//		emiWriter.init(scenario);
//		this.anaModules.add(emiWriter);
//		
//		// emission events file required for this analysis module:
//		String emissionsEventsFile = "/Users/Ihab/Documents/workspace/shared-svn/studies/ihab/test/output/test_fakePt/ITERS/it.0/analysis/EmissionEventsWriter/emission.events.xml";
//		EmissionsAnalyzer emiAna = new EmissionsAnalyzer(ptDriverPrefix, emissionsEventsFile);
//		emiAna.init(scenario);
//		this.anaModules.add(emiAna);
//		
//		// number of detailed slots required for this analysis module
//		int numberOfDetailedSlots = 5;
//		VehDelayAtStopHistogramAnalyzer delayHistogramAna = new VehDelayAtStopHistogramAnalyzer(ptDriverPrefix, numberOfDetailedSlots);
//		delayHistogramAna.init(scenario);
//		this.anaModules.add(delayHistogramAna);
		
//		WelfareAnalyzer welfareAna = new WelfareAnalyzer();
//		welfareAna.init(scenario);
//		this.anaModules.add(welfareAna);
		
		WaitingTimesAnalyzer waitingTimeAna = new WaitingTimesAnalyzer();
		waitingTimeAna.init(scenario);
		this.anaModules.add(waitingTimeAna);

		// END ugly code - Initialization needs to be configurable
				
//		for (Person person : this.scenario.getPopulation().getPersons().values()) {
//			if (person.getId().toString().startsWith(ptDriverPrefix)){
//				throw new RuntimeException("Person " + person.getId() + " has the same prefix as the pt Driver: " + ptDriverPrefix + ". Aborting...");
//			}
//		}
		
		log.info("Initializing all modules with defaults... done.");
	}

	public void preProcess(){
		log.info("Preprocessing all modules...");
		for (AbstractAnalysisModule module : this.anaModules) {
			module.preProcessData();
		}
		log.info("Preprocessing all modules... done.");
	}
	
	public void run(){
		EventsManager eventsManager = EventsUtils.createEventsManager();
		for (AbstractAnalysisModule module : this.anaModules) {
			for (EventHandler handler : module.getEventHandler()) {
				eventsManager.addHandler(handler);
			}
		}
		try {
			log.info("Trying to parse eventsFile " + this.eventsFile);
			EventsReaderXMLv1 reader = new EventsReaderXMLv1(eventsManager);
			reader.readFile(this.eventsFile);
		} catch (UncheckedIOException e) {
			log.warn("Failed parsing " + this.eventsFile + ". Skipping events handling...");
		}
	}
	
	public void postProcess(){
		log.info("Postprocessing all modules...");
		for (AbstractAnalysisModule module : this.anaModules) {
			module.postProcessData();
		}
		log.info("Postprocessing all modules... done.");
	}
	
	public void writeResults(){
		log.info("Writing results for all modules...");
		log.info("Generating output directory " + this.outputDir);
		new File(this.outputDir).mkdir();
		
		for (AbstractAnalysisModule module : this.anaModules) {
			String moduleOutputDir = this.outputDir + module.getName() + "/";
			log.info("Writing results of module " + module.getName() + " to " + moduleOutputDir + "...");
			new File(moduleOutputDir).mkdir();
			module.writeResults(moduleOutputDir);
			log.info("Writing results of module " + module.getName() + " to " + moduleOutputDir + "... done.");
		}
		log.info("Writing results for all modules... done.");

		Gbl.printElapsedTime();
		Gbl.printMemoryUsage();
	}
}
