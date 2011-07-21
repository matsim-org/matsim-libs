/* *********************************************************************** *
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
/**  @author friederike**/

package playground.fhuelsmann.emission;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

import playground.fhuelsmann.emission.objects.HbefaObject;
import playground.fhuelsmann.emission.objects.VisumObject;

public class WarmEmissionAnalysisModule implements AnalysisModule{
	private static final Logger logger = Logger.getLogger(WarmEmissionAnalysisModule.class);

	private VisumObject[] roadTypes = null;
	private EmissionsPerEvent emissionFactor = null;
	private String[][] vehicleCharacteristic = new String[100][4];
//	private int counter=0;
	private HbefaHot hbefaHot = new HbefaHot();
//	private Map<Id, double[]> linkIdWalkBkePt2emissionsInGrammPerType = new TreeMap<Id,double[]>();
	private Map<Id, double[]> linkIdComHdvPec2emissionsInGrammPerType =(new TreeMap<Id,double[]>());
    private Map<Id, double[]> linkId2emissionsInGrammPerType = new TreeMap<Id,double[]>();
	private Map<Id, double[]> personId2emissionsInGrammPerType = new TreeMap<Id,double[]>();
	private ArrayList<String> listOfPollutant;
//	private Population population = null;

	public WarmEmissionAnalysisModule(ArrayList<String> listOfPollutant,VisumObject[] roadTypes, EmissionsPerEvent emissionFactor, HbefaHot hbefahot) {
		this.roadTypes = roadTypes;
		this.emissionFactor = emissionFactor;
		this.hbefaHot = hbefahot;
		this.listOfPollutant = listOfPollutant;
	}

	public  String findHbefaFromVisumRoadType(int roadType){
		return this.roadTypes[roadType].getHBEFA_RT_NR();
	}

	/** emission calculation per link for heavy duty vehicles, passenger cars without a vehicle assigned and commuter traffic (pt and car) **/
	public  void calculateEmissionsPerLinkForComHdvPecWithoutVeh(double travelTime, Id linkId, Id personId, double averageSpeed, 
			int roadType, double freeVelocity, double distance, HbefaObject[][] hbefaTable, HbefaObject[][] hbefaHdvTable) {
		
		/* the arrays double[] are going to be filled with:
		massOfFuel [0]
		noxEmissions [1]
		co2Emissions[2]
		no2Emissions[3]
		pmEmissions[4]*/
		int NumberOfPollutant = listOfPollutant.size();
		int hbefaRoadType = Integer.valueOf(findHbefaFromVisumRoadType(roadType));
		//get emissions calculated per event by fraction approach
		double [] inputForEmissions = emissionFactor.collectInputForEmissionFraction (listOfPollutant,hbefaRoadType, averageSpeed, distance,hbefaTable);
		//only for CO2 and FC are values available, otherwise 0.0
		double [] inputForHdvEmissions = emissionFactor.collectInputForEmissionFraction(listOfPollutant,hbefaRoadType, averageSpeed, distance, hbefaHdvTable);

		if(!personId.toString().contains("gv_")){
			if(this.linkIdComHdvPec2emissionsInGrammPerType.get(linkId) == null) {
				this.linkIdComHdvPec2emissionsInGrammPerType.put(linkId, inputForEmissions);// data is read for the first time, doesn't need to be summed up per link
			}else{
				double [] actualEmissions = new double[NumberOfPollutant]; // new data is saved after summation
				double [] previousEmissions = this.linkIdComHdvPec2emissionsInGrammPerType.get(linkId);		
				for(int i = 0; i < actualEmissions.length ; i++){
					actualEmissions[i]= previousEmissions[i] + inputForEmissions[i];
					}
				linkIdComHdvPec2emissionsInGrammPerType.put(linkId, actualEmissions);
					}
		//// else of gv,...
		}else{
			if(this.linkIdComHdvPec2emissionsInGrammPerType.get(linkId) == null) {
				this.linkIdComHdvPec2emissionsInGrammPerType.put(linkId, inputForHdvEmissions);// data is read for the first time, doesn't need to be summed up per link
			}else{
				double [] actualEmissions = new double[NumberOfPollutant]; // new data is saved after summation
				double [] previousEmissions = this.linkIdComHdvPec2emissionsInGrammPerType.get(linkId); // previousEmissions is the previous sum
				for(int i = 0; i < actualEmissions.length ; i++){
					actualEmissions[i]= previousEmissions[i] + inputForHdvEmissions[i];
					}
				linkIdComHdvPec2emissionsInGrammPerType.put(linkId, actualEmissions);// put newValue in the Map
			}
		}
	}
	
