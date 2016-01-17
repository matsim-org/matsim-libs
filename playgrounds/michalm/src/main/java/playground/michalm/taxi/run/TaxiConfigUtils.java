/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.michalm.taxi.run;

import java.io.File;
import java.util.*;

import org.apache.commons.configuration.*;


class TaxiConfigUtils
{
    static final String OUTPUT_DIR = "outputDir";

    static final String DELIMITER = ".";
    static final String SCHEDULER = "scheduler";
    static final String OPTIMIZER = "optimizer";
    static final String ETAXI = "eTaxi";


    static Configuration loadConfig(String configFile)
    {
        try {
            Configuration config = new PropertiesConfiguration(configFile);
            if (!config.containsKey(OUTPUT_DIR)) {
                config.setProperty(OUTPUT_DIR, new File(configFile).getParent());
            }
            return config;
        }
        catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }
    }


    static Configuration getSchedulerConfig(Configuration config)
    {
        return new SubsetConfiguration(config, SCHEDULER, DELIMITER);
    }


    static Configuration getOptimizerConfig(Configuration config)
    {
        return new SubsetConfiguration(config, OPTIMIZER, DELIMITER);
    }


    static List<Configuration> getOptimizerConfigs(Configuration config)
    {
        List<Configuration> algoCfgs = new ArrayList<>();

        int idx = 0;
        while (config.containsKey(OPTIMIZER + "_" + idx)) {
            algoCfgs.add(new SubsetConfiguration(config, OPTIMIZER + "_" + idx, DELIMITER));
            idx++;
        }

        return algoCfgs;
    }


    static Configuration getETaxiConfig(Configuration config)
    {
        return new SubsetConfiguration(config, ETAXI, DELIMITER);
    }
}
