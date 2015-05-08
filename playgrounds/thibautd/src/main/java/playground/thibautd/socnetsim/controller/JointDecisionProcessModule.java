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
package playground.thibautd.socnetsim.controller;

import java.util.Collection;
import java.util.Collections;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.corelisteners.DumpDataAtEnd;
import org.matsim.core.controler.corelisteners.PlansReplanning;
import org.matsim.core.controler.corelisteners.PlansScoring;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.ivt.utils.TripModeShares;
import playground.thibautd.router.PlanRoutingAlgorithmFactory;
import playground.thibautd.socnetsim.controller.listeners.DumpJointDataAtEnd;
import playground.thibautd.socnetsim.controller.listeners.GroupReplanningListenner;
import playground.thibautd.socnetsim.controller.listeners.JointPlansDumping;
import playground.thibautd.socnetsim.events.CourtesyEventsGenerator;
import playground.thibautd.socnetsim.qsim.JointQSimFactory;
import playground.thibautd.socnetsim.replanning.PlanLinkIdentifierUtils;
import playground.thibautd.socnetsim.replanning.grouping.GroupIdentifier;
import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;
import playground.thibautd.socnetsim.replanning.modules.PlanLinkIdentifier;
import playground.thibautd.socnetsim.replanning.selectors.EmptyIncompatiblePlansIdentifierFactory;
import playground.thibautd.socnetsim.replanning.selectors.IncompatiblePlansIdentifierFactory;
import playground.thibautd.socnetsim.router.JointTripRouterFactory;
import playground.thibautd.socnetsim.run.RunUtils.JointPlanCompositionMinimalityChecker;
import playground.thibautd.socnetsim.run.RunUtils.JointPlanSelectionConsistencyChecker;
import playground.thibautd.socnetsim.scoring.CharyparNagelWithJointModesScoringFunctionFactory;
import playground.thibautd.socnetsim.scoring.UniformlyInternalizingPlansScoring;

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
		bind( PlansReplanning.class ).to( GroupReplanningListenner.class );
		bind( PlansScoring.class ).to( UniformlyInternalizingPlansScoring.class );
		bind( DumpDataAtEnd.class ).to( DumpJointDataAtEnd.class );

		addControlerListenerBinding().to( JointPlansDumping.class );

		addEventHandlerBinding().to( CourtesyEventsGenerator.class );

		// consistency
		addControlerListenerBinding().to( JointPlanCompositionMinimalityChecker.class );
		addControlerListenerBinding().to( JointPlanSelectionConsistencyChecker.class );

		// default elements
		bind( GroupIdentifier.class ).toInstance(
			// by default, no groups (results in individual replanning)
			 new GroupIdentifier() {
					@Override
					public Collection<ReplanningGroup> identifyGroups(
							final Population population) {
						return Collections.<ReplanningGroup>emptyList();
					}
				} );

		bind( ScoringFunctionFactory.class ).to( CharyparNagelWithJointModesScoringFunctionFactory.class );
		bind( PlanRoutingAlgorithmFactory.class ).toInstance(
				new PlanRoutingAlgorithmFactory() {
					@Override
					public PlanAlgorithm createPlanRoutingAlgorithm(
							final TripRouter tripRouter) {
						return new PlanRouter( tripRouter );
					}
				} );
		bind( Mobsim.class ).toProvider( JointQSimFactory.class );
		bind( PlanLinkIdentifier.class ).annotatedWith( PlanLinkIdentifier.Strong.class ).toProvider( PlanLinkIdentifierUtils.LinkIdentifierProvider.class );
		bind( PlanLinkIdentifier.class ).annotatedWith( PlanLinkIdentifier.Weak.class ).toProvider( PlanLinkIdentifierUtils.WeakLinkIdentifierProvider.class );
		bind( TripRouter.class ).toProvider( JointTripRouterFactory.class );
		bind( IncompatiblePlansIdentifierFactory.class ).toInstance( new EmptyIncompatiblePlansIdentifierFactory() );

		addControlerListenerBinding().to( TripModeShares.class );
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

	}
}

