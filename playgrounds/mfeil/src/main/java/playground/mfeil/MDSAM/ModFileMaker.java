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
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;



/**
 * Creates mod-file for Biogeme estimation.
 *
 * @author mfeil
 */
public class ModFileMaker {

	protected final Population population;
	protected final ArrayList<List<PlanElement>> actChains;
	protected static final Logger log = Logger.getLogger(ModFileMaker.class);



	public ModFileMaker(final Population population, final ArrayList<List<PlanElement>> actChains) {
		this.population = population;
		this.actChains = actChains;
	}

	public ModFileMaker() {
		this.population = null;
		this.actChains = null;
		log.info("This constructor is allowed only when calling writeForSeasonTicket(outputFile)");
	}

	public void writeForSeasonTicket (String outputFile){
		log.info("Writing mod file...");

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

		//stream.println("beta_no_age \t1  \t-100 \t100  \t0");
		stream.println("beta_no_gender \t1  \t-100 \t100  \t0");
		stream.println("beta_no_license \t1  \t-100 \t100  \t0");
		stream.println("beta_no_income \t1  \t-100 \t100  \t0");
		stream.println("beta_no_carAlways \t1  \t-100 \t100  \t0");
		stream.println("beta_no_carSometimes \t1  \t-100 \t100  \t0");
		//stream.println("beta_ht_age \t1  \t-100 \t100  \t0");
		stream.println("beta_ht_gender \t1  \t-100 \t100  \t0");
		stream.println("beta_ht_license \t1  \t-100 \t100  \t0");
		stream.println("beta_ht_income \t1  \t-100 \t100  \t0");
		stream.println("beta_ht_carAlways \t1  \t-100 \t100  \t0");
		stream.println("beta_ht_carSometimes \t1  \t-100 \t100  \t0");
		//stream.println("beta_ga_age \t1  \t-100 \t100  \t0");
		stream.println("beta_ga_gender \t1  \t-100 \t100  \t0");
		stream.println("beta_ga_license \t1  \t-100 \t100  \t0");
		stream.println("beta_ga_income \t1  \t-100 \t100  \t0");
		stream.println("beta_ga_carAlways \t1  \t-100 \t100  \t0");
		stream.println("beta_ga_carSometimes \t1  \t-100 \t100  \t0");

		stream.println("constant_ht \t1  \t-100 \t100  \t0");
		stream.println("constant_ga \t1  \t-100 \t100  \t0");
		stream.println();

		//Utilities
		stream.println("[Utilities]");
		stream.println("//Id \tName  \tAvail  \tlinear-in-parameter expression (beta1*x1 + beta2*x2 + ... )");
		/*
		stream.print("1\tAlt1\tav1\t"); // Nothing
		stream.println("beta_no_age * Age + beta_no_gender * Gender + beta_no_license * License + beta_no_income * Income + " +
				"beta_no_carAlways * Car_always + beta_no_carSometimes * Car_sometimes");

		stream.print("2\tAlt2\tav2\t"); // Halbtax
		stream.println("constant_ht * one + beta_ht_age * Age + beta_ht_gender * Gender + beta_ht_license * License + beta_ht_income * Income + " +
				"beta_ht_carAlways * Car_always + beta_ht_carSometimes * Car_sometimes");

		stream.print("3\tAlt3\tav3\t"); // GA
		stream.println("constant_ga * one + beta_ga_age * Age + beta_ga_gender * Gender + beta_ga_license * License + beta_ga_income * Income + " +
				"beta_ga_carAlways * Car_always + beta_ga_carSometimes * Car_sometimes");
		*/
		stream.print("1\tAlt1\tav1\t"); // Nothing
		stream.println("beta_no_income * Income");

		stream.print("2\tAlt2\tav2\t"); // Halbtax
		stream.println("constant_ht * one + beta_ht_income * Income");

		stream.print("3\tAlt3\tav3\t"); // GA
		stream.println("constant_ga * one + beta_ga_income * Income");

		stream.println();

		//GeneralizedUtilities
		stream.println("[GeneralizedUtilities]");
		stream.println("//Id \tnonlinear-in-parameter expression");

		stream.print("1\t"); // Nothing
		stream.println("beta_no_carAlways * Car_always * Income");

		stream.print("2\t"); // HT
		stream.println("beta_ht_carAlways * Car_always * Income");

		stream.print("3\t"); // GA
		stream.println("beta_ga_carAlways * Car_always * Income");

		stream.println();

		//Expressions
		stream.println("[Expressions]");
		stream.println("one = 1");
		//stream.println("one_point_two = 1.2");
		stream.println();

		//Model
		stream.println("[Model]");
		stream.println("// Currently, only $MNL (multinomial logit), $NL (nested logit), $CNL\n//(cross-nested logit) and $NGEV (Network GEV model) are valid keywords.");
		stream.println("$MNL");
		stream.println();

		stream.close();
		log.info("done.");
	}


	/*
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

		stream.println("HomeUmax \t  \t0 \t100  \t0");
		stream.println("WorkUmax \t55  \t0 \t100  \t0");
		stream.println("EducationUmax \t40  \t0 \t100  \t0");
		stream.println("ShoppingUmax \t35  \t0 \t100  \t0");
		stream.println("LeisureUmax \t12  \t0 \t100  \t0");

		stream.println("HomeAlpha \t6  \t-5 \t20  \t0");
		stream.println("WorkAlpha \t4  \t-5 \t20  \t0");
		stream.println("EducationAlpha \t3  \t-5 \t20  \t0");
		stream.println("ShoppingAlpha \t2  \t-5 \t20  \t0");
		stream.println("LeisureAlpha \t1  \t-5 \t20  \t0");

		stream.println("Ucar \t-6  \t-50 \t30  \t0");
		stream.println("Upt \t-6  \t-50 \t30  \t0");
		stream.println("Uwalk \t-6  \t-50 \t30  \t0");
		stream.println("Ubike \t-6  \t-50 \t30  \t0");

		stream.println("Sim \t0 \t-100 \t100 \t0");
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
				stream.print(" + Sim * x"+(i+1)+""+plan.getPlanElements().size());
			}
			else {
				stream.print("Sim * x"+(i+1)+""+(plan.getPlanElements().size()+1));
			}
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
	}*/

	/*
	public void writeWithSequence (String outputFile){
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

		stream.println("HomeAlpha \t6  \t-5 \t20  \t0");
		stream.println("WorkAlpha \t4  \t-5 \t20  \t0");
		stream.println("EducationAlpha \t3  \t-5 \t20  \t0");
		stream.println("ShoppingAlpha \t2  \t-5 \t20  \t0");
		stream.println("LeisureAlpha \t1  \t-5 \t20  \t0");

		stream.println("Ucar \t-6  \t-50 \t30  \t0");
		stream.println("Upt \t-6  \t-50 \t30  \t0");
		stream.println("Uwalk \t-6  \t-50 \t30  \t0");
		stream.println("Ubike \t-6  \t-50 \t30  \t0");

		stream.println("Sim \t0 \t-100 \t100 \t0");
		stream.println("Repeat \t0 \t-100 \t100 \t0");
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
				stream.print(" + Sim * x"+(i+1)+""+plan.getPlanElements().size());
			}
			else {
				stream.print("Sim * x"+(i+1)+""+(plan.getPlanElements().size()+1));
			}
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
				if (act.getType().toString().equals("h")) stream.print(" + ( one - x"+(i+1)+""+(j+1)+"_1 * Repeat ) * ( HomeUmax * one / ( one + exp( one_point_two * ( HomeAlpha * one - x"+(i+1)+""+(j+1)+" ) ) ) )");
				else if (act.getType().toString().equals("w")) stream.print(" + ( one - x"+(i+1)+""+(j+1)+"_1 * Repeat ) * ( WorkUmax * one / ( one + exp( one_point_two * ( WorkAlpha * one - x"+(i+1)+""+(j+1)+" ) ) ) )");
				else if (act.getType().toString().equals("e")) stream.print(" + ( one - x"+(i+1)+""+(j+1)+"_1 * Repeat ) * ( EducationUmax * one / ( one + exp( one_point_two * ( EducationAlpha * one - x"+(i+1)+""+(j+1)+" ) ) ) )");
				else if (act.getType().toString().equals("s")) stream.print(" + ( one - x"+(i+1)+""+(j+1)+"_1 * Repeat ) * ( ShoppingUmax * one / ( one + exp( one_point_two * ( ShoppingAlpha * one - x"+(i+1)+""+(j+1)+" ) ) ) )");
				else if (act.getType().toString().equals("l")) stream.print(" + ( one - x"+(i+1)+""+(j+1)+"_1 * Repeat ) * ( LeisureUmax * one / ( one + exp( one_point_two * ( LeisureAlpha * one - x"+(i+1)+""+(j+1)+" ) ) ) )");
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
	}*/