	/** emission calculation per person for heavy duty vehicles, passenger cars without a vehicle assigned and commuter traffic (pt and car) **/
	public  void calculateEmissionsPerCommuterHdvPcWithoutVeh(double travelTime, Id personId, double averageSpeed, int roadType, double freeVelocity, double distance, HbefaObject[][] hbefaTable,HbefaObject[][] hbefaHdvTable) {
		
		/* the arrays double[] are going to be filled with:
		massOfFuel [0]
		noxEmissions [1]
		co2Emissions[2]
		no2Emissions[3]
		pmEmissions[4]*/
		
		//linkage between Hbefa road types and Visum road types
		int hbefaRoadType = Integer.valueOf(findHbefaFromVisumRoadType(roadType));
		
		//get emissions calculated per event by fraction approach
		double [] inputForEmissions = emissionFactor.collectInputForEmissionFraction (listOfPollutant,hbefaRoadType, averageSpeed, distance,hbefaTable);
		//only for CO2 and FC are values available, otherwise 0.0
		double [] inputForHdvEmissions = emissionFactor.collectInputForEmissionFraction(listOfPollutant,hbefaRoadType, averageSpeed, distance, hbefaHdvTable);
		
		if(  !personId.toString().contains("gv_")){
			if(this.personId2emissionsInGrammPerType.get(personId) == null) {
				this.personId2emissionsInGrammPerType.put(personId, inputForEmissions);// data is read for the first time, doesn't need to be summed up per link
			}else{
				double [] actualEmissions = new double[listOfPollutant.size()]; // new data is saved after summation
				double [] previousEmissions = this.personId2emissionsInGrammPerType.get(personId);
				for(int i = 0; i < actualEmissions.length ; i++)
					actualEmissions[i]= previousEmissions[i] + inputForEmissions[i];		
				personId2emissionsInGrammPerType.put(personId, actualEmissions);
			}
		}else{
			if(this.personId2emissionsInGrammPerType.get(personId) == null) {
				this.personId2emissionsInGrammPerType.put(personId, inputForHdvEmissions);// data is read for the first time, doesn't need to be summed up per link
			}else{
				double [] actualEmissions = new double[listOfPollutant.size()]; // new data is saved after summation
				double [] previousEmissions = this.personId2emissionsInGrammPerType.get(personId); //oldValue is the previous sum
				for(int i = 0; i < actualEmissions.length ; i++){
					actualEmissions[i] = previousEmissions[i] + inputForHdvEmissions[i];
				}
				personId2emissionsInGrammPerType.put(personId, actualEmissions);// put newValue in the Map
			}	
		}
	}
	
