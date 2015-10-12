package playground.gregor.ctsim.simulation.physics;
/* *********************************************************************** *
 * project: org.matsim.*
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.vehicles.Vehicle;

/**
 * Created by laemmel on 12/10/15.
 */
public class CTVehicle implements CTPed {
	public CTVehicle(Id<Vehicle> vehicleId, MobsimDriverAgent agent, CTLink link) {

	}

	@Override
	public double getDesiredDir() {
		return 0;
	}

	@Override
	public CTCell getNextCellAndJump() {
		return null;
	}

	@Override
	public CTCell getTentativeNextCell() {
		return null;
	}

	@Override
	public void setTentativeNextCell(CTCell tentativeNextCell) {

	}

	@Override
	public void notifyMoveOverNode() {

	}

	@Override
	public Id<Link> getNextLinkId() {
		return null;
	}
}
