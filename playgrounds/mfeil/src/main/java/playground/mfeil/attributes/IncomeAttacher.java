/* *********************************************************************** *
 * project: org.matsim.*
 * IncomeAttacher.java
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

package playground.mfeil.attributes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import org.apache.commons.math.optimization.GoalType;
import org.apache.commons.math.optimization.OptimizationException;
import org.apache.commons.math.optimization.RealPointValuePair;
import org.apache.commons.math.optimization.linear.LinearConstraint;
import org.apache.commons.math.optimization.linear.LinearObjectiveFunction;
import org.apache.commons.math.optimization.linear.Relationship;
import org.apache.commons.math.optimization.linear.SimplexSolver;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.world.Layer;
import org.matsim.world.MatsimWorldReader;

import playground.balmermi.census2000.data.Municipalities;
import playground.balmermi.census2000.data.Municipality;


/**
 * Class to run attach agents with an income via municipalities' average income
 * @param args
 */
public class IncomeAttacher {


	public static void main(String[] args) {
		log.info("Process started...");


		final String facilities = "/home/baug/mfeil/data/Zurich10/facilities.xml";
		final String world = "/home/baug/mfeil/data/Zurich10/world.xml";
		final String municipalityIncome = "/home/baug/mfeil/data/Zurich10/gg25_2001_infos.txt";
		final String agentsEducation = "/home/baug/mfeil/data/Zurich10/highestEducCensus2000.txt";
		final String network = "/home/baug/mfeil/data/Zurich10/network.xml";
		final String populationInput = "/home/baug/mfeil/data/Zurich10/plans.xml";
		final String haushalte = "/home/baug/mfeil/data/Zurich10/Haushalte.txt";
		final String zielpersonen = "/home/baug/mfeil/data/Zurich10/Zielpersonen.txt";
		final String agentsIncome = "/home/baug/mfeil/data/Zurich10/agents_income_neu.txt";
		final String munStatsOutput = "/home/baug/mfeil/data/Zurich10/mun_stats_neu.txt";


		/*
		final String populationFilename = "./plans/output_plans.xml";
		final String networkFilename = "./plans/network.xml";
		final String facilitiesFilename = "./plans/facilities.xml";
		final String worldFilename = "./plans/world.xml";
		final String worldAddFilename = "./plans/gg25_2001_infos.txt";
		final String outputFilename = "./plans/output.xls";
		*/

		ScenarioImpl scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario).readFile(network);
		new MatsimFacilitiesReader(scenario).readFile(facilities);
		new MatsimPopulationReader(scenario).readFile(populationInput);
		//new MatsimWorldReader(scenario).readFile(world);

