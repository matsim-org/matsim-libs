/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2022 by the members listed in the COPYING,        *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package lsp.shipment;

import java.util.Comparator;

public final class ShipmentPlanElementComparator implements Comparator<ShipmentPlanElement>{

	public int compare(ShipmentPlanElement o1, ShipmentPlanElement o2) {
		if(o1.getStartTime() > o2.getStartTime()){
				return 1;	
		}
		if(o1.getStartTime() < o2.getStartTime()){
			return -1;
		}
		if(o1.getStartTime() == o2.getStartTime()) {
			if(o1.getEndTime() > o2.getEndTime()) {
				return 1;
			}
			if(o1.getEndTime() < o2.getEndTime()) {
				return -1;
			}
		}
		return 0;		
	}	

}
