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
import playground.johannes.studies.matrix2014.gis.ActivityLocationLayerLoader;
import playground.johannes.studies.matrix2014.gis.ValidateFacilities;
import playground.johannes.studies.matrix2014.gis.ZoneSetLAU2Class;
import playground.johannes.synpop.gis.*;

/**
 * @author jillenberger
 */
public class DataPoolLoader {

    public static void load(Simulator engine, Config config) {
        DataPool dataPool = engine.getDataPool();
        ConfigGroup configGroup = config.getModule(Simulator.MODULE_NAME);

        dataPool.register(new FacilityDataLoader(configGroup.getValue("facilities"), engine.getRandom()), FacilityDataLoader.KEY);
        dataPool.register(new ZoneDataLoader(configGroup), ZoneDataLoader.KEY);
        dataPool.register(new ActivityLocationLayerLoader(dataPool), ActivityLocationLayerLoader.KEY);

        ValidateFacilities.validate(dataPool, "modena");
        ValidateFacilities.validate(dataPool, "lau2");
        ValidateFacilities.validate(dataPool, "nuts3");
        ValidateFacilities.validate(dataPool, "tomtom");

        ZoneCollection lau2Zones = ((ZoneData) dataPool.get(ZoneDataLoader.KEY)).getLayer("lau2");
        new ZoneSetLAU2Class().apply(lau2Zones);
    }
}
