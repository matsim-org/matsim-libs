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

package playground.jbischoff.taxibus.algorithm.optimizer.fifo.Lines;

import java.util.*;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.*;

import playground.jbischoff.taxibus.algorithm.passenger.TaxibusRequest;
import playground.jbischoff.taxibus.run.configuration.TaxibusConfigGroup;

/**
 * @author jbischoff
 *
 */

public class LineDispatcher implements ActivityStartEventHandler {

	private static final Logger log = Logger.getLogger(LineDispatcher.class);
	private Map<Id<TaxibusLine>, TaxibusLine> lines = new HashMap<>();
	private Map<Id<Link>, Id<TaxibusLine>> holdingPositions = new HashMap<>();
	private final TaxibusConfigGroup tbcg;
	private final LineBalanceMethod balanceMethod;
	enum LineBalanceMethod {SAME, RETURN, BALANCED};
	private final VrpData vrpData;
	
	public LineDispatcher(VrpData vrpData, TaxibusConfigGroup tbcg) {
		this.vrpData = vrpData;
		this.tbcg = tbcg;
		switch (this.tbcg.getBalancingMethod()){
		case "same":
			balanceMethod = LineBalanceMethod.SAME;
			break;
		case "return":
			balanceMethod = LineBalanceMethod.RETURN;
			break;
		case "balanced":
			balanceMethod = LineBalanceMethod.BALANCED;
			break;
		
		default:
			log.error("invalid balancing method set "+ tbcg.getBalancingMethod());
			throw new RuntimeException("invalid balancing method set "+ tbcg.getBalancingMethod());
		}
		
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
//		log.error("no taxibus line was found for request: Person " + req.getPassenger().getId().toString() + " from "
//				+ req.getFromLink().getId().toString() + " to " + req.getToLink().getId().toString());
		return null;
	}

	@Override
	public void reset(int iteration) {
		for (TaxibusLine line : this.lines.values()){
			line.reset();
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		
		if ((event.getActType().startsWith("Stay")) || (event.getActType().startsWith("Before schedule"))) {

			if (this.holdingPositions.containsKey(event.getLinkId())) {
				Id<TaxibusLine> lineId = this.holdingPositions.get(event.getLinkId());
				Id<Vehicle> busId = Id.create(event.getPersonId(), Vehicle.class);
				if (vrpData.getVehicles().containsKey(busId)) {
					Vehicle veh = vrpData.getVehicles().get(busId);
					this.lines.get(lineId).addVehicleToHold(veh);
//					log.info(veh.getId() + " on hold for line "+lineId);
				} else {
					log.error("bus not found? " + event.getPersonId());
				}

			}
		}
	}
	public boolean coordIsServedByLine(Coord coord){
		for (TaxibusLine line : this.lines.values()){
			if (line.lineCoversCoordinate(coord)) return true;
		}
		
		return false;
	}

	public Id<Link> calculateNextHoldingPointForTaxibus(Vehicle vehicle, Id<TaxibusLine> id) {
		switch (this.balanceMethod){
		case SAME:
			return this.lines.get(id).getHoldingPosition();
		case RETURN:
			return this.lines.get(this.lines.get(id).getReturnRouteId()).getHoldingPosition();
		case BALANCED:
		default:
			throw new RuntimeException("not yet implemented");
			
		}
	}

}
