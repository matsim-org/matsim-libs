/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionPrinter.java
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
package playground.fhuelsmann.emission;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;

/**
 * @author benjamin
 *
 */
public class EmissionPrinter {

	private String runDirectory;

	public EmissionPrinter(String runDirectory) {
		this.runDirectory = runDirectory;
	}

	void printHomeLocation2Emissions(Population population,	Map<Id, double[]> personId2emissionsInGrammPerType, String filename) {
		Double fcEmissions = null;
		Double noxEmissions = null;
		Double co2Emissions = null;
		Double no2Emissions = null;
		Double pmEmissions = null;
		
		String outLine = null;
		String outFile = runDirectory + filename;
		try{ 
			FileWriter fstream = new FileWriter(outFile);			
			BufferedWriter out = new BufferedWriter(fstream);
			out.write("personId \t xHome \t yHome \t fc[g] \t nox[g] \t co2[g] \t no2[g] \t pm[g] \n");   

			for(Person person: population.getPersons().values()){
				Id personId = person.getId();
				Plan plan = person.getSelectedPlan();
				Activity homeAct = (Activity) plan.getPlanElements().get(0);
				Coord homeCoord = homeAct.getCoord();
				Double xHome = homeCoord.getX();
				Double yHome = homeCoord.getY();
				if(!personId2emissionsInGrammPerType.containsKey(personId)){
					// pt, bike, and walk are assumed to run emission free.
					fcEmissions = 0.0;
					noxEmissions = 0.0;
					co2Emissions = 0.0;
					no2Emissions = 0.0;
					pmEmissions = 0.0;
				}
				else{
					// only values of the fraction approach
					fcEmissions = personId2emissionsInGrammPerType.get(personId)[5];
					noxEmissions = personId2emissionsInGrammPerType.get(personId)[6];
					co2Emissions = personId2emissionsInGrammPerType.get(personId)[7];
					no2Emissions = personId2emissionsInGrammPerType.get(personId)[8];
					pmEmissions = personId2emissionsInGrammPerType.get(personId)[9];
				}
				outLine = personId.toString()+ "\t" + xHome.toString() + "\t" + yHome.toString() + "\t"
				+ fcEmissions.toString() + "\t" + noxEmissions.toString() + "\t" + co2Emissions.toString() + "\t" 
				+ no2Emissions.toString() + "\t" + pmEmissions.toString() + "\n";
				out.write(outLine);
			}
			//Close the output stream
			out.close();
			System.out.println("Finished writing output to " + outFile);
		}
		catch (Exception e){
			System.err.println("Error: " + e.getMessage());
		}
	}

	void printEmissionTable(Map<Id, double[]> id2emissionsInGrammPerType, String filename) {
		String outFile = runDirectory + filename;
		try{ 
			String idEmissionTypeAndGramm = null;
			FileWriter fstream = new FileWriter(outFile);			
			BufferedWriter out = new BufferedWriter(fstream);
			out.write("Id \t AirPollutant \t Emissions \n");   

			for(Entry<Id, double[]> personIdEntry : id2emissionsInGrammPerType.entrySet()){
				for(Integer i = 0 ; i < personIdEntry.getValue().length ; i++){
					Double emissionLevel = personIdEntry.getValue()[i];
					String emissionLevelString = emissionLevel.toString();
					idEmissionTypeAndGramm = personIdEntry.getKey().toString()+ "\t" + i.toString() + "\t" + emissionLevelString + "\n";
					out.write(idEmissionTypeAndGramm);
				}
			}
			//Close the output stream
			out.close();
			System.out.println("Finished writing output to " + outFile);
		}
		catch (Exception e){
			System.err.println("Error: " + e.getMessage());
		}
	}

	public void printColdEmissionTable(Map<Id, Map<String, Double>> personId2ColdEmissions, String filename){
		String outFile = runDirectory + filename;
		try{
			FileWriter fstream = new FileWriter(outFile);			
			BufferedWriter out = new BufferedWriter(fstream);
			out.write("Id \t AirPollutant \t ColdStartEmissionen \n"); 

			for(Entry<Id,Map<String,Double>> personId : personId2ColdEmissions.entrySet()){
				for(Entry<String,Double> component : personId2ColdEmissions.get(personId.getKey()).entrySet()){
					out.append(personId.getKey() + "\t  " + component.getKey() + " \t " +component.getValue() + "\n" );
				}
			}	
			//Close the output stream
			out.close();
			System.out.println("Finished writing output to " + outFile);
		}catch (Exception e){
			System.err.println("Error: " + e.getMessage());
		}
	}
}
