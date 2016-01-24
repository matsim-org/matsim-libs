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
import playground.johannes.studies.matrix2014.sim.CachedModePredicate;
import playground.johannes.studies.matrix2014.sim.DelayedHamiltonian;
import playground.johannes.studies.matrix2014.sim.ODCalibrator;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.CommonValues;

/**
 * @author jillenberger
 */
public class ODCalibratorHamiltonian {

    private static final String MODULE_NAME = "odCalibratorHamiltonian";

    public static void build(Simulator engine, Config config) {
        ConfigGroup configGroup = config.getModule(MODULE_NAME);
        ODCalibrator hamiltonian = new ODCalibratorConfigurator(
                engine.getDataPool())
                .configure(config.getModule("tomtomCalibrator"));

        hamiltonian.setUseWeights(true);
        hamiltonian.setPredicate(new CachedModePredicate(CommonKeys.LEG_MODE, CommonValues.LEG_MODE_CAR));

        long delay = (long) Double.parseDouble(configGroup.getValue("delay"));
        double theta = Double.parseDouble(configGroup.getValue("theta"));

        engine.getHamiltonian().addComponent(new DelayedHamiltonian(hamiltonian, delay), theta);
        engine.getAttributeListeners().get(CommonKeys.ACTIVITY_FACILITY).addComponent(hamiltonian);
    }
}
