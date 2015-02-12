package playground.artemc.smartCardDataTools.zoneMerger;

import java.util.ArrayList;

public class Zone {
	Integer zoneNumber;
	Integer observations;
//	Integer newZoneNumber;
	ArrayList<Integer> adjacentZones = new ArrayList<Integer>();
	ArrayList<Integer> zonesMergedTogether = new ArrayList<Integer>();
	
	public Zone(String zoneToParse){
		
		String[] tokens = zoneToParse.split(",");
		zoneNumber = Integer.parseInt(tokens[0]);
		if(tokens.length>1){
			String[] adZones = (tokens[1].split(";"));
			for(String z:adZones){
				adjacentZones.add(Integer.parseInt(z));
			}
		}
		observations = 0;
//		newZoneNumber = zoneNumber;
		
	}
	
	
	public Integer getZoneNumber() {
		return zoneNumber;
	}

	public void setZoneNumber(Integer zoneNumber) {
		this.zoneNumber = zoneNumber;
	}
	
	
	public Integer getObservations() {
		return observations;
	}

	public void setObservations(Integer observations) {
		this.observations = observations;
	}


//	public Integer getNewZoneNumber() {
//		return newZoneNumber;
//	}
//
//
//	public void setNewZoneNumber(Integer newZoneNumber) {
//		this.newZoneNumber = newZoneNumber;
//	}


	public ArrayList<Integer> getAdjacentZones() {
		return adjacentZones;
	}
	
	public ArrayList<Integer> getZonesMergedWith() {
		return zonesMergedTogether;
	}


	public void setZonesMergedTogether(int value) {
		this.zonesMergedTogether.add(value);
	}
	
}
