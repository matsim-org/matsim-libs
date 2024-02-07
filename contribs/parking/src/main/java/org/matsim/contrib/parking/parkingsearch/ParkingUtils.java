/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.parking.parkingsearch;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author  jbischoff, tschlenther, Ricardo Ewert
 *
 */
public class ParkingUtils {

	static public final String ParkingStageInteractionType = "parking";
	static public final String ParkingActivityType = "parking_activity";
	static public final String WaitingForParkingActivityType = "waitingForParkingSpace_activity";
	static public final int NO_OF_LINKS_TO_GET_ON_ROUTE = 5;


	public ParkingUtils() {
	}

	public static Coord getRandomPointAlongLink(Random rnd, Link link){
		Coord fromNodeCoord = link.getFromNode().getCoord();
		Coord toNodeCoord = link.getToNode().getCoord();
		double r = rnd.nextDouble();

		double x = (fromNodeCoord.getX()*r)+(toNodeCoord.getX()*(1-r));
		double y = (fromNodeCoord.getY()*r)+(toNodeCoord.getY()*(1-r));

		return new Coord(x,y);
	}

	public static List<Coord> getEvenlyDistributedCoordsAlongLink(Link link, int numberOfCoords){
		Coord fromNodeCoord = link.getFromNode().getCoord();
		Coord toNodeCoord = link.getToNode().getCoord();
		double fX = fromNodeCoord.getX();
		double fY = fromNodeCoord.getY();
		double tX = toNodeCoord.getX();
		double tY = toNodeCoord.getY();
		double nrSlots = (double) numberOfCoords;

		if(fX == tX){
			double x;
			double yDistance = tY-fY;

			if(tY > fY) x = tX+10;
			else x = tX -10;

			List<Coord> points = new ArrayList<Coord>();
			if(numberOfCoords == 1){
				points.add(new Coord( x , (fY + 0.5*yDistance) ));
				return points;
			}

			if(Math.abs(yDistance) > (nrSlots+10) ){
				if(yDistance < -5 ) yDistance += 10;
				if(yDistance > 0) yDistance -= 10;
			}

			for(int i = 1; i <= numberOfCoords ; i ++){
				double y = (fY + (i*(1/nrSlots)*yDistance));
				points.add(new Coord(x,y));
			}
				return points;
		}
		else{
			// f(x) = mx + b

			// m = y2-y1/x2-x1
			double m = (tY-fY)/(tX-fX);

			//b = y-mx
			double b = fY - m*(fX);

			//displace
			if(m>0){
				if(tY>fY)	b -= 10;
				if(tY<fY)	b += 10;
			}
			if(m<0){
				if(tY>fY)	b += 10;
				if(tY<fY)	b -= 10;
			}
			if(m == 0){
				if(tX > fX) b -= 10;
				if(tX < fX) b += 10;
			}

			double xDistance = tX-fX;

			//calc Coords
			List<Coord> points = new ArrayList<Coord>();
			if(numberOfCoords == 1){
				double x = (fX + 0.5*xDistance);
				points.add(new Coord( x , (m*x+b) ));
				return points;
			}

			//distance to nodes
			if(Math.abs(xDistance) > (nrSlots+10) ){
				if(xDistance < -5 ) xDistance += 10;
				if(xDistance > 5) xDistance -= 10;
			}

			for(int i = 1; i <= numberOfCoords ; i ++){
				double x = (fX + (i*(1/nrSlots)*xDistance));
				double y = m*x+b;
				points.add(new Coord(x,y));
			}
				return points;
		}
	}

	public static List<Link> getOutgoingLinksForMode(Link link, String mode) {
		List<Link> outGoingModeLinks = new ArrayList();
		for (Link outLink : link.getToNode().getOutLinks().values()) {
			if (outLink.getAllowedModes().contains(mode)) outGoingModeLinks.add(outLink);
		}
		if (outGoingModeLinks.size() == 0) {
			throw new RuntimeException("could not find an outgoing link for mode " + mode +
					" from link " + link + ". Consequences are not checked. Please check the network. \n Aborting...");
		}
		return outGoingModeLinks;
	}

	/**
	 * Checks if the activity has parking while the activity.
	 *
	 * @param followingActivity
	 * @return
	 */
	public static boolean checkIfActivityHasNoParking(Activity followingActivity) {
        return followingActivity.getAttributes().getAsMap().containsKey("parking") && followingActivity.getAttributes().getAttribute(
			"parking").equals("noParking");

	}

	/**
	 * Sets that while this activity we simulate no parking activities.
	 *
	 * @param activity
	 */
	public static void setNoParkingForActivity(Activity activity) {
		activity.getAttributes().putAttribute("parking", "noParking");
	}

	/**
	 * This activity has a passenger interaction. This would mean that the location is fixed, and can not be changed.
	 *
	 * @param activity
	 */
	public static void setPassangerInteractionForActivity(Activity activity) {
		activity.getAttributes().putAttribute("parking", "PassangerInteraction");
	}

	/**
	 * Checks if the activity has a passanger interaction. This would mean that the location is fixed, and can not be changed.
	 *
	 * @param activity
	 * @return
	 */
	public static boolean checkIfActivityHasPassengerInteraction(Activity activity) {
		return activity.getAttributes().getAsMap().containsKey("parking") && activity.getAttributes().getAttribute(
			"parking").equals(
			"PassangerInteraction");
	}
}
