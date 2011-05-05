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

import playground.fhuelsmann.emission.objects.HbefaObject;
import playground.fhuelsmann.emission.objects.VisumObject;

public class WarmEmissionAnalysisModule implements AnalysisModule{

	private VisumObject[] roadTypes = null;
	private EmissionsPerEvent emissionFactor = null;

	private Map<Id, double[]> linkId2emissionsInGrammPerType = new TreeMap<Id,double[]>();
	private Map<Id, double[]> personId2emissionsInGrammPerType = new TreeMap<Id,double[]>();
	/* the arrays double[] are going to be filled with:
			massOfFuelBasedOnAverageSpeed [0]
			noxEmissionsBasedOnAverageSpeed [1]
			co2EmissionsBasedOnAverageSpeed [2]
			no2EmissionsBasedOnAverageSpeed[3]
			pmEmissionsBasedOnAverageSpeed[4]
		
			massOfFuelBasedOnFractions[5]
			noxEmissionsBasedOnFractions[6]
			co2EmissionsBasedOnFractions[7]
			no2EmissionsBasedOnFractions[8]
			pmEmissionsBasedOnFractions[9] */

	public WarmEmissionAnalysisModule(VisumObject[] roadTypes, EmissionsPerEvent emissionFactor) {
		this.roadTypes = roadTypes;
		this.emissionFactor = emissionFactor;
	}

	public  String findHbefaFromVisumRoadType(int roadType){
		return this.roadTypes[roadType].getHBEFA_RT_NR();
	}

	@Override
	public void calculateEmissionsPerLink(double travelTime, Id linkId, Id personId, double averageSpeed, 
			int roadType, String hubSizeAge, double freeVelocity, double distance, HbefaObject[][] hbefaTable, HbefaObject[][] hbefaHdvTable) {

		//linkage between Hbefa road types and Visum road types
		int hbefaRoadType = Integer.valueOf(findHbefaFromVisumRoadType(roadType));
		

		//get emissions calculated per event differentiated by fraction and average speed approach
		double [] inputForEmissions = emissionFactor.collectInputForEmission(hbefaRoadType, averageSpeed, distance, hbefaTable);
		double [] inputForHdvEmissions = emissionFactor.collectInputForEmission(hbefaRoadType, averageSpeed, distance, hbefaHdvTable);
		//if no link in the map
		if(  !personId.toString().contains("gv_")){
		if(this.linkId2emissionsInGrammPerType.get(linkId) == null) {
			this.linkId2emissionsInGrammPerType.put(linkId, inputForEmissions);// data is read for the first time, doesn't need to be summed up per link
		}
		else{
			double [] actualEmissions = new double[10]; // new data is saved after summation
			double [] previousEmissions = this.linkId2emissionsInGrammPerType.get(linkId); // previousEmissions is the previous sum

			for(int i = 0; i < actualEmissions.length ; i++){
				actualEmissions[i]= previousEmissions[i] + inputForEmissions[i];
			}
			// put newValue in the Map
			linkId2emissionsInGrammPerType.put(linkId, actualEmissions);
		}
		}
		else {if(this.linkId2emissionsInGrammPerType.get(linkId) == null) {
			this.linkId2emissionsInGrammPerType.put(linkId, inputForHdvEmissions);// data is read for the first time, doesn't need to be summed up per link
			}
			else{
				double [] actualEmissions = new double[10]; // new data is saved after summation
				double [] previousEmissions = this.linkId2emissionsInGrammPerType.get(linkId); // previousEmissions is the previous sum

				for(int i = 0; i < actualEmissions.length ; i++){
					actualEmissions[i]= previousEmissions[i] + inputForHdvEmissions[i];
	//				System.out.println("PersonID "+ personId);
				}
			// put newValue in the Map
				linkId2emissionsInGrammPerType.put(linkId, actualEmissions);
			}
		}
	}

	@Override
	public void calculateEmissionsPerPerson(double travelTime, Id personId, double averageSpeed, int roadType, String hubSizeAge, double freeVelocity, double distance, HbefaObject[][] hbefaTable,HbefaObject[][] hbefaHdvTable) {

		//linkage between Hbefa road types and Visum road types
		int hbefaRoadType = Integer.valueOf(findHbefaFromVisumRoadType(roadType));

		//get emissions calculated per event differentiated by fraction and average speed approach
		double [] inputForEmissions = emissionFactor.collectInputForEmission(hbefaRoadType, averageSpeed, distance,hbefaTable);
		double [] inputForHdvEmissions = emissionFactor.collectInputForEmission(hbefaRoadType, averageSpeed, distance, hbefaHdvTable);
		
		if(  !personId.toString().contains("gv_")){
		//if no link in the map
		if(this.personId2emissionsInGrammPerType.get(personId) == null) {
			this.personId2emissionsInGrammPerType.put(personId, inputForEmissions);// data is read for the first time, doesn't need to be summed up per link
		}
		else{
			double [] actualEmissions = new double[10]; // new data is saved after summation
			double [] previousEmissions = this.personId2emissionsInGrammPerType.get(personId); //oldValue is the previous sum

			for(int i = 0; i < actualEmissions.length ; i++){
				actualEmissions[i]= previousEmissions[i] + inputForEmissions[i];
			}
			// put newValue in the Map
			personId2emissionsInGrammPerType.put(personId, actualEmissions);
		}
		}
		else{if(this.personId2emissionsInGrammPerType.get(personId) == null) {
			this.personId2emissionsInGrammPerType.put(personId, inputForHdvEmissions);// data is read for the first time, doesn't need to be summed up per link
		}
		else{
			double [] actualEmissions = new double[10]; // new data is saved after summation
			double [] previousEmissions = this.personId2emissionsInGrammPerType.get(personId); //oldValue is the previous sum

			for(int i = 0; i < actualEmissions.length ; i++){
				actualEmissions[i]= previousEmissions[i] + inputForHdvEmissions[i];
			}
			// put newValue in the Map
			personId2emissionsInGrammPerType.put(personId, actualEmissions);
		}
			
		}
	}
/*	public void createRoadTypesTafficSituation(String filename) {
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
	}*/

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

	public Map<Id, double[]> getWarmEmissionsPerLink() {
		return this.linkId2emissionsInGrammPerType;
	}

	public Map<Id, double[]> getWarmEmissionsPerPerson() {
		return this.personId2emissionsInGrammPerType;
	}
}