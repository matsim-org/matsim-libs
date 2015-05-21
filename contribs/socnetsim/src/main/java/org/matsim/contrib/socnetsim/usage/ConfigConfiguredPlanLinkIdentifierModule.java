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

package org.matsim.contrib.socnetsim.usage;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.AbstractModule;
import org.matsim.contrib.socnetsim.framework.replanning.modules.PlanLinkIdentifier;
import org.matsim.contrib.socnetsim.framework.replanning.selectors.EmptyIncompatiblePlansIdentifierFactory;
import org.matsim.contrib.socnetsim.framework.replanning.selectors.IncompatiblePlansIdentifierFactory;
import org.matsim.contrib.socnetsim.sharedvehicles.SharedVehicleUtils;
import org.matsim.contrib.socnetsim.sharedvehicles.VehicleBasedIncompatiblePlansIdentifierFactory;
import org.matsim.contrib.socnetsim.sharedvehicles.VehicleRessources;
import org.matsim.contrib.socnetsim.usage.replanning.GroupReplanningConfigGroup;
import org.matsim.contrib.socnetsim.usage.replanning.PlanLinkIdentifierUtils;

/**
 * @author thibautd
 */
public class ConfigConfiguredPlanLinkIdentifierModule extends AbstractModule {
    @Override
    public void install() {
        bind( PlanLinkIdentifier.class ).annotatedWith( PlanLinkIdentifier.Strong.class ).toProvider( PlanLinkIdentifierUtils.LinkIdentifierProvider.class );
        bind( PlanLinkIdentifier.class ).annotatedWith( PlanLinkIdentifier.Weak.class ).toProvider( PlanLinkIdentifierUtils.WeakLinkIdentifierProvider.class );

		bind(IncompatiblePlansIdentifierFactory.class).toProvider(
		new Provider<IncompatiblePlansIdentifierFactory>() {
			@Inject
			Scenario sc;

			@Override
			public IncompatiblePlansIdentifierFactory get() {
				final GroupReplanningConfigGroup conf = (GroupReplanningConfigGroup) sc.getConfig().getModule( GroupReplanningConfigGroup.GROUP_NAME );
				return conf.getConsiderVehicleIncompatibilities() &&
						sc.getScenarioElement( VehicleRessources.ELEMENT_NAME ) != null ?
							new VehicleBasedIncompatiblePlansIdentifierFactory(
									SharedVehicleUtils.DEFAULT_VEHICULAR_MODES ) :
							new EmptyIncompatiblePlansIdentifierFactory();
			}
		} );

    }
}
