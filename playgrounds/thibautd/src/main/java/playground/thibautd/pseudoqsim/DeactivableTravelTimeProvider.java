/* *********************************************************************** *
 * project: org.matsim.*
 * DeactivableTravelTimeProvider.java
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
package playground.thibautd.pseudoqsim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleAbortsEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleAbortsEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.Wait2LinkEventHandler;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

/**
 * Necessary for "teleporting" QSim: otherwise, the TravelTimeCalculator
 * listen to no traffic, and assumes freeflow for the next re-routing and
 * PSim teleportation, which is terrible...
 *
 * @author thibautd
 */
public class DeactivableTravelTimeProvider implements LinkEnterEventHandler, LinkLeaveEventHandler, 
			Wait2LinkEventHandler, VehicleLeavesTrafficEventHandler, VehicleArrivesAtFacilityEventHandler, 
			VehicleAbortsEventHandler {
	private static final Logger log =
		Logger.getLogger(DeactivableTravelTimeProvider.class);

	private final ActiveIterationCriterion criterion;
	private final TravelTimeCalculator delegate;

	private boolean isListenning = true;

	public DeactivableTravelTimeProvider(
			final ActiveIterationCriterion criterion,
			final TravelTimeCalculator delegate) {
		this.criterion = criterion;
		this.delegate = delegate;
	}

	@Override
	public void handleEvent(LinkEnterEvent e) {
		if ( !isListenning ) return;
		delegate.handleEvent(e);
	}

	@Override
	public void handleEvent(LinkLeaveEvent e) {
		if ( !isListenning ) return;
		delegate.handleEvent(e);
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		if ( !isListenning ) return;
		delegate.handleEvent(event);
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		if ( !isListenning ) return;
		delegate.handleEvent(event);
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		if ( !isListenning ) return;
		delegate.handleEvent(event);
	}

	@Override
	public void handleEvent(VehicleAbortsEvent event) {
		if ( !isListenning ) return;
		delegate.handleEvent(event);
	}

	@Override
	public void reset(int iteration) {
		isListenning = criterion.isListenning( iteration );

		log.info( getClass().getSimpleName()+( isListenning ? " WILL" : " will NOT" )+
				" be listenning to events for iteration "+iteration+" using criterion "+criterion );

		delegate.reset(iteration);
	}

	public TravelTime getLinkTravelTimes() {
		return delegate.getLinkTravelTimes();
	}

	public static interface ActiveIterationCriterion {
		public boolean isListenning( int iteration );
	}

	public static class AllIterationsCriterion implements ActiveIterationCriterion {
		@Override
		public boolean isListenning(int iteration) {
			return true;
		}
	}

	public static class PSimIterationsCriterion implements ActiveIterationCriterion {
		private final PseudoSimConfigGroup config;

		public PSimIterationsCriterion(
				final PseudoSimConfigGroup config) {
			this.config = config;
		}

		public PSimIterationsCriterion(
				final Config config) {
			this( (PseudoSimConfigGroup) config.getModule( PseudoSimConfigGroup.GROUP_NAME ) );
		}

		@Override
		public boolean isListenning(int iteration) {
			return !config.isPSimIter( iteration );
		}

		@Override
		public String toString() {
			return "[PSimCritertion: psimType="+config.getPsimType()
				+"; period="+config.getPeriod()
				+"; nPsimIters="+config.getNPSimIters()+"]";
		}
	}
}