	@Override
	/** emission calculation per link for passenger cars with a vehicle assigned **/
	public  void calculateEmissionsPerLink(
			double travelTime,
			Id linkId, 
			Id personId,
			double averageSpeed,
			int roadType,
			String fuelSizeAge, 
			double freeVelocity, 
			double distance,
			HbefaObject[][] hbefaTable,
			HbefaObject[][] hbefaHdvTable) {
		
		String[] hubSizeAgeArray = fuelSizeAge.split(";");
		
		String[] VehicleAttributes = new String[3];
		VehicleAttributes = calculateVehicleAttributes(hubSizeAgeArray);
		
		String[] keys = new String[4];
		Map<String, double[][]> hashOfPollutant = new TreeMap<String,double[][]>();
		
		for( String Pollutant : listOfPollutant ){
			for(int i = 0; i < 4; i++)
				keys[i] = makeKey(Pollutant, roadType, VehicleAttributes[0], VehicleAttributes[1], VehicleAttributes[2], i);
			// place 0 for freeFlow ....
			double[][] emissionsInFourSituations = new double[4][2];	
			for(int i = 0; i < 4; i++){
				try{
					emissionsInFourSituations[i][0]= this.hbefaHot.getHbefaHot().get(keys[i]).getV();
					emissionsInFourSituations[i][1]= this.hbefaHot.getHbefaHot().get(keys[i]).getEFA();
				} 
				catch(Exception e){
				}
			}
			// in the hashPfPollutant we save the V and EFA in 4 Situations
			hashOfPollutant.put(Pollutant, emissionsInFourSituations);	
		}
		// as result we have here a hashmap with the pollutant and an array as value with v and Efa
		int NumberOfPollutant = hashOfPollutant.size();
	
		/** get emissions calculated per event for the average speed approach or the fraction approach **/
		//double [] arrayOfemissionFactor =	emissionFactor.emissionAvSpeedCalculateDetailed(hashOfPollutant, averageSpeed, distance);
		double [] arrayOfemissionFactor = emissionFactor.emissionFractionCalculateDetailed(hashOfPollutant, averageSpeed, distance);
		
		if(this.linkId2emissionsInGrammPerType.get(linkId) == null) {
			this.linkId2emissionsInGrammPerType.put(linkId, arrayOfemissionFactor);// data is read for the first time, doesn't need to be summed up per link
		}
		else{
			double [] actualEmissions = new double[NumberOfPollutant]; // new data is saved after summation
			double [] previousEmissions = this.linkId2emissionsInGrammPerType.get(linkId); //oldValue is the previous sum
			for(int i = 0; i < actualEmissions.length ; i++){
				actualEmissions[i] = previousEmissions[i] + arrayOfemissionFactor[i];
			}
			linkId2emissionsInGrammPerType.put(linkId, actualEmissions);// put newValue in the Map
		}
	}
	
	@Override
	/** emission calculation per person for passenger cars with a vehicle assigned **/
	public synchronized void calculateEmissionsPerPerson(double travelTime,
			Id personId, 
			double averageSpeed,
			int roadType,
			String fuelSizeAge, 
			double freeVelocity, 
			double distance,
			HbefaObject[][] hbefaTable,
			HbefaObject[][] hbefaHdvTable) {
		
		String[] hubSizeAgeArray = fuelSizeAge.split(";");
		String[] VehicleAttributes = new String[3];
		VehicleAttributes = calculateVehicleAttributes(hubSizeAgeArray);
		
		// make 4 keys for each person 
		String[] keys = new String[4];
		Map<String, double[][]> hashOfPollutant = new TreeMap<String,double[][]>();
		for( String Pollutant : listOfPollutant ){
			for(int i = 0; i < 4; i++)
				keys[i] = makeKey(Pollutant, roadType, VehicleAttributes[0], VehicleAttributes[1], VehicleAttributes[2], i);
			// place 0 for freeFlow ....
			double[][] emissionsInFourSituations = new double[4][2];	
			for(int i = 0; i < 4; i++){
				try{
					emissionsInFourSituations[i][0] = this.hbefaHot.getHbefaHot().get(keys[i]).getV();
					emissionsInFourSituations[i][1] = this.hbefaHot.getHbefaHot().get(keys[i]).getEFA();
				} 
				catch(Exception e){
				}
			}
			// in the hashOfPollutant we save the V and EFA in 4 Situations
			hashOfPollutant.put(Pollutant, emissionsInFourSituations);
		}
		// as result we have here a hashmap with the pollutant and an array with value v and EFA
		int NumberOfPollutant = hashOfPollutant.size();

		/** get emissions calculated per event for the average speed approach or the fraction approach **/
		// double [] arrayOfemissionFactor = emissionFactor.emissionAvSpeedCalculateDetailed(hashOfPollutant, averageSpeed, distance);
		double [] arrayOfemissionFactor = emissionFactor.emissionFractionCalculateDetailed(hashOfPollutant, averageSpeed, distance);

		if(this.personId2emissionsInGrammPerType.get(personId) == null) {
			this.personId2emissionsInGrammPerType.put(personId, arrayOfemissionFactor); // data is read for the first time, doesn't need to be summed up per link
			
//			if(personId.equals(new IdImpl("556128.2#3625"))){
//				logger.warn(arrayOfemissionFactor[0]);
//			}
			
		}
		else{
			double [] actualEmissions = new double[NumberOfPollutant]; // new data is saved after summation
			double [] previousEmissions = this.personId2emissionsInGrammPerType.get(personId); // oldValue is the previous sum
			for(int i = 0; i < actualEmissions.length ; i++){
				actualEmissions[i] = previousEmissions[i] + arrayOfemissionFactor[i];
			}
			
//			if(personId.equals(new IdImpl("556128.2#3625"))){
//				logger.warn(arrayOfemissionFactor[0]);
//			}
			
			// put newValue in the Map
			personId2emissionsInGrammPerType.put(personId, actualEmissions);
		}
	}
	
