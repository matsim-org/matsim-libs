/* *********************************************************************** *
 * project: org.matsim.*
 * EllipseSearch.java
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

/**
 * 
 */
package playground.ikaddoura.parkAndRide.pRstrategy;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import playground.ikaddoura.parkAndRide.pR.ParkAndRideFacility;

/**
 * @author Ihab
 *
 */
public class EllipseSearch {

	public List<ParkAndRideFacility> getPRFacilities(Network network, List<ParkAndRideFacility> prFacilities, Coord homeCoord, Coord workCoord) {
		
		List<ParkAndRideFacility> prFacilitiesInEllipse = new ArrayList<ParkAndRideFacility>();

		double xHomeToWork = homeCoord.getX() - workCoord.getX();
		double yHomeToWork = homeCoord.getY() - workCoord.getY();

		double distHomeToWork = getHyp(xHomeToWork, yHomeToWork);
		double addDistanceOtherDirection = 0;
		double majorAxis = distHomeToWork + (2 * addDistanceOtherDirection);
		double minorAxis = majorAxis / 2.0;

		double xCenter = Math.abs(homeCoord.getX() + workCoord.getX()) / 2.0;
		double yCenter = Math.abs(homeCoord.getY() + workCoord.getY()) / 2.0;
		System.out.println("xCenter: " + xCenter);
		System.out.println("yCenter: " + yCenter);
		
		for (ParkAndRideFacility pr : prFacilities){
			Id prId = pr.getPrLink3in();
			for (Link link : network.getLinks().values()){
				if (prId.toString().equals(link.getId().toString())){
					Coord prCoord = link.getToNode().getCoord();					
					double a = majorAxis / 2.0;
					double b = minorAxis / 2.0;
					double v = (Math.pow((prCoord.getX() - xCenter), 2) / Math.pow(a, 2)) + (Math.pow((prCoord.getY() - yCenter), 2) / Math.pow(b, 2));
					if ( v <= 1){
						System.out.println("ParkAndRideFacility " + prCoord + " liegt innerhalb der Ellipse. Adding to List...");
					} else {
						System.out.println("ParkAndRideFacility " + prCoord + " liegt außerhalb der Ellipse.");
					}
				}
			}
		}
		return prFacilitiesInEllipse;
	}

	private double getHyp(double a, double b) {
		double aSquare = Math.pow(a, 2);
		double bSquare = Math.pow(b, 2);
		return Math.sqrt(aSquare + bSquare);
	}

}
