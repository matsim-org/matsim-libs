/*
 * Copyright (C) 2022 MOIA GmbH - All Rights Reserved
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.drt.extension.fiss;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.dvrp.run.MultiModals;
import org.matsim.contrib.dynagent.run.DynActivityEngine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.qsim.PreplanningEngineQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigurator;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineModule;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author nkuehnel / MOIA, hrewald
 */
public class FISSConfigurator {

    private static final Logger LOG = LogManager.getLogger(FISSConfigurator.class);

    private FISSConfigurator() {
        throw new IllegalStateException("Utility class");
    }

    // Should be called late in the setup process (at least after DRT/DVRP modules)
    public static void configure(Controler controler) {

        Scenario scenario = controler.getScenario();
        Config config = controler.getConfig();

        if (!config.qsim().getVehiclesSource()
                .equals(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData)) {
            throw new IllegalArgumentException("For the time being, FISS works with vehicle types from vehicles data, please check config!");
        }

        FISSConfigGroup fissConfigGroup = ConfigUtils.addOrGetModule(config, FISSConfigGroup.class);

        Vehicles vehiclesContainer = scenario.getVehicles();

		Set<String> finalFissModes = new HashSet<>(fissConfigGroup.sampledModes);

        for (String sampledMode : fissConfigGroup.sampledModes) {
            if (!config.qsim().getMainModes().contains(sampledMode)) {
                LOG.warn("{} is not a qsim mode, can and will not apply FISS.", sampledMode);
                finalFissModes.remove(sampledMode);

                continue;
            }
            final Id<VehicleType> vehicleTypeId = Id.create(sampledMode, VehicleType.class);
            VehicleType vehicleType = vehiclesContainer.getVehicleTypes().get(vehicleTypeId);
            if (vehicleType == null) {
                vehicleType = VehicleUtils.createVehicleType(vehicleTypeId);
                vehiclesContainer.addVehicleType(vehicleType);
                LOG.info("Created explicit default vehicle type for mode '{}'", sampledMode);
            }
            final double pcu = vehicleType.getPcuEquivalents() / fissConfigGroup.sampleFactor;
            LOG.info("Set pcuEquivalent of vehicleType '{}' to {}", vehicleTypeId, pcu);
            vehicleType.setPcuEquivalents(pcu);
        }

        fissConfigGroup.sampledModes = finalFissModes;
        controler.addOverridingQSimModule(new FISSQSimModule());
        // controler.configureQSimComponents(activateModes(MultiModeDrtConfigGroup.get(config).modes().collect(Collectors.toList())));
    }

    public static QSimComponentsConfigurator activateModes() {
        return activateModes(List.of(), List.of()); // no dvrp modes
    }

    public static QSimComponentsConfigurator activateModes(List<String> additionalNamedComponents,List<String> dvrpModes) {
        return components -> {
            if (!dvrpModes.isEmpty()) {
                components.addNamedComponent(DynActivityEngine.COMPONENT_NAME);
                components.addNamedComponent(PreplanningEngineQSimModule.COMPONENT_NAME);
            }

            components.removeNamedComponent(QNetsimEngineModule.COMPONENT_NAME);
            components.addNamedComponent(FISSQSimModule.COMPONENT_NAME);
            components.addNamedComponent(QNetsimEngineModule.COMPONENT_NAME);

            additionalNamedComponents.forEach(components::addNamedComponent);


            if (!dvrpModes.isEmpty()) {
                // activate all DvrpMode components
                MultiModals.requireAllModesUnique(dvrpModes);
                for (String m : dvrpModes) {
                    components.addComponent(DvrpModes.mode(m));
                }
            }
        };
    }
}
