/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2022 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.extension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.matsim.contrib.drt.extension.DrtWithExtensionsConfigGroup;
import org.matsim.contrib.drt.extension.companions.DrtCompanionParams;
import org.matsim.contrib.drt.extension.operations.DrtOperationsParams;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilitiesParams;
import org.matsim.contrib.drt.extension.operations.shifts.config.ShiftsParams;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

/**
 *
 * @author Steffen Axer
 *
 */
public class DrtWithExtensionsConfigGroupTest {
	private final static double WEIGHT_1 = 4711.;
	private final static double WEIGHT_2 = 1337.;
	private final static double WEIGHT_3 = 911.;

    @TempDir
    public File tempFolder;

    private static Config createConfig(List<Double> values) {
        Config config = ConfigUtils.createConfig();

        DrtWithExtensionsConfigGroup configGroup = new DrtWithExtensionsConfigGroup();
        DrtCompanionParams drtCompanionParams = new DrtCompanionParams();

        DrtOperationsParams operationsParams = new DrtOperationsParams();
        ShiftsParams shiftsParams = new ShiftsParams();
        OperationFacilitiesParams operationFacilitiesParams = new OperationFacilitiesParams();

        operationsParams.addParameterSet(shiftsParams);
        operationsParams.addParameterSet(operationFacilitiesParams);

        drtCompanionParams.setDrtCompanionSamplingWeights(values);
        configGroup.addParameterSet(drtCompanionParams);
        configGroup.addParameterSet(operationsParams);
        config.addModule(configGroup);
        return config;
    }

    private static Path writeConfig(final File tempFolder, List<Double> weights) throws IOException {
        Config config = createConfig(weights);
        Path configFile = new File(tempFolder, "config.xml").toPath();
        ConfigUtils.writeConfig(config, configFile.toString());
        return configFile;
    }

	@Test
	void loadConfigGroupTest() throws IOException {

		/* Test that exported values are correct imported again */
		Path configFile = writeConfig(tempFolder, List.of(WEIGHT_1,WEIGHT_2,WEIGHT_3));
        Config config = ConfigUtils.createConfig();
        ConfigUtils.loadConfig(config, configFile.toString());
        DrtWithExtensionsConfigGroup loadedCfg = ConfigUtils.addOrGetModule(config, DrtWithExtensionsConfigGroup.class);
		Assertions.assertTrue(loadedCfg.getDrtCompanionParams().isPresent());
		Assertions.assertTrue(loadedCfg.getDrtCompanionParams().get().getDrtCompanionSamplingWeights().get(0).equals(WEIGHT_1));
		Assertions.assertTrue(loadedCfg.getDrtCompanionParams().get().getDrtCompanionSamplingWeights().get(1).equals(WEIGHT_2));
		Assertions.assertTrue(loadedCfg.getDrtCompanionParams().get().getDrtCompanionSamplingWeights().get(2).equals(WEIGHT_3));
    }

}