	public synchronized void calculatePerLinkPtBikeWalk(
			Id linkId,
			Id personId) {
		
		double[] emissions = new double [5];
		emissions[0]=0.0;
		emissions[1]=0.0;
		emissions[2]=0.0;
		emissions[3]=0.0;
		emissions[4]=0.0;	
		
		if(this.linkId2emissionsInGrammPerType.get(linkId) == null) {
			this.linkId2emissionsInGrammPerType.put(linkId, emissions);
			// data is read for the first time, doesn't need to be summed up per link
		}
	}
	
	public synchronized void calculatePerPersonPtBikeWalk(
			Id personId,
			Id linkId) {
		
		double[] emissions = new double [5];
		emissions[0]=0.0;
		emissions[1]=0.0;
		emissions[2]=0.0;
		emissions[3]=0.0;
		emissions[4]=0.0;	
		
		if(this.personId2emissionsInGrammPerType.get(personId) == null) {
			this.personId2emissionsInGrammPerType.put(personId, emissions);// data is read for the first time, doesn't need to be summed up per link
		}

	}

	public void createRoadTypesTafficSituation(String filename) {
		
		int[] counter = new int[100];
		for(int i=0; i<100;i++)
			counter[i]=0;
		try{
			FileInputStream fstream = new FileInputStream(filename);
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine="";
			//Read File Line By Line
			br.readLine();

			while ((strLine = br.readLine()) != null){

				//for all lines (whole text) we split the line to an array 
				String[] array = strLine.split(";");
				int roadtype=Integer.valueOf(array[0]);
				int traficSitIndex = counter[roadtype]++;
	//			System.out.println(roadtype+";"+traficSitIndex);
				this.vehicleCharacteristic[roadtype][traficSitIndex] = array[3]; 	
		//		System.out.println("5.05" + this.vehicleCharacteristic);
			}
			in.close();
		}
		catch (Exception e){
			System.err.println("Error: " + e);
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

	public Map<Id, double[]> getWarmEmissionsPerLink() {
		return this.linkId2emissionsInGrammPerType;
	}
	
	public Map<Id, double[]> getWarmEmissionsPerLinkComHdvPec() {
		return this.linkIdComHdvPec2emissionsInGrammPerType;
	}

	public Map<Id, double[]> getWarmEmissionsPerPerson() {
		return this.personId2emissionsInGrammPerType;
	}
	
	public String makeKey(String pollutant, int roadType,String technology,String Sizeclass,String EmConcept,int traficSitNumber){
		return "PC[3.1]"+";"+"pass. car"+";"+"2010"+";"+";"+pollutant+";"+";"+this.vehicleCharacteristic[roadType][traficSitNumber]+";"+"0%"+";"+technology+";"+Sizeclass+";"+EmConcept+";";
	}

	// is used in order to split a phrase like baujahr:1900 , we are only interested in 1900 as Integer
	private int splitAndConvert(String str,String splittZeichen){
		
		String[] array = str.split(splittZeichen);
		return Integer.valueOf(array[1]);
	}
	
	private String[] calculateVehicleAttributes(String[] hubSizeAgeArray){
		
		String[] result = new String[3];
		int antriebsart=0;
		try{
			antriebsart =  Integer.valueOf(splitAndConvert(hubSizeAgeArray[1],":"));
		} catch(ArrayIndexOutOfBoundsException e){}
		
		result[0]="null"; //technology
		if (antriebsart==1) result[0]="petrol (4S)"; 	
		else if(antriebsart==2) result[0]="diesel";	
		else if (antriebsart==99998 && antriebsart==99999) result[0]="petrol (4S)";
		else result[0]="diesel";
		
		result[1]="null";
		int hubraum=0;
		try{
			hubraum = Integer.valueOf(splitAndConvert(hubSizeAgeArray[2],":"));
		} catch(ArrayIndexOutOfBoundsException e){}
		
		if (hubraum <= 1400) result[1]="<1,4L";	
		else if(hubraum <= 2000 && hubraum > 1400) result[1]="1,4-<2L";
		else if(hubraum >2000 ) result[1]=">=2L";
		else if (hubraum == 99998 && hubraum ==99999) result[1]="1,4-<2L";
		else result[1]="1,4-<2L";
		
		result[2]="null";//emConcept
		int bauJahr=0;	
		try{
		
			bauJahr = Integer.valueOf(splitAndConvert(hubSizeAgeArray[0],":"));
		
		} catch(ArrayIndexOutOfBoundsException e){}
		
		if (bauJahr < 1993 && result[0].equals("petrol (4S)")) result[2]="PC-P-Euro-0";
		else if (bauJahr < 1993 && result[0].equals("diesel")) result[2]="PC-D-Euro-0";
		else if(bauJahr <1997 && result[0].equals("petrol (4S)")) result[2]="PC-P-Euro-1";
		else if(bauJahr <1997 && result[0].equals("diesel")) result[2]="PC-D-Euro-1";
		else if(bauJahr <2001 && result[0].equals("petrol (4S)") ) result[2]="PC-P-Euro-2";
		else if(bauJahr <2001 && result[0].equals("diesel") ) result[2]="PC-D-Euro-2";
		else if(bauJahr <2006 && result[0].equals("petrol (4S)")) result[2]="PC-P-Euro-3";
		else if(bauJahr <2006 && result[0].equals("diesel")) result[2]="PC-D-Euro-3";
		else if(bauJahr <2011 && result[0].equals("petrol (4S)") ) result[2]="PC-P-Euro-4";
		else if(bauJahr <2011 && result[0].equals("diesel") ) result[2]="PC-D-Euro-4";
		else if(bauJahr <2015 && result[0].equals("petrol (4S)") ) result[2]="PC-P-Euro-5";
		else if(bauJahr <2015 && result[0].equals("diesel") ) result[2]="PC-D-Euro-5";
		else if (bauJahr==99998 && bauJahr==99999 && result[0].equals("diesel")) result[2]="PC-D-Euro-2";
		else if (bauJahr==99998 && bauJahr==99999 && result[0].equals("petrol (4S)")) result[2]="PC-P-Euro-2";
		else if (result[0].equals("petrol (4S)")) result[2]="PC-P-Euro-2";
		else if (result[0].equals("diesel")) result[2]="PC-D-Euro-2";

		return result;
	}
}