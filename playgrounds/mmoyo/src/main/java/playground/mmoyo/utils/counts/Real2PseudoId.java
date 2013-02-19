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

package playground.mmoyo.utils.counts;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.mmoyo.utils.DataLoader;

abstract class Real2PseudoId {
	final static char point = '.';
	
	public static String convertRealIdtoPseudo(String strRealId){
		int pseudoLenght = strRealId.length();
		int pointIndex = strRealId.indexOf(point);
		if ( pointIndex > -1 ){
			pseudoLenght = pointIndex; 
		}
		return strRealId.substring(0, pseudoLenght);
	}
	
	public static void main(String[] args) {
		String scheduleFile = "../../";
		DataLoader dLoader = new DataLoader();
		TransitSchedule schedule = dLoader.readTransitSchedule(scheduleFile);
		
		String sp = " ";
		for (Id stopId : schedule.getFacilities().keySet()){
			String strStopId = stopId.toString();
			System.out.println(strStopId  + sp + convertRealIdtoPseudo(strStopId));
		}
		
	}
	
	
}
