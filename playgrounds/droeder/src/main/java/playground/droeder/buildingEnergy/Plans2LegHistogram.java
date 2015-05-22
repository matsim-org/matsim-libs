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
package playground.droeder.buildingEnergy;

import org.apache.log4j.Logger;
import org.matsim.analysis.LegHistogram;
import org.matsim.analysis.LegHistogramChart;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.algorithms.PersonAlgorithm;

import java.io.File;
import java.util.List;

/**
 * @author droeder
 *
 */
public abstract class Plans2LegHistogram {

	private static final Logger log = Logger
			.getLogger(Plans2LegHistogram.class);
	
	public static void main(String[] args) {
		if(args.length == 0){
			args = new String[]{
					"E:\\VSP\\svn\\studies\\countries\\de\\berlin\\plans\\baseplan_900s.xml.gz",
					"C:\\Users\\Daniel\\Desktop\\buildingEnergy\\compareData\\"
			};
		}
		String plansfile = args[0];
		String outputpath = new File(args[1]).getAbsolutePath() + System.getProperty("file.separator");
		OutputDirectoryLogging.initLogging(new OutputDirectoryHierarchy(outputpath, "legHistograms", OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles));
		OutputDirectoryLogging.catchLogEntries();
		log.info("plansfile: " + plansfile);
		log.info("outputpath: " + outputpath);
		
		final LegHistogram histo = new LegHistogram(300);
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		((PopulationImpl) sc.getPopulation()).setIsStreaming(true);
		((PopulationImpl) sc.getPopulation()).addAlgorithm(new PersonAlgorithm() {
			
			@Override
			public void run(Person person) {
				List<PlanElement> pe = person.getSelectedPlan().getPlanElements();
				for(int i = 1; i < pe.size(); i += 2){
					LegImpl l = (LegImpl) pe.get(i);
					histo.handleEvent(new PersonDepartureEvent(l.getDepartureTime(), null, null, l.getMode()));
					double arrivaltime = (l.getArrivalTime() == Time.UNDEFINED_TIME) ? l.getDepartureTime() + l.getTravelTime() : l.getArrivalTime();
					histo.handleEvent(new PersonArrivalEvent(arrivaltime, null, null, l.getMode()));
				}
			}
		});
		
		new MatsimPopulationReader(sc).readFile(plansfile);
		
		LegHistogramChart.writeGraphic(histo, outputpath + "legHistogram_all.png");
		log.info(outputpath + "legHistogram_all.png written.");
		for(String mode : histo.getLegModes()){
			String name = new String(mode).replace(System.getProperty("file.separator"), "");
			LegHistogramChart.writeGraphic(histo, outputpath + "legHistogram_" + name + ".png", mode);
			log.info(outputpath + "legHistogram_" + name + ".png written.");
		}
		OutputDirectoryLogging.closeOutputDirLogging();
	}
}

