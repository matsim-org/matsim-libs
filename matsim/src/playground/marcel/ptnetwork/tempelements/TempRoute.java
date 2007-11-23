/* *********************************************************************** *
 * project: org.matsim.*
 * TempRoute.java
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

package playground.marcel.ptnetwork.tempelements;

import java.util.ArrayList;

public class TempRoute{
	public TempLine line;
	public String id;
	public int direct=0;
	public ArrayList<TempRP> rps = new ArrayList<TempRP>();
	public ArrayList<TempFZP> fzps = new ArrayList<TempFZP>();
	public ArrayList<TempTrip> trips = new ArrayList<TempTrip>();
	
	public TempRoute(){
		super();
	}

	public TempRoute(String newid,int dir){
		this.id=newid;
		this.direct=dir;
	}
	
	public TempFZP getTempFZP(String id){
		for (TempFZP fzp : fzps) {
			if(fzp.id.equals(id)){
				return fzp;
			}
		}
		return null;
	}

	public TempRP getTempRP(int pos){
		for (TempRP rp : rps) {
			if(rp.pos==pos) {
				return rp;
			}
		}
		return null;
	}
	
}