	/*
	public void writeWithRandomSelection (String outputFile,
			String similarity,
			String incomeConstant,
			String incomeDivided,
			String incomeDividedLN,
			String incomeBoxCox,
			String age,
			String gender,
			String employed,
			String license,
			String carAvail,
			String seasonTicket,
			String travelDistance,
			String travelCost,
			String travelConstant,
			String bikeIn){

		log.info("Writing mod file...");

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

		stream.println("HomeUmax \t4  \t0 \t100  \t0");
		stream.println("WorkUmax \t3  \t0 \t100  \t0");
		stream.println("EducationUmax \t3  \t0 \t100  \t0");
		stream.println("ShoppingUmax \t1  \t0 \t100  \t0");
		stream.println("LeisureUmax \t2  \t0 \t100  \t0");

		stream.println("HomeAlpha \t8  \t-5 \t20  \t0");
		stream.println("WorkAlpha \t4  \t-5 \t20  \t0");
		stream.println("EducationAlpha \t2  \t-5 \t20  \t0");
		stream.println("ShoppingAlpha \t0  \t-5 \t20  \t0");
		stream.println("LeisureAlpha \t1  \t-5 \t20  \t0");

		stream.println("beta_time \t0  \t-5 \t5  \t0");
		if (travelCost.equals("yes")) stream.println("beta_cost \t0  \t-5 \t5  \t0");
		if (travelDistance.equals("yes"))stream.println("beta_distance \t0  \t-5 \t5  \t0");
		if (incomeBoxCox.equals("yes")) {
			stream.println("lambda_time_income \t0  \t-5 \t5  \t0");
			stream.println("lambda_cost_income \t0  \t-5 \t5  \t0");
			stream.println("lambda_distance_income \t0  \t-5 \t5  \t0");
		}

		// Car
		stream.println("beta_time_car \t-4  \t-50 \t30  \t0");
		if (travelCost.equals("yes")) stream.println("beta_cost_car \t0  \t-50 \t30  \t0");
		if (travelDistance.equals("yes")) stream.println("beta_distance_car \t0  \t-50 \t30  \t0");
		if (travelConstant.equals("yes")) stream.println("constant_car \t0  \t-50 \t50  \t0");
		if (incomeDivided.equals("yes")) stream.println("beta_cost_car_div_income \t0  \t-50 \t50  \t0");
		if (incomeDivided.equals("yes")) stream.println("beta_cost_car_div_LNincome \t0  \t-50 \t50  \t0");
		if (incomeBoxCox.equals("yes")) {
			stream.println("lambda_time_car_income \t0  \t-50 \t50  \t0");
			stream.println("lambda_cost_car_income \t0  \t-50 \t50  \t0");
			stream.println("lambda_distance_car_income \t0  \t-50 \t50  \t0");
		}

		// PT
		stream.println("beta_time_pt \t-2  \t-50 \t30  \t0");
		if (travelCost.equals("yes")) stream.println("beta_cost_pt \t0  \t-50 \t30  \t0");
		if (travelDistance.equals("yes")) stream.println("beta_distance_pt \t0  \t-50 \t30  \t0");
		if (travelConstant.equals("yes")) stream.println("constant_pt \t0  \t-50 \t50  \t0");
		if (incomeDivided.equals("yes")) stream.println("beta_cost_pt_div_income \t0  \t-50 \t50  \t0");
		if (incomeDivided.equals("yes")) stream.println("beta_cost_pt_div_LNincome \t0  \t-50 \t50  \t0");
		if (incomeBoxCox.equals("yes")) {
			stream.println("lambda_time_pt_income \t0  \t-50 \t50  \t0");
			stream.println("lambda_cost_pt_income \t0  \t-50 \t50  \t0");
			stream.println("lambda_distance_pt_income \t0  \t-50 \t50  \t0");
		}

		// Walk
		stream.println("beta_time_walk \t-1  \t-50 \t30  \t0");
		if (travelDistance.equals("yes")) stream.println("beta_distance_walk \t0  \t-50 \t30  \t0");
		if (travelConstant.equals("yes")) stream.println("constant_walk \t0  \t-50 \t50  \t0");
		if (incomeBoxCox.equals("yes")) {
			stream.println("lambda_time_walk_income \t0  \t-50 \t50  \t0");
			stream.println("lambda_distance_walk_income \t0  \t-50 \t50  \t0");
		}

		// Bike
		if (bikeIn.equals("yes")) {
			stream.println("beta_time_bike \t-3  \t-50 \t30  \t0");
			if (travelDistance.equals("yes")) stream.println("beta_distance_bike \t0  \t-50 \t30  \t0");
			if (travelConstant.equals("yes")) stream.println("constant_bike \t0  \t-50 \t50  \t0");
			if (incomeBoxCox.equals("yes")) {
				stream.println("lambda_time_bike_income \t0  \t-50 \t50  \t0");
				stream.println("lambda_distance_bike_income \t0  \t-50 \t50  \t0");
			}
		}

		if (incomeConstant.equals("yes")) stream.println("constant_income \t0  \t-50 \t50  \t0");

		stream.println();

		//Utilities
		stream.println("[Utilities]");
		stream.println("//Id \tName  \tAvail  \tlinear-in-parameter expression (beta1*x1 + beta2*x2 + ... )");
		for (int i=0;i<this.actChains.size();i++){
			List<PlanElement> actslegs = this.actChains.get(i);
			stream.print((i+1)+"\tAlt"+(i+1)+"\tav"+(i+1)+"\t");
			boolean onlyBike = true;
			boolean started = false;
			if (incomeConstant.equals("yes")) {
				stream.print("constant_income * Income");
				started = true;
			}
			if (actslegs.size()>1){
				for (int j=1;j<actslegs.size();j+=2){
					LegImpl legs = (LegImpl)actslegs.get(j);
					if (!started){
						if (legs.getMode().equals(TransportMode.car)) {
							stream.print("beta_time_car * x"+(i+1)+""+(j+1));
							if (travelCost.equals("yes")) stream.print(" + beta_cost_car * x"+(i+1)+""+(j+1)+"_1");
							if (travelDistance.equals("yes")) stream.print(" + beta_distance_car * x"+(i+1)+""+(j+1)+"_2");
							if (travelConstant.equals("yes")) stream.print(" + constant_car * one");
							onlyBike = false;
							started = true;
						}
						else if (legs.getMode().equals(TransportMode.pt)) {
							stream.print("beta_time_pt * x"+(i+1)+""+(j+1));
							if (travelCost.equals("yes")) stream.print(" + beta_cost_pt * x"+(i+1)+""+(j+1)+"_1");
							if (travelDistance.equals("yes")) stream.print(" + beta_distance_pt * x"+(i+1)+""+(j+1)+"_2");
							if (travelConstant.equals("yes")) stream.print(" + constant_pt * one");
							onlyBike = false;
							started = true;
						}
						else if (legs.getMode().equals(TransportMode.walk)) {
							stream.print("beta_time_walk * x"+(i+1)+""+(j+1));
							if (travelDistance.equals("yes")) stream.print(" + beta_distance_walk * x"+(i+1)+""+(j+1)+"_2");
							if (travelConstant.equals("yes")) stream.print(" + constant_walk * one");
							onlyBike = false;
							started = true;
						}
						else if (legs.getMode().equals(TransportMode.bike) && bikeIn.equals("yes")) {
							stream.print("beta_time_bike * x"+(i+1)+""+(j+1));
							if (travelDistance.equals("yes")) stream.print(" + beta_distance_bike * x"+(i+1)+""+(j+1)+"_2");
							if (travelConstant.equals("yes")) stream.print(" + constant_bike * one");
							started = true;
						}
						else log.warn("Leg has no valid mode! ActChains position: "+i);
					}
					else {
						if (legs.getMode().equals(TransportMode.car)) {
							stream.print(" + beta_time_car * x"+(i+1)+""+(j+1));
							if (travelCost.equals("yes")) stream.print(" + beta_cost_car * x"+(i+1)+""+(j+1)+"_1");
							if (travelDistance.equals("yes")) stream.print(" + beta_distance_car * x"+(i+1)+""+(j+1)+"_2");
							if (travelConstant.equals("yes")) stream.print(" + constant_car * one");
							onlyBike = false;
						}
						else if (legs.getMode().equals(TransportMode.pt)) {
							stream.print(" + beta_time_pt * x"+(i+1)+""+(j+1));
							if (travelCost.equals("yes")) stream.print(" + beta_cost_pt * x"+(i+1)+""+(j+1)+"_1");
							if (travelDistance.equals("yes")) stream.print(" + beta_distance_pt * x"+(i+1)+""+(j+1)+"_2");
							if (travelConstant.equals("yes")) stream.print(" + constant_pt * one");
							onlyBike = false;
						}
						else if (legs.getMode().equals(TransportMode.walk)) {
							stream.print(" + beta_time_walk * x"+(i+1)+""+(j+1));
							if (travelDistance.equals("yes")) stream.print(" + beta_distance_walk * x"+(i+1)+""+(j+1)+"_2");
							if (travelConstant.equals("yes")) stream.print(" + constant_walk * one");
							onlyBike = false;
						}
						else if (legs.getMode().equals(TransportMode.bike) && bikeIn.equals("yes")) {
							stream.print(" + beta_time_bike * x"+(i+1)+""+(j+1));
							if (travelDistance.equals("yes")) stream.print(" + beta_distance_bike * x"+(i+1)+""+(j+1)+"_2");
							if (travelConstant.equals("yes")) stream.print(" + constant_bike * one");
						}
						else log.warn("Leg has no valid mode! ActChains position: "+i);
					}
				}
				if (onlyBike && bikeIn.equals("no") && incomeConstant.equals("no")){
					stream.print("beta_time * ( x"+(i+1)+""+2+" ");
					for (int j=3;j<actslegs.size()-1;j+=2) {
						stream.print(" + x"+(i+1)+""+(j+1)+" ");
					}
					stream.print(" )");
				}
				else {
					stream.print(" + beta_time * ( x"+(i+1)+""+2+" ");
					for (int j=3;j<actslegs.size()-1;j+=2) {
						stream.print(" + x"+(i+1)+""+(j+1)+" ");
					}
					stream.print(" )");
				}
				if (travelCost.equals("yes")){
					stream.print(" + beta_cost * ( x"+(i+1)+""+2+"_1 ");
					for (int j=3;j<actslegs.size()-1;j+=2) {
						stream.print(" + x"+(i+1)+""+(j+1)+"_1 ");
					}
					stream.print(" )");
				}
				if (travelDistance.equals("yes")){
					stream.print(" + beta_distance * ( x"+(i+1)+""+2+"_2 ");
					for (int j=3;j<actslegs.size()-1;j+=2) {
						stream.print(" + x"+(i+1)+""+(j+1)+"_2 ");
					}
					stream.print(" )");
				}
			}
			else if (incomeConstant.equals("no")){
				stream.print("$NONE");
			}
			stream.println();
		}
		stream.println();

		//GeneralizedUtilities
		stream.println("[GeneralizedUtilities]");
		stream.println("//Id \tnonlinear-in-parameter expression");
		for (int i=0;i<this.actChains.size();i++){
			List<PlanElement> actslegs = this.actChains.get(i);
			stream.print((i+1)+"\t");

			stream.print("HomeUmax * one / ( one + exp( one_point_two * ( HomeAlpha * one - x"+(i+1)+""+1+" ) ) )");

			for (int j=2;j<actslegs.size()-1;j+=2){
				ActivityImpl act = (ActivityImpl)actslegs.get(j);
				if (act.getType().toString().equals("h")) stream.print(" + HomeUmax * one / ( one + exp( one_point_two * ( HomeAlpha * one - x"+(i+1)+""+(j+1)+" ) ) )");
				else if (act.getType().toString().equals("h_inner")) stream.print(" + HomeInnerUmax * one / ( one + exp( one_point_two * ( HomeInnerAlpha * one - x"+(i+1)+""+(j+1)+" ) ) )");
				else if (act.getType().toString().equals("w")) stream.print(" + WorkUmax * one / ( one + exp( one_point_two * ( WorkAlpha * one - x"+(i+1)+""+(j+1)+" ) ) )");
				else if (act.getType().toString().equals("e")) stream.print(" + EducationUmax * one / ( one + exp( one_point_two * ( EducationAlpha * one - x"+(i+1)+""+(j+1)+" ) ) )");
				else if (act.getType().toString().equals("shop")) stream.print(" + ShoppingUmax * one / ( one + exp( one_point_two * ( ShoppingAlpha * one - x"+(i+1)+""+(j+1)+" ) ) )");
				else if (act.getType().toString().equals("leisure")) stream.print(" + LeisureUmax * one / ( one + exp( one_point_two * ( LeisureAlpha * one - x"+(i+1)+""+(j+1)+" ) ) )");
				else log.warn("Act has no valid type! ActChains position: "+i);
			}
			if (incomeDividedLN.equals("yes")) {
				for (int j=1;j<actslegs.size()-1;j+=2){
					LegImpl legs = (LegImpl)actslegs.get(j);
					if (legs.getMode().equals(TransportMode.car)) {
						stream.print(" + beta_cost_car_div_LNincome * x"+(i+1)+""+(j+1)+"_1 / LN( Income * one + one_point_two )");
					}
					else if (legs.getMode().equals(TransportMode.pt)) {
						stream.print(" + beta_cost_pt_div_LNincome * x"+(i+1)+""+(j+1)+"_1 / LN( Income * one + one_point_two )");
					}
				}
			}
			if (incomeDivided.equals("yes")) {
				for (int j=1;j<actslegs.size()-1;j+=2){
					LegImpl legs = (LegImpl)actslegs.get(j);
					if (legs.getMode().equals(TransportMode.car)) {
						stream.print(" + beta_cost_car_div_income * x"+(i+1)+""+(j+1)+"_1 / ( Income * one + one )");
					}
					else if (legs.getMode().equals(TransportMode.pt)) {
						stream.print(" + beta_cost_pt_div_income * x"+(i+1)+""+(j+1)+"_1 / ( Income * one + one )");
					}
				}
			}
			if (incomeBoxCox.equals("yes")) {
				// mode specific coefficients
				for (int j=1;j<actslegs.size()-1;j+=2){
					LegImpl legs = (LegImpl)actslegs.get(j);
					if (legs.getMode().equals(TransportMode.car)) {
						stream.print(" + beta_time_car * x"+(i+1)+""+(j+1)+" * ( Income_IncomeAverage * one ) ^ lambda_time_car_income");
						stream.print(" + beta_cost_car * x"+(i+1)+""+(j+1)+"_1 * ( Income_IncomeAverage * one ) ^ lambda_cost_car_income");
						stream.print(" + beta_distance_car * x"+(i+1)+""+(j+1)+"_2 * ( Income_IncomeAverage * one ) ^ lambda_distance_car_income");
					}
					else if (legs.getMode().equals(TransportMode.pt)) {
						stream.print(" + beta_time_pt * x"+(i+1)+""+(j+1)+" * ( Income_IncomeAverage * one ) ^ lambda_time_pt_income");
						stream.print(" + beta_cost_pt * x"+(i+1)+""+(j+1)+"_1 * ( Income_IncomeAverage * one ) ^ lambda_cost_pt_income");
						stream.print(" + beta_distance_pt * x"+(i+1)+""+(j+1)+"_2 * ( Income_IncomeAverage * one ) ^ lambda_distance_pt_income");
					}
					else if (legs.getMode().equals(TransportMode.bike) && bikeIn.equals("yes")) {
						stream.print(" + beta_time_bike * x"+(i+1)+""+(j+1)+" * ( Income_IncomeAverage * one ) ^ lambda_time_bike_income");
						stream.print(" + beta_distance_bike * x"+(i+1)+""+(j+1)+"_2 * ( Income_IncomeAverage * one ) ^ lambda_distance_bike_income");
					}
					else if (legs.getMode().equals(TransportMode.walk)) {
						stream.print(" + beta_time_walk * x"+(i+1)+""+(j+1)+" * ( Income_IncomeAverage * one ) ^ lambda_time_walk_income");
						stream.print(" + beta_distance_walk * x"+(i+1)+""+(j+1)+"_2 * ( Income_IncomeAverage * one ) ^ lambda_distance_walk_income");
					}
				}

				// cross-mode coefficients
				if (actslegs.size()>1){
					stream.print(" + beta_time * ( x"+(i+1)+""+2+" ");
					for (int j=3;j<actslegs.size()-1;j+=2) {
						stream.print(" + x"+(i+1)+""+(j+1)+" ");
					}
					stream.print(" ) * ( Income_IncomeAverage * one ) ^ lambda_time_income");

					stream.print(" + beta_cost * ( x"+(i+1)+""+2+"_1 ");
					for (int j=3;j<actslegs.size()-1;j+=2) {
						stream.print(" + x"+(i+1)+""+(j+1)+"_1 ");
					}
					stream.print(" ) * ( Income_IncomeAverage * one ) ^ lambda_cost_income");

					stream.print(" + beta_distance * ( x"+(i+1)+""+2+"_2 ");
					for (int j=3;j<actslegs.size()-1;j+=2) {
						stream.print(" + x"+(i+1)+""+(j+1)+"_2 ");
					}
					stream.print(" ) * ( Income_IncomeAverage * one ) ^ lambda_distance_income");
				}
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
	}*/


