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
import java.util.Iterator;
import java.util.List;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.controler.Controler;
import org.apache.log4j.Logger;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;


/**
 * Creates mod-file for Biogeme estimation.
 *
 * @author mfeil
 */
public class MDSAM {

	private final PopulationImpl population;
	private List<List<Double>> sims;
	private final double GWact, GWmode, GWlocation; 
	private static final Logger log = Logger.getLogger(MDSAM.class);
	private final String outputFile;
	private final boolean printing;


	public MDSAM(final PopulationImpl population) {
		this.population=population;
		this.GWact = 2.0;
		this.GWmode = 1.0;
		this.GWlocation = 1.0;
		this.outputFile = "./plans/plans_similarity.xls";	
	//	this.outputFile = "/home/baug/mfeil/data/mz/simlog.xls";	
		this.printing = false;
	}
	
	public List<List<Double>> runPopulation () {
		log.info("Calculating similarity of plans of population...");
		
		PrintStream stream;
		try {
			stream = new PrintStream (new File(outputFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return new ArrayList<List<Double>>();
		}	
		
		this.sims = new ArrayList<List<Double>>();
		int counter = 0;
		for (Iterator<PersonImpl> iterator = this.population.getPersons().values().iterator(); iterator.hasNext();){
			PersonImpl person = iterator.next();
			counter++;
			if (counter%10==0){
				log.info("Handled "+counter+" persons.");
				Gbl.printMemoryUsage();
			}
			this.sims.add(new ArrayList<Double>());
			for (Iterator<PlanImpl> iterator2 = person.getPlans().iterator(); iterator2.hasNext();){
				PlanImpl plan = iterator2.next();
				if (plan.equals(person.getSelectedPlan())) {
					this.sims.get(this.sims.size()-1).add(0.0);
					continue;
				}
				if (this.printing){
					stream.println("Person "+person.getId());
					stream.println("origPlan");
					stream.print("Acts\t");
					for (int i=0;i<person.getSelectedPlan().getPlanElements().size();i+=2){
						stream.print(((ActivityImpl)(person.getSelectedPlan().getPlanElements().get(i))).getType()+"\t");
					}
					if (person.getSelectedPlan().getPlanElements().size()<plan.getPlanElements().size()){
						for (int z=0;z<(plan.getPlanElements().size()-person.getSelectedPlan().getPlanElements().size())/2;z++) stream.print("\t");
					}
					stream.print("\tModes\t");
					for (int i=1;i<person.getSelectedPlan().getPlanElements().size();i+=2){
						stream.print(((LegImpl)(person.getSelectedPlan().getPlanElements().get(i))).getMode()+"\t");
					}
					if (person.getSelectedPlan().getPlanElements().size()<plan.getPlanElements().size()){
						for (int z=0;z<(plan.getPlanElements().size()-person.getSelectedPlan().getPlanElements().size())/2;z++) stream.print("\t");
					}
					stream.print("\tLocations\t");
					for (int i=0;i<person.getSelectedPlan().getPlanElements().size();i+=2){
						stream.print(((ActivityImpl)(person.getSelectedPlan().getPlanElements().get(i))).getLinkId()+"\t");
					}
					stream.println();
					stream.println("comparePlan");
					stream.print("Acts\t");
					for (int i=0;i<plan.getPlanElements().size();i+=2){
						stream.print(((ActivityImpl)(plan.getPlanElements().get(i))).getType()+"\t");
					}
					if (person.getSelectedPlan().getPlanElements().size()>plan.getPlanElements().size()){
						for (int z=0;z<(person.getSelectedPlan().getPlanElements().size()-plan.getPlanElements().size())/2;z++) stream.print("\t");
					}
					stream.print("\tModes\t");
					for (int i=1;i<plan.getPlanElements().size();i+=2){
						stream.print(((LegImpl)(plan.getPlanElements().get(i))).getMode()+"\t");
					}
					if (person.getSelectedPlan().getPlanElements().size()>plan.getPlanElements().size()){
						for (int z=0;z<(person.getSelectedPlan().getPlanElements().size()-plan.getPlanElements().size())/2;z++) stream.print("\t");
					}
					stream.print("\tLocations\t");
					for (int i=0;i<plan.getPlanElements().size();i+=2){
						stream.print(((ActivityImpl)(plan.getPlanElements().get(i))).getLinkId()+"\t");
					}
					stream.println();
				}
				this.sims.get(this.sims.size()-1).add(this.runPlans(person.getSelectedPlan(), plan, stream));		
			}
		}
		stream.close();
		log.info("done...");
		return this.sims;
	}
	
	public double runPlans(PlanImpl origPlan, PlanImpl comparePlan, PrintStream stream){
		
		// Calculate tables per attribute dimension
		// Length is number of acts minus last home plus 0th position, or number of legs respectively
		double [][][] table = new double [3][origPlan.getPlanElements().size()/2+1][comparePlan.getPlanElements().size()/2+1];
		
		for (int k=0;k<table.length;k++){
			double GW = 0;
			if (k==0) GW = this.GWact;
			else if (k==1) GW = this.GWmode;
			else GW = this.GWlocation;
			// Levenshtein distance
			for (int i=0;i<table[k].length;i++){
				for (int j=0;j<table[k][0].length;j++){
					if (j==0){
						// margin orig plan
						table[k][i][j]= i * GW;
					}
					else {
						//margin compare plan
						if (i==0) table[k][i][j]= j * GW;
						else if (k==0) { // Activity type sequence
							table[k][i][j] = this.minPath(((ActivityImpl)(origPlan.getPlanElements().get((i-1)*2))).getType(), ((ActivityImpl)(comparePlan.getPlanElements().get((j-1)*2))).getType(), table[k],i, j, GW);
						}
						else if (k==1){ // Modes
							table[k][i][j] = this.minPath(((LegImpl)(origPlan.getPlanElements().get((i-1)*2+1))).getMode(), ((LegImpl)(comparePlan.getPlanElements().get((j-1)*2+1))).getMode(), table[k],i, j, GW);
						}
						else { // Locations (via linkIDs)
							table[k][i][j] = this.minPath(((ActivityImpl)(origPlan.getPlanElements().get((i-1)*2))).getLinkId(), ((ActivityImpl)(comparePlan.getPlanElements().get((j-1)*2))).getLinkId(), table[k],i, j, GW);
						}
					}
				}
			}
		}
		
		// Print tables
		if (this.printing){			
			for (int i=0;i<table[0].length;i++){
				for (int k=0;k<table.length;k++){
					if (k!=2) stream.print("\t");
					for (int j=0;j<table[k][0].length;j++){
						stream.print(table[k][i][j]+"\t");
					}
					if (table[0].length>table[0][0].length) {
						for (int z=0;z<table[0].length-table[0][0].length;z++) stream.print("\t");
					}
					stream.print("\t");
				}
				stream.println();
			}
			stream.println();
		}
		
		// Find one optimal trajectory close to the diagonal, for each attribute dimension
		ArrayList<int[]> oset = new ArrayList<int[]>();	// contains the operation and position
		ArrayList<ArrayList<Integer>> dimensions = new ArrayList<ArrayList<Integer>>(); // contains the attribute dimensions of operation and position
		for (int k=0;k<table.length;k++){
			//System.out.println("k = "+k);
			int i=0;
			int j=0;
			boolean goRight = true;
			while (i!=table[k].length-1 || j!=table[k][0].length-1){
				if (i<table[k].length-1 &&
					j<table[k][0].length-1 &&
					table[k][i+1][j+1]==table[k][i][j]){
					//System.out.println("Identity.");
					i++;
					j++;
				}
				// check insertion {1,x}
				else if (j<table[k][0].length-1 &&
						osetContains(oset,dimensions,k,1,j+1)){
					//System.out.println("Insertion.");
					j++;
				}
				// check deletion {2,x}
				else if (i<table[k].length-1 &&
						osetContains(oset,dimensions,k,2,i+1)){
					//System.out.println("Deletion.");
					i++;
				}
				// go new path (insertion)
				else if (goRight && j!=table[k][0].length-1) {
					oset.add(new int[]{1,j+1});
					ArrayList<Integer> l = new ArrayList<Integer>();
					l.add(k);
					dimensions.add(l);
					j++;
					goRight = false;
					//System.out.println("New insertion.");
				}
				// go new path (deletion)
				else {
					oset.add(new int[]{2,i+1});
					ArrayList<Integer> l = new ArrayList<Integer>();
					l.add(k);
					dimensions.add(l);
					i++;
					goRight = true;
					//System.out.println("New deletion.");
				}
			}
		}
		
		// Print arrays
		if (this.printing){ 
			for (int m=0;m<oset.size();m++){
				stream.print("("+oset.get(m)[0]+","+oset.get(m)[1]+")");
				for (int n=0;n<dimensions.get(m).size();n++) stream.print(", "+dimensions.get(m).get(n));
				stream.println();
			}
		}
		
		
		double sum=0;
		for (int m=0;m<oset.size();m++){
			double GW=0;
			if (dimensions.get(m).contains(0)) GW = this.GWact; 
			if (dimensions.get(m).contains(1)) GW = java.lang.Math.max(GW, this.GWmode);
			if (dimensions.get(m).contains(2)) GW = java.lang.Math.max(GW, this.GWlocation);
			sum += GW;
		}
		if (this.printing){
			stream.println("Sum is "+sum);
			stream.println();
		}
		return sum;
	}
	
	private boolean osetContains (ArrayList<int[]> oset, ArrayList<ArrayList<Integer>> dimensions ,int k, int operation, int position){
		for (int m=0;m<oset.size();m++){
			if (oset.get(m)[0]==operation && oset.get(m)[1]==position){
				dimensions.get(m).add(k);
				return true;
			}
		}
		return false;
	}
	
	private double minPath(Object orig, Object compare, double[][]table, int i, int j, double GW){
		double del = table[i-1][j]+GW;
		double ins = table[i][j-1]+GW;
		double sub = Double.MAX_VALUE;
		
		// identity (position-sensitive)
		if (orig.equals(compare)){
			sub =  table[i-1][j-1] + GW/java.lang.Math.max(table.length-1, table[0].length-1)*java.lang.Math.abs(i-j);
		}
		// substitution
		else {
			sub = table[i-1][j-1] + 2 * GW;	
		}
		// return minimum of ins, del, sub
		del = java.lang.Math.min(del, ins);		
		return java.lang.Math.min(del, sub);
	}
	
	public static void main(final String [] args) {
		/*		final String facilitiesFilename = "/home/baug/mfeil/data/Zurich10/facilities.xml";
				final String networkFilename = "/home/baug/mfeil/data/Zurich10/network.xml";
				final String populationFilename = "/home/baug/mfeil/data/mz/output_plans.xml";
		*/		final String populationFilename = "./plans/output_plans.xml";
				final String networkFilename = "./plans/network.xml";
				final String facilitiesFilename = "./plans/facilities.xml";

		//		final String outputFile = "/home/baug/mfeil/data/mz/output_plans.dat";
				final String outputFile = "./plans/output_plans.dat";

				ScenarioImpl scenario = new ScenarioImpl();
				new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFilename);
				new MatsimFacilitiesReader(scenario.getActivityFacilities()).readFile(facilitiesFilename);
				new MatsimPopulationReader(scenario).readFile(populationFilename);
				
				List<List<Double>> sims = new MDSAM(scenario.getPopulation()).runPopulation();

				PlansConstructor pc = new PlansConstructor(scenario.getPopulation(), sims);
				pc.writePlansForBiogeme(outputFile);
				log.info("Process finished.");
			}
}

