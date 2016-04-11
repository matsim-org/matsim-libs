/* *********************************************************************** *
 * project: org.matsim.*
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


package playground.polettif.multiModalMap.mapping.container;


import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.List;

/**
 * Container for a Public Transport path between two stop facilities.
 * Provides methods for link candidates (i.e. start and end link of a path)
 * and the path between those links.
 *
 * @author polettif
 */
public interface PTPath {

	/**
	 * @return all link ids of the path, including the first and last link
	 */
	List<Id<Link>> getLinkIds();

	/**
	 * @return all link ids of the path, excluding the first and last link
	 */
	List<Id<Link>> getIntermediateLinkIds();

	/**
	 * @return all link ids of the path, excluding the last link
	 */
	List<Id<Link>> getLinkIdsExcludingToLink();

	/**
	 * @return all link ids of the path, excluding the first link
	 */
	List<Id<Link>> getLinkIdsExcludingFromLink();

	/**
	 * @return the first link of a path
	 */
	Link getFromLink();
	TransitStopFacility getFromStopFacility();

	/**
	 * @return the last link of a path
	 */
	Link getToLink();
	TransitStopFacility getToStopFacility();

	/**
	 * @return the total travel time on the path
	 */
	double getTravelTime();

	/**
	 * @return the travel time on the links excluding the first and last link
	 */
	double getInterTravelTime();

	/**
	 * @return the euclidian distance between the stopFacility and the first link
	 */
	double getDistanceStartFacilityToLink();

	/**
	 * @return the euclidian distance between the stopFacility and the last link
	 */
	double getDistanceEndFacilityToLink();

	Tuple<TransitStopFacility,TransitStopFacility> getStopPair();

	void addViaLink(Link link);

	List<Id<Link>> getLinkCandidateIds();


	/**
	 * checks if a path has u-turns
	 * @return
	 */
//	boolean hasUturns();

	/**
	 * checks if a path has loops (the same link is used twice)
	 * @return
	 */
//	boolean linkSequenceHasLoops();

}
