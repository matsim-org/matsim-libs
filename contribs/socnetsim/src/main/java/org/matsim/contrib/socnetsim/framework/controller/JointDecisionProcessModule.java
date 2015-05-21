/* *********************************************************************** *
 * project: org.matsim.*
 * JointDecisionProcessModule.java
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
package org.matsim.contrib.socnetsim.framework.controller;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.corelisteners.DumpDataAtEnd;
import org.matsim.core.controler.corelisteners.PlansReplanning;
import org.matsim.core.controler.corelisteners.PlansScoring;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.contrib.socnetsim.framework.PlanRoutingAlgorithmFactory;
import org.matsim.contrib.socnetsim.framework.controller.listeners.DumpJointDataAtEnd;
import org.matsim.contrib.socnetsim.framework.controller.listeners.GroupReplanningListenner;
import org.matsim.contrib.socnetsim.framework.controller.listeners.JointPlansDumping;
import org.matsim.contrib.socnetsim.framework.events.CourtesyEventsGenerator;
import org.matsim.contrib.socnetsim.framework.population.JointPlans;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.contrib.socnetsim.framework.replanning.CompositePlanLinkIdentifier;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.GroupIdentifier;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.ReplanningGroup;
import org.matsim.contrib.socnetsim.framework.replanning.modules.PlanLinkIdentifier;
import org.matsim.contrib.socnetsim.framework.replanning.selectors.EmptyIncompatiblePlansIdentifierFactory;
import org.matsim.contrib.socnetsim.framework.replanning.selectors.IncompatiblePlansIdentifierFactory;
import org.matsim.contrib.socnetsim.framework.scoring.InternalizingPlansScoring;
import org.matsim.contrib.socnetsim.framework.scoring.InternalizingPlansScoring.ConfigBasedInternalizationSettings;
import org.matsim.contrib.socnetsim.framework.scoring.InternalizingPlansScoring.InternalizationSettings;

import java.util.Collection;
import java.util.Collections;

/**
 * Defines the basic elements for the matsim process with joint plans.
 * Should be enough to run without crash, but not very useful as such.
 *
 * A useful module would first add this module, and then an "overriding"
 * module defining more useful elements.
 *
 * @author thibautd
 */
public class JointDecisionProcessModule extends AbstractModule {

	@Override
	public void install() {
		// process as such
		bind(PlansReplanning.class).to( GroupReplanningListenner.class );
		bind(PlansScoring.class).to( InternalizingPlansScoring.class );
		bind(DumpDataAtEnd.class).to( DumpJointDataAtEnd.class );

		bind( InternalizationSettings.class ).to( ConfigBasedInternalizationSettings.class );

		addControlerListenerBinding().to(JointPlansDumping.class);

		addEventHandlerBinding().to(CourtesyEventsGenerator.class);

		// consistency
		addControlerListenerBinding().to(JointPlanCompositionMinimalityChecker.class);
		addControlerListenerBinding().to(JointPlanSelectionConsistencyChecker.class);

		// default elements
		bind(GroupIdentifier.class).toInstance(
				// by default, no groups (results in individual replanning)
				new GroupIdentifier() {
					@Override
					public Collection<ReplanningGroup> identifyGroups(
							final Population population) {
						return Collections.<ReplanningGroup>emptyList();
					}
				});

		bind(PlanRoutingAlgorithmFactory.class).toInstance(
				new PlanRoutingAlgorithmFactory() {
					@Override
					public PlanAlgorithm createPlanRoutingAlgorithm(
							final TripRouter tripRouter) {
						return new PlanRouter(tripRouter);
					}
				});

		bind( PlanLinkIdentifier.class ).annotatedWith( PlanLinkIdentifier.Strong.class ).toInstance(new CompositePlanLinkIdentifier());
		bind( PlanLinkIdentifier.class ).annotatedWith( PlanLinkIdentifier.Weak.class ).toInstance(new CompositePlanLinkIdentifier());
		bind( IncompatiblePlansIdentifierFactory.class ).toInstance( new EmptyIncompatiblePlansIdentifierFactory() );

		//addControlerListenerBinding().to( TripModeShares.class );
		//final CompositeStageActivityTypes actTypesForAnalysis = new CompositeStageActivityTypes();
		//actTypesForAnalysis.addActivityTypes(
		//		controller.getRegistry().getTripRouterFactory().get().getStageActivityTypes() );
		//actTypesForAnalysis.addActivityTypes( JointActingTypes.JOINT_STAGE_ACTS );
		//controller.addControlerListener(
		//		new TripModeShares(
		//			graphWriteInterval,
		//			controller.getControlerIO(),
		//			controller.getRegistry().getScenario(),
		//			new JointMainModeIdentifier( new MainModeIdentifierImpl() ),
		//			actTypesForAnalysis));

		// For convenience
		bind( JointPlans.class ).toProvider( new ScenarioElementProvider<JointPlans>( JointPlans.ELEMENT_NAME ) );
		bind( SocialNetwork.class ).toProvider( new ScenarioElementProvider<SocialNetwork>( SocialNetwork.ELEMENT_NAME ) );
	}
}

