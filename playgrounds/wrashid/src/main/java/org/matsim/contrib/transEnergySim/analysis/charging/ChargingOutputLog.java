/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.transEnergySim.analysis.charging;

import java.util.LinkedList;

public abstract class ChargingOutputLog {
	
	
	private LinkedList<ChargingLogRow> log;
	
	
	public ChargingOutputLog(){
		reset();
	}
	
	public void reset(){
		log=new LinkedList<ChargingLogRow>();
	}
	
	public void add(ChargingLogRow row){
		log.add(row);
	}
	
	public ChargingLogRow get(int i){
		return log.get(i);
	}
	
	public int getNumberOfEntries(){
		return log.size();
	}
	
	public abstract String getTitleRowFileOutput();
	
	
	
}
