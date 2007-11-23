/* *********************************************************************** *
 * project: org.matsim.*
 * TempFZP.java
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
import java.util.Iterator;


public class TempFZP{
	public String id;
	public ArrayList<TempFZPPkt> pkte = new ArrayList<TempFZPPkt>();
	public int direct = 0;
	
	public TempFZP() {
		super();
	}
	
	public TempFZP(String newid){
		this.id=newid;
	}
	
	public TempFZPPkt getTempFZPPkt(int pos){
		TempFZPPkt pkt = null;
		for(Iterator it = pkte.iterator(); it.hasNext();){
			TempFZPPkt pkt2 = (TempFZPPkt)it.next();
			if(pkt2.pos==pos){
				pkt = pkt2;
			}
		}
		return pkt;
	}
	
	public void trimInValidPkte(){
		for(int i=0;i<pkte.size();i++){
			if (pkte.get(i).passengerChange==false){
				/*
				 * if fzppkt is not a passenger stop, remove from route
				 * ttime and length is added to wtime of following
				 * */
				if(pkte.size()>(i+1)){
					pkte.get(i+1).wtime+=pkte.get(i).ttime;
					pkte.get(i+1).length+=pkte.get(i).length;
				}
				pkte.remove(i);
				i--;
			}		
		}
		for(int i=0;i<pkte.size();i++){
			if(pkte.size()>(i+1)){
				if(pkte.get(i+1).hp.getName().equals(pkte.get(i).hp.getName())){

					pkte.get(i+1).wtime+=pkte.get(i).ttime;
					pkte.get(i+1).length+=pkte.get(i).length;
					pkte.remove(i);
					i--;
				}
			}
		}
		for(int i=0;i<pkte.size();i++){
			pkte.get(i).hp_Id=pkte.get(i).hp.getHp_Id()+direct;
		}
	}
	
}