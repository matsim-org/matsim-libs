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

package playground.mfeil.MDSAM;

import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.ArrayList;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.File;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.world.Layer;
import org.matsim.world.MatsimWorldReader;
import org.matsim.api.basic.v01.Id;
import org.matsim.world.World;
import org.matsim.core.population.PersonImpl;
import org.matsim.api.core.v01.population.Person;
import playground.balmermi.census2000.data.Municipality;
import playground.balmermi.census2000.data.Municipalities;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.apache.commons.math.optimization.linear.*;

import org.apache.commons.math.optimization.GoalType;
import org.apache.commons.math.optimization.OptimizationException;
import org.apache.commons.math.optimization.RealPointValuePair;


/**
 * Class to run attach agents with an income via municipalities' average income
 * @param args
 */
public class IncomeAttacher {
	
	
	public static void main(String[] args) {
		log.info("Process started...");
		
		
		final String facilitiesFilename = "/home/baug/mfeil/data/Zurich10/facilities.xml";
		final String worldFilename = "/home/baug/mfeil/data/Zurich10/world.xml";
		final String worldAddFilename = "/home/baug/mfeil/data/Zurich10/gg25_2001_infos.txt";
		final String highestEducationFilename = "/home/baug/mfeil/data/Zurich10/highestEducCensus2000.txt";
		final String networkFilename = "/home/baug/mfeil/data/Zurich10/network.xml";
		final String populationFilename = "/home/baug/mfeil/data/Zurich10/plans.xml";
		final String outputFilename = "/home/baug/mfeil/data/Zurich10/income.xls";
		
		/*
		final String populationFilename = "./plans/output_plans.xml";
		final String networkFilename = "./plans/network.xml";
		final String facilitiesFilename = "./plans/facilities.xml";
		final String worldFilename = "./plans/world.xml";
		final String worldAddFilename = "./plans/gg25_2001_infos.txt";
		final String outputFilename = "./plans/output.xls";
		*/				
		
		ScenarioImpl scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFilename);
		new MatsimFacilitiesReader(scenario.getActivityFacilities()).readFile(facilitiesFilename);
		new MatsimPopulationReader(scenario).readFile(populationFilename);
		new MatsimWorldReader(scenario.getWorld()).readFile(worldFilename);

		IncomeAttacher att = new IncomeAttacher(scenario);
		att.run(worldAddFilename, highestEducationFilename, outputFilename);
		log.info("Process finished.");
	}
	
	private static final Logger log = Logger.getLogger(IncomeAttacher.class);
	private HashMap<Id,Double> income;
	private ScenarioImpl scenario;
	private Municipalities municipalities;
	private Map<Id, Integer> education;
	private HashMap<Id,Id> listings;

	
	public IncomeAttacher (ScenarioImpl scenario){
		this.scenario = scenario;
	}
	
	
	private void run (String inputFile, String educationFile, String outputFile){
		
		log.info("  parsing additional municipality information... ");
		this.municipalities = new Municipalities(inputFile);
		Layer municipalityLayer = scenario.getWorld().getLayer(new IdImpl(Municipalities.MUNICIPALITY));
		this.municipalities.parse(municipalityLayer);
		log.info("  done.");
		
		log.info("  parsing education information... ");
		AgentsHighestEducationAdder adder = new AgentsHighestEducationAdder();
		adder.run(educationFile);
		this.education = adder.getEducation();
		log.info("  done.");
		
		
		String outputfile = outputFile;
		PrintStream stream;
		try {
			stream = new PrintStream (new File(outputfile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		stream.println("AgentID\tEducation\tMunicipalityID\tAverageIncome\tIndividualIncome");		
		
		this.income = new HashMap<Id,Double>();
		this.listings = new HashMap<Id,Id>();
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
				this.listings.put(person.getId(), munAtts.get(position));
				this.income.put(person.getId(), this.municipalities.getMunicipality(munAtts.get(position)).getIncome());
				
			}
			else if (munAtts.size()== 0){
				noListing++;
				log.warn("Agent "+person.getId()+" without valid home zone. This may never happen!");
				this.listings.put(person.getId(), new IdImpl (-1));
				this.income.put(person.getId(), -1.0);
			}
			else {
				this.listings.put(person.getId(), munAtts.get(0));
				this.income.put(person.getId(), this.municipalities.getMunicipality(munAtts.get(0)).getIncome());
			}
			
			stream.println(person.getId()+"\t"+this.education.get(person.getId())+"\t"+listings.get(person.getId())+"\t"+income.get(person.getId()));
		}
		log.info(listings.size()+" agents in the scenario. Thereof "+doubleListings+" with double-listings and "+noListing+" with no-listings.");
		this.runSimplex();
	}
	
	private void runSimplex (){
		log.info("  running simplex... ");

		// create objective function
		double[] coefficients = new double [this.income.size()];
		for (int i=0; i<coefficients.length;i++) coefficients[i]=1;
		LinearObjectiveFunction f = new LinearObjectiveFunction (coefficients, 0);
		
		// writing constraints
		ArrayList<double[]> table = new ArrayList<double[]>();
		int[] rowTotal = new int[this.municipalities.getMunicipalities().size()];
		for (int i=0;i<this.municipalities.getMunicipalities().size();i++){
			table.add(new double[]{0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0}); // 12 education types x + 1 variable y
			rowTotal[i]=0; // Number of persons per municipality
		}
		for (Iterator<Id> iterator = this.education.keySet().iterator(); iterator.hasNext();){
			Id id = iterator.next(); // count through the number of education types and inhabitants per municipality
			if (this.education.get(id).equals(11)) {
				table.get(Integer.parseInt(this.listings.get(id).toString())-1)[0]++; // Obligatorische Schule
				rowTotal[Integer.parseInt(this.listings.get(id).toString())-1]++;
			}
			else if (this.education.get(id).equals(12)) {
				table.get(Integer.parseInt(this.listings.get(id).toString())-1)[1]++; // Diplommittelschule
				rowTotal[Integer.parseInt(this.listings.get(id).toString())-1]++;
			}
			else if (this.education.get(id).equals(21)) {
				table.get(Integer.parseInt(this.listings.get(id).toString())-1)[2]++; // Berufslehre
				rowTotal[Integer.parseInt(this.listings.get(id).toString())-1]++;
			}
			else if (this.education.get(id).equals(22)) {
				table.get(Integer.parseInt(this.listings.get(id).toString())-1)[3]++; // Maturitätsschule
				rowTotal[Integer.parseInt(this.listings.get(id).toString())-1]++;
				}
			else if (this.education.get(id).equals(23)) {
				table.get(Integer.parseInt(this.listings.get(id).toString())-1)[4]++; // Lehrerseminar
				rowTotal[Integer.parseInt(this.listings.get(id).toString())-1]++;
			}
			else if (this.education.get(id).equals(31)) {
				table.get(Integer.parseInt(this.listings.get(id).toString())-1)[5]++; // Höhere Fach- und Berufsausbildung
				rowTotal[Integer.parseInt(this.listings.get(id).toString())-1]++;
			}
			else if (this.education.get(id).equals(32)) {
				table.get(Integer.parseInt(this.listings.get(id).toString())-1)[6]++; // Höhere Fachschule
				rowTotal[Integer.parseInt(this.listings.get(id).toString())-1]++;
			}
			else if (this.education.get(id).equals(33)) {
				table.get(Integer.parseInt(this.listings.get(id).toString())-1)[7]++; // FH
				rowTotal[Integer.parseInt(this.listings.get(id).toString())-1]++;
			}
			else if (this.education.get(id).equals(34)) {
				table.get(Integer.parseInt(this.listings.get(id).toString())-1)[8]++; // Universität
				rowTotal[Integer.parseInt(this.listings.get(id).toString())-1]++;
			}
			else if (this.education.get(id).equals(-7)) {
				table.get(Integer.parseInt(this.listings.get(id).toString())-1)[9]++; // Ohne Angabe
				rowTotal[Integer.parseInt(this.listings.get(id).toString())-1]++;
			}
			else if (this.education.get(id).equals(-8)) {
				table.get(Integer.parseInt(this.listings.get(id).toString())-1)[10]++; // Keine Ausbildung
				rowTotal[Integer.parseInt(this.listings.get(id).toString())-1]++;
			}
			else if (this.education.get(id).equals(-9)) {
				table.get(Integer.parseInt(this.listings.get(id).toString())-1)[11]++; // Noch nicht schulpflichtig
				rowTotal[Integer.parseInt(this.listings.get(id).toString())-1]++;
			}
		}
		ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
		for (int i=0;i<table.size();i++){
			constraints.add(new LinearConstraint(table.get(i), Relationship.EQ, this.municipalities.getMunicipality(i+1).getIncome()*rowTotal[i])); // 12 education types x + 1 variable y
		}
		
		// Goal type
		GoalType goalType = GoalType.MINIMIZE;
		RealPointValuePair result = new RealPointValuePair(new double[]{0.0},0.0); // Dummy initialization
		try{
			result = new SimplexSolver().optimize(f, constraints, goalType, false) ;
		}
		catch (OptimizationException e)	{
			log.warn("Error in optimization: "+e);	
		}
		log.info("Result value = "+result.getValue());
		for (int i=0;i<result.getPoint().length;i++) log.info(i+" Koeffizient = "+result.getPoint()[i]);
		
		log.info("  done.");
	}
}


