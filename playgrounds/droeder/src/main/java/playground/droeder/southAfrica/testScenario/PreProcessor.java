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
package playground.droeder.southAfrica.testScenario;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;

import playground.andreas.utils.pt.TransitScheduleCleaner;
import playground.balmermi.census2000v2.PopulationSample;
import playground.southafrica.population.HouseholdSampler;
import playground.southafrica.utilities.FileUtils;
import uk.co.randomjunk.osmosis.transform.Output;

/**
 * @author droeder
 *
 */
public class PreProcessor {

	private static final Logger log = Logger.getLogger(PreProcessor.class);

	private PreProcessor() {
		//Auto-generated constructor stub
	}
	
	private final static String INPUTDIR = "E:/rsa/Data-nmbm/";
	private final static double SAMPLESIZE = 0.01;
	private final static String OUTDIR = "E:/rsa/server/sample_" + String.valueOf(SAMPLESIZE) + "/";
	
	public static void main(String[] args) {
		if(!new File(OUTDIR).exists()){
			new File(OUTDIR ).mkdirs();
		}
		// sample households#########################
		String[] arg = new String[4];
		arg[0] = INPUTDIR + "population/20120817_100pct/";
		arg[1] = OUTDIR;
		arg[2] = String.valueOf(SAMPLESIZE);
		arg[3] = INPUTDIR + "transit/bus&train/NMBM_PT_V1.xml.gz";
		HouseholdSampler.main(arg);
		//###########################################
		
		// create data for run ######################
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		sc.getConfig().scenario().setUseTransit(true);
		
		// remove car and bus from rail-network######
		new MatsimNetworkReader(sc).readFile(INPUTDIR + "transit/bus&train/NMBM_PT_V1.xml.gz");
		log.info("making rail links accesible for rails only...");
		Set<String> modes = new HashSet<String>(){{
			add("rail");
		}};
		for(Link l: sc.getNetwork().getLinks().values()){
			if(l.getAllowedModes().contains("rail")){
				l.setAllowedModes(modes);
			}
		}
		new NetworkWriter(sc.getNetwork()).write(OUTDIR + "NMBM_PT_V1_railOnlyRail.xml.gz");
		log.info("new Network written to " + OUTDIR + "NMBM_PT_V1_railOnlyRail.xml.gz");
		// ##########################################
		
		log.info("changing legModes as follows. ride->ride2, pt1->bus, pt2->rail");
		class LocalPersonAlgorithm implements PersonAlgorithm{
			
			Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			
			@Override
			public void run(Person person) {
				for(PlanElement  pe: person.getSelectedPlan().getPlanElements()){
					if(pe instanceof Leg){
						if(((Leg) pe).getMode().equals(TransportMode.ride)){
							((Leg) pe).setMode("ride2");
						} 
						if(((Leg) pe).getMode().equals("pt1")){
							((Leg) pe).setMode("bus");
						}
						if(((Leg) pe).getMode().equals("pt2")){
							((Leg) pe).setMode("rail");
						}
					}
				}
				sc.getPopulation().addPerson(person);
			}
			
			public Population getPopulation(){
				return sc.getPopulation();
			}
			
		}
		((PopulationImpl) sc.getPopulation()).setIsStreaming(true);
		PersonAlgorithm pa = new LocalPersonAlgorithm();
		((PopulationImpl) sc.getPopulation()).addAlgorithm(pa);
		log.info("read " + OUTDIR + "population.xml.gz");
		new MatsimPopulationReader(sc).parse(OUTDIR + "population.xml.gz");
		new PopulationWriter(((LocalPersonAlgorithm) pa).getPopulation(), sc.getNetwork()).write(OUTDIR + "population.changedLegModes.xml.gz");
		log.info("new Population written to " + OUTDIR + "population.changedLegModes.xml.gz");
		
		
		// remove routes without departure and lines without route from the schedule
		new TransitScheduleReader(sc).readFile(INPUTDIR + "transit/bus&train/Transitschedule_PT_V1_WithVehicles.xml.gz");
		TransitSchedule sched = TransitScheduleCleaner.removeRoutesWithoutDepartures(sc.getTransitSchedule());
		sched = TransitScheduleCleaner.removeEmptyLines(sched);
		new TransitScheduleWriter(sched).writeFile(OUTDIR + "Transitschedule_PT_V1_WithVehicles_emptyRemoved.xml.gz");
		log.info("new TransitSchedule written to " + OUTDIR + "Transitschedule_PT_V1_WithVehicles_emptyRemoved.xml.gz");
		IOUtils.copyFile(new File(INPUTDIR + "transit/bus&train/transitVehicles_PT_V1.xml.gz"), 
				new File(OUTDIR + "transitVehicles_PT_V1.xml.gz"));
		log.info("finished...");
	}
}

