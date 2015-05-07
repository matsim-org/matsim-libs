/* *********************************************************************** *
 * project: org.matsim.*
 * GroupReplanningListennerWithPSimLoop.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

import org.apache.log4j.Logger;
import org.matsim.analysis.IterationStopWatch;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.corelisteners.PlansReplanning;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.mobsim.framework.MobsimFactory;

import playground.thibautd.pseudoqsim.PseudoSimConfigGroup;
import playground.thibautd.socnetsim.controller.ControllerRegistry;
import playground.thibautd.socnetsim.scoring.UniformlyInternalizingPlansScoring;

/**
 * @author thibautd
 */
public class GroupReplanningListennerWithPSimLoop implements PlansReplanning, ReplanningListener {
	private static final Logger log =
		Logger.getLogger(GroupReplanningListennerWithPSimLoop.class);

	private OutputDirectoryHierarchy controlerIO = null;

	private final GroupStrategyManager mainStrategyManager;
	private final GroupStrategyManager innovativeStrategyManager;
	private final ControllerRegistry registry;
	private final MobsimFactory pSimFactory;

	private IterationStopWatch stopWatch = null;

	public GroupReplanningListennerWithPSimLoop(
			final ControllerRegistry registry,
			final GroupStrategyManager mainStrategyManager,
			final GroupStrategyManager innovativeStrategyManager,
			final MobsimFactory pSimFactory) {
		this.registry = registry;
		this.mainStrategyManager = mainStrategyManager;
		this.innovativeStrategyManager = innovativeStrategyManager;
		this.pSimFactory = pSimFactory;
	}


	@Override
	public void notifyReplanning(final ReplanningEvent event) {
		final PseudoSimConfigGroup config = getConfigGroup();

		if ( event.getIteration() % config.getPeriod() == 0 ) {
			doInnerLoop( event );
		}

		mainStrategyManager.run(
				event.getIteration(),
				registry );
	}

	private void doInnerLoop(final ReplanningEvent event) {
		final int nIters = getConfigGroup().getNPSimIters();

		// XXX Uuuuuuuuuuuuuuuglyyyyyyyyyyyyyyyyyyyy
		// - impossible to configure which listenners are used
		// - if scoring listenner in controler changes, not automatically
		// retrofited here.
		// TODO now possible using DI
		final EventsManager events = EventsUtils.createEventsManager( registry.getScenario().getConfig() );
		final UniformlyInternalizingPlansScoring scoring =
				new UniformlyInternalizingPlansScoring(
					registry.getScenario(),
					events,
					registry.getScoringFunctionFactory());

		log.info( "### start inner loop" );
		final PseudoSimConfigGroup pSimConfig = (PseudoSimConfigGroup)
			registry.getScenario().getConfig().getModule(
					PseudoSimConfigGroup.GROUP_NAME );
		if ( stopWatch != null ) stopWatch.beginOperation( "Inner PSim loop" );
		for ( int i=0; i < nIters; i++ ) {
			log.info( "### inner loop: start iteration "+event.getIteration()+"."+i );

			final EventWriterXML writer =
				controlerIO != null && pSimConfig.getWriteEventsAndPlansIntervalInPSim() != 0 ?
					new EventWriterXML(
							controlerIO.getIterationFilename( 
								event.getIteration(),
								"pSim."+i+".events.xml.gz" ) ) :
					null;

			if ( writer != null ) events.addHandler( writer );

			innovativeStrategyManager.run(
					i, // what makes sense here???
					registry );

			// iteration starts *after* replanning.
			// This is important, as the plan selected at this stage will
			// be use to construct the scoring function
			scoring.notifyIterationStarts( new IterationStartsEvent( null , i ) );

			events.initProcessing();
			try {
				if ( stopWatch != null ) stopWatch.beginOperation( "PSim iter "+i );
				pSimFactory.createMobsim(
						registry.getScenario(),
						events ).run();
				if ( stopWatch != null ) stopWatch.endOperation( "PSim iter "+i );
			}
			finally {
				events.finishProcessing();
				if ( writer != null ) {
					events.removeHandler( writer );
					writer.closeFile();
				}
			}

			scoring.notifyScoring( new ScoringEvent( null , i ) );
			scoring.notifyIterationEnds( new IterationEndsEvent( null , i ) );
			log.info( "### inner loop: end iteration "+event.getIteration()+"."+i );
		}
		if ( stopWatch != null ) stopWatch.endOperation( "Inner PSim loop" );
	}

	private PseudoSimConfigGroup getConfigGroup() {
		return (PseudoSimConfigGroup)
			registry.getScenario().getConfig().getModule(
					PseudoSimConfigGroup.GROUP_NAME );
	}

	public void setOutputDirectoryHierarchy(
			final OutputDirectoryHierarchy controlerIO) {
		this.controlerIO = controlerIO;
	}

	public void setStopWatch(final IterationStopWatch stopWatch) {
		this.stopWatch = stopWatch;
	}
}
