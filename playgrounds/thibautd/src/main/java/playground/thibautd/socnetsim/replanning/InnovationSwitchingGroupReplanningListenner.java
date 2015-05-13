/* *********************************************************************** *
 * project: org.matsim.*
 * InovationSwitchingGroupReplanningListenner.java
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
package playground.thibautd.socnetsim.replanning;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.log4j.Logger;
import org.matsim.analysis.IterationStopWatch;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.corelisteners.PlansReplanning;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.listener.ReplanningListener;

import playground.thibautd.pseudoqsim.PseudoSimConfigGroup;
import playground.thibautd.socnetsim.framework.replanning.GroupStrategyManager;
import playground.thibautd.socnetsim.framework.replanning.GroupStrategyRegistry;
import playground.thibautd.socnetsim.framework.replanning.grouping.GroupIdentifier;

import com.google.inject.BindingAnnotation;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author thibautd
 */
@Singleton
public class InnovationSwitchingGroupReplanningListenner implements ReplanningListener, PlansReplanning {
	private static final Logger log =
		Logger.getLogger(InnovationSwitchingGroupReplanningListenner.class);

	private final GroupStrategyManager mainStrategyManager;
	private final GroupStrategyManager innovativeStrategyManager;
	private final Scenario scenario;

	@Retention( RetentionPolicy.RUNTIME )
	@BindingAnnotation
	@Target( { ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.PARAMETER } )
	public @interface Innovative {}

	@Retention( RetentionPolicy.RUNTIME )
	@BindingAnnotation
	@Target( { ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.PARAMETER } )
	public @interface NonInnovative {}


	@Inject
	public InnovationSwitchingGroupReplanningListenner(
			final Scenario scenario,
			final IterationStopWatch stopWatch,
			final GroupIdentifier groups,
			final @Innovative GroupStrategyRegistry innovManager,
			final @NonInnovative GroupStrategyRegistry nonInnovManager) {
		this.mainStrategyManager = new GroupStrategyManager( stopWatch , groups , innovManager );
		this.innovativeStrategyManager = new GroupStrategyManager( stopWatch , groups , nonInnovManager );
		this.scenario = scenario;
	}


	@Override
	public void notifyReplanning(final ReplanningEvent event) {
		final PseudoSimConfigGroup config = (PseudoSimConfigGroup)
			scenario.getConfig().getModule(
					PseudoSimConfigGroup.GROUP_NAME );
		final GroupReplanningConfigGroup repl = (GroupReplanningConfigGroup) 
			scenario.getConfig().getModule(
					GroupReplanningConfigGroup.GROUP_NAME );

		if ( event.getIteration() < repl.getDisableInnovationAfterIter() &&
				config.isPSimIter( event.getIteration() ) ) {
			log.info( "performing INNOVATION ONLY iteration (for feeding PSim)" );
			innovativeStrategyManager.run(
				event.getReplanningContext(),
				scenario );
		}
		else {
			log.info( "performing normal iteration (with non-innovative strategies)" );
			mainStrategyManager.run(
					event.getReplanningContext(),
					scenario );
		}
	}
}
