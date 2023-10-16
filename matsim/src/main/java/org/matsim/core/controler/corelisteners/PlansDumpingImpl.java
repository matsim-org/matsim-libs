/* *********************************************************************** *
 * project: org.matsim.*
 * DumpPlans.java.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.core.controler.corelisteners;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.analysis.IterationStopWatch;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * {@link org.matsim.core.controler.listener.ControlerListener} that dumps the
 * complete plans regularly at the start of an iteration
 * ({@link ControllerConfigGroup#getWritePlansInterval()} as well as in the first
 * iteration, just in case someone might check that the replanning worked
 * correctly in the first iteration.
 *
 * @author mrieser
 */
@Singleton
final class PlansDumpingImpl implements PlansDumping, BeforeMobsimListener {

	static final private Logger log = LogManager.getLogger(PlansDumpingImpl.class);

	@Inject private Config config;
	@Inject private Network network;
	@Inject private Population population;
	@Inject private IterationStopWatch stopwatch;
	@Inject private OutputDirectoryHierarchy controlerIO;
	private int writePlansInterval ;

	private int writeMoreUntilIteration;

	@Inject
	PlansDumpingImpl(ControllerConfigGroup config) {
		this.writePlansInterval = config.getWritePlansInterval();
		this.writeMoreUntilIteration = config.getWritePlansUntilIteration() ;
	}

	@Override
	public void notifyBeforeMobsim(final BeforeMobsimEvent event) {
		final boolean writingPlansAtAll = writePlansInterval > 0;
		final boolean regularWritePlans = writingPlansAtAll && (event.getIteration()>0 && event.getIteration() % writePlansInterval== 0);
		final boolean earlyIteration = event.getIteration() <= writeMoreUntilIteration ;
		if ( writingPlansAtAll && (regularWritePlans || earlyIteration) ) {
			stopwatch.beginOperation("dump all plans");
			log.info("dumping plans...");
			final String inputCRS = config.plans().getInputCRS();
			final String internalCRS = config.global().getCoordinateSystem();

			if ( inputCRS == null ) {
				new PopulationWriter(population, network).write(controlerIO.getIterationFilename(event.getIteration(), Controler.DefaultFiles.population));
			}
			else {
				log.info( "re-projecting population from "+internalCRS+" back to "+inputCRS+" for export" );

				final CoordinateTransformation transformation =
						TransformationFactory.getCoordinateTransformation(
								internalCRS,
								inputCRS );

				new PopulationWriter(transformation, population, network).write(controlerIO.getIterationFilename(event.getIteration(), Controler.DefaultFiles.population));
			}
			log.info("finished plans dump.");
			stopwatch.endOperation("dump all plans");
		}
	}

}
