/* *********************************************************************** *
 * project: org.matsim.*
 * ConvertOsmToMatsim.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.jjoubert.projects.wasteCollection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.facilities.Facility;

/**
 * @author jwjoubert
 *
 */
public class MclarpifMobsimAgent implements MobsimAgent {

	@Override
	public Id<Link> getCurrentLinkId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Id<Link> getDestinationLinkId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Id<Person> getId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public State getState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getActivityEndTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void endActivityAndComputeNextState(double now) {
		// TODO Auto-generated method stub

	}

	@Override
	public void endLegAndComputeNextState(double now) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setStateToAbort(double now) {
		// TODO Auto-generated method stub

	}

	@Override
	public Double getExpectedTravelTime() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Double getExpectedTravelDistance() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getMode() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void notifyArrivalOnLinkByNonNetworkMode(Id<Link> linkId) {
		// TODO Auto-generated method stub

	}

	@Override
	public Facility<? extends Facility<?>> getCurrentFacility() {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public Facility<? extends Facility<?>> getDestinationFacility() {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

}
