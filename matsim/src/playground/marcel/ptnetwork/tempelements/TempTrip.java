/* *********************************************************************** *
 * project: org.matsim.*
 * TempTrip.java
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

public class TempTrip{
	public String fzpid;
	public TempFZP fzp;
	public int deptime;
	public long id;
	public boolean passengerTrip=true;
	
	public TempTrip(){
		super();
	}
	public TempTrip(long newid,int newdeptime,String newfzpid){
		this.fzpid=newfzpid;
		this.deptime=newdeptime;
		this.id=newid;
		this.passengerTrip=true;
	}
}
