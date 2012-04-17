/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.droeder.eMobility.energy;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

/**
 * @author droeder
 *
 */
public class ChargingProfiles {
	
	public static final Id NONE = new IdImpl("none");
	
	private HashMap<Id, ChargingProfile> profiles;

	public ChargingProfiles(){
		this.profiles = new HashMap<Id , ChargingProfile>();
	}
	
	public void addValue(Id id, Double currentState, Double duration, double newState){
		if(!this.profiles.containsKey(id)){
			this.profiles.put(id, new ChargingProfile(id));
		}
		this.profiles.get(id).addNewEntry(currentState, duration, newState);
	}
	
	public Double getNewState(Id id, Double  duration, Double currentState){
		if(this.profiles.containsKey(id)){
			return this.profiles.get(id).getNewState(duration, currentState);
		}else{
			return currentState;
		}
	}
	
//	// TODO probably own reader
//	public void readAndAddDataFromFile(String inputFile){
//		Set<String[]> values = DaFileReader.readFileContent(inputFile, "\t", true);
//		
//		for(String[] s: values){
//			this.addValue(new IdImpl(s[0]), Double.parseDouble(s[1]), Double.parseDouble(s[2]), Double.parseDouble(s[3]));
//		}
//		
//	}
	
	@Override
	public String toString(){
		StringBuffer b = new StringBuffer();
		
		for(ChargingProfile p: this.profiles.values()){
			b.append(p.toString());
			b.append("\n");
		}
		
		return b.toString();
	}
	
	public static void main(String[] args){
		String inputFile =  "C:/Users/Daniel/Desktop/Dokumente_MATSim_AP1und2/ChargingLookupTable_2011-11-30.txt";
		ChargingProfiles profiles = EmobEnergyProfileReader.readChargingProfiles(inputFile);
		
//		System.out.println(profiles.toString());
		System.out.println(profiles.getNewState(new IdImpl("SLOW"), 60., 20.));
	}

}
