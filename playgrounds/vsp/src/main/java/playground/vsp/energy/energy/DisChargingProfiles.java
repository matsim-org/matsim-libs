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
package playground.vsp.energy.energy;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;

/**
 * @author droeder
 *
 */
public class DisChargingProfiles {
	public static final Id<DisChargingProfile> NONE = Id.create("none", DisChargingProfile.class);

	private HashMap<Id<DisChargingProfile>, DisChargingProfile> profiles;

	public DisChargingProfiles(){
		this.profiles = new HashMap<Id<DisChargingProfile> , DisChargingProfile>();
	}
	
	public void addValue(Id<DisChargingProfile> id, Double slope, Double speed, double usagePerKm){
		if(!this.profiles.containsKey(id)){
			this.profiles.put(id, new DisChargingProfile(id));
		}
		this.profiles.get(id).addNewEntry(slope, speed, usagePerKm);
	}
	
	public Double getJoulePerKm(Id<DisChargingProfile> id, Double  speed, Double slope){
		if(this.profiles.containsKey(id)){
			return this.profiles.get(id).getNewState(speed, slope);
		}else{
			return 0.0;
		}
	}
	
//	//TODO probably own reader
//	public void readAndAddDataFromFile(String inputFile){
//		Set<String[]> values = DaFileReader.readFileContent(inputFile, "\t", true);
//		
//		for(String[] s: values){
//			this.addValue(Id.create(s[0]), Double.parseDouble(s[2]), Double.parseDouble(s[1]), Double.parseDouble(s[3]));
//		}
//	}
	
	@Override
	public String toString(){
		StringBuffer b = new StringBuffer();
		
		for(DisChargingProfile p: this.profiles.values()){
			b.append(p.toString());
			b.append("\n");
		}
		
		return b.toString();
	}
	
	public static void main(String[] args){
		String inputFile =  "C:/Users/Daniel/Desktop/Dokumente_MATSim_AP1und2/DrivingLookupTable_2011-11-25.txt";
		DisChargingProfiles profiles = EmobEnergyProfileReader.readDisChargingProfiles(inputFile);
		
		System.out.println(profiles.toString());
		System.out.println(profiles.getJoulePerKm(Id.create("LOW", DisChargingProfile.class), 21.773336, 22.));
		System.out.println(profiles.getJoulePerKm(Id.create("LOW", DisChargingProfile.class), 21.773336, 20.));
	}
}
