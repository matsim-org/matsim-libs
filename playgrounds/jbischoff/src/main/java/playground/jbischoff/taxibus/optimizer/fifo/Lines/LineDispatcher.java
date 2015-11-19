/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.jbischoff.taxibus.optimizer.fifo.Lines;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.data.Vehicle;

import playground.jbischoff.taxibus.passenger.TaxibusRequest;

/**
 * @author jbischoff
 *
 */

public class LineDispatcher implements ActivityStartEventHandler {

	private static final Logger log = Logger.getLogger(LineDispatcher.class);
	private Map<Id<TaxibusLine>, TaxibusLine> lines = new HashMap<>();
	private Map<Id<Link>, Id<TaxibusLine>> holdingPositions = new HashMap<>();
	private final MatsimVrpContext context;

	public LineDispatcher(MatsimVrpContext context) {
		this.context = context;
	}

	public void addLine(TaxibusLine line) {
		this.lines.put(line.getId(), line);

		if (this.holdingPositions.containsKey(line.getHoldingPosition())) {
			throw new RuntimeException("no more than one line should hold on one link for the time being");
		}
		this.holdingPositions.put(line.getHoldingPosition(), line.getId());
	}

	public Map<Id<TaxibusLine>, TaxibusLine> getLines() {
		return lines;
	}

	public TaxibusLine findLineForRequest(TaxibusRequest req) {
		for (TaxibusLine line : this.lines.values()) {
			if (line.lineServesRequest(req)) {
				return line;
			}
		}
		log.error("no taxibus line was found for request: Person " + req.getPassenger().getId().toString() + " from "
				+ req.getFromLink().getId().toString() + " to " + req.getToLink().getId().toString());
		return null;
	}

	@Override
	public void reset(int iteration) {
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		
		if ((event.getActType().startsWith("Stay")) || (event.getActType().startsWith("Before schedule"))) {

			if (this.holdingPositions.containsKey(event.getLinkId())) {
				Id<TaxibusLine> lineId = this.holdingPositions.get(event.getLinkId());
				Id<Vehicle> busId = Id.create(event.getPersonId(), Vehicle.class);
				if (context.getVrpData().getVehicles().containsKey(busId)) {
					Vehicle veh = context.getVrpData().getVehicles().get(busId);
					this.lines.get(lineId).addVehicleToHold(veh);
					log.info(veh.getId() + " on hold for line "+lineId);
				} else {
					log.error("bus not found? " + event.getPersonId());
				}

			}
		}
	}

}
