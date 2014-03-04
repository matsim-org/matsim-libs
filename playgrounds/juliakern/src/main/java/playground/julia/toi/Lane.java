/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.julia.toi;

public class Lane {
	
	String identifier;
	int numberOfForwardLanes;
	int numberOfBackwardLanes;
	boolean toll; 
	Double capacity;
	

	public Lane(String[] lanetypeStr) {
		this.identifier = new String(lanetypeStr[1]);
		this.identifier = identifier.replace("1|#2", "1#2"); // typo in the input tabular?
		
		// toll
		toll = false;
		if(identifier.contains("B")){
			toll=true;
			System.out.println("toll on link");
		}
		
		String ident = new String(identifier);
		
		// number of lanes
		ident = ident.replace("1|#2", "1#2"); // typo in the input tabular?
		ident = ident.replace("K", "");
		ident = ident.replace("O", "");
		ident = ident.replace("B", "");
		ident = ident.replace("V1", "");
		ident = ident.replace("H1", "");
		
		numberOfBackwardLanes = 0;
		numberOfForwardLanes = 0;
		
		String[] split = ident.split("#");
		
		for(String sub: split){
			try {
				int num = Integer.parseInt(sub);
				if(num%2==0){
					int back = num/2;
					if(back>numberOfBackwardLanes) this.numberOfBackwardLanes = back;
				}else{
					int forw = (num+1)/2;
					if(forw > numberOfForwardLanes) this.numberOfForwardLanes = forw;
				}
			} catch (NumberFormatException e) {
			}
		}
				
		// capacity
		this.capacity = Double.parseDouble(lanetypeStr[2]);
	}

	public Double getNumberOfBackLanes() {
		return 1.0*numberOfBackwardLanes;
	}

	public Double getNumberOfForwardLanes() {
		return 1.0*numberOfForwardLanes;
	}

	public Double getCapacity() {
		return capacity;
	}

}
