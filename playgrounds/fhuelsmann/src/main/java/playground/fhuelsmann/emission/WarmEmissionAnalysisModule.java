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
import org.matsim.api.core.v01.Id;



import playground.fhuelsmann.emission.objects.HbefaObject;

import playground.fhuelsmann.emission.objects.VisumObject;

public class WarmEmissionAnalysisModule implements AnalysisModule{

	private VisumObject[] roadTypes = null;
	private EmissionsPerEvent emissionFactor = null;
	private String[][] vehicleCharacteristic = new String[100][4];
//	private int counter=0;
	private HbefaHot hbefaHot = new HbefaHot();
	private Map<Id, double[]> linkId2emissionsInGrammPerType = new TreeMap<Id,double[]>();
	private Map<Id, double[]> personId2emissionsInGrammPerType = new TreeMap<Id,double[]>();
	private Map<Id, double[]> commuterHdv2WarmEmissionsInGrammPerType = new TreeMap<Id,double[]>();
//	private Population population = null;



	public WarmEmissionAnalysisModule(VisumObject[] roadTypes, EmissionsPerEvent emissionFactor, HbefaHot hbefahot) {
		this.roadTypes = roadTypes;
		this.emissionFactor = emissionFactor;
		this.hbefaHot = hbefahot;
	}

	public  String findHbefaFromVisumRoadType(int roadType){
		return this.roadTypes[roadType].getHBEFA_RT_NR();
	}

	@Override
	/** emission calculation per link for heavy duty vehicles, passenger cars without a vehicle assigned and commuter traffic (pt and car) **/
	public void calculateEmissionsPerLinkForComHdvPecWithoutVeh(double travelTime, Id linkId, Id personId, double averageSpeed, 
			int roadType, double freeVelocity, double distance, HbefaObject[][] hbefaTable, HbefaObject[][] hbefaHdvTable) {
		
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
		
		//linkage between Hbefa road types and Visum road types
		int hbefaRoadType = Integer.valueOf(findHbefaFromVisumRoadType(roadType));
		//get emissions calculated per event differentiated by fraction and average speed approach
		double [] inputForEmissions = emissionFactor.collectInputForEmission(hbefaRoadType, averageSpeed, distance, hbefaTable);
		double [] inputForHdvEmissions = emissionFactor.collectInputForEmission(hbefaRoadType, averageSpeed, distance, hbefaHdvTable);
		//if no link in the map
		if(  !personId.toString().contains("gv_")){
			
			if (!personId.toString().contains("pv_pt")) {
				if(this.linkId2emissionsInGrammPerType.get(linkId) == null) {
					this.linkId2emissionsInGrammPerType.put(linkId, inputForEmissions);// data is read for the first time, doesn't need to be summed up per link
					}else{
						double [] actualEmissions = new double[10]; // new data is saved after summation
						double [] previousEmissions = this.linkId2emissionsInGrammPerType.get(linkId);
						for(int i = 0; i < actualEmissions.length ; i++)
						actualEmissions[i]= previousEmissions[i] + inputForEmissions[i];									
						linkId2emissionsInGrammPerType.put(linkId, actualEmissions);
						
					}}else {
					
						if(this.linkId2emissionsInGrammPerType.get(linkId) == null) {
							this.linkId2emissionsInGrammPerType.put(linkId, inputForEmissions);// data is read for the first time, doesn't need to be summed up per link
						}else{
							double [] actualEmissions = new double[10]; // new data is saved after summation
						//	double [] previousEmissions = this.commuterHdv2WarmEmissionsInGrammPerType.get(linkId);
							for(int i = 0; i < actualEmissions.length ; i++){
								actualEmissions[i]= 0.0;
								}
							// put newValue in the Map
							linkId2emissionsInGrammPerType.put(linkId, actualEmissions);
							}
						}
			}
		else {
			if(this.linkId2emissionsInGrammPerType.get(linkId) == null) {
				this.linkId2emissionsInGrammPerType.put(linkId, inputForHdvEmissions);// data is read for the first time, doesn't need to be summed up per link
			}
			else{
				double [] actualEmissions = new double[10]; // new data is saved after summation
				double [] previousEmissions = this.linkId2emissionsInGrammPerType.get(linkId); // previousEmissions is the previous sum
				for(int i = 0; i < actualEmissions.length ; i++){
					actualEmissions[i]= previousEmissions[i] + inputForHdvEmissions[i];
					//System.out.println("PersonID "+ personId);
				}
				// put newValue in the Map
				linkId2emissionsInGrammPerType.put(linkId, actualEmissions);
			}
		}
	}
	
	
	/** emission calculation per person for heavy duty vehicles, passenger cars without a vehicle assigned and commuter traffic (pt and car) **/
	public void calculateEmissionsPerCommuterHdvPcWithoutVeh(double travelTime, Id personId, double averageSpeed, int roadType, double freeVelocity, double distance, HbefaObject[][] hbefaTable,HbefaObject[][] hbefaHdvTable) {
		
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
		
		//linkage between Hbefa road types and Visum road types
		int hbefaRoadType = Integer.valueOf(findHbefaFromVisumRoadType(roadType));
		//get emissions calculated per event differentiated by fraction and average speed approach
		double [] inputForEmissions = emissionFactor.collectInputForEmission(hbefaRoadType, averageSpeed, distance,hbefaTable);
		double [] inputForHdvEmissions = emissionFactor.collectInputForEmission(hbefaRoadType, averageSpeed, distance, hbefaHdvTable);
		
		if(  !personId.toString().contains("gv_")){
		
					if (!personId.toString().contains("pv_pt")) {
						if(this.personId2emissionsInGrammPerType.get(personId) == null) {
							this.personId2emissionsInGrammPerType.put(personId, inputForEmissions);// data is read for the first time, doesn't need to be summed up per link
							}else{
								double [] actualEmissions = new double[10]; // new data is saved after summation
								double [] previousEmissions = this.personId2emissionsInGrammPerType.get(personId);
								for(int i = 0; i < actualEmissions.length ; i++)
								actualEmissions[i]= previousEmissions[i] + inputForEmissions[i];									
								personId2emissionsInGrammPerType.put(personId, actualEmissions);
								
							}}else {
							
								if(this.personId2emissionsInGrammPerType.get(personId) == null) {
									this.personId2emissionsInGrammPerType.put(personId, inputForEmissions);// data is read for the first time, doesn't need to be summed up per link
								}else{
									double [] actualEmissions = new double[10]; // new data is saved after summation
								//	double [] previousEmissions = this.commuterHdv2WarmEmissionsInGrammPerType.get(personId);
									for(int i = 0; i < actualEmissions.length ; i++){
										actualEmissions[i]= 0.0;
										}
									// put newValue in the Map
									personId2emissionsInGrammPerType.put(personId, actualEmissions);
									}
								}
					}else{
						if(this.personId2emissionsInGrammPerType.get(personId) == null) {
							this.personId2emissionsInGrammPerType.put(personId, inputForHdvEmissions);// data is read for the first time, doesn't need to be summed up per link
							}else{
				
								double [] actualEmissions = new double[10]; // new data is saved after summation
								double [] previousEmissions = this.personId2emissionsInGrammPerType.get(personId); //oldValue is the previous sum
								for(int i = 0; i < actualEmissions.length ; i++){
				
									actualEmissions[i]= previousEmissions[i] + inputForHdvEmissions[i];
					}
				// put newValue in the Map
								personId2emissionsInGrammPerType.put(personId, actualEmissions);
				//	System.out.println("System current time"+System.currentTimeMillis());
				}	
			}
	}
	