		IncomeAttacher att = new IncomeAttacher(scenario);
		att.run(municipalityIncome, agentsEducation, haushalte, zielpersonen);
		//this.runSimplex(outputFile);
		att.assignIncome(agentsIncome);
		att.analyzeMunIncomes(munStatsOutput);
		log.info("Process finished.");
	}

	private static final Logger log = Logger.getLogger(IncomeAttacher.class);
	private final ScenarioImpl scenario;
	private Municipalities municipalities;
	private Map<Id, Integer> education;					// stores an agent's education level
	private Map<Integer, double[]> incomePerEducation;	// stores the average income and the average income difference from overall's income for the education levels
	private HashMap<Id,Id> agentsMuns;					// stores an agent's municipality he lives in
	private HashMap<Id,Double> incomePerMunicipality;	// stores an agent's income according to his municipality income
	private Map<Id, double[]> muns;

	public IncomeAttacher (ScenarioImpl scenario){
		this.scenario = scenario;
	}

	
	/** 
	 * Method that reads/calculates all necessary input data (agents' municipality and education, and income per education) 
	 * @param municipalityIncome
	 * @param agentsEducation
	 * @param haushalte
	 * @param zielpersonen
	 */
	private void run (String municipalityIncome, 
			String agentsEducation, 
			String haushalte, 
			String zielpersonen){

		log.info("  reading municipality income information... ");
		this.municipalities = new Municipalities(municipalityIncome);
		// Layer municipalityLayer = scenario.getWorld().getLayer(new IdImpl(Municipalities.MUNICIPALITY));
		Layer municipalityLayer = null;
		this.municipalities.parse(municipalityLayer);
		log.info("  done.");

		log.info("  reading education information for the scenario's agents... ");
		AgentsHighestEducationAdder adder = new AgentsHighestEducationAdder();
		adder.runHighestEducation(agentsEducation);
		this.education = adder.getEducation();
		log.info("  done.");

		log.info("  calculating average income per education from microcensus data... ");
		adder = new AgentsHighestEducationAdder();
		adder.runIncomePerEducation(haushalte, zielpersonen);
		this.incomePerEducation = adder.getIncomePerEducation();
		log.info("  done.");


		this.incomePerMunicipality = new HashMap<Id,Double>();
		this.agentsMuns = new HashMap<Id,Id>();
		int doubleListings = 0;
		int noListing =0;

		for (Iterator<? extends Person> iterator = this.scenario.getPopulation().getPersons().values().iterator(); iterator.hasNext();){
			PersonImpl person = (PersonImpl) iterator.next();

			ArrayList<Id> munAtts = new ArrayList<Id>();

			for (Iterator<Municipality> iteratorMun = this.municipalities.getMunicipalities().values().iterator(); iteratorMun.hasNext();){
				Municipality muni = iteratorMun.next();

				double min_x= muni.getZone().getMin().getX();
				double min_y= muni.getZone().getMin().getY();
				double max_x= muni.getZone().getMax().getX();
				double max_y= muni.getZone().getMax().getY();

				double x = ((ActivityImpl)(person.getSelectedPlan().getPlanElements().get(0))).getCoord().getX();
				double y = ((ActivityImpl)(person.getSelectedPlan().getPlanElements().get(0))).getCoord().getY();

				if (x >= min_x && x <= max_x && y >= min_y && y <= max_y){
					munAtts.add(muni.getId());
				}
			}
			if (munAtts.size()>1){
				doubleListings++;
				//log.info("Agent "+person.getId()+" with "+munAtts.size()+" potential home zones. Finding best one...");

				double distance = Double.MAX_VALUE;
				int position = -1;
				for (int j=0;j<munAtts.size();j++){
					double dis = CoordUtils.calcDistance(((ActivityImpl)(person.getSelectedPlan().getPlanElements().get(0))).getCoord(), this.municipalities.getMunicipality(munAtts.get(j)).getZone().getCoord());
					if (distance>dis){
						distance = dis;
						position = j;
					}
				}
				this.agentsMuns.put(person.getId(), munAtts.get(position));
				this.incomePerMunicipality.put(person.getId(), this.municipalities.getMunicipality(munAtts.get(position)).getIncome());
			}
			else if (munAtts.size()== 0){
				noListing++;
				log.warn("Agent "+person.getId()+" without valid home zone. This may never happen!");
				this.agentsMuns.put(person.getId(), new IdImpl (-1));
				this.incomePerMunicipality.put(person.getId(), -1.0);
			}
			else {
				this.agentsMuns.put(person.getId(), munAtts.get(0));
				this.incomePerMunicipality.put(person.getId(), this.municipalities.getMunicipality(munAtts.get(0)).getIncome());
			}

		}
		log.info(agentsMuns.size()+" agents in the scenario. Thereof "+doubleListings+" with double-listings and "+noListing+" with no-listings.");
	}


	protected void writePop(String populationOutput){
		log.info("   Writing plans...");
		new PopulationWriter(this.scenario.getPopulation(), this.scenario.getNetwork()).write(populationOutput);
		log.info("   done.");
	}


	protected void assignIncome (String outputFile){
		log.info("   Assigning incomes...");
		PrintStream stream;
		try {
			stream = new PrintStream (new File(outputFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		stream.println("Agent_Id\tMunicipality_Id\tMunicipalityType\tMunicipality_income\tIncome_Education\tMun_factor\tAdjusted_income\tIncome");
		Random random = new Random();

		this.muns = new HashMap<Id, double[]>(); // {count, income of agents according to education, differenceToMove}
		for (Iterator<? extends Person> iterator = this.scenario.getPopulation().getPersons().values().iterator(); iterator.hasNext();){
			PersonImpl person = (PersonImpl) iterator.next();

			// Retrieve average incomes. Match between tables is as follows:
			// 1=-8; 1=-9; 2=11; 2=12; 3=21; 4=21; 5=22; 5=23; 6=31; 7=32; 7=33; 8=34; 9=-7; 10=1
			if (this.education.get(person.getId())==11 || this.education.get(person.getId())==12) person.getCustomAttributes().put("income", this.incomePerEducation.get(2)[0]);
			else if (this.education.get(person.getId())==21) person.getCustomAttributes().put("income", (this.incomePerEducation.get(3)[0]+this.incomePerEducation.get(4)[0])/2);
			else if (this.education.get(person.getId())==22 || this.education.get(person.getId())==23) person.getCustomAttributes().put("income", this.incomePerEducation.get(5)[0]);
			else if (this.education.get(person.getId())==31) person.getCustomAttributes().put("income", this.incomePerEducation.get(6)[0]);
			else if (this.education.get(person.getId())==32 || this.education.get(person.getId())==33) person.getCustomAttributes().put("income", this.incomePerEducation.get(7)[0]);
			else if (this.education.get(person.getId())==34) person.getCustomAttributes().put("income", this.incomePerEducation.get(8)[0]);
			else if (this.education.get(person.getId())==-7 || this.education.get(person.getId())==1) person.getCustomAttributes().put("income", this.incomePerEducation.get(9)[0]);
			else if (this.education.get(person.getId())==-8 || this.education.get(person.getId())==-9) person.getCustomAttributes().put("income", this.incomePerEducation.get(1)[0]);
			else log.warn("No valid education match possible for agent "+person.getId()+" with education "+this.education.get(person.getId()));

			// set income of unavailable education types/keine Angabe to average mun income
			if (Double.parseDouble(person.getCustomAttributes().get("income").toString())==0) {
				person.getCustomAttributes().clear();
				person.getCustomAttributes().put("income", this.municipalities.getMunicipality(this.agentsMuns.get(person.getId())).getIncome());
			}

			if (muns.containsKey(this.agentsMuns.get(person.getId()))){
				muns.get(this.agentsMuns.get(person.getId()))[0]+=1.0;
				muns.get(this.agentsMuns.get(person.getId()))[1]+=Double.parseDouble(person.getCustomAttributes().get("income").toString());
			}
			else {
				muns.put(this.agentsMuns.get(person.getId()), new double[]{1.0,Double.parseDouble(person.getCustomAttributes().get("income").toString()),0});
			}
		}

		double adjIncome = 0;
		double finIncome = 0;
		double munScaling = 0;
		// Compare average agents income after education with average municipality income and find out difference
		for (Iterator<Id> iterator = muns.keySet().iterator(); iterator.hasNext();){
			Id id = iterator.next();
			muns.get(id)[2]=(this.municipalities.getMunicipality(id).getIncome()-(muns.get(id)[1]/muns.get(id)[0]))*1.0; // Scale up only by 0% to match MZ income data
		}

		// Adjust agent's income according to the municipality information and apply normal distribution
		for (Iterator<? extends Person> iterator = this.scenario.getPopulation().getPersons().values().iterator(); iterator.hasNext();){
			PersonImpl person = (PersonImpl) iterator.next();
			double incomeAdjusted = Double.parseDouble(person.getCustomAttributes().get("income").toString())+muns.get(this.agentsMuns.get(person.getId()))[2];
			adjIncome+=incomeAdjusted;

			// Application of normal distribution
			double income = incomeAdjusted+(random.nextGaussian()*0.5)*incomeAdjusted;
			if (income<0) income=0; // no negative incomes
			finIncome+=income;

			munScaling += muns.get(this.agentsMuns.get(person.getId()))[2];

			double eduIncome = Double.parseDouble(person.getCustomAttributes().get("income").toString());
			person.getCustomAttributes().clear();
			person.getCustomAttributes().put("income", income);
			stream.println(person.getId()+"\t"+this.agentsMuns.get(person.getId())+"\t"+this.municipalities.getMunicipality(this.agentsMuns.get(person.getId())).getRegType()+"\t"+this.incomePerMunicipality.get(person.getId())+"\t"+eduIncome+"\t"+muns.get(this.agentsMuns.get(person.getId()))[2]+"\t"+incomeAdjusted+"\t"+income);
		}
		stream.println("\t\t\t\t\t"+(munScaling/172598)+"\t"+(adjIncome/172598)+"\t"+(finIncome/172598));
		log.info("   done.");
	}

	protected void analyzeMunIncomes(String outputFile){
		log.info("  analyzing municipality incomes after income assignment... ");

		PrintStream stream;
		try {
			stream = new PrintStream (new File(outputFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		stream.println("mun_Id\tmun_income\tagents_income_after_education\tmun_scaling\tagents_simulated_income\tno_of_agents\tweighted_surplusof_simulated_income");


		Map<Id, double[]> agents = new HashMap<Id, double[]>(); // final incomes
		for (Iterator<? extends Person> iterator2 = this.scenario.getPopulation().getPersons().values().iterator(); iterator2.hasNext();){
			PersonImpl person = (PersonImpl) iterator2.next();
			if (agents.containsKey(this.agentsMuns.get(person.getId()))){
				agents.get(this.agentsMuns.get(person.getId()))[0]+=1.0;
				agents.get(this.agentsMuns.get(person.getId()))[1]+=Double.parseDouble(person.getCustomAttributes().get("income").toString());
			}
			else {
				agents.put(this.agentsMuns.get(person.getId()), new double[]{1,Double.parseDouble(person.getCustomAttributes().get("income").toString())});
			}
		}

		for (Iterator<Id> iterator = muns.keySet().iterator(); iterator.hasNext();){
			Id id = iterator.next();
			if (this.muns.get(id)[0]!=agents.get(id)[0]) log.warn("Different agents counts for mun id "+id);
			stream.println(id+"\t"+
					this.municipalities.getMunicipality(id).getIncome()+"\t"+
					(this.municipalities.getMunicipality(id).getIncome()-this.muns.get(id)[2])+"\t"+
					(this.muns.get(id)[2])+"\t"+
					(agents.get(id)[1]/agents.get(id)[0])+"\t"+
					muns.get(id)[0]+"\t"+
					(this.municipalities.getMunicipality(id).getIncome()-agents.get(id)[1]/agents.get(id)[0])*-1*muns.get(id)[0]/172598);
		}
		log.info("  done. ");
	}

	protected void runSimplex (String tableFile){
		log.info("  running simplex... ");

		String outputfile = tableFile;
		PrintStream stream;
		try {
			stream = new PrintStream (new File(outputfile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}

		// length of table
		ArrayList<int[]> index = new ArrayList<int[]>();
		for (Iterator<Municipality> iterator = this.municipalities.getMunicipalities().values().iterator(); iterator.hasNext();){ // = length of table
			Municipality mun = iterator.next();
			index.add(new int[]{Integer.parseInt(mun.getId().toString()),0,0,0,0,0,0,0,0,0,0,0,0,0,0}); // mun_id, row_total, 13 education x
		}

		// count through the number of education types and inhabitants per municipality
		int count = 0;
		int failures = 0;
		for (Iterator<Id> iterator = this.agentsMuns.keySet().iterator(); iterator.hasNext();){
			Id id = iterator.next();
			count=0;
			while ((Integer.parseInt(this.agentsMuns.get(id).toString()))!=index.get(count)[0]){
				count++;
			}
			if (this.education.get(id)==11) {
				index.get(count)[2]+=1; // Obligatorische Schule
				index.get(count)[1]++;
			}
			else if (this.education.get(id)==12) {
				index.get(count)[3]+=1; // Diplommittelschule
				index.get(count)[1]++;
			}
			else if (this.education.get(id)==21) {
				index.get(count)[4]+=1; // Berufslehre
				index.get(count)[1]++;
			}
			else if (this.education.get(id)==22) {
				index.get(count)[5]+=1; // Maturitätsschule
				index.get(count)[1]++;
				}
			else if (this.education.get(id)==23) {
				index.get(count)[6]+=1; // Lehrerseminar
				index.get(count)[1]++;
			}
			else if (this.education.get(id)==31) {
				index.get(count)[7]+=1; // Höhere Fach- und Berufsausbildung
				index.get(count)[1]++;
			}
			else if (this.education.get(id)==32) {
				index.get(count)[8]+=1; // Höhere Fachschule
				index.get(count)[1]++;
			}
			else if (this.education.get(id)==33) {
				index.get(count)[9]+=1; // FH
				index.get(count)[1]++;
			}
			else if (this.education.get(id)==34) {
				index.get(count)[10]+=1; // Universität
				index.get(count)[1]++;
			}
			else if (this.education.get(id)==-7) {
				index.get(count)[11]+=1; // Ohne Angabe
				index.get(count)[1]++;
			}
			else if (this.education.get(id)==-8) {
				index.get(count)[12]+=1; // Keine Ausbildung
				index.get(count)[1]++;
			}
			else if (this.education.get(id)==-9) {
				index.get(count)[13]+=1; // Noch nicht schulpflichtig
				index.get(count)[1]++;
			}
			else if (this.education.get(id)==1) {
				index.get(count)[14]+=1; // ?
				index.get(count)[1]++;
			}
			else {
				log.warn("No education information found for agent "+id+" with education = "+this.education.get(id));
				failures++;
			}
		}
		log.info(failures+" unidentified education cases.");

		// remove "0" rows
		for (int i = index.size()-1;i>=0;i--){
			if (index.get(i)[1]==0){
				index.remove(i);
			}
		}

		// table
		ArrayList<double[]> table = new ArrayList<double[]>();
		for (int i=0;i<index.size();i++){
			double[] consCoef = new double[index.size() + 13];// 13 education types x + 1 variable y per row
			for (int j=0;j<13;j++){
				consCoef[j]=index.get(i)[j+2];
			}
			for (int j=13;j<consCoef.length;j++){
				consCoef[j]=0;
			}
			consCoef[i+13]=index.get(i)[1]; // set rowTotal as coefficient
			table.add(consCoef);
		}


		// now translate into Collection<LinearConstraint> language
		ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
		for (int i=0;i<index.size();i++){
			constraints.add(new LinearConstraint(table.get(i), Relationship.EQ, this.municipalities.getMunicipality(index.get(i)[0]).getIncome()*index.get(i)[1])); // 12 education types x + 1 variable y
		}
		log.info("Finished constraints...");

		// Goal type
		GoalType goalType = GoalType.MINIMIZE;

		// objective function
		double [] coefficients = new double[index.size()+13];
		for (int i=0;i<13;i++){
			coefficients[i] = 0;
		}
		for (int i=13;i<coefficients.length;i++){
			coefficients[i] = this.municipalities.getMunicipality(index.get(i-13)[0]).getIncome();
		}
		LinearObjectiveFunction f = new LinearObjectiveFunction (coefficients, 0);

		// write output coefficients and table
		stream.println("Municipality\t11\t12\t21\t22\t23\t31\t32\t33\t34\t-7\t-8\t-9\t1\tIncome\tCoefficient\tRowTotal");
		for (int i=0;i<index.size();i++){
			stream.print(index.get(i)[0]+"\t");
			for (int j=0;j<13;j++) stream.print(table.get(i)[j]+"\t");
			stream.print(this.municipalities.getMunicipality(index.get(i)[0]).getIncome()+"\t");
			stream.print(coefficients[i+13]+"\t");
			stream.println(index.get(i)[1]);
		}


		// Running the simplex
		RealPointValuePair result = new RealPointValuePair(new double[]{0.0},0.0); // Dummy initialization
		try{
			SimplexSolver solver = new SimplexSolver();
			solver.setMaxIterations(1000);
			result = solver.optimize(f, constraints, goalType, true) ;
		}
		catch (OptimizationException e)	{
			log.warn("Error in optimization: "+e);
		}
		log.info("Result value = "+result.getValue());
		for (int i=0;i<result.getPoint().length;i++) log.info(i+" Koeffizient = "+result.getPoint()[i]);

		log.info("  done.");
	}
}


