/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.ivt.analysis;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.socnetsim.jointtrips.JointMainModeIdentifier;
import org.matsim.contrib.socnetsim.jointtrips.population.JointActingTypes;
import org.matsim.contrib.socnetsim.usage.replanning.GroupReplanningConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.router.CompositeStageActivityTypes;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.TripRouter;
import playground.ivt.utils.TripModeShares;

/**
 * @author thibautd
 */
public class IvtAnalysisModule extends AbstractModule {
	@Override
	public void install() {
		this.addControlerListenerBinding().toProvider(
				new Provider<ControlerListener>() {
					@Inject
					OutputDirectoryHierarchy controlerIO;
					@Inject
					Scenario scenario;
					@Inject
					TripRouter tripRouter;

					@Override
					public ControlerListener get() {
						final CompositeStageActivityTypes actTypesForAnalysis = new CompositeStageActivityTypes();
						actTypesForAnalysis.addActivityTypes( tripRouter.getStageActivityTypes() );
						actTypesForAnalysis.addActivityTypes( JointActingTypes.JOINT_STAGE_ACTS );
						return new TripModeShares(
									((GroupReplanningConfigGroup) scenario.getConfig().getModule( GroupReplanningConfigGroup.GROUP_NAME )).getGraphWriteInterval(),
									controlerIO,
									scenario,
									new JointMainModeIdentifier( new MainModeIdentifierImpl() ),
									actTypesForAnalysis);
					}
				});

		bind(ActivityHistogram.class);
		addEventHandlerBinding().to(ActivityHistogram.class);
		addControlerListenerBinding().to( ActivityHistogramListener.class );
	}
}
