package playground.fhuelsmann.emission;
/* *********************************************************************** *
 * project: org.matsim.*
 * FhMain.java
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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;


public class ColdEmissionFactor {

	// coldDistance has Information about the sumDistance
	public Map<Id,Map<Integer,DistanceObject>> coldDistance;

	// parkingTime has Information about the stops times 
	public Map<Id,Map<Integer,ParkingTimeObject>> parkingTime;
	
	public Map<Id,Map<String,ObjectOfColdEF>> coldEmissionFactor
	= new TreeMap<Id,Map<String,ObjectOfColdEF>>();
	
	
	private final  Map<String,Map<Integer,Map<Integer,HbefaColdObject>>> hbefaColdTable ;


	public ColdEmissionFactor(
			Map<Id, Map<Integer, DistanceObject>> coldDistance,
			Map<Id, Map<Integer, ParkingTimeObject>> parkingTime,
			Map<String,Map<Integer,Map<Integer,HbefaColdObject>>> hbefaColdTable) {
		super();
		this.coldDistance = coldDistance;
		this.parkingTime = parkingTime;
		this.coldEmissionFactor = coldEmissionFactor;
		this.hbefaColdTable = hbefaColdTable;
	}


	public void  MapForColdEmissionFactor(){
		
		// visiting all cells of the both maps parallel (sorted after Person_ID).
		// the aim is to export the Information in order to use them for the calculation and
		// then to save the result in a new map which has been defined in the class with the name 
		// coldEmissionFactor
		
		// for each user, parkingTim_Id -> find the same user in coldDistance


		for(Entry<Id,Map<Integer,ParkingTimeObject>> parkingTime_id  : this.parkingTime.entrySet()){	
	
			// Access the Object in the 0-place, becasue he has the Information about the sumDistance
				ParkingTimeObject parkingTime_zeroCell = this.parkingTime.get(parkingTime_id.getKey()).get(0);
			
				Id person_id = parkingTime_id.getKey();
				double sumDistance=0.0;
				double parkingTime = parkingTime_zeroCell.getTimedifference();

				
				String actitityInParking_time = parkingTime_zeroCell.activity;
				String actitityInDistanceCold="NOT_FOUND";
				
				try{
					 sumDistance = this.coldDistance.get(person_id).get(0).getSumDistance();
					 actitityInDistanceCold =this.coldDistance.get(person_id).get(0).getActivity();
				} catch(Exception e){
					System.err.println(person_id +" PersonId or Id ind coldDistance not found");
					} 
				
				int dis=-1;
				
				if ((sumDistance/1000) <1.0  ) {
					dis =0;
				}
				else {
					dis=1;
				}
				
				int time =(int)(parkingTime/3600);
				if (time>12) time =12; 
				
			for(Entry<String,Map<Integer,Map<Integer,HbefaColdObject>>> component : this.hbefaColdTable.entrySet()){
				
				double coldEf = this.hbefaColdTable.get(component.getKey()).get(dis).get(time).getColdEF();
									
				ObjectOfColdEF obj = new ObjectOfColdEF(parkingTime+"",component.getKey(),this.coldDistance.get(person_id).get(0).getSumDistance(),coldEf);
			
				if (this.coldEmissionFactor.get(person_id) == null){
					
					 Map<String,ObjectOfColdEF> value = new TreeMap<String,ObjectOfColdEF>();
					 value.put(component.getKey(), obj);
					this.coldEmissionFactor.put(person_id, value);
				}// if /(this.coldEmissionFactor.get(person_id)!=null)
		
				else{
					this.coldEmissionFactor.get(person_id).put(component.getKey(),obj);
					 }	
				}
			}// second for
		}// first for


	public void printColdEmissionFactor() throws IOException{
		
		FileWriter fstream = 
			new FileWriter("../../detailedEval/teststrecke/sim/outputEmissions/outColdEmission.txt");
		BufferedWriter out = new BufferedWriter(fstream);
		out.write("PersonalId \t Componenet \t EmissionFactor  \t TimeDiffernce \t distance \n");

		System.out.println("PersonalId \t Componenet \t EmissionFactor  \tParktingTime \tDistance");

	for(Entry<String,Map<Integer,Map<Integer,HbefaColdObject>>> component : this.hbefaColdTable.entrySet()){

		for(Entry<Id,Map<String,ObjectOfColdEF>> object  : this.coldEmissionFactor.entrySet()){
			System.out.println(
					
					 object.getKey() 
					+ "\t" +object.getValue().get(component.getKey()).getComponent()
					+"\t"+ object.getValue().get(component.getKey()).getColdEf() 
					+"\t" + object.getValue().get(component.getKey()).getParktinTime() 
					+ "\t" + object.getValue().get(component.getKey()).getDistance()  +
							"\n");
			
			out.append(object.getKey() 
					+ "\t" +object.getValue().get(component.getKey()).getComponent()
					+"\t"+ object.getValue().get(component.getKey()).getColdEf() 
					+"\t" + object.getValue().get(component.getKey()).getParktinTime()
					+"\t" + object.getValue().get(component.getKey()).getDistance() + "\n");

		}
	}
	out.close();

	}
}


