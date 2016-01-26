/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,       *
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
package playground.johannes.studies.matrix2014.sim.run;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import playground.johannes.studies.matrix2014.config.ODCalibratorConfigurator;
import playground.johannes.studies.matrix2014.sim.AnnealingHamiltonian;
import playground.johannes.studies.matrix2014.sim.AnnealingHamiltonianConfigurator;
import playground.johannes.studies.matrix2014.sim.CachedModePredicate;
import playground.johannes.studies.matrix2014.sim.ODCalibrator;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.CommonValues;
import playground.johannes.synpop.sim.HamiltonianLogger;

/**
 * @author jillenberger
 */
public class ODCalibratorHamiltonian {

    private static final String MODULE_NAME = "odCalibratorHamiltonian";

    public static void build(Simulator engine, Config config) {
        ConfigGroup configGroup = config.getModule(MODULE_NAME);
        ODCalibrator hamiltonian = new ODCalibratorConfigurator(
                engine.getDataPool())
                .configure(configGroup);

        hamiltonian.setUseWeights(engine.getUseWeights());
        hamiltonian.setPredicate(new CachedModePredicate(CommonKeys.LEG_MODE, CommonValues.LEG_MODE_CAR));

        AnnealingHamiltonian annealingHamiltonian = AnnealingHamiltonianConfigurator.configure(hamiltonian,
                configGroup);
        engine.getHamiltonian().addComponent(annealingHamiltonian);
        engine.getAttributeListeners().get(CommonKeys.ACTIVITY_FACILITY).addComponent(hamiltonian);
        engine.getEngineListeners().addComponent(annealingHamiltonian);
        /*
        Add a hamiltonian logger.
         */
        engine.getEngineListeners().addComponent(new HamiltonianLogger(hamiltonian,
                engine.getLoggingInterval(),
                "odCalibrator",
                engine.getIOContext().getRoot()));
    }
}
