/* *********************************************************************** *
 * project: org.matsim.*
 * FhEmissions.java
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
 *                                                                         
 * *********************************************************************** */
package playground.fhuelsmann.emission;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;

public class LinkAndAgentAccountAnalysisModule implements AnalysisModule{

	public  String findHbefaFromVisumRoadType(int roadType){
		return this.roadTypes[roadType].getHBEFA_RT_NR();
	}

	private final VisumObject[] roadTypes = new VisumObject[100];

	EmissionsPerEvent emissionFactor = new EmissionsPerEvent();

	private Map<Id, double[]> linkId2emissionsInGrammPerType = new TreeMap<Id,double[]>();
	private Map<Id, double[]> personId2emissionsInGrammPerType = new TreeMap<Id,double[]>();

	/* the arrays are to be filled with:
	massOfFluelBasedOnAverageSpeed [0]
	noxEmissionsBasedOnAverageSpeed [1]
	co2repEmissionsBasedOnAverageSpeed [2]
	co2EmissionsBasedOnAverageSpeed [3]
	no2EmissionsBasedOnAverageSpeed[4]
	pmEmissionsBasedOnAverageSpeed[5]

	massOfFluelBasedOnFractions[6]
	noxEmissionsBasedOnFractions[7]
	co2repEmissionsBasedOnFractions[8]
	co2EmissionsBasedOnFractions[9]
	no2EmissionsBasedOnFractions[10]
	pmEmissionsBasedOnFractions[11] */

	@Override
	public void calculateEmissionsPerLink(double travelTime, Id linkId, double averageSpeed, int roadType, double freeVelocity, double distance, HbefaObject[][] hbefaTable) {

		//linkage between Hbefa road types and Visum road types
		createRoadTypes("../../detailedEval/teststrecke/sim/inputEmissions/road_types.txt");
		int Hbefa_road_type = Integer.valueOf(findHbefaFromVisumRoadType(roadType));

		//get emissions calculated per event differentiated by fraction and average speed approach
		double [] inputForEmissions = emissionFactor.collectInputForEmission(Hbefa_road_type, averageSpeed, distance,hbefaTable);

		//if no link in the map
		if(this.linkId2emissionsInGrammPerType.get(linkId) == null) {
			this.linkId2emissionsInGrammPerType.put(linkId, inputForEmissions);// data is read for the first time, doesn't need to be summed up per link
		}
		else{
			double [] actualEmissions = new double[12]; // new data is saved after summation
			double [] previousEmissions = this.linkId2emissionsInGrammPerType.get(linkId); // previousEmissions is the previous sum

			for(int i = 0; i < 12 ; i++){
				actualEmissions[i]= previousEmissions[i] + inputForEmissions[i];
			}
			// put newValue in the Map
			linkId2emissionsInGrammPerType.put(linkId, actualEmissions);
		}
	}

	@Override
	public void calculateEmissionsPerPerson(double travelTime, Id personId, double averageSpeed, int roadType, double freeVelocity, double distance, HbefaObject[][] hbefaTable) {

		//linkage between Hbefa road types and Visum road types
		createRoadTypes("../../detailedEval/teststrecke/sim/inputEmissions/road_types.txt");
		int Hbefa_road_type = Integer.valueOf(findHbefaFromVisumRoadType(roadType));

		//get emissions calculated per event differentiated by fraction and average speed approach
		double [] inputForEmissions = emissionFactor.collectInputForEmission(Hbefa_road_type, averageSpeed, distance,hbefaTable);

		//if no link in the map
		if(this.personId2emissionsInGrammPerType.get(personId) == null) {
			this.personId2emissionsInGrammPerType.put(personId, inputForEmissions);// data is read for the first time, doesn't need to be summed up per link
		}
		else{
			double [] actualEmissions = new double[12]; // new data is saved after summation
			double [] previousEmissions = this.personId2emissionsInGrammPerType.get(personId); //oldValue is the previous sum

			for(int i = 0; i < 12 ; i++){
				actualEmissions[i]= previousEmissions[i] + inputForEmissions[i];
			}
			// put newValue in the Map
			personId2emissionsInGrammPerType.put(personId, actualEmissions);
		}
	}

	public void createRoadTypes(String filename){
		try{
			FileInputStream fstream = new FileInputStream(filename);
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			//Read File Line By Line
			br.readLine();
			while ((strLine = br.readLine()) != null){

				//for all lines (whole text) we split the line to an array 
				String[] array = strLine.split(",");
				VisumObject obj = new VisumObject(Integer.parseInt(array[0]), array[2]);
				this.roadTypes[obj.getVISUM_RT_NR()] = obj;
			}
			in.close();
		}
		catch (Exception e){
			System.err.println("Error: " + e.getMessage());
		}
	}

	public void printTotalEmissionTable(Map<Id, double[]> id2emissionsInGrammPerType, String outputFile) {
		try{ 
			String idEmissionTypeAndGramm = null;
			FileWriter fstream = new FileWriter(outputFile);			
			BufferedWriter out = new BufferedWriter(fstream);
			out.write("Id \t Luftschadstoff \t Emissionen \n");   

			for(Entry<Id, double[]> PersonIdEntry : id2emissionsInGrammPerType.entrySet()){
				for(Integer i = 0 ; i < 12 ; i++){
					Double emissionLevel = PersonIdEntry.getValue()[i];
					String emissionLevelString = emissionLevel.toString();
					idEmissionTypeAndGramm = PersonIdEntry.getKey().toString()+ "\t" + i.toString() + "\t" + emissionLevelString + "\n";
					out.write(idEmissionTypeAndGramm);
				}
			}
			//Close the output stream
			out.close();
		}
		catch (Exception e){
			System.err.println("Error: " + e.getMessage());
		}
	}

	public Map<Id, double[]> getTotalEmissionsPerLink() {
		return this.linkId2emissionsInGrammPerType;
	}

	public Map<Id, double[]> getTotalEmissionsPerPerson() {
		return this.personId2emissionsInGrammPerType;
	}
}