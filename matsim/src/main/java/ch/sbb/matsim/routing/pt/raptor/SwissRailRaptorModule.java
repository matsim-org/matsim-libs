/* *********************************************************************** *
 * project: org.matsim.* 												   *
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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

package ch.sbb.matsim.routing.pt.raptor;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;
import org.matsim.pt.router.TransitRouter;

/**
 * @author mrieser / SBB
 */
public class SwissRailRaptorModule extends AbstractModule {

	private final OccupancyData occupancyData = new OccupancyData();

	public OccupancyData getExecutionData() {
		return this.occupancyData;
	}

	@Override
	public void install() {
		if (getConfig().transit().isUseTransit()) {
			bind(TransitRouter.class).toProvider(SwissRailRaptorFactory.class);
			bind(SwissRailRaptor.class).toProvider(SwissRailRaptorFactory.class);

			for (String mode : getConfig().transit().getTransitModes()) {
				addRoutingModuleBinding(mode).toProvider(SwissRailRaptorRoutingModuleProvider.class);
			}

			SwissRailRaptorConfigGroup srrConfig = ConfigUtils.addOrGetModule(getConfig(), SwissRailRaptorConfigGroup.class);

			if (srrConfig.isUseRangeQuery()) {
				bind(RaptorRouteSelector.class).to(ConfigurableRaptorRouteSelector.class);
			} else {
				bind(RaptorRouteSelector.class).to(LeastCostRaptorRouteSelector.class); // just a simple default in case it ever gets used.
			}

			switch (srrConfig.getScoringParameters()) {
			case Default:
				bind(RaptorParametersForPerson.class).to(DefaultRaptorParametersForPerson.class);
				break;
			case Individual:
				bind(RaptorParametersForPerson.class).to(IndividualRaptorParametersForPerson.class);
				break;
			}

			bind(RaptorStopFinder.class).to(DefaultRaptorStopFinder.class);
			bind(OccupancyData.class).toInstance(this.occupancyData);
			if (srrConfig.isUseCapacityConstraints()) {
				bindOccupancyTracker();
			}

			bind(RaptorIntermodalAccessEgress.class).to(DefaultRaptorIntermodalAccessEgress.class);
			bind(RaptorInVehicleCostCalculator.class).to(DefaultRaptorInVehicleCostCalculator.class);

			if (srrConfig.getModeToModeTransferPenaltyParameterSets().isEmpty()) {
				bind(RaptorTransferCostCalculator.class).to(DefaultRaptorTransferCostCalculator.class);
			} else {
				bind(ModeSpecificTransferCostCalculator.PenaltySupplier.class).toInstance(
					new ModeSpecificTransferCostCalculator.DefaultPenaltySupplier());
				bind(RaptorTransferCostCalculator.class).to(ModeSpecificTransferCostCalculator.class);
			}
		}

	}

	/**
	 * Binds the occupancy tracker as mobsim-scoped event handler and mobsim component. This works for the QSim as well as for the DSim, where each
	 * partition gets its own tracker.
	 */
	private void bindOccupancyTracker() {
		binder().requestInjection(new Object() {
			@Inject
			void addToComponents(QSimComponentsConfig components) {
				if (!components.hasNamedComponent(OccupancyTracker.COMPONENT_NAME)) {
					components.addNamedComponent(OccupancyTracker.COMPONENT_NAME);
				}
			}
		});
		installQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(OccupancyTracker.class).in(Singleton.class);
				addMobsimScopeEventHandlerBinding().to(OccupancyTracker.class);
				addQSimComponentBinding(OccupancyTracker.COMPONENT_NAME).to(OccupancyTracker.class);
			}
		});
	}

}