	public void calculateEmissionsPerLink(
			double travelTime,
			Id linkId, 
			Id personId,
			double averageSpeed,
			int roadType,
			String fuelSizeAge, 
			double freeVelocity, 
			double distance,
			HbefaObject[][] hbefaTable,
			HbefaObject[][] hbefaHdvTable,
			ArrayList<String> listOfPollutant) {
		
		String[] hubSizeAgeArray =fuelSizeAge.split(";");
	
		int antriebsart=0;
		try{
			antriebsart =  Integer.valueOf(splitAndConvert(hubSizeAgeArray[1],":"));
		} catch(ArrayIndexOutOfBoundsException e){}
		
	//	System.out.print("*********** " + hubSizeAgeArray[0]+" "+hubSizeAgeArray[1]+" "+hubSizeAgeArray[2]);
		
		String technology="null";
		if (antriebsart==1) technology="petrol (4S)"; 	
		else if(antriebsart==2) technology="diesel";	
		else if (antriebsart==99998 && antriebsart==99999) technology="petrol (4S)";
		else technology="diesel";
		
		String sizeClass="null";
		int hubraum=0;
		try{
			hubraum = Integer.valueOf(splitAndConvert(hubSizeAgeArray[2],":"));
		} catch(ArrayIndexOutOfBoundsException e){}
		
		if (hubraum <= 1400) sizeClass="<1,4L";	
		else if(hubraum <= 2000 && hubraum > 1400) sizeClass="1,4-<2L";
		else if(hubraum >2000 ) sizeClass=">=2L";
		else if (hubraum == 99998 && hubraum ==99999) sizeClass="1,4-<2L";
		else sizeClass="1,4-<2L";
		
		String emConcept="null";
		int bauJahr=0;	
		try{
		
			bauJahr = Integer.valueOf(splitAndConvert(hubSizeAgeArray[0],":"));
		
		} catch(ArrayIndexOutOfBoundsException e){}
		
		if (bauJahr < 1993 && technology.equals("petrol (4S)")) emConcept="PC-P-Euro-0";
		else if (bauJahr < 1993 && technology.equals("diesel")) emConcept="PC-D-Euro-0";
		else if(bauJahr <1997 && technology.equals("petrol (4S)")) emConcept="PC-P-Euro-1";
		else if(bauJahr <1997 && technology.equals("diesel")) emConcept="PC-D-Euro-1";
		else if(bauJahr <2001 && technology.equals("petrol (4S)") ) emConcept="PC-P-Euro-2";
		else if(bauJahr <2001 && technology.equals("diesel") ) emConcept="PC-D-Euro-2";
		else if(bauJahr <2006 && technology.equals("petrol (4S)")) emConcept="PC-P-Euro-3";
		else if(bauJahr <2006 && technology.equals("diesel")) emConcept="PC-D-Euro-3";
		else if(bauJahr <2011 && technology.equals("petrol (4S)") ) emConcept="PC-P-Euro-4";
		else if(bauJahr <2011 && technology.equals("diesel") ) emConcept="PC-D-Euro-4";
		else if(bauJahr <2015 && technology.equals("petrol (4S)") ) emConcept="PC-P-Euro-5";
		else if(bauJahr <2015 && technology.equals("diesel") ) emConcept="PC-D-Euro-5";
		else if (bauJahr==99998 && bauJahr==99999 && technology.equals("diesel")) emConcept="PC-D-Euro-2";
		else if (bauJahr==99998 && bauJahr==99999 && technology.equals("petrol (4S)")) emConcept="PC-P-Euro-2";
		else if (technology.equals("petrol (4S)")) emConcept="PC-P-Euro-2";
		else if (technology.equals("diesel")) emConcept="PC-D-Euro-2";
	
		// make 4 keys for each person 
		
		
		String[] keys = new String[4];
		Map<String, double[][]> hashOfPollutant = new TreeMap<String,double[][]>();
		for( String Pollutant : listOfPollutant ){
			for(int i=0; i<4;i++)
				keys[i]=makeKey(Pollutant,roadType, technology, sizeClass, emConcept, i);
		
			// place 0 for freeFlow ....
			double[][] emissionsInFourSituations = new double[4][2];	
			for(int i=0; i<4;i++){
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
		double [] arrayOfemissionFactor =	emissionFactor.emissionFractionCalculateDetailed(hashOfPollutant, averageSpeed, distance);
	
		if(this.linkId2emissionsInGrammPerType.get(linkId) == null) {
			this.linkId2emissionsInGrammPerType.put(linkId, arrayOfemissionFactor);// data is read for the first time, doesn't need to be summed up per link
		}
		else{
			double [] actualEmissions = new double[NumberOfPollutant]; // new data is saved after summation
			double [] previousEmissions = this.linkId2emissionsInGrammPerType.get(linkId); //oldValue is the previous sum
			for(int i = 0; i < actualEmissions.length ; i++){
				actualEmissions[i]= previousEmissions[i] + arrayOfemissionFactor[i];
			}
			// put newValue in the Map
			linkId2emissionsInGrammPerType.put(linkId, actualEmissions);
		}
	}
	
	
	@Override
	/** emission calculation per person for passenger cars with a vehicle assigned **/
	public void calculateEmissionsPerPerson(double travelTime,
			Id personId, 
			double averageSpeed,
			int roadType,
			String fuelSizeAge, 
			double freeVelocity, 
			double distance,
			HbefaObject[][] hbefaTable,
			HbefaObject[][] hbefaHdvTable,
			ArrayList<String> listOfPollutant) {
		
		String[] hubSizeAgeArray =fuelSizeAge.split(";");

		int antriebsart=0;
		try{
			antriebsart =  Integer.valueOf(splitAndConvert(hubSizeAgeArray[1],":"));
		} catch(ArrayIndexOutOfBoundsException e){}
		
	//	System.out.print("*********** " + hubSizeAgeArray[0]+" "+hubSizeAgeArray[1]+" "+hubSizeAgeArray[2]);
		
		String technology="null";
		if (antriebsart==1) technology="petrol (4S)"; 	
		else if(antriebsart==2) technology="diesel";	
		else if (antriebsart==99998 && antriebsart==99999) technology="petrol (4S)";
		else technology="diesel";
		
		String sizeClass="null";
		int hubraum=0;
		try{
			hubraum = Integer.valueOf(splitAndConvert(hubSizeAgeArray[2],":"));
		} catch(ArrayIndexOutOfBoundsException e){}
		
		if (hubraum <= 1400) sizeClass="<1,4L";	
		else if(hubraum <= 2000 && hubraum > 1400) sizeClass="1,4-<2L";
		else if(hubraum >2000 ) sizeClass=">=2L";
		else if (hubraum == 99998 && hubraum ==99999) sizeClass="1,4-<2L";
		else sizeClass="1,4-<2L";
		
		String emConcept="null";
		int bauJahr=0;	
		try{
		
			bauJahr = Integer.valueOf(splitAndConvert(hubSizeAgeArray[0],":"));
		
		} catch(ArrayIndexOutOfBoundsException e){}
		
		if (bauJahr < 1993 && technology.equals("petrol (4S)")) emConcept="PC-P-Euro-0";
		else if (bauJahr < 1993 && technology.equals("diesel")) emConcept="PC-D-Euro-0";
		else if(bauJahr <1997 && technology.equals("petrol (4S)")) emConcept="PC-P-Euro-1";
		else if(bauJahr <1997 && technology.equals("diesel")) emConcept="PC-D-Euro-1";
		else if(bauJahr <2001 && technology.equals("petrol (4S)") ) emConcept="PC-P-Euro-2";
		else if(bauJahr <2001 && technology.equals("diesel") ) emConcept="PC-D-Euro-2";
		else if(bauJahr <2006 && technology.equals("petrol (4S)")) emConcept="PC-P-Euro-3";
		else if(bauJahr <2006 && technology.equals("diesel")) emConcept="PC-D-Euro-3";
		else if(bauJahr <2011 && technology.equals("petrol (4S)") ) emConcept="PC-P-Euro-4";
		else if(bauJahr <2011 && technology.equals("diesel") ) emConcept="PC-D-Euro-4";
		else if(bauJahr <2015 && technology.equals("petrol (4S)") ) emConcept="PC-P-Euro-5";
		else if(bauJahr <2015 && technology.equals("diesel") ) emConcept="PC-D-Euro-5";
		else if (bauJahr==99998 && bauJahr==99999 && technology.equals("diesel")) emConcept="PC-D-Euro-2";
		else if (bauJahr==99998 && bauJahr==99999 && technology.equals("petrol (4S)")) emConcept="PC-P-Euro-2";
		else if (technology.equals("petrol (4S)")) emConcept="PC-P-Euro-2";
		else if (technology.equals("diesel")) emConcept="PC-D-Euro-2";

		// make 4 keys for each person 
		
		
		String[] keys = new String[4];
		Map<String, double[][]> hashOfPollutant = new TreeMap<String,double[][]>();
		for( String Pollutant : listOfPollutant ){
			for(int i=0; i<4;i++)
				keys[i]=makeKey(Pollutant,roadType, technology, sizeClass, emConcept, i);
			/*	if (personId.toString().contains("561367.2#11169")){
			try {
				System.err.println("*************" + keys[0] );
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}}*/

			// place 0 for freeFlow ....
			double[][] emissionsInFourSituations = new double[4][2];	
			for(int i=0; i<4;i++){
				try{
					emissionsInFourSituations[i][0]= this.hbefaHot.getHbefaHot().get(keys[i]).getV();
					emissionsInFourSituations[i][1]= this.hbefaHot.getHbefaHot().get(keys[i]).getEFA();
				} 
			
				catch(Exception e){
					//System.out.println("has no key in hbefaHot" + personId);
				}
			}
			// in the hashPfPollutant we save the V and EFA in 4 Situations
			hashOfPollutant.put(Pollutant, emissionsInFourSituations);	
		}

		// as result we have here a hashmap with the pollutant and an array as value with v and Efa
		int NumberOfPollutant = hashOfPollutant.size();

		/** get emissions calculated per event for the average speed approach or the fraction approach **/
		//double [] arrayOfemissionFactor =	emissionFactor.emissionAvSpeedCalculateDetailed(hashOfPollutant, averageSpeed, distance);
		double [] arrayOfemissionFactor =	emissionFactor.emissionFractionCalculateDetailed(hashOfPollutant, averageSpeed, distance);

		if(this.personId2emissionsInGrammPerType.get(personId) == null) {
			this.personId2emissionsInGrammPerType.put(personId, arrayOfemissionFactor);// data is read for the first time, doesn't need to be summed up per link
		}
		else{
			double [] actualEmissions = new double[NumberOfPollutant]; // new data is saved after summation
			double [] previousEmissions = this.personId2emissionsInGrammPerType.get(personId); //oldValue is the previous sum
			for(int i = 0; i < actualEmissions.length ; i++){
				actualEmissions[i]= previousEmissions[i] + arrayOfemissionFactor[i];
			}
			// put newValue in the Map
			personId2emissionsInGrammPerType.put(personId, actualEmissions);
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
}