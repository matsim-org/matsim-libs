/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeComparer.java
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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;


/**
 * Compares two plans files with respect to the car travel times. If the population sizes are different 
 * (census data and extracted census file) all modes are compared.
 *
 * @author mfeil
 */
public class TravelTimeComparer {

	private final PopulationImpl populationOrig, populationNew;
	private static final Logger log = Logger.getLogger(TravelTimeComparer.class);
	private final String outputFile;


	public TravelTimeComparer(final PopulationImpl populationOrig, final PopulationImpl populationNew) {
		this.populationOrig = populationOrig;
		this.populationNew = populationNew;
	//	this.outputFile = "./plans/plans_similarity.xls";	
		this.outputFile = "/home/baug/mfeil/data/largeSet/it1/ttcompare1929.xls";	
	}
	
	public void run(){
		log.info("Calculating travel time differences...");
		log.info("populationOrig: "+this.populationOrig.getPersons().size());
		log.info("populationNew: "+this.populationNew.getPersons().size());
		
		if (this.populationNew.getPersons().size()!=this.populationOrig.getPersons().size()){
			log.warn("Different populations! Using the latter iteration's population.");
			this.runSpecial();
			return;
		}		
		
		PrintStream stream;
		try {
			stream = new PrintStream (new File(this.outputFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		// First row
		int counter=0;
		stream.print("Id\tChoice\t");
		Person p = this.populationNew.getPersons().values().iterator().next();
		for (int i = 0;i<p.getPlans().size();i++){
			for (int j =1;j<java.lang.Math.max(p.getPlans().get(i).getPlanElements().size()-1,1);j+=2){
				if (((LegImpl)(p.getPlans().get(i).getPlanElements().get(j))).getMode().equals(TransportMode.car)){
					stream.print("x"+(i+1)+""+(j+1)+"\t");
					counter++;
				}
			}
		}
		stream.println("increase\tdecrease\tsame\tsum_delta");
		
		double [][] stats = new double[this.populationNew.getPersons().size()][counter];
		counter = 0;
		
		// Filling plans
		double value=0;
		double valueSelected=0;
		double diffSelected=0;
		int numberSelected=0;
		for (Person personOrig : this.populationOrig.getPersons().values()) {
			int counterIn=0;
			
			Person personNew = this.populationNew.getPersons().get(personOrig.getId());
			
			stream.print(personOrig.getId()+"\t");
			int position = -1;
			for (int i=0;i<personOrig.getPlans().size();i++){
				if (personOrig.getPlans().get(i).equals(personOrig.getSelectedPlan())) {
					position = i+1;
					break;
				}
			}
			stream.print(position+"\t");
			
			for (int i=0;i<personOrig.getPlans().size();i++){
				Plan planOrig = personOrig.getPlans().get(i);
				Plan planNew = personNew.getPlans().get(i);
				for (int j=1;j<planOrig.getPlanElements().size();j+=2){
					if (((LegImpl)(planOrig.getPlanElements().get(j))).getMode().equals(TransportMode.car)){
						value += ((LegImpl)(planOrig.getPlanElements().get(j))).getTravelTime();
						double diff = (((LegImpl)(planNew.getPlanElements().get(j))).getTravelTime()-((LegImpl)(planOrig.getPlanElements().get(j))).getTravelTime());
						if (planNew.isSelected()){
							valueSelected+=((LegImpl)(planOrig.getPlanElements().get(j))).getTravelTime();
							diffSelected+=diff;
							numberSelected++;
						}						
						stream.print(diff+"\t");
						stats[counter][counterIn]=diff;
						counterIn++;
					}
				}
			}
			int increase=0;
			int decrease=0;
			int same=0;
			double delta=0;
			for (int i=0;i<stats[counter].length;i++){
				if(stats[counter][i]>0) increase++;
				else if (stats[counter][i]<0) decrease++;
				else same++;
				delta+=stats[counter][i];
			}
			stream.println(increase+"\t"+decrease+"\t"+same+"\t"+delta);
			
			counter++;
		}
		//Analysis
		//Increase
		stream.print("\tincrease\t");
		for (int j=0;j<stats[0].length;j++){
			int increase=0;
			for (int i=0;i<stats.length;i++){
				if(stats[i][j]>0) increase++;
			}
			stream.print(increase+"\t");
		}
		stream.println();
		
		//Decrease
		stream.print("\tdecrease\t");
		for (int j=0;j<stats[0].length;j++){
			int decrease=0;
			for (int i=0;i<stats.length;i++){
				if(stats[i][j]<0) decrease++;
			}
			stream.print(decrease+"\t");
		}
		stream.println();
		
		//Same
		stream.print("\tsame\t");
		for (int j=0;j<stats[0].length;j++){
			int same=0;
			for (int i=0;i<stats.length;i++){
				if(stats[i][j]==0) same++;
			}
			stream.print(same+"\t");
		}
		stream.println();
		
		//Delta
		stream.print("\tsumDeltaPerCarTrip\t");
		double deltaPerTrip=0;
		for (int j=0;j<stats[0].length;j++){
			int delta=0;
			for (int i=0;i<stats.length;i++){
				delta+=stats[i][j];
			}
			stream.print((delta/stats[0].length)+"\t");
			deltaPerTrip += delta;
		}
		stream.println();
		stream.println("\taveValuePerCarTrip\t"+(value/(stats.length*stats[0].length)));	
		stream.println("\taveDeltaPerCarTrip\t"+(deltaPerTrip/(stats.length*stats[0].length)));	
		stream.println();
		stream.println("\taveValuePerSelectedCarTrip\t"+(valueSelected/numberSelected));	
		stream.println("\taveDeltaPerSelectedCarTrip\t"+(diffSelected/numberSelected));	
		
		stream.close();
		log.info("done.");
	}
	
	private void runSpecial(){
		PrintStream stream;
		try {
			stream = new PrintStream (new File(this.outputFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		// get maximum number of legs
		int size = 0;
		for (Person person : this.populationNew.getPersons().values()) {
			int sizeIn = person.getSelectedPlan().getPlanElements().size()/2;
			if (sizeIn>size) size= sizeIn;
		}		
		stream.print("PersonI\t");
		for (int i=1;i<size+1;i++){
			stream.print("x'x'"+(i*2)+"\t");
		}
		stream.println("increase\tdecrease\tsame\tsum_delta");
		
		int counter = 0;
		
		double Car=0;
		int increaseCar=0;
		int decreaseCar=0;
		int sameCar=0;
		double valueCar=0;
		double PT=0;
		int increasePT=0;
		int decreasePT=0;
		int samePT=0;
		double valuePT=0;
		double Walk=0;
		int increaseWalk=0;
		int decreaseWalk=0;
		int sameWalk=0;
		double valueWalk=0;
		double Bike=0;
		int increaseBike=0;
		int decreaseBike=0;
		int sameBike=0;
		double valueBike=0;
		
		// compare populations
		for (Person person : this.populationNew.getPersons().values()) {
			Plan planNew = person.getSelectedPlan();
			Plan planOrig = this.populationOrig.getPersons().get(person.getId()).getSelectedPlan();
			
			stream.print(person.getId()+"\t");
			/*
			for (int i=0;i<planOrig.getPlanElements().size();i++){
				if (i%2==0) log.info("Orig Act"+i+": "+((ActivityImpl)(planOrig.getPlanElements().get(i))).getType());
				else log.info("Orig Act"+i+": "+((LegImpl)(planOrig.getPlanElements().get(i))).getMode());
			}
			
			for (int i=0;i<planNew.getPlanElements().size();i++){
				if (i%2==0) log.info("New Act"+i+": "+((ActivityImpl)(planNew.getPlanElements().get(i))).getType());
				else log.info("New Act"+i+": "+((LegImpl)(planNew.getPlanElements().get(i))).getMode());
			}
			*/
			
			int increase=0;
			int decrease=0;
			int same=0;
			double delta=0;
			
			if (planOrig.getPlanElements().size()>1){
				for (int j=1;j<planOrig.getPlanElements().size();j+=2){
					double value = ((LegImpl)(planOrig.getPlanElements().get(j))).getTravelTime();
					double diff = (((LegImpl)(planNew.getPlanElements().get(j))).getTravelTime()-((LegImpl)(planOrig.getPlanElements().get(j))).getTravelTime());
					stream.print(diff+"\t");	
					if (diff>0)increase++;
					else if (diff<0)decrease++;
					else same++;
					delta+=diff;
					
					if (((LegImpl)(planNew.getPlanElements().get(j))).getMode().equals(TransportMode.car)){
						if (diff>0)increaseCar++;
						else if (diff<0)decreaseCar++;
						else sameCar++;
						valueCar+=diff;
						Car+=value;
					}
					else if (((LegImpl)(planNew.getPlanElements().get(j))).getMode().equals(TransportMode.pt)){
						if (diff>0)increasePT++;
						else if (diff<0)decreasePT++;
						else samePT++;
						valuePT+=diff;
						PT+=value;
					}
					else if (((LegImpl)(planNew.getPlanElements().get(j))).getMode().equals(TransportMode.walk)){
						if (diff>0)increaseWalk++;
						else if (diff<0)decreaseWalk++;
						else sameWalk++;
						valueWalk+=diff;
						Walk+=value;
					}
					else if (((LegImpl)(planNew.getPlanElements().get(j))).getMode().equals(TransportMode.bike)){
						if (diff>0)increaseBike++;
						else if (diff<0)decreaseBike++;
						else sameBike++;
						valueBike+=diff;
						Bike+=value;
					}
					else {
						log.warn("Unknown mode: "+((LegImpl)(planNew.getPlanElements().get(j))).getMode());
					}
				}
			}/*
			if (planOrig.getPlanElements().size()==1){
				stream.print("\t");
			}*/
			for (int j=planOrig.getPlanElements().size()/2+1;j<=size;j++){
				stream.print("\t");
			}

			stream.println(increase+"\t"+decrease+"\t"+same+"\t"+delta);			
			counter++;
		}
		stream.println();
		stream.println("increaseCar\t"+increaseCar);
		stream.println("decreaseCar\t"+decreaseCar);
		stream.println("sameCar\t"+sameCar);
		stream.println("valueCar\t"+valueCar);
		stream.println("valuePerTrip\t"+(Car/(increaseCar+decreaseCar+sameCar)));
		stream.println("deltaPerTrip\t"+(valueCar/(increaseCar+decreaseCar+sameCar)));
		stream.println();
		stream.println("increasePT\t"+increasePT);
		stream.println("decreasePT\t"+decreasePT);
		stream.println("samePT\t"+samePT);
		stream.println("valuePT\t"+valuePT);
		stream.println("valuePerTrip\t"+(PT/(increasePT+decreasePT+samePT)));
		stream.println("deltaPerTrip\t"+(valuePT/(increasePT+decreasePT+samePT)));
		stream.println();
		stream.println("increaseWalk\t"+increaseWalk);
		stream.println("decreaseWalk\t"+decreaseWalk);
		stream.println("sameWalk\t"+sameWalk);
		stream.println("valueWalk\t"+valueWalk);
		stream.println("valuePerTrip\t"+(Walk/(increaseWalk+decreaseWalk+sameWalk)));
		stream.println("deltaPerTrip\t"+(valueWalk/(increaseWalk+decreaseWalk+sameWalk)));
		stream.println();
		stream.println("increaseBike\t"+increaseBike);
		stream.println("decreaseBike\t"+decreaseBike);
		stream.println("sameBike\t"+sameBike);
		stream.println("valueBike\t"+valueBike);
		stream.println("valuePerTrip\t"+(Bike/(increaseBike+decreaseBike+sameBike)));
		stream.println("deltaPerTrip\t"+(valueBike/(increaseBike+decreaseBike+sameBike)));

		
		stream.close();
		log.info("done.");
		
		
	}
	
	public static void main(final String [] args) {
				final String facilitiesFilename = "/home/baug/mfeil/data/Zurich10/facilities.xml";
				final String networkFilename = "/home/baug/mfeil/data/Zurich10/network.xml";
				final String populationFilenameOrig = "/home/baug/mfeil/data/largeSet/it1/output_plans_mz19.xml";
				final String populationFilenameNew = "/home/baug/mfeil/data/largeSet/it2/output_plans_mz29.xml";
		/*		final String populationFilename = "./plans/output_plans.xml";
				final String networkFilename = "./plans/network.xml";
				final String facilitiesFilename = "./plans/facilities.xml";
		*/
				ScenarioImpl scenarioOrig = new ScenarioImpl();
				new MatsimNetworkReader(scenarioOrig).readFile(networkFilename);
				new MatsimFacilitiesReader(scenarioOrig).readFile(facilitiesFilename);
				new MatsimPopulationReader(scenarioOrig).readFile(populationFilenameOrig);
				
				ScenarioImpl scenarioNew = new ScenarioImpl();
				scenarioNew.setNetwork(scenarioOrig.getNetwork());
				new MatsimFacilitiesReader(scenarioNew).readFile(facilitiesFilename);
				new MatsimPopulationReader(scenarioNew).readFile(populationFilenameNew);
								
				TravelTimeComparer ttc = new TravelTimeComparer(scenarioOrig.getPopulation(), scenarioNew.getPopulation());
				ttc.run();
				log.info("Process finished.");
			}
}

