/* *********************************************************************** *
 * project: org.matsim.*
 * SwitchingJointQSimConfigGroup.java
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
package playground.thibautd.socnetsim.qsim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;

import playground.thibautd.pseudoqsim.DeactivableTravelTimeProvider;
import playground.thibautd.pseudoqsim.PseudoSimConfigGroup;
import playground.thibautd.socnetsim.GroupReplanningConfigGroup;

/**
 * @author thibautd
 */
public class SwitchingJointQSimFactory implements MobsimFactory, IterationStartsListener {
	private static final Logger log =
		Logger.getLogger(SwitchingJointQSimFactory.class);

	private int iteration = Integer.MIN_VALUE;
	private final DeactivableTravelTimeProvider travelTime;

	public SwitchingJointQSimFactory(
			final DeactivableTravelTimeProvider travelTime) {
		this.travelTime = travelTime;
	}

	@Override
	public Mobsim createMobsim(
			final Scenario sc,
			final EventsManager eventsManager) {
		final PseudoSimConfigGroup config = (PseudoSimConfigGroup)
			sc.getConfig().getModule(
					PseudoSimConfigGroup.GROUP_NAME );
		final GroupReplanningConfigGroup repl = (GroupReplanningConfigGroup) 
			sc.getConfig().getModule(
					GroupReplanningConfigGroup.GROUP_NAME );

		if ( iteration == Integer.MIN_VALUE ) throw new IllegalStateException( "undefined iteration" );

		if ( iteration < repl.getDisableInnovationAfterIter() &&
				config.isPSimIter( iteration ) ) {
			switch ( config.getPsimType() ) {
			case detailled:
				log.info( "Using detailled pseudo simulation for iteration "+iteration );
				return new JointPseudoSimFactory( travelTime.getLinkTravelTimes() ).createMobsim( sc , eventsManager );
			case teleported:
				log.info( "Using teleported pseudo simulation for iteration "+iteration );
				return new JointTeleportationSimFactory().createMobsim( sc , eventsManager );
			default:
				throw new RuntimeException( config.getPsimType().toString() );
			}
		}

		log.info( "Using physical simulation for iteration "+iteration );
		return new JointQSimFactory().createMobsim( sc , eventsManager );
	}

	@Override
	public void notifyIterationStarts(final IterationStartsEvent event) {
		iteration = event.getIteration();
	}
}

