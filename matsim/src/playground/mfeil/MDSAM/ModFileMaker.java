/* *********************************************************************** *
 * project: org.matsim.*
 * DatFileMaker.java
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

import java.io.File;
import playground.mfeil.ActChainEqualityCheck;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.population.PlanElement;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.knowledges.Knowledges;


/**
 * Creates mod-file for Biogeme estimation.
 *
 * @author mfeil
 */
public class ModFileMaker {

	protected final PopulationImpl population;
	protected ArrayList<List<PlanElement>> activityChains;
	protected static final Logger log = Logger.getLogger(ModFileMaker.class);
	


	public ModFileMaker(final PopulationImpl population) {
		this.population = population;
	}
	
	public void write (String outputFile){
		
		//Choose any person
		PersonImpl person = this.population.getPersons().values().iterator().next();
		PrintStream stream;
		try {
			stream = new PrintStream (new File(outputFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		// Model description
		stream.println("[ModelDescription]");
		stream.println("\"Multinomial logit, estimating parameters of MATSim utility function.\"");
		stream.println();
		
		//Choice
		stream.println("[Choice]");
		stream.println();
		
		//Beta
		stream.println("[Beta]");
		stream.println("//Name \tValue  \tLowerBound \tUpperBound  \tstatus (0=variable, 1=fixed");
		
		stream.println("HomeUmax \t60  \t-10000 \t10000  \t0");
		stream.println("WorkUmax \t55  \t-10000 \t10000  \t0");
		stream.println("EducationUmax \t40  \t-10000 \t10000  \t0");
		stream.println("ShoppingUmax \t35  \t-10000 \t10000  \t0");
		stream.println("LeisureUmax \t12  \t-10000 \t10000  \t0");
		
		stream.println("HomeAlpha \t6  \t-10000 \t10000  \t0");
		stream.println("WorkAlpha \t4  \t-10000 \t10000  \t0");
		stream.println("EducationAlpha \t3  \t-10000 \t10000  \t0");
		stream.println("ShoppingAlpha \t2  \t-10000 \t10000  \t0");
		stream.println("LeisureAlpha \t1  \t-10000 \t10000  \t0");
		
		stream.println("UCar \t-6  \t-10000 \t10000  \t0");
		stream.println("Upt \t-6  \t-10000 \t10000  \t0");
		stream.println("Uwalk \t-6  \t-10000 \t10000  \t0");
		stream.println("Ubike \t-6  \t-10000 \t10000  \t0");
	
	
		
		
		
		for (Iterator<PlanImpl> iterator = person.getPlans().iterator(); iterator.hasNext();){
			PlanImpl plan = iterator.next();
			for (int i=0;i<plan.getPlanElements().size();i++){
				if (i%2==0){
					
				}
			}
		}
		
		
		
		PersonImpl p = this.population.getPersons().get(this.population.getPersons().keySet().iterator().next());
		for (int i = 0;i<p.getPlans().size();i++){
			for (int j =0;j<p.getPlans().get(i).getPlanElements().size();j++){
				stream.print("x"+(i+1)+""+(j+1)+"\t");
			}
		}
		stream.println();
		
		/*
		// Filling plans
		for (Iterator<PersonImpl> iterator = this.population.getPersons().values().iterator(); iterator.hasNext();){
			PersonImpl person = iterator.next();
			stream.print(person.getId()+"\t");
			int position = -1;
			for (int i=0;i<person.getPlans().size();i++){
				if (person.getPlans().get(i).equals(person.getSelectedPlan())) {
					position = i+1;
					break;
				}
			}
			stream.print(position+"\t");
			for (Iterator<PlanImpl> iterator2 = person.getPlans().iterator(); iterator2.hasNext();){
				PlanImpl plan = iterator2.next();
				for (int i=0;i<plan.getPlanElements().size()-1;i++){
					if (i%2==0) stream.print(((ActivityImpl)(plan.getPlanElements().get(i))).calculateDuration()+"\t");
					else stream.print(((LegImpl)(plan.getPlanElements().get(i))).getTravelTime()+"\t");
				}
			}
			stream.println();
		}
		stream.close();*/
	}
	
	

	public static void main(final String [] args) {
//		final String facilitiesFilename = "/home/baug/mfeil/data/Zurich10/facilities.xml";
//		final String networkFilename = "/home/baug/mfeil/data/Zurich10/network.xml";
//		final String populationFilename = "/home/baug/mfeil/data/mz/plans.xml";
		final String populationFilename = "./plans/output_plans.xml.gz";
		final String networkFilename = "./plans/network.xml";
		final String facilitiesFilename = "./plans/facilities.xml.gz";

//		final String outputDir = "/home/baug/mfeil/data/Zurich10";
		final String outputFile = "./plans/model.mod";

		ScenarioImpl scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFilename);
		new MatsimFacilitiesReader(scenario.getActivityFacilities()).readFile(facilitiesFilename);
		new MatsimPopulationReader(scenario).readFile(populationFilename);

		ModFileMaker sp = new ModFileMaker(scenario.getPopulation());
		sp.write(outputFile);
		log.info("Analysis of plan finished.");
	}

}

