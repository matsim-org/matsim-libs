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
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.api.basic.v01.population.PlanElement;



/**
 * Creates mod-file for Biogeme estimation.
 *
 * @author mfeil
 */
public class ModFileMaker {

	protected final PopulationImpl population;
	protected final ArrayList<List<PlanElement>> actChains;
	protected static final Logger log = Logger.getLogger(ModFileMaker.class);
	


	public ModFileMaker(final PopulationImpl population, final ArrayList<List<PlanElement>> actChains) {
		this.population = population;
		this.actChains = actChains;
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
			String employed,
			String license,
			String carAvail,
			String seasonTicket,
			String travelDistance,
			String travelCost,
			String travelConstant,
			String beta_travel,
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
		
		// Home
		stream.println("HomeUmax \t4  \t0 \t100  \t0");
		stream.println("HomeAlpha \t8  \t-5 \t20  \t0");
		if (beta.equals("yes")) stream.println("HomeBeta \t1.2  \t0 \t5  \t0");
		if (gamma.equals("yes")) stream.println("HomeGamma \t1  \t0 \t5  \t0");
		
		// Work
		stream.println("WorkUmax \t3  \t0 \t100  \t0");
		stream.println("WorkAlpha \t4  \t-5 \t20  \t0");
		if (beta.equals("yes")) stream.println("WorkBeta \t1.2  \t0 \t5  \t0");
		if (gamma.equals("yes")) stream.println("WorkGamma \t1  \t0 \t5  \t0");
		
		// Education
		stream.println("EducationUmax \t3  \t0 \t100  \t0");
		stream.println("EducationAlpha \t2  \t-5 \t20  \t0");
		if (beta.equals("yes")) stream.println("EducationBeta \t1.2  \t0 \t5  \t0");
		if (gamma.equals("yes")) stream.println("EducationGamma \t1  \t0 \t5  \t0");
		
		// Shop
		stream.println("ShopUmax \t1  \t0 \t100  \t0");
		stream.println("ShopAlpha \t1  \t-5 \t20  \t0");
		if (beta.equals("yes")) stream.println("ShopBeta \t1.2  \t0 \t5  \t0");
		if (gamma.equals("yes")) stream.println("ShopGamma \t1  \t0 \t5  \t0");
		
		// Leisure
		stream.println("LeisureUmax \t2  \t0 \t100  \t0");
		stream.println("LeisureAlpha \t1  \t-5 \t20  \t0");
		if (beta.equals("yes")) stream.println("LeisureBeta \t1.2  \t0 \t5  \t0");
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
				// beta_<mode>_<type> and constant_<mode>
				int car = 0;
				int pt = 0;
				int bike = 0;
				int walk = 0;
				boolean carStarted = false;
				boolean ptStarted = false;
				boolean bikeStarted = false;
				boolean walkStarted = false;
				for (int j=1;j<actslegs.size();j+=2){
					LegImpl leg = (LegImpl)actslegs.get(j);	
					if (leg.getMode().equals(TransportMode.car)) car = 1;
					else if (leg.getMode().equals(TransportMode.pt)) pt = 1;
					else if (leg.getMode().equals(TransportMode.bike) && bikeIn.equals("yes")) bike = 1;
					else if (leg.getMode().equals(TransportMode.walk)) walk = 1;
					else log.warn("Leg mode "+leg.getMode()+" in act chain "+i+" could not be identified!");
				}
				if (!started){
					if (car==1) {
						stream.print("beta_time_car * x"+(i+1)+"_car_time");
						if (travelCost.equals("yes")) stream.print(" + beta_cost_car * x"+(i+1)+"_car_cost");
						if (travelDistance.equals("yes")) stream.print(" + beta_distance_car * x"+(i+1)+"_car_distance");
		//				if (travelConstant.equals("yes")) stream.print(" + constant_car * one");
						onlyBike = false;
						started = true;
						carStarted = true;
					}
					else if (pt==1) {
						stream.print("beta_time_pt * x"+(i+1)+"_pt_time");
						if (travelCost.equals("yes")) stream.print(" + beta_cost_pt * x"+(i+1)+"_pt_cost");
						if (travelDistance.equals("yes")) stream.print(" + beta_distance_pt * x"+(i+1)+"_pt_distance");
						if (travelConstant.equals("yes")) stream.print(" + constant_pt * one");
						onlyBike = false;
						started = true;
						ptStarted = true;
					}
					else if (bike==1) {
						stream.print("beta_time_bike * x"+(i+1)+"_bike_time");
						if (travelDistance.equals("yes")) stream.print(" + beta_distance_bike * x"+(i+1)+"_bike_distance");
						if (travelConstant.equals("yes")) stream.print(" + constant_bike * one");
						started = true;
						bikeStarted = true;
					}
					else if (walk==1) {
						stream.print("beta_time_walk * x"+(i+1)+"_walk_time");
						if (travelDistance.equals("yes")) stream.print(" + beta_distance_walk * x"+(i+1)+"_walk_distance");
						if (travelConstant.equals("yes")) stream.print(" + constant_walk * one");
						onlyBike = false;
						started = true;
						walkStarted = true;
					}
					else log.warn("Leg has no valid mode! ActChains position: "+i);
				}
				if (started) {
					if (car==1 && !carStarted) {
						stream.print(" + beta_time_car * x"+(i+1)+"_car_time");
						if (travelCost.equals("yes")) stream.print(" + beta_cost_car * x"+(i+1)+"_car_cost");
						if (travelDistance.equals("yes")) stream.print(" + beta_distance_car * x"+(i+1)+"_car_distance");
	//					if (travelConstant.equals("yes")) stream.print(" + constant_car * one");
						onlyBike = false;
					}
					if (pt==1 && !ptStarted) {
						stream.print(" + beta_time_pt * x"+(i+1)+"_pt_time");
						if (travelCost.equals("yes")) stream.print(" + beta_cost_pt * x"+(i+1)+"_pt_cost");
						if (travelDistance.equals("yes")) stream.print(" + beta_distance_pt * x"+(i+1)+"_pt_distance");
						if (travelConstant.equals("yes")) stream.print(" + constant_pt * one");
						onlyBike = false;
					}
					if (bike==1 && !bikeStarted) {
						stream.print(" + beta_time_bike * x"+(i+1)+"_bike_time");
						if (travelDistance.equals("yes")) stream.print(" + beta_distance_bike * x"+(i+1)+"_bike_distance");
						if (travelConstant.equals("yes")) stream.print(" + constant_bike * one");							
					}
					if (walk==1 && !walkStarted) {
						stream.print(" + beta_time_walk * x"+(i+1)+"_walk_time");
						if (travelDistance.equals("yes")) stream.print(" + beta_distance_walk * x"+(i+1)+"_walk_distance");
						if (travelConstant.equals("yes")) stream.print(" + constant_walk * one");
						onlyBike = false;
					}
				}
				// beta_time
				if (beta_travel.equals("yes")){
					if (onlyBike && bikeIn.equals("no") && incomeConstant.equals("no")) {
						if (car+pt+bike+walk==1) {
							stream.print("beta_time *");
							if (car==1) stream.print(" x"+(i+1)+"_car_time");
							else if (pt==1) stream.print(" x"+(i+1)+"_pt_time");
							else if (walk==1) stream.print(" x"+(i+1)+"_walk_time");
						}
						else if (car+pt+bike+walk>1) {
							if (car==1) stream.print("beta_time * x"+(i+1)+"_car_time");
							if (car==0 && pt==1) stream.print("beta_time * x"+(i+1)+"_pt_time");
							else if (car==1 && pt==1) stream.print(" + beta_time * x"+(i+1)+"_pt_time");
							if (walk==1) stream.print(" + beta_time * x"+(i+1)+"_walk_time");
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
			int car = 0;
			int pt = 0;
			int bike = 0;
			int walk = 0;
			
			// Alt
			stream.print((i+1)+"\t");
			
			// Activities
			if (beta.equals("no") && gamma.equals("no")) stream.print("HomeUmax * one / ( one + exp( one_point_two * ( HomeAlpha * one - x"+(i+1)+""+1+" ) ) )");			
			else if (beta.equals("yes") && gamma.equals("no")) stream.print("HomeUmax * one / ( one + exp( HomeBeta * ( HomeAlpha * one - x"+(i+1)+""+1+" ) ) )");			
			else if (beta.equals("no") && gamma.equals("yes")) stream.print("HomeUmax * one / ( ( one + HomeGamma * exp( one_point_two * ( HomeAlpha * one - x"+(i+1)+""+1+" ) ) ) ^ ( one / HomeGamma * one ) )");			
			else stream.print("HomeUmax * one / ( ( one + HomeGamma * exp( HomeBeta * ( HomeAlpha * one - x"+(i+1)+""+1+" ) ) ) ^ ( one / HomeGamma * one ) )");					
			
			for (int j=1;j<actslegs.size()-1;j++){
				if (j%2==0){
					ActivityImpl act = (ActivityImpl)actslegs.get(j);
					if (beta.equals("no") && gamma.equals("no")){				
						if (act.getType().toString().equals("h")) stream.print(" + HomeUmax * one / ( one + exp( one_point_two * ( HomeAlpha * one - x"+(i+1)+""+(j+1)+" ) ) )");
						else if (act.getType().toString().equals("h_inner")) stream.print(" + HomeInnerUmax * one / ( one + exp( one_point_two * ( HomeInnerAlpha * one - x"+(i+1)+""+(j+1)+" ) ) )");
						else if (act.getType().toString().equals("w")) stream.print(" + WorkUmax * one / ( one + exp( one_point_two * ( WorkAlpha * one - x"+(i+1)+""+(j+1)+" ) ) )");
						else if (act.getType().toString().equals("e")) stream.print(" + EducationUmax * one / ( one + exp( one_point_two * ( EducationAlpha * one - x"+(i+1)+""+(j+1)+" ) ) )");
						else if (act.getType().toString().equals("shop")) stream.print(" + ShopUmax * one / ( one + exp( one_point_two * ( ShopAlpha * one - x"+(i+1)+""+(j+1)+" ) ) )");
						else if (act.getType().toString().equals("leisure")) stream.print(" + LeisureUmax * one / ( one + exp( one_point_two * ( LeisureAlpha * one - x"+(i+1)+""+(j+1)+" ) ) )");
						else log.warn("Act has no valid type! ActChains position: "+i);
					}
					else if (beta.equals("yes") && gamma.equals("no")){
						if (act.getType().toString().equals("h")) stream.print(" + HomeUmax * one / ( one + exp( HomeBeta * ( HomeAlpha * one - x"+(i+1)+""+1+" ) ) )");
						else if (act.getType().toString().equals("h_inner")) stream.print(" + HomeInnerUmax * one / ( one + exp( HomeInnerBeta * ( HomeInnerAlpha * one - x"+(i+1)+""+1+" ) ) )");
						else if (act.getType().toString().equals("w")) stream.print(" + WorkUmax * one / ( one + exp( WorkBeta * ( WorkAlpha * one - x"+(i+1)+""+1+" ) ) )");
						else if (act.getType().toString().equals("e")) stream.print(" + EducationUmax * one / ( one + exp( EducationBeta * ( EducationAlpha * one - x"+(i+1)+""+1+" ) ) )");
						else if (act.getType().toString().equals("shop")) stream.print(" + ShopUmax * one / ( one + exp( ShopBeta * ( ShopAlpha * one - x"+(i+1)+""+1+" ) ) )");
						else if (act.getType().toString().equals("leisure")) stream.print(" + LeisureUmax * one / ( one + exp( LeisureBeta * ( LeisureAlpha * one - x"+(i+1)+""+1+" ) ) )");
						else log.warn("Act has no valid type! ActChains position: "+i);
					}
					else if (beta.equals("no") && gamma.equals("yes")){
						if (act.getType().toString().equals("h")) stream.print(" + HomeUmax * one / ( ( one + HomeGamma * exp( one_point_two * ( HomeAlpha * one - x"+(i+1)+""+1+" ) ) ) ^ ( one / HomeGamma * one ) )");
						else if (act.getType().toString().equals("h_inner")) stream.print(" + HomeInnerUmax * one / ( ( one + HomeInnerGamma * exp( one_point_two * ( HomeInnerAlpha * one - x"+(i+1)+""+1+" ) ) ) ^ ( one / HomeInnerGamma * one ) )");
						else if (act.getType().toString().equals("w")) stream.print(" + WorkUmax * one / ( ( one + WorkGamma * exp( one_point_two * ( WorkAlpha * one - x"+(i+1)+""+1+" ) ) ) ^ ( one / WorkGamma * one ) )");
						else if (act.getType().toString().equals("e")) stream.print(" + EducationUmax * one / ( ( one + EducationGamma * exp( one_point_two * ( EducationAlpha * one - x"+(i+1)+""+1+" ) ) ) ^ ( one / EducationGamma * one ) )");
						else if (act.getType().toString().equals("shop")) stream.print(" + ShopUmax * one / ( ( one + ShopGamma * exp( one_point_two * ( ShopAlpha * one - x"+(i+1)+""+1+" ) ) ) ^ ( one / ShopGamma * one ) )");
						else if (act.getType().toString().equals("leisure")) stream.print(" + LeisureUmax * one / ( ( one + LeisureGamma * exp( one_point_two * ( LeisureAlpha * one - x"+(i+1)+""+1+" ) ) ) ^ ( one / LeisureGamma * one ) )");
						else log.warn("Act has no valid type! ActChains position: "+i);
					}
					else {
						if (act.getType().toString().equals("h")) stream.print(" + HomeUmax * one / ( ( one + HomeGamma * exp( HomeBeta * ( HomeAlpha * one - x"+(i+1)+""+1+" ) ) ) ^ ( one / HomeGamma * one ) )");
						else if (act.getType().toString().equals("h_inner")) stream.print(" + HomeInnerUmax * one / ( ( one + HomeInnerGamma * exp( HomeInnerBeta * ( HomeInnerAlpha * one - x"+(i+1)+""+1+" ) ) ) ^ ( one / HomeInnerGamma * one ) )");
						else if (act.getType().toString().equals("w")) stream.print(" + WorkUmax * one / ( ( one + WorkGamma * exp( WorkBeta * ( WorkAlpha * one - x"+(i+1)+""+1+" ) ) ) ^ ( one / WorkGamma * one ) )");
						else if (act.getType().toString().equals("e")) stream.print(" + EducationUmax * one / ( ( one + EducationGamma * exp( EducationBeta * ( EducationAlpha * one - x"+(i+1)+""+1+" ) ) ) ^ ( one / EducationGamma * one ) )");
						else if (act.getType().toString().equals("shop")) stream.print(" + ShopUmax * one / ( ( one + ShopGamma * exp( ShopBeta * ( ShopAlpha * one - x"+(i+1)+""+1+" ) ) ) ^ ( one / ShopGamma * one ) )");
						else if (act.getType().toString().equals("leisure")) stream.print(" + LeisureUmax * one / ( ( one + LeisureGamma * exp( LeisureBeta * ( LeisureAlpha * one - x"+(i+1)+""+1+" ) ) ) ^ ( one / LeisureGamma * one ) )");
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
			
			// beta_cost_<mode>_div_LNincome
			if (incomeDividedLN.equals("yes") && travelCost.equals("yes")) {
				for (int j=1;j<actslegs.size()-1;j+=2){
					LegImpl legs = (LegImpl)actslegs.get(j);
					if (legs.getMode().equals(TransportMode.car)) {
						stream.print(" + beta_cost_car_div_LNincome * x"+(i+1)+"_car_cost / LN( Income * one + one_point_two )");
					}
					else if (legs.getMode().equals(TransportMode.pt)) {
						stream.print(" + beta_cost_pt_div_LNincome * x"+(i+1)+"_pt_cost / LN( Income * one + one_point_two )");
					}					
				}
			}
			
			// beta_cost_<mode>_div_income
			if (incomeDivided.equals("yes") && travelCost.equals("yes")) {
				for (int j=1;j<actslegs.size()-1;j+=2){
					LegImpl legs = (LegImpl)actslegs.get(j);
					if (legs.getMode().equals(TransportMode.car)) {
						stream.print(" + beta_cost_car_div_income * x"+(i+1)+"_car_cost / ( Income * one + one )");
					}
					else if (legs.getMode().equals(TransportMode.pt)) {
						stream.print(" + beta_cost_pt_div_income * x"+(i+1)+"_pt_cost / ( Income * one + one )");
					}					
				}
			}
			if (incomeBoxCox.equals("yes")) {
				// mode specific coefficients
				for (int j=1;j<actslegs.size()-1;j+=2){
					LegImpl legs = (LegImpl)actslegs.get(j);
					if (legs.getMode().equals(TransportMode.car)) {
						stream.print(" + beta_time_car * x"+(i+1)+"_car_time * ( Income_IncomeAverage * one ) ^ lambda_time_car_income");
						if (travelCost.equals("yes")) stream.print(" + beta_cost_car * x"+(i+1)+"_car_cost * ( Income_IncomeAverage * one ) ^ lambda_cost_car_income");
						if (travelDistance.equals("yes")) stream.print(" + beta_distance_car * x"+(i+1)+"_car_distance * ( Income_IncomeAverage * one ) ^ lambda_distance_car_income");
					}
					else if (legs.getMode().equals(TransportMode.pt)) {
						stream.print(" + beta_time_pt * x"+(i+1)+"_pt_time * ( Income_IncomeAverage * one ) ^ lambda_time_pt_income");
						if (travelCost.equals("yes")) stream.print(" + beta_cost_pt * x"+(i+1)+"_pt_cost * ( Income_IncomeAverage * one ) ^ lambda_cost_pt_income");
						if (travelDistance.equals("yes")) stream.print(" + beta_distance_pt * x"+(i+1)+"_pt_distance * ( Income_IncomeAverage * one ) ^ lambda_distance_pt_income");
					}
					else if (legs.getMode().equals(TransportMode.bike) && bikeIn.equals("yes")) {
						stream.print(" + beta_time_bike * x"+(i+1)+"_bike_time * ( Income_IncomeAverage * one ) ^ lambda_time_bike_income");
						if (travelDistance.equals("yes")) stream.print(" + beta_distance_bike * x"+(i+1)+"_bike_distance * ( Income_IncomeAverage * one ) ^ lambda_distance_bike_income");
					}
					else if (legs.getMode().equals(TransportMode.walk)) {
						stream.print(" + beta_time_walk * x"+(i+1)+"_walk_time * ( Income_IncomeAverage * one ) ^ lambda_time_walk_income");
						if (travelDistance.equals("yes")) stream.print(" + beta_distance_walk * x"+(i+1)+"_walk_distance * ( Income_IncomeAverage * one ) ^ lambda_distance_walk_income");
					}
				}
				
				// cross-mode coefficients
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

