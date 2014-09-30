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

package playground.juliakern.toi;

public class Lane {
	
	String identifier;
	int numberOfForwardLanes;
	int numberOfBackwardLanes;
	private boolean toll; 
	

	public Lane(String[] lanetypeStr) {
		this.identifier = new String(lanetypeStr[0]);
		this.identifier = identifier.replace("1|#2", "1#2"); // typo in the input tabular?
		
		// toll
		setToll(false);
		if(identifier.contains("B")){
			setToll(true);
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
	}

	public Double getNumberOfBackLanes() {
		return 1.0*numberOfBackwardLanes;
	}

	public Double getNumberOfForwardLanes() {
		return 1.0*numberOfForwardLanes;
	}

	public boolean isToll() {
		return toll;
	}

	public void setToll(boolean toll) {
		this.toll = toll;
	}


}