	public void writeWithRandomSelectionAccumulated (String outputFile,
			String beta,
			String gamma,
			String similarity,
			String incomeConstant,
			String incomeDivided,
			String incomeDividedLN,
			String incomeBoxCox,
			String age,
			String gender,
			String income,
			String license,
			String carAvail,
			String seasonTicket,
			String travelDistance,
			String travelCost,
			String travelConstant,
			String beta_travel,
			String bikeIn,
			String munType,
			String innerHome){

		log.info("Writing mod file...");

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

		// Home
		stream.println("HomeUmax \t4  \t0 \t100  \t0");
		stream.println("HomeAlpha \t8  \t-5 \t20  \t0");
		if (beta.equals("yes")) stream.println("HomeBeta \t0.249  \t0 \t100  \t1"); // from 0983_ohne_timings
		if (gamma.equals("yes")) stream.println("HomeGamma \t1  \t0 \t5  \t0");

		// InnerHome
		if (innerHome.equals("yes")){
			stream.println("InnerHomeUmax \t4  \t0 \t100  \t0");
			stream.println("InnerHomeAlpha \t8  \t-5 \t20  \t0");
			if (beta.equals("yes")) stream.println("InnerHomeBeta \t15.2  \t0 \t100  \t1"); // from 0983_ohne_timings
			if (gamma.equals("yes")) stream.println("InnerHomeGamma \t1  \t0 \t5  \t0");
		}

		// Work
		stream.println("WorkUmax \t3  \t0 \t100  \t0");
		stream.println("WorkAlpha \t4  \t-5 \t20  \t0");
		if (beta.equals("yes")) stream.println("WorkBeta \t0.491  \t0 \t100  \t1"); // from 0983_ohne_timings
		if (gamma.equals("yes")) stream.println("WorkGamma \t1  \t0 \t5  \t0");

		// Education
		stream.println("EducationUmax \t3  \t0 \t100  \t0");
		stream.println("EducationAlpha \t2  \t-5 \t20  \t0");
		if (beta.equals("yes")) stream.println("EducationBeta \t2.29  \t0 \t100  \t1"); // from 0983_ohne_timings
		if (gamma.equals("yes")) stream.println("EducationGamma \t1  \t0 \t5  \t0");

		// Shop
		stream.println("ShopUmax \t1  \t0 \t100  \t0");
		stream.println("ShopAlpha \t1  \t-5 \t20  \t0");
		if (beta.equals("yes")) stream.println("ShopBeta \t100  \t0 \t100  \t1"); // from 0983_ohne_timings
		if (gamma.equals("yes")) stream.println("ShopGamma \t1  \t0 \t5  \t0");

		// Leisure
		stream.println("LeisureUmax \t2  \t0 \t100  \t0");
		stream.println("LeisureAlpha \t1  \t-5 \t20  \t0");
		if (beta.equals("yes")) stream.println("LeisureBeta \t100  \t0 \t100  \t1"); // from 0983_ohne_timings
		if (gamma.equals("yes")) stream.println("LeisureGamma \t1  \t0 \t5  \t0");

		// betas and gammas
		if (beta_travel.equals("yes")){
			stream.println("beta_time \t0  \t-5 \t5  \t0");
			if (travelCost.equals("yes")) stream.println("beta_cost \t0  \t-5 \t5  \t0");
			if (travelDistance.equals("yes"))stream.println("beta_distance \t0  \t-5 \t5  \t0");
			if (incomeBoxCox.equals("yes")) {
				stream.println("lambda_time_income \t0  \t-5 \t5  \t0");
				stream.println("lambda_cost_income \t0  \t-5 \t5  \t0");
				stream.println("lambda_distance_income \t0  \t-5 \t5  \t0");
			}
		}

		// Car
		stream.println("beta_time_car \t-4  \t-50 \t30  \t0");
		if (travelCost.equals("yes")) stream.println("beta_cost_car \t0  \t-50 \t30  \t0");
		if (travelDistance.equals("yes")) stream.println("beta_distance_car \t0  \t-50 \t30  \t0");
	//	if (travelConstant.equals("yes")) stream.println("constant_car \t0  \t-50 \t50  \t0");
		if (incomeDivided.equals("yes")) stream.println("beta_cost_car_div_income \t0  \t-50 \t50  \t0");
		if (incomeDividedLN.equals("yes")) stream.println("beta_cost_car_div_LNincome \t0  \t-50 \t50  \t0");
		if (incomeBoxCox.equals("yes")) {
			if (travelCost.equals("yes")) stream.println("lambda_cost_car_income \t0  \t-50 \t50  \t0");
			if (travelDistance.equals("yes")) stream.println("lambda_distance_car_income \t0  \t-50 \t50  \t0");
		}

		// PT
		stream.println("beta_time_pt \t-2  \t-50 \t30  \t0");
		if (travelCost.equals("yes")) stream.println("beta_cost_pt \t0  \t-50 \t30  \t0");
		if (travelDistance.equals("yes")) stream.println("beta_distance_pt \t0  \t-50 \t30  \t0");
		if (travelConstant.equals("yes")) stream.println("constant_pt \t0  \t-50 \t50  \t0");
		if (incomeDivided.equals("yes")) stream.println("beta_cost_pt_div_income \t0  \t-50 \t50  \t0");
		if (incomeDividedLN.equals("yes")) stream.println("beta_cost_pt_div_LNincome \t0  \t-50 \t50  \t0");
		if (incomeBoxCox.equals("yes")) {
			if (travelCost.equals("yes")) stream.println("lambda_cost_pt_income \t0  \t-50 \t50  \t0");
			if (travelDistance.equals("yes")) stream.println("lambda_distance_pt_income \t0  \t-50 \t50  \t0");
		}

		// Walk
		stream.println("beta_time_walk \t-1  \t-50 \t30  \t0");
		if (travelDistance.equals("yes")) stream.println("beta_distance_walk \t0  \t-50 \t30  \t0");
		if (travelConstant.equals("yes")) stream.println("constant_walk \t0  \t-50 \t50  \t0");
		if (incomeBoxCox.equals("yes")) {
			if (travelDistance.equals("yes")) stream.println("lambda_distance_walk_income \t0  \t-50 \t50  \t0");
		}

		// Bike
		if (bikeIn.equals("yes")) {
			stream.println("beta_time_bike \t-3  \t-50 \t30  \t0");
			if (travelDistance.equals("yes")) stream.println("beta_distance_bike \t0  \t-50 \t30  \t0");
			if (travelConstant.equals("yes")) stream.println("constant_bike \t0  \t-50 \t50  \t0");
			if (incomeBoxCox.equals("yes")) {
				if (travelDistance.equals("yes")) stream.println("lambda_distance_bike_income \t0  \t-50 \t50  \t0");
			}
		}

		// Income constant
		if (incomeConstant.equals("yes")) stream.println("constant_income \t0  \t-50 \t50  \t0");

		// Gender, age, carAvail, income, license, munType, seasonTicket
		if (gender.equals("yes")) {
	/*		stream.println("beta_female_travel_car \t0.283  \t-50 \t50  \t0");
		//	stream.println("beta_female_travel_pt \t-0.240  \t-50 \t50  \t1");
			stream.println("beta_female_travel_bike \t0.688  \t-50 \t50  \t0");
		//	stream.println("beta_female_travel_walk \t0  \t-50 \t50  \t0");
		//	stream.println("beta_female_home \t0.259  \t-50 \t50  \t1");
		//	stream.println("beta_female_innerHome \t0  \t-50 \t50  \t0");
		//	stream.println("beta_female_work \t-0.134  \t-50 \t50  \t1");
		//	stream.println("beta_female_education \t0  \t-50 \t50  \t0");
			stream.println("beta_female_shop \t0.698  \t-50 \t50  \t0");
		//	stream.println("beta_female_leisure \t0  \t-50 \t50  \t0");*/
			stream.println("beta_female_travel \t0.12  \t-50 \t50  \t0");
			stream.println("beta_female_act \t0.12  \t-50 \t50  \t0");
		}
		if (age.equals("yes")) {
		/*	stream.println("beta_age_0_15_travel \t0  \t-50 \t50  \t0");
			stream.println("beta_age_16_30_travel \t0  \t-50 \t50  \t0");
			stream.println("beta_age_31_60_travel \t0  \t-50 \t50  \t0");
			stream.println("beta_age_61_travel \t0  \t-50 \t50  \t0");*/
		//	stream.println("beta_age_home \t0  \t-50 \t50  \t0");
		//	stream.println("beta_age_innerHome \t0  \t-50 \t50  \t0");
			stream.println("beta_age_work \t0  \t-50 \t50  \t0");
			stream.println("beta_age_education \t0  \t-50 \t50  \t0");
		//	stream.println("beta_age_shop \t0  \t-50 \t50  \t0");
		//	stream.println("beta_age_leisure \t0  \t-50 \t50  \t0");
		//	stream.println("beta_age_car \t0  \t-50 \t50  \t0");
		//	stream.println("beta_age_pt \t0  \t-50 \t50  \t0");
		//	stream.println("beta_age_bike \t0  \t-50 \t50  \t0");
		//	stream.println("beta_age_walk \t0  \t-50 \t50  \t0");
		}
		if (carAvail.equals("yes")) {
			stream.println("beta_carAlways_car \t0  \t-50 \t50  \t0");
			stream.println("beta_carSometimes_car \t0  \t-50 \t50  \t0");
			stream.println("beta_carNever_car \t0  \t-50 \t50  \t0");
			stream.println("beta_carAlways_pt \t0  \t-50 \t50  \t0");
			stream.println("beta_carSometimes_pt \t0  \t-50 \t50  \t0");
			stream.println("beta_carNever_pt \t0  \t-50 \t50  \t0");
			stream.println("beta_carAlways_bike \t0  \t-50 \t50  \t0");
			stream.println("beta_carSometimes_bike \t0  \t-50 \t50  \t0");
			stream.println("beta_carNever_bike \t0  \t-50 \t50  \t0");
			stream.println("beta_carAlways_walk \t0  \t-50 \t50  \t0");
			stream.println("beta_carSometimes_walk \t0  \t-50 \t50  \t0");
			stream.println("beta_carNever_walk \t0  \t-50 \t50  \t0");
		}
		if (income.equals("yes")) {
			stream.println("beta_income_home \t0  \t-50 \t50  \t0");
			stream.println("beta_income_innerHome \t0  \t-50 \t50  \t0");
			stream.println("beta_income_work \t0  \t-50 \t50  \t0");
			stream.println("beta_income_education \t0  \t-50 \t50  \t0");
			stream.println("beta_income_leisure \t0  \t-50 \t50  \t0");
			stream.println("beta_income_shop \t0  \t-50 \t50  \t0");
		/*	stream.println("beta_income_car \t0  \t-50 \t50  \t0");
			stream.println("beta_income_pt \t0  \t-50 \t50  \t0");
			stream.println("beta_income_bike \t0  \t-50 \t50  \t0");
			stream.println("beta_income_act \t0  \t-50 \t50  \t0");*/
		}
		if (license.equals("yes")) {
			stream.println("beta_license_car \t0  \t-50 \t50  \t0");
		//	stream.println("beta_license_pt \t0  \t-50 \t50  \t0");
		//	stream.println("beta_license_bike \t0  \t-50 \t50  \t0");
		//	stream.println("beta_license_walk \t0  \t-50 \t50  \t0");
		}
		if (munType.equals("yes")) {
			stream.println("beta_munType_leisure_1 \t0  \t-50 \t50  \t0");
			stream.println("beta_munType_leisure_2 \t0  \t-50 \t50  \t0");
			stream.println("beta_munType_leisure_3 \t0  \t-50 \t50  \t0");
			stream.println("beta_munType_leisure_4 \t0  \t-50 \t50  \t0");
			stream.println("beta_munType_leisure_5 \t0  \t-50 \t50  \t0");
			stream.println("beta_munType_shop_1 \t0  \t-50 \t50  \t0");
			stream.println("beta_munType_shop_2 \t0  \t-50 \t50  \t0");
			stream.println("beta_munType_shop_3 \t0  \t-50 \t50  \t0");
			stream.println("beta_munType_shop_4 \t0  \t-50 \t50  \t0");
			stream.println("beta_munType_shop_5 \t0  \t-50 \t50  \t0");
		}
		if (seasonTicket.equals("yes")) {
			stream.println("beta_seasonTicket_car_1 \t0  \t-50 \t50  \t0");
			stream.println("beta_seasonTicket_car_2 \t0  \t-50 \t50  \t0");
			stream.println("beta_seasonTicket_car_3 \t0  \t-50 \t50  \t0");
			stream.println("beta_seasonTicket_pt_1 \t0  \t-50 \t50  \t0");
			stream.println("beta_seasonTicket_pt_2 \t0  \t-50 \t50  \t0");
			stream.println("beta_seasonTicket_pt_3 \t0  \t-50 \t50  \t0");
		}
		if (similarity.equals("yes")){
			stream.println("beta_sim \t0  \t-10000  \t1000  \t0");
	//		stream.println("lambda_sim \t1  \t-50  \t50  \t0");
		}

		stream.println();

		//Utilities
		stream.println("[Utilities]");
		stream.println("//Id \tName  \tAvail  \tlinear-in-parameter expression (beta1*x1 + beta2*x2 + ... )");
		for (int i=0;i<this.actChains.size();i++){
			List<PlanElement> actslegs = this.actChains.get(i);
			stream.print((i+1)+"\tAlt"+(i+1)+"\tav"+(i+1)+"\t");
			boolean started = false;
			if (incomeConstant.equals("yes")) {
				stream.print("constant_income * Income");
				started = true;
			}
			if (actslegs.size()>1){
				// beta_<mode>_<type> and constant_<mode>
				int car = 0;
				int pt = 0;
				int bike = 0;
				int walk = 0;
				for (int j=1;j<actslegs.size();j+=2){
					LegImpl leg = (LegImpl)actslegs.get(j);
					if (leg.getMode().equals(TransportMode.car)) car = 1;
					else if (leg.getMode().equals(TransportMode.pt)) pt = 1;
					else if (leg.getMode().equals(TransportMode.bike) && bikeIn.equals("yes")) bike = 1;
					else if (leg.getMode().equals(TransportMode.walk)) walk = 1;
					else log.warn("Leg mode "+leg.getMode()+" in act chain "+i+" could not be identified!");
				}
				if (car==1) {
					if (gender.equals("no") && age.equals("no") && carAvail.equals("no")) {
						if (!started){
							stream.print("beta_time_car * x"+(i+1)+"_car_time");
							started = true;
						}
						else stream.print(" + beta_time_car * x"+(i+1)+"_car_time");
					}
					if (travelCost.equals("yes") && incomeBoxCox.equals("no")) {
						if (!started){
							stream.print("beta_cost_car * x"+(i+1)+"_car_cost");
							started = true;
						}
						else stream.print(" + beta_cost_car * x"+(i+1)+"_car_cost");
					}
					if (travelDistance.equals("yes")) {
						if (!started){
							stream.print("beta_distance_car * x"+(i+1)+"_car_distance");
							started = true;
						}
						else stream.print(" + beta_distance_car * x"+(i+1)+"_car_distance");
					}
	//				if (travelConstant.equals("yes")) {
	//					stream.print(" + constant_car * one");
	//					started = true;
	//				}
				}
				if (pt==1) {
					if (gender.equals("no") && age.equals("no") && carAvail.equals("no")) {
						if (!started){
							stream.print("beta_time_pt * x"+(i+1)+"_pt_time");
							started = true;
						}
						else stream.print(" + beta_time_pt * x"+(i+1)+"_pt_time");
					}
					if (travelCost.equals("yes") && incomeBoxCox.equals("no")) {
						if (!started){
							stream.print("beta_cost_pt * x"+(i+1)+"_pt_cost");
							started = true;
						}
						else stream.print(" + beta_cost_pt * x"+(i+1)+"_pt_cost");
					}
					if (travelDistance.equals("yes")) {
						if (!started){
							stream.print("beta_distance_pt * x"+(i+1)+"_pt_distance");
							started = true;
						}
						else stream.print(" + beta_distance_pt * x"+(i+1)+"_pt_distance");
					}
					if (travelConstant.equals("yes")) {
						if (!started){
							stream.print("constant_pt * x"+(i+1)+"_pt_legs");
							started = true;
						}
						else stream.print(" + constant_pt * x"+(i+1)+"_pt_legs");
					}
				}
				if (bike==1) {
					if (gender.equals("no") && age.equals("no") && carAvail.equals("no")) {
						if (!started){
							stream.print("beta_time_bike * x"+(i+1)+"_bike_time");
							started = true;
						}
						else stream.print(" + beta_time_bike * x"+(i+1)+"_bike_time");
					}
					if (travelDistance.equals("yes")) {
						if (!started){
							stream.print("beta_distance_bike * x"+(i+1)+"_bike_distance");
							started = true;
						}
						else stream.print(" + beta_distance_bike * x"+(i+1)+"_bike_distance");
					}
					if (travelConstant.equals("yes")) {
						if (!started){
							stream.print("constant_bike * x"+(i+1)+"_bike_legs");
							started = true;
						}
						else stream.print(" + constant_bike * x"+(i+1)+"_bike_legs");
					}
				}
				if (walk==1) {
					if (gender.equals("no") && age.equals("no") && carAvail.equals("no")) {
						if (!started){
							stream.print("beta_time_walk * x"+(i+1)+"_walk_time");
							started = true;
						}
						else stream.print(" + beta_time_walk * x"+(i+1)+"_walk_time");
					}
					if (travelDistance.equals("yes")) {
						if (!started){
							stream.print("beta_distance_walk * x"+(i+1)+"_walk_distance");
							started = true;
						}
						else stream.print(" + beta_distance_walk * x"+(i+1)+"_walk_distance");
					}
					if (travelConstant.equals("yes")) {
						if (!started){
							stream.print("constant_walk * x"+(i+1)+"_walk_legs");
							started = true;
						}
						else stream.print(" + constant_walk * x"+(i+1)+"_walk_legs");
					}
				}

				// beta_time
				if (beta_travel.equals("yes")){
					if (!started) {
						if (car+pt+bike+walk==1) {
							stream.print("beta_time *");
							if (car==1) stream.print(" x"+(i+1)+"_car_time");
							else if (pt==1) stream.print(" x"+(i+1)+"_pt_time");
							else if (walk==1) stream.print(" x"+(i+1)+"_walk_time");
							started = true;
						}
						else if (car+pt+bike+walk>1) {
							if (car==1) stream.print("beta_time * x"+(i+1)+"_car_time");
							if (car==0 && pt==1) stream.print("beta_time * x"+(i+1)+"_pt_time");
							else if (car==1 && pt==1) stream.print(" + beta_time * x"+(i+1)+"_pt_time");
							if (walk==1) stream.print(" + beta_time * x"+(i+1)+"_walk_time");
							started = true;
						}
					}
					else {
						if (car+pt+bike+walk==1) {
							stream.print(" + beta_time *");
							if (car==1) stream.print(" x"+(i+1)+"_car_time");
							else if (pt==1) stream.print(" x"+(i+1)+"_pt_time");
							else if (bike==1) stream.print(" x"+(i+1)+"_bike_time");
							else if (walk==1) stream.print(" x"+(i+1)+"_walk_time");
						}
						else if (car+pt+bike+walk>1) {
							if (car==1) stream.print(" + beta_time * x"+(i+1)+"_car_time");
							if (car==0 && pt==1) stream.print(" + beta_time * x"+(i+1)+"_pt_time");
							else if (car==1 && pt==1) stream.print(" + beta_time * x"+(i+1)+"_pt_time");
							if (car==0 && pt==0 && bike==1) stream.print(" + beta_time * x"+(i+1)+"_bike_time");
							else if ((car==1 || pt==1) && bike==1) stream.print(" + beta_time * x"+(i+1)+"_bike_time");
							if (walk==1) stream.print(" + beta_time * x"+(i+1)+"_walk_time");
						}
					}

					// beta cost
					if (travelCost.equals("yes")){
						if (car+pt==1){
							stream.print(" + beta_cost * ");
							if (car==1) stream.print(" x"+(i+1)+"_car_cost");
							else stream.print(" x"+(i+1)+"_pt_cost");
						}
						else if (car+pt>1){
							stream.print(" + beta_cost * x"+(i+1)+"_car_cost + beta_cost * x"+(i+1)+"_pt_cost");;
						}
					}

					// beta distance
					if (travelDistance.equals("yes")){
						if (car+pt+bike+walk==1) {
							stream.print(" + beta_distance *");
							if (car==1) stream.print(" x"+(i+1)+"_car_distance");
							else if (pt==1) stream.print(" x"+(i+1)+"_pt_distance");
							else if (bike==1) stream.print(" x"+(i+1)+"_bike_distance");
							else if (walk==1) stream.print(" x"+(i+1)+"_walk_distance");
						}
						else if (car+pt+bike+walk>1) {
							if (car==1) stream.print(" + beta_distance * x"+(i+1)+"_car_distance");
							if (car==0 && pt==1) stream.print(" + beta_distance * x"+(i+1)+"_pt_distance");
							else if (car==1 && pt==1) stream.print(" + beta_distance * x"+(i+1)+"_pt_distance");
							if (car==0 && pt==0 && bike==1) stream.print(" + beta_distance * x"+(i+1)+"_bike_distance");
							else if ((car==1 || pt==1) && bike==1) stream.print(" + beta_distance * x"+(i+1)+"_bike_distance");
							if (walk==1) stream.print(" + beta_distance * x"+(i+1)+"_walk_distance");
						}
					}
				}
			}
			if (similarity.equals("yes")){
				if (started) stream.print(" + beta_sim * x"+(i+1)+"_sim");
				else {
					stream.print("beta_sim * x"+(i+1)+"_sim");
					started = true;
				}
			}
			if (!started) stream.print("$NONE");
			stream.println();
		}
		stream.println();

		//GeneralizedUtilities
		stream.println("[GeneralizedUtilities]");
		stream.println("//Id \tnonlinear-in-parameter expression");
		for (int i=0;i<this.actChains.size();i++){
			List<PlanElement> actslegs = this.actChains.get(i);
			int car = 0;
			int pt = 0;
			int bike = 0;
			int walk = 0;

			// Alt
			stream.print((i+1)+"\t");

			// Activities
		//	if (gender.equals("yes")) stream.print("( one ");
			if (gender.equals("yes")) stream.print("( one + beta_female_act * Female ");
		//	if (gender.equals("yes")) stream.print("( one + beta_female_home * Female ");
		//	if (age.equals("yes")) stream.print("+ beta_age_home * Age ");
		//	if (munType.equals("yes") && gender.equals("yes")) stream.print("+ beta_munType_act_1 * MunType_1 + beta_munType_act_2 * MunType_2 + beta_munType_act_3 * MunType_3 + beta_munType_act_4 * MunType_4 + beta_munType_act_5 * MunType_5 ");
		//	if (munType.equals("yes") && gender.equals("no")) stream.print("( one + beta_munType_act * MunType ");
		//	if (income.equals("yes")) stream.print("+ beta_income_home * Income ");
			if (gender.equals("yes")) stream.print(") * ");
			if (beta.equals("no") && gamma.equals("no")) stream.print("HomeUmax * one / ( one + exp( one_point_two * ( HomeAlpha * one - x"+(i+1)+"_"+1+" ) ) )");
			else if (beta.equals("yes") && gamma.equals("no")) stream.print("HomeUmax * one / ( one + exp( HomeBeta * ( HomeAlpha * one - x"+(i+1)+"_"+1+" ) ) )");
			else if (beta.equals("no") && gamma.equals("yes")) stream.print("HomeUmax * one / ( ( one + HomeGamma * exp( one_point_two * ( HomeAlpha * one - x"+(i+1)+"_"+1+" ) ) ) ^ ( one / HomeGamma * one ) )");
			else stream.print("HomeUmax * one / ( ( one + HomeGamma * exp( HomeBeta * ( HomeAlpha * one - x"+(i+1)+"_"+1+" ) ) ) ^ ( one / HomeGamma * one ) )");

			for (int j=1;j<actslegs.size()-1;j++){
				if (j%2==0){
					ActivityImpl act = (ActivityImpl)actslegs.get(j);
					stream.print(" + ");
					if (gender.equals("yes")) stream.print("( one + beta_female_act * Female ");
				/*	if (gender.equals("yes")) {
						if (act.getType().toString().equals("h")) stream.print("( one ");
						if (act.getType().toString().equals("w")) stream.print("( one ");
						else if (act.getType().toString().equals("e")) stream.print("( one ");
						else if (act.getType().toString().equals("shop")) stream.print("( one + beta_female_shop * Female ");
						else if (act.getType().toString().equals("leisure")) stream.print("( one ");
					}*/
					if (age.equals("yes")) {
					//	if (act.getType().toString().equals("h")) stream.print("+ beta_age_innerHome * Age ");
						if (act.getType().toString().equals("w")) stream.print("+ beta_age_work * Age ");
						else if (act.getType().toString().equals("e")) stream.print(" + beta_age_education * Age ");
					//	else if (act.getType().toString().equals("shop")) stream.print("+ beta_age_shop * Age ");
					//	else if (act.getType().toString().equals("leisure")) stream.print("+ beta_age_leisure * Age ");
					}
					if (income.equals("yes")) {
						if (act.getType().toString().equals("h")) stream.print("+ beta_income_innerHome * Income ");
						else if (act.getType().toString().equals("w")) stream.print("+ beta_income_work * Income ");
						else if (act.getType().toString().equals("e")) stream.print("+ beta_income_education * Income ");
						else if (act.getType().toString().equals("shop")) stream.print("+ beta_income_shop * Income ");
						else if (act.getType().toString().equals("leisure")) stream.print("+ beta_income_leisure * Income ");
					}
					if (munType.equals("yes")) {
					//	if (act.getType().toString().equals("h")) stream.print("+ beta_munType_ * MunType ");
					//	else if (act.getType().toString().equals("w")) stream.print("+ beta_munType_act * MunType ");
					//	else if (act.getType().toString().equals("e")) stream.print("+ beta_munType_act * MunType ");
						if (act.getType().toString().equals("shop")) stream.print("+ beta_munType_shop_1 * MunType_1 + beta_munType_shop_2 * MunType_2 + beta_munType_shop_3 * MunType_3 + beta_munType_shop_4 * MunType_4 + beta_munType_shop_5 * MunType_5 ");
						else if (act.getType().toString().equals("leisure")) stream.print("+ beta_munType_leisure_1 * MunType_1 + beta_munType_leisure_2 * MunType_2 + beta_munType_leisure_3 * MunType_3 + beta_munType_leisure_4 * MunType_4 + beta_munType_leisure_5 * MunType_5 ");
					}
					if (gender.equals("yes")) stream.print(") * ");

					if (beta.equals("no") && gamma.equals("no")){
						if (act.getType().toString().equals("h")) {
							if (innerHome.equals("yes")) stream.print("InnerHomeUmax * one / ( one + exp( one_point_two * ( InnerHomeAlpha * one - x"+(i+1)+"_"+(j+1)+" ) ) )");
							else stream.print("HomeUmax * one / ( one + exp( one_point_two * ( HomeAlpha * one - x"+(i+1)+"_"+(j+1)+" ) ) )");
						}
						else if (act.getType().toString().equals("w")) stream.print("WorkUmax * one / ( one + exp( one_point_two * ( WorkAlpha * one - x"+(i+1)+"_"+(j+1)+" ) ) )");
						else if (act.getType().toString().equals("e")) stream.print("EducationUmax * one / ( one + exp( one_point_two * ( EducationAlpha * one - x"+(i+1)+"_"+(j+1)+" ) ) )");
						else if (act.getType().toString().equals("shop")) stream.print("ShopUmax * one / ( one + exp( one_point_two * ( ShopAlpha * one - x"+(i+1)+"_"+(j+1)+" ) ) )");
						else if (act.getType().toString().equals("leisure")) stream.print("LeisureUmax * one / ( one + exp( one_point_two * ( LeisureAlpha * one - x"+(i+1)+"_"+(j+1)+" ) ) )");
						else log.warn("Act has no valid type! ActChains position: "+i);
					}
					else if (beta.equals("yes") && gamma.equals("no")){
						if (act.getType().toString().equals("h")) {
							if (innerHome.equals("yes")) stream.print("InnerHomeUmax * one / ( one + exp( InnerHomeBeta * ( InnerHomeAlpha * one - x"+(i+1)+"_"+(j+1)+" ) ) )");
							else stream.print("HomeUmax * one / ( one + exp( HomeBeta * ( HomeAlpha * one - x"+(i+1)+"_"+(j+1)+" ) ) )");
						}
						else if (act.getType().toString().equals("w")) stream.print("WorkUmax * one / ( one + exp( WorkBeta * ( WorkAlpha * one - x"+(i+1)+"_"+(j+1)+" ) ) )");
						else if (act.getType().toString().equals("e")) stream.print("EducationUmax * one / ( one + exp( EducationBeta * ( EducationAlpha * one - x"+(i+1)+"_"+(j+1)+" ) ) )");
						else if (act.getType().toString().equals("shop")) stream.print("ShopUmax * one / ( one + exp( ShopBeta * ( ShopAlpha * one - x"+(i+1)+"_"+(j+1)+" ) ) )");
						else if (act.getType().toString().equals("leisure")) stream.print("LeisureUmax * one / ( one + exp( LeisureBeta * ( LeisureAlpha * one - x"+(i+1)+"_"+(j+1)+" ) ) )");
						else log.warn("Act has no valid type! ActChains position: "+i);
					}
					else if (beta.equals("no") && gamma.equals("yes")){
						if (act.getType().toString().equals("h")) {
							if (innerHome.equals("yes")) stream.print("InnerHomeUmax * one / ( ( one + InnerHomeGamma * exp( one_point_two * ( InnerHomeAlpha * one - x"+(i+1)+"_"+(j+1)+" ) ) ) ^ ( one / InnerHomeGamma * one ) )");
							else stream.print("HomeUmax * one / ( ( one + HomeGamma * exp( one_point_two * ( HomeAlpha * one - x"+(i+1)+"_"+(j+1)+" ) ) ) ^ ( one / HomeGamma * one ) )");
						}
						else if (act.getType().toString().equals("w")) stream.print("WorkUmax * one / ( ( one + WorkGamma * exp( one_point_two * ( WorkAlpha * one - x"+(i+1)+"_"+(j+1)+" ) ) ) ^ ( one / WorkGamma * one ) )");
						else if (act.getType().toString().equals("e")) stream.print("EducationUmax * one / ( ( one + EducationGamma * exp( one_point_two * ( EducationAlpha * one - x"+(i+1)+"_"+(j+1)+" ) ) ) ^ ( one / EducationGamma * one ) )");
						else if (act.getType().toString().equals("shop")) stream.print("ShopUmax * one / ( ( one + ShopGamma * exp( one_point_two * ( ShopAlpha * one - x"+(i+1)+"_"+(j+1)+" ) ) ) ^ ( one / ShopGamma * one ) )");
						else if (act.getType().toString().equals("leisure")) stream.print("LeisureUmax * one / ( ( one + LeisureGamma * exp( one_point_two * ( LeisureAlpha * one - x"+(i+1)+"_"+(j+1)+" ) ) ) ^ ( one / LeisureGamma * one ) )");
						else log.warn("Act has no valid type! ActChains position: "+i);
					}
					else {
						if (act.getType().toString().equals("h")) {
							if (innerHome.equals("yes")) stream.print("InnerHomeUmax * one / ( ( one + InnerHomeGamma * exp( InnerHomeBeta * ( InnerHomeAlpha * one - x"+(i+1)+"_"+(j+1)+" ) ) ) ^ ( one / InnerHomeGamma * one ) )");
							else stream.print("HomeUmax * one / ( ( one + HomeGamma * exp( HomeBeta * ( HomeAlpha * one - x"+(i+1)+"_"+(j+1)+" ) ) ) ^ ( one / HomeGamma * one ) )");
						}
						else if (act.getType().toString().equals("w")) stream.print("WorkUmax * one / ( ( one + WorkGamma * exp( WorkBeta * ( WorkAlpha * one - x"+(i+1)+"_"+(j+1)+" ) ) ) ^ ( one / WorkGamma * one ) )");
						else if (act.getType().toString().equals("e")) stream.print("EducationUmax * one / ( ( one + EducationGamma * exp( EducationBeta * ( EducationAlpha * one - x"+(i+1)+"_"+(j+1)+" ) ) ) ^ ( one / EducationGamma * one ) )");
						else if (act.getType().toString().equals("shop")) stream.print("ShopUmax * one / ( ( one + ShopGamma * exp( ShopBeta * ( ShopAlpha * one - x"+(i+1)+"_"+(j+1)+" ) ) ) ^ ( one / ShopGamma * one ) )");
						else if (act.getType().toString().equals("leisure")) stream.print("LeisureUmax * one / ( ( one + LeisureGamma * exp( LeisureBeta * ( LeisureAlpha * one - x"+(i+1)+"_"+(j+1)+" ) ) ) ^ ( one / LeisureGamma * one ) )");
						else log.warn("Act has no valid type! ActChains position: "+i);
					}
				}
				else {
					LegImpl leg = (LegImpl)actslegs.get(j);
					if (leg.getMode().equals(TransportMode.car)) car = 1;
					else if (leg.getMode().equals(TransportMode.pt)) pt = 1;
					else if (leg.getMode().equals(TransportMode.bike) && bikeIn.equals("yes")) bike = 1;
					else if (leg.getMode().equals(TransportMode.walk)) walk = 1;
					else log.warn("Leg mode "+leg.getMode()+" in act chain "+i+" could not be identified!");
				}
			}

			// Legs

			// beta_cost_<mode>_div_income
			if (incomeDivided.equals("yes")) {
				if (car==1) stream.print(" + beta_cost_car_div_income * x"+(i+1)+"_car_cost / ( Income * one + one )");
				if (pt==1) stream.print(" + beta_cost_pt_div_income * x"+(i+1)+"_pt_cost / ( Income * one + one )");
			}
			// beta_cost_<mode>_div_LNincome
			if (incomeDividedLN.equals("yes")) {
				if (car==1) stream.print(" + beta_cost_car_div_LNincome * x"+(i+1)+"_car_cost / LN( Income * one + one_point_two )");
				if (pt==1) stream.print(" + beta_cost_pt_div_LNincome * x"+(i+1)+"_pt_cost / LN( Income * one + one_point_two )");
			}
			// lamda
			if (incomeBoxCox.equals("yes")) {
				// lamda_<cost/distance>_<mode>
				if (car==1) {
					if (travelCost.equals("yes")) stream.print(" + beta_cost_car * x"+(i+1)+"_car_cost * ( Income_IncomeAverage * one ) ^ lambda_cost_car_income");
					if (travelDistance.equals("yes")) stream.print(" + beta_distance_car * x"+(i+1)+"_car_distance * ( Income_IncomeAverage * one ) ^ lambda_distance_car_income");
				}
				if (pt==1) {
					if (travelCost.equals("yes")) stream.print(" + beta_cost_pt * x"+(i+1)+"_pt_cost * ( Income_IncomeAverage * one ) ^ lambda_cost_pt_income");
					if (travelDistance.equals("yes")) stream.print(" + beta_distance_pt * x"+(i+1)+"_pt_distance * ( Income_IncomeAverage * one ) ^ lambda_distance_pt_income");
				}
				if (bike==1){
					if (travelDistance.equals("yes")) stream.print(" + beta_distance_bike * x"+(i+1)+"_bike_distance * ( Income_IncomeAverage * one ) ^ lambda_distance_bike_income");
				}
				if (walk==1) {
					if (travelDistance.equals("yes")) stream.print(" + beta_distance_walk * x"+(i+1)+"_walk_distance * ( Income_IncomeAverage * one ) ^ lambda_distance_walk_income");
				}

				// cross-mode lambda coefficients
				if (actslegs.size()>1 && beta_travel.equals("yes")){
					if (car+pt+bike+walk==1) {
						stream.print(" + beta_time *");
						if (car==1) stream.print(" x"+(i+1)+"_car_time");
						else if (pt==1) stream.print(" x"+(i+1)+"_pt_time");
						else if (bike==1) stream.print(" x"+(i+1)+"_bike_time");
						else if (walk==1) stream.print(" x"+(i+1)+"_walk_time");
						stream.print(" * ( Income_IncomeAverage * one ) ^ lambda_time_income");
					}
					else if (car+pt+bike+walk>1) {
						stream.print(" + beta_time * (");
						if (car==1) stream.print(" x"+(i+1)+"_car_time * one");
						if (car==0 && pt==1) stream.print(" x"+(i+1)+"_pt_time * one");
						else if (car==1 && pt==1) stream.print(" + x"+(i+1)+"_pt_time * one");
						if (car==0 && pt==0 && bike==1) stream.print(" x"+(i+1)+"_bike_time * one");
						else if ((car==1 || pt==1) && bike==1) stream.print(" + x"+(i+1)+"_bike_time * one");
						if (walk==1) stream.print(" + x"+(i+1)+"_walk_time * one");
						stream.print(" ) * ( Income_IncomeAverage * one ) ^ lambda_time_income");
					}
					if (travelCost.equals("yes")){
						if (car+pt==1){
							stream.print(" + beta_cost * ");
							if (car==1) stream.print(" x"+(i+1)+"_car_cost");
							else stream.print(" x"+(i+1)+"_pt_cost");
							stream.print(" * ( Income_IncomeAverage * one ) ^ lambda_cost_income");
						}
						else if (car+pt>1){
							stream.print(" + beta_cost * ( x"+(i+1)+"_car_cost * one + x"+(i+1)+"_pt_cost * one ) * ( Income_IncomeAverage * one ) ^ lambda_cost_income");;
						}
					}
					if (travelDistance.equals("yes")){
						if (car+pt+bike+walk==1) {
							stream.print(" + beta_distance *");
							if (car==1) stream.print(" x"+(i+1)+"_car_distance");
							else if (pt==1) stream.print(" x"+(i+1)+"_pt_distance");
							else if (bike==1) stream.print(" x"+(i+1)+"_bike_distance");
							else if (walk==1) stream.print(" x"+(i+1)+"_walk_distance");
							stream.print(" * ( Income_IncomeAverage * one ) ^ lambda_distance_income");
						}
						else if (car+pt+bike+walk>1) {
							stream.print(" + beta_distance * (");
							if (car==1) stream.print(" x"+(i+1)+"_car_distance * one");
							if (car==0 && pt==1) stream.print(" x"+(i+1)+"_pt_distance * one");
							else if (car==1 && pt==1) stream.print(" + x"+(i+1)+"_pt_distance * one");
							if (car==0 && pt==0 && bike==1) stream.print(" x"+(i+1)+"_bike_distance * one");
							else if ((car==1 || pt==1) && bike==1) stream.print(" + x"+(i+1)+"_bike_distance * one");
							if (walk==1) stream.print(" + x"+(i+1)+"_walk_distance * one");
							stream.print(" ) * ( Income_IncomeAverage * one ) ^ lambda_distance_income");
						}
					}
				}
			}

			// beta_female and beta_age and beta_carAvail and beta_income and beta_license
			if (age.equals("yes") || gender.equals("yes") || carAvail.equals("yes")) {
				if (car==1){
					stream.print(" + beta_time_car * x"+(i+1)+"_car_time");
					if (gender.equals("yes")) {
						stream.print(" * ( one + beta_female_travel * Female ");
				//		stream.print(" * ( one + beta_female_travel_car * Female ");
				//		if (age.equals("yes")) stream.print("+ beta_age_car * Age ");
						if (carAvail.equals("yes")) stream.print("+ beta_carAlways_car * CarAlways + beta_carSometimes_car * CarSometimes + beta_carNever_car * CarNever ");
						if (income.equals("yes")) stream.print("+ beta_income_car * Income ");
						if (license.equals("yes")) stream.print("+ beta_license_car * License ");
						if (munType.equals("yes")) stream.print("+ beta_munType_travel_1 * MunType_1 + beta_munType_travel_2 * MunType_2 + beta_munType_travel_3 * MunType_3 + beta_munType_travel_4 * MunType_4 + beta_munType_travel_5 * MunType_5 ");
						if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_car_1 * SeasonTicket_1 + beta_seasonTicket_car_2 * SeasonTicket_2 + beta_seasonTicket_car_3 * SeasonTicket_3 ");
						stream.print(")");
					}
					else if (age.equals("yes")) {
						stream.print(" * ( one + beta_age_car * Age ");
						if (carAvail.equals("yes")) stream.print("+ beta_carAlways_car * CarAlways + beta_carSometimes_car * CarSometimes + beta_carNever_car * CarNever ");
				//		if (income.equals("yes")) stream.print("+ beta_income_travel * Income ");
						if (license.equals("yes")) stream.print("+ beta_license_car * License ");
						if (munType.equals("yes")) stream.print("+ beta_munType_travel_1 * MunType_1 + beta_munType_travel_2 * MunType_2 + beta_munType_travel_3 * MunType_3 + beta_munType_travel_4 * MunType_4 + beta_munType_travel_5 * MunType_5 ");
					//	if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_car_1 * SeasonTicket_1 + beta_seasonTicket_car_2 * SeasonTicket_2 + beta_seasonTicket_car_3 * SeasonTicket_3 ");
						if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_travel * SeasonTicket ");
						stream.print(")");
					}
					else if (carAvail.equals("yes")) {
						stream.print(" * ( one + beta_carAlways_car * CarAlways + beta_carSometimes_car * CarSometimes + beta_carNever_car * CarNever ");
				//		if (income.equals("yes")) stream.print("+ beta_income_travel * Income ");
						if (license.equals("yes")) stream.print("+ beta_license_car * License ");
						if (munType.equals("yes")) stream.print("+ beta_munType_travel_1 * MunType_1 + beta_munType_travel_2 * MunType_2 + beta_munType_travel_3 * MunType_3 + beta_munType_travel_4 * MunType_4 + beta_munType_travel_5 * MunType_5 ");
				//		if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_car_1 * SeasonTicket_1 + beta_seasonTicket_car_2 * SeasonTicket_2 + beta_seasonTicket_car_3 * SeasonTicket_3 ");
						if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_travel * SeasonTicket ");
						stream.print(")");
					}
				//	else if (income.equals("yes")) {
				//		stream.print(" * ( one + beta_income_travel * Income ");
				//		if (license.equals("yes")) stream.print("+ beta_license_car * License ");
				//		if (munType.equals("yes")) stream.print("+ beta_munType_travel * MunType ");
				//		if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_car_1 * SeasonTicket_1 + beta_seasonTicket_car_2 * SeasonTicket_2 + beta_seasonTicket_car_3 * SeasonTicket_3 ");
				//		if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_travel * SeasonTicket ");
				//		stream.print(")");
				//	}
					else if (license.equals("yes")) {
						stream.print("+ beta_license_car * License ");
						if (munType.equals("yes")) stream.print("+ beta_munType_travel * MunType ");
				//		if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_car_1 * SeasonTicket_1 + beta_seasonTicket_car_2 * SeasonTicket_2 + beta_seasonTicket_car_3 * SeasonTicket_3 ");
						if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_travel * SeasonTicket ");
						stream.print(")");
					}
					else if (munType.equals("yes")) {
						stream.print("+ beta_munType_car_1 * MunType_1 + beta_munType_car_2 * MunType_2 + beta_munType_car_3 * MunType_3 + beta_munType_car_4 * MunType_4 + beta_munType_car_5 * MunType_5 ");
						if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_car_1 * SeasonTicket_1 + beta_seasonTicket_car_2 * SeasonTicket_2 + beta_seasonTicket_car_3 * SeasonTicket_3 ");
						if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_car * SeasonTicket ");
						stream.print(")");
					}
					else if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_car_1 * SeasonTicket_1 + beta_seasonTicket_car_2 * SeasonTicket_2 + beta_seasonTicket_car_3 * SeasonTicket_3 )");
					else if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_travel * SeasonTicket )");
				}
				if (pt==1){
					stream.print(" + beta_time_pt * x"+(i+1)+"_pt_time");
					if (gender.equals("yes")) {
						stream.print(" * ( one + beta_female_travel * Female ");
				//		stream.print(" * ( one ");
				//		if (age.equals("yes")) stream.print("+ beta_age_pt * Age ");
						if (carAvail.equals("yes")) stream.print("+ beta_carAlways_pt * CarAlways + beta_carSometimes_pt * CarSometimes + beta_carNever_pt * CarNever ");
						if (income.equals("yes")) stream.print("+ beta_income_travel * Income ");
				//		if (license.equals("yes")) stream.print("+ beta_license_pt * License ");
						if (munType.equals("yes")) stream.print("+ beta_munType_travel_1 * MunType_1 + beta_munType_travel_2 * MunType_2 + beta_munType_travel_3 * MunType_3 + beta_munType_travel_4 * MunType_4 + beta_munType_travel_5 * MunType_5 ");
						if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_pt_1 * SeasonTicket_1 + beta_seasonTicket_pt_2 * SeasonTicket_2 + beta_seasonTicket_pt_3 * SeasonTicket_3 ");
				//		if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_travel * SeasonTicket ");
						stream.print(")");
					}
					else if (age.equals("yes")) {
						stream.print(" * ( one + beta_age_pt * Age ");
						if (carAvail.equals("yes")) stream.print("+ beta_carAlways_pt * CarAlways + beta_carSometimes_pt * CarSometimes + beta_carNever_pt * CarNever ");
				//		if (income.equals("yes")) stream.print("+ beta_income_travel * Income ");
						if (license.equals("yes")) stream.print("+ beta_license_pt * License ");
						if (munType.equals("yes")) stream.print("+ beta_munType_travel * MunType ");
				//		if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_pt_1 * SeasonTicket_1 + beta_seasonTicket_pt_2 * SeasonTicket_2 + beta_seasonTicket_pt_3 * SeasonTicket_3 ");
						if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_travel * SeasonTicket ");
						stream.print(")");
					}
					else if (carAvail.equals("yes")) {
						stream.print(" * ( one + beta_carAlways_pt * CarAlways + beta_carSometimes_pt * CarSometimes + beta_carNever_pt * CarNever ");
				//		if (income.equals("yes")) stream.print("+ beta_income_travel * Income ");
						if (license.equals("yes")) stream.print("+ beta_license_pt * License ");
						if (munType.equals("yes")) stream.print("+ beta_munType_travel * MunType ");
				//		if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_pt_1 * SeasonTicket_1 + beta_seasonTicket_pt_2 * SeasonTicket_2 + beta_seasonTicket_pt_3 * SeasonTicket_3 ");
						if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_travel * SeasonTicket ");
						stream.print(")");
					}
				//	else if (income.equals("yes")) {
				//		stream.print(" * ( one + beta_income_travel * Income ");
				//		if (license.equals("yes")) stream.print("+ beta_license_pt * License ");
				//		if (munType.equals("yes")) stream.print("+ beta_munType_travel * MunType ");
				//		if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_pt_1 * SeasonTicket_1 + beta_seasonTicket_pt_2 * SeasonTicket_2 + beta_seasonTicket_pt_3 * SeasonTicket_3 ");
				//		if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_travel * SeasonTicket ");
				//		stream.print(")");
				//	}
					else if (license.equals("yes")) {
						stream.print("+ beta_license_pt * License ");
						if (munType.equals("yes")) stream.print("+ beta_munType_travel * MunType ");
				//		if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_pt_1 * SeasonTicket_1 + beta_seasonTicket_pt_2 * SeasonTicket_2 + beta_seasonTicket_pt_3 * SeasonTicket_3 ");
						if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_travel * SeasonTicket ");
						stream.print(")");
					}
					else if (munType.equals("yes")) {
						stream.print("+ beta_munType_pt_1 * MunType_1 + beta_munType_pt_2 * MunType_2 + beta_munType_pt_3 * MunType_3 + beta_munType_pt_4 * MunType_4 + beta_munType_pt_5 * MunType_5 ");
						if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_pt_1 * SeasonTicket_1 + beta_seasonTicket_pt_2 * SeasonTicket_2 + beta_seasonTicket_pt_3 * SeasonTicket_3 ");
						if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_pt * SeasonTicket ");
						stream.print(")");
					}
				//	else if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_pt_1 * SeasonTicket_1 + beta_seasonTicket_pt_2 * SeasonTicket_2 + beta_seasonTicket_pt_3 * SeasonTicket_3 )");
					else if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_travel * SeasonTicket )");
				}
				if (bike==1){
					stream.print(" + beta_time_bike * x"+(i+1)+"_bike_time");
					if (gender.equals("yes")) {
						stream.print(" * ( one + beta_female_travel * Female ");
				//		stream.print(" * ( one + beta_female_travel_bike * Female ");
				//		if (age.equals("yes")) stream.print("+ beta_age_bike * Age ");
						if (carAvail.equals("yes")) stream.print("+ beta_carAlways_bike * CarAlways + beta_carSometimes_bike * CarSometimes + beta_carNever_bike * CarNever ");
						if (income.equals("yes")) stream.print("+ beta_income_travel * Income ");
				//		if (license.equals("yes")) stream.print("+ beta_license_bike * License ");
						if (munType.equals("yes")) stream.print("+ beta_munType_travel_1 * MunType_1 + beta_munType_travel_2 * MunType_2 + beta_munType_travel_3 * MunType_3 + beta_munType_travel_4 * MunType_4 + beta_munType_travel_5 * MunType_5 ");
						if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_bike_1 * SeasonTicket_1 + beta_seasonTicket_bike_2 * SeasonTicket_2 + beta_seasonTicket_bike_3 * SeasonTicket_3 ");
				//		if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_travel * SeasonTicket ");
						stream.print(")");
					}
					else if (age.equals("yes")) {
						stream.print(" * ( one + beta_age_bike * Age ");
						if (carAvail.equals("yes")) stream.print("+ beta_carAlways_bike * CarAlways + beta_carSometimes_bike * CarSometimes + beta_carNever_bike * CarNever ");
				//		if (income.equals("yes")) stream.print("+ beta_income_travel * Income ");
						if (license.equals("yes")) stream.print("+ beta_license_bike * License ");
						if (munType.equals("yes")) stream.print("+ beta_munType_travel * MunType ");
				//		if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_bike_1 * SeasonTicket_1 + beta_seasonTicket_bike_2 * SeasonTicket_2 + beta_seasonTicket_bike_3 * SeasonTicket_3 ");
						if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_travel * SeasonTicket ");
						stream.print(")");
					}
					else if (carAvail.equals("yes")) {
						stream.print(" * ( one + beta_carAlways_bike * CarAlways + beta_carSometimes_bike * CarSometimes + beta_carNever_bike * CarNever ");
				//		if (income.equals("yes")) stream.print("+ beta_income_travel * Income ");
						if (license.equals("yes")) stream.print("+ beta_license_bike * License ");
						if (munType.equals("yes")) stream.print("+ beta_munType_travel * MunType ");
				//		if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_bike_1 * SeasonTicket_1 + beta_seasonTicket_bike_2 * SeasonTicket_2 + beta_seasonTicket_bike_3 * SeasonTicket_3 ");
						if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_travel * SeasonTicket ");
						stream.print(")");
					}
				//	else if (income.equals("yes")) {
				//		stream.print(" * ( one + beta_income_travel * Income ");
				//		if (license.equals("yes")) stream.print("+ beta_license_bike * License ");
				//		if (munType.equals("yes")) stream.print("+ beta_munType_travel * MunType ");
				//		if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_bike_1 * SeasonTicket_1 + beta_seasonTicket_bike_2 * SeasonTicket_2 + beta_seasonTicket_bike_3 * SeasonTicket_3 ");
				//		if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_travel * SeasonTicket ");
				//		stream.print(")");
				//	}
					else if (license.equals("yes")) {
						stream.print("+ beta_license_bike * License ");
						if (munType.equals("yes")) stream.print("+ beta_munType_travel * MunType ");
				//		if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_bike_1 * SeasonTicket_1 + beta_seasonTicket_bike_2 * SeasonTicket_2 + beta_seasonTicket_bike_3 * SeasonTicket_3 ");
						if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_travel * SeasonTicket ");
						stream.print(")");
					}
					else if (munType.equals("yes")) {
						stream.print("+ beta_munType_bike_1 * MunType_1 + beta_munType_bike_2 * MunType_2 + beta_munType_bike_3 * MunType_3 + beta_munType_bike_4 * MunType_4 + beta_munType_bike_5 * MunType_5 ");
						if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_bike_1 * SeasonTicket_1 + beta_seasonTicket_bike_2 * SeasonTicket_2 + beta_seasonTicket_bike_3 * SeasonTicket_3 ");
						if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_bike * SeasonTicket ");
						stream.print(")");
					}
				//	else if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_bike_1 * SeasonTicket_1 + beta_seasonTicket_bike_2 * SeasonTicket_2 + beta_seasonTicket_bike_3 * SeasonTicket_3 )");
					else if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_travel * SeasonTicket )");
				}
				if (walk==1){
					stream.print(" + beta_time_walk * x"+(i+1)+"_walk_time");
					if (gender.equals("yes")) {
						stream.print(" * ( one + beta_female_travel * Female ");
				//		stream.print(" * ( one + beta_female_travel_walk * Female ");
				//		stream.print(" * ( one ");
				//		if (age.equals("yes")) stream.print("+ beta_age_walk * Age ");
				//		if (age.equals("yes")) stream.print("+ beta_age_0_15_travel * Age_0_15 + beta_age_16_30_travel * Age_16_30 + beta_age_31_60_travel * Age_31_60 + beta_age_61_travel * Age_61 ");
						if (carAvail.equals("yes")) stream.print("+ beta_carAlways_walk * CarAlways + beta_carSometimes_walk * CarSometimes + beta_carNever_walk * CarNever ");
				//		if (income.equals("yes")) stream.print("+ beta_income_travel * Income ");
				//		if (license.equals("yes")) stream.print("+ beta_license_walk * License ");
				//		if (munType.equals("yes")) stream.print("+ beta_munType_travel_1 * MunType_1 + beta_munType_travel_2 * MunType_2 + beta_munType_travel_3 * MunType_3 + beta_munType_travel_4 * MunType_4 + beta_munType_travel_5 * MunType_5 ");
			//			if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_walk_1 * SeasonTicket_1 + beta_seasonTicket_walk_2 * SeasonTicket_2 + beta_seasonTicket_walk_3 * SeasonTicket_3 ");
				//		if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_travel * SeasonTicket ");
						stream.print(")");
					}
					else if (age.equals("yes")) {
						stream.print(" * ( one + beta_age_walk * Age ");
						if (carAvail.equals("yes")) stream.print("+ beta_carAlways_walk * CarAlways + beta_carSometimes_walk * CarSometimes + beta_carNever_walk * CarNever ");
				//		if (income.equals("yes")) stream.print("+ beta_income_travel * Income ");
						if (license.equals("yes")) stream.print("+ beta_license_walk * License ");
						if (munType.equals("yes")) stream.print("+ beta_munType_travel * MunType ");
				//		if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_walk_1 * SeasonTicket_1 + beta_seasonTicket_walk_2 * SeasonTicket_2 + beta_seasonTicket_walk_3 * SeasonTicket_3 ");
						if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_travel * SeasonTicket ");
						stream.print(")");
					}
					else if (carAvail.equals("yes")) {
						stream.print(" * ( one + beta_carAlways_walk * CarAlways + beta_carSometimes_walk * CarSometimes + beta_carNever_walk * CarNever ");
				//		if (income.equals("yes")) stream.print("+ beta_income_travel * Income ");
						if (license.equals("yes")) stream.print("+ beta_license_walk * License ");
						if (munType.equals("yes")) stream.print("+ beta_munType_travel * MunType ");
				//		if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_walk_1 * SeasonTicket_1 + beta_seasonTicket_walk_2 * SeasonTicket_2 + beta_seasonTicket_walk_3 * SeasonTicket_3 ");
						if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_travel * SeasonTicket ");
						stream.print(")");
					}
				//	else if (income.equals("yes")) {
				//		stream.print(" * ( one + beta_income_travel * Income ");
				//		if (license.equals("yes")) stream.print("+ beta_license_walk * License ");
				//		if (munType.equals("yes")) stream.print("+ beta_munType_travel * MunType ");
				//		if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_walk_1 * SeasonTicket_1 + beta_seasonTicket_walk_2 * SeasonTicket_2 + beta_seasonTicket_walk_3 * SeasonTicket_3 ");
				//		if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_travel * SeasonTicket ");
				//		stream.print(")");
				//	}
					else if (license.equals("yes")) {
						stream.print("+ beta_license_walk * License ");
						if (munType.equals("yes")) stream.print("+ beta_munType_travel * MunType ");
				//		if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_walk_1 * SeasonTicket_1 + beta_seasonTicket_walk_2 * SeasonTicket_2 + beta_seasonTicket_walk_3 * SeasonTicket_3 ");
						if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_travel * SeasonTicket ");
						stream.print(")");
					}
					else if (munType.equals("yes")) {
						stream.print("+ beta_munType_walk_1 * MunType_1 + beta_munType_walk_2 * MunType_2 + beta_munType_walk_3 * MunType_3 + beta_munType_walk_4 * MunType_4 + beta_munType_walk_5 * MunType_5 ");
						if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_walk_1 * SeasonTicket_1 + beta_seasonTicket_walk_2 * SeasonTicket_2 + beta_seasonTicket_walk_3 * SeasonTicket_3 ");
						if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_walk * SeasonTicket ");
						stream.print(")");
					}
				//	else if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_walk_1 * SeasonTicket_1 + beta_seasonTicket_walk_2 * SeasonTicket_2 + beta_seasonTicket_walk_3 * SeasonTicket_3 )");
					else if (seasonTicket.equals("yes")) stream.print("+ beta_seasonTicket_travel * SeasonTicket )");
				}
			}

			// box cox formulation of similarity attribute
		/*	if (similarity.equals("yes")){
				stream.print(" + beta_sim * ( ( x"+(i+1)+"_sim ^ lambda_sim - one ) / lambda_sim * one )");
			}*/

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
}

