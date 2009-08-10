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
import org.matsim.api.basic.v01.TransportMode;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;


/**
 * Creates mod-file for Biogeme estimation.
 *
 * @author mfeil
 */
public class ModFileMaker {

	protected final PopulationImpl population;
	protected final List<List<Double>> sims;
	protected static final Logger log = Logger.getLogger(ModFileMaker.class);
	


	public ModFileMaker(final PopulationImpl population, final List<List<Double>> sims) {
		this.population = population;
		this.sims = sims;
	}
	
	public void write (String outputFile){
		log.info("Writing mod file...");
		
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
		stream.println("Choice");
		stream.println();
		
		//Beta
		stream.println("[Beta]");
		stream.println("//Name \tValue  \tLowerBound \tUpperBound  \tstatus (0=variable, 1=fixed");
		
		stream.println("HomeUmax \t60  \t0 \t100  \t0");
		stream.println("WorkUmax \t55  \t0 \t100  \t0");
		stream.println("EducationUmax \t40  \t0 \t100  \t0");
		stream.println("ShoppingUmax \t35  \t0 \t100  \t0");
		stream.println("LeisureUmax \t12  \t0 \t100  \t0");
		
		stream.println("HomeAlpha \t6  \t0 \t20  \t0");
		stream.println("WorkAlpha \t4  \t0 \t20  \t0");
		stream.println("EducationAlpha \t3  \t0 \t20  \t0");
		stream.println("ShoppingAlpha \t2  \t0 \t20  \t0");
		stream.println("LeisureAlpha \t1  \t0 \t20  \t0");
		
		stream.println("Ucar \t-6  \t-50 \t30  \t0");
		stream.println("Upt \t-6  \t-50 \t30  \t0");
		stream.println("Uwalk \t-6  \t-50 \t30  \t0");
		stream.println("Ubike \t-6  \t-50 \t30  \t0");	
		
		stream.println("Sim \t0 \t-100 \100 \t0");
		stream.println();
	
		//Utilities
		stream.println("[Utilities]");
		stream.println("//Id \tName  \tAvail  \tlinear-in-parameter expression (beta1*x1 + beta2*x2 + ... )");	
		for (int i=0;i<person.getPlans().size();i++){
			PlanImpl plan = person.getPlans().get(i);
			stream.print((i+1)+"\tAlt"+(i+1)+"\tav"+(i+1)+"\t");
			
			if (plan.getPlanElements().size()>1){
				LegImpl leg = (LegImpl)plan.getPlanElements().get(1);
				if (leg.getMode().equals(TransportMode.car)) stream.print("Ucar * x"+(i+1)+""+2);
				else if (leg.getMode().equals(TransportMode.bike)) stream.print("Ubike * x"+(i+1)+""+2);
				else if (leg.getMode().equals(TransportMode.pt)) stream.print("Upt * x"+(i+1)+""+2);
				else if (leg.getMode().equals(TransportMode.walk)) stream.print("Uwalk * x"+(i+1)+""+2);
				else log.warn("Leg has no valid mode! Person: "+person);
				
				for (int j=3;j<plan.getPlanElements().size();j+=2){
					LegImpl legs = (LegImpl)plan.getPlanElements().get(j);
					if (legs.getMode().equals(TransportMode.car)) stream.print(" + Ucar * x"+(i+1)+""+(j+1));
					else if (legs.getMode().equals(TransportMode.bike)) stream.print(" + Ubike * x"+(i+1)+""+(j+1));
					else if (legs.getMode().equals(TransportMode.pt)) stream.print(" + Upt * x"+(i+1)+""+(j+1));
					else if (legs.getMode().equals(TransportMode.walk)) stream.print(" + Uwalk * x"+(i+1)+""+(j+1));
					else log.warn("Leg has no valid mode! Person: "+person);
				}
			}
			else {
				stream.print("$NONE");
			}
			
			stream.print(" + Sim * x"+(i+1)+""+plan.getPlanElements().size());
			stream.println();
		}		
		stream.println();
		
		//GeneralizedUtilities
		stream.println("[GeneralizedUtilities]");
		stream.println("//Id \tnonlinear-in-parameter expression");	
		for (int i=0;i<person.getPlans().size();i++){
			PlanImpl plan = person.getPlans().get(i);
			stream.print((i+1)+"\t");
			
			stream.print("HomeUmax * one / ( one + exp( one_point_two * ( HomeAlpha * one - x"+(i+1)+""+1+" ) ) )");
						
			for (int j=2;j<plan.getPlanElements().size()-1;j+=2){
				ActivityImpl act = (ActivityImpl)plan.getPlanElements().get(j);
				if (act.getType().toString().equals("h")) stream.print(" + HomeUmax * one / ( one + exp( one_point_two * ( HomeAlpha * one - x"+(i+1)+""+(j+1)+" ) ) )");
				else if (act.getType().toString().equals("w")) stream.print(" + WorkUmax * one / ( one + exp( one_point_two * ( WorkAlpha * one - x"+(i+1)+""+(j+1)+" ) ) )");
				else if (act.getType().toString().equals("e")) stream.print(" + EducationUmax * one / ( one + exp( one_point_two * ( EducationAlpha * one - x"+(i+1)+""+(j+1)+" ) ) )");
				else if (act.getType().toString().equals("s")) stream.print(" + ShoppingUmax * one / ( one + exp( one_point_two * ( ShoppingAlpha * one - x"+(i+1)+""+(j+1)+" ) ) )");
				else if (act.getType().toString().equals("l")) stream.print(" + LeisureUmax * one / ( one + exp( one_point_two * ( LeisureAlpha * one - x"+(i+1)+""+(j+1)+" ) ) )");
				else log.warn("Act has no valid type! Person: "+person);
			}
			stream.println();
		}
		stream.println();
		
		//Expressions
		stream.println("[Expressions]");
		stream.println("one = 1");
		stream.println("one_point_two = 1.2");
		stream.println();
		
		//Model
		stream.println("[Model]");
		stream.println("// Currently, only $MNL (multinomial logit), $NL (nested logit), $CNL\n//(cross-nested logit) and $NGEV (Network GEV model) are valid keywords.");
		stream.println("$MNL");
		stream.println();
		
		stream.close();
		log.info("done.");
	}
	
	

	public static void main(final String [] args) {
/*		final String facilitiesFilename = "/home/baug/mfeil/data/Zurich10/facilities.xml";
		final String networkFilename = "/home/baug/mfeil/data/Zurich10/network.xml";
		final String populationFilename = "/home/baug/mfeil/data/mz/output_plans.xml";
	*/	final String populationFilename = "./plans/output_plans.xml.gz";
		final String networkFilename = "./plans/network.xml";
		final String facilitiesFilename = "./plans/facilities.xml.gz";

//		final String outputFile = "/home/baug/mfeil/data/mz/model.mod";
		final String outputFile = "./plans/model.mod";

		ScenarioImpl scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFilename);
		new MatsimFacilitiesReader(scenario.getActivityFacilities()).readFile(facilitiesFilename);
		new MatsimPopulationReader(scenario).readFile(populationFilename);
		
		List<List<Double>> sims = new SimilarityInitializer(scenario.getPopulation()).getSimilarityOfPlans();

		ModFileMaker sp = new ModFileMaker(scenario.getPopulation(), sims);
		sp.write(outputFile);
		log.info("Model finished.");
	}

}

