/* *********************************************************************** *
 * project: org.matsim.*
 * UniSAM.java
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
package playground.mfeil.MDSAM;

import org.apache.log4j.Logger;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.api.experimental.ScenarioImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import org.matsim.core.facilities.MatsimFacilitiesReader;

/**
 * Java re-implementation of Joh's uni-dimensional sequence alignment method
 * 
 * @author Matthias Feil
 */
public class UniSAM {
	
	private final PopulationImpl population;
	private final int agentIDthreshold;
	private static final Logger 			log = Logger.getLogger(UniSAM.class);
	
	public UniSAM (PopulationImpl population) {
		this.population = population;
		this.agentIDthreshold = 324;
	}
	
	private void run(){
		
	}
	
	private void write (String output){
	
		PrintStream stream;
		try {
			stream = new PrintStream (new File(output));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		stream.println("Id\tChoice\tx11\tx12\tx13\tx14\tx15\tx16\tx17\tx18\tx19\tx110\tx111" +
				"\tx21\tx22\tx23\tx24\tx25\tx26\tx27\tx28\tx29\tx210\tx211" +
				"\tx31\tx32\tx33\tx34\tx35\tx36\tx37\tx38\tx39\tx310\tx311" +
				"\tx41\tx42\tx43\tx44\tx45\tx46\tx47\tx48\tx49\tx410\tx411" +
				"\tx51\tx52\tx53\tx54\tx55\tx56\tx57\tx58\tx59\tx510\tx511");
		
		/* Write selected plan*/
		PlanImpl plan = this.population.getPersons().get(new IdImpl ("324")).getSelectedPlan();
		stream.print(1);
		stream.print("\t"+1);
		stream.print("\t"+(((ActivityImpl)(plan.getPlanElements().get(0))).getEndTime()-((ActivityImpl)(plan.getPlanElements().get(0))).getStartTime()+
				((ActivityImpl)(plan.getPlanElements().get(6))).getEndTime()-((ActivityImpl)(plan.getPlanElements().get(6))).getStartTime()));
		stream.print("\t"+(((ActivityImpl)(plan.getPlanElements().get(2))).getEndTime()-((ActivityImpl)(plan.getPlanElements().get(2))).getStartTime()));
		stream.print("\t"+0);
		stream.print("\t"+(((ActivityImpl)(plan.getPlanElements().get(2))).getEndTime()-((ActivityImpl)(plan.getPlanElements().get(2))).getStartTime()));
		stream.print("\t"+2);
		stream.print("\t"+1);
		stream.print("\t"+0);
		stream.print("\t"+1);
		double traveltime = 0;
		for (int i=1;i<plan.getPlanElements().size();i+=2){
			traveltime += ((LegImpl)(plan.getPlanElements().get(i))).getTravelTime();
		}
		stream.print("\t"+traveltime);
		stream.print("\t"+0);
		stream.print("\t"+0);		
	}
	
	private static ScenarioImpl read (String populationFilename, String networkFilename, String facilitiesFilename){
		ScenarioImpl scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFilename);
		new MatsimFacilitiesReader(scenario.getActivityFacilities()).readFile(facilitiesFilename);
		new MatsimPopulationReader(scenario).readFile(populationFilename);
		return scenario;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		final String populationFilename = "./plans/output_plans.xml.gz";
		final String datFilename = "./plans/plans.dat";
		final String networkFilename = "./test/scenarios/chessboard/network.xml";
		final String facilitiesFilename = "./test/scenarios/chessboard/facilities.xml";
		
		UniSAM sam = new UniSAM (UniSAM.read(populationFilename, networkFilename, facilitiesFilename).getPopulation());

		log.info("Analyzing similarity of plans ...");
		sam.run();
		log.info("Analyzing similarity of plans ... done");
		log.info("Writing plans.dat ...");
		sam.write(datFilename);
		log.info("Writing plans.dat ... done");

	}

}
