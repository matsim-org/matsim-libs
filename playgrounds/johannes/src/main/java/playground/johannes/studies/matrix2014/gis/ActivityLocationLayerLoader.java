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

package playground.johannes.studies.matrix2014.gis;

import playground.johannes.synpop.gis.DataLoader;
import playground.johannes.synpop.gis.DataPool;
import playground.johannes.synpop.gis.FacilityData;
import playground.johannes.synpop.gis.FacilityDataLoader;

/**
 * @author johannes
 */
public class ActivityLocationLayerLoader implements DataLoader {

    public static final String KEY = "activityLocationLayer";

    private final DataPool dataPool;

    public ActivityLocationLayerLoader(DataPool dataPool) {
        this.dataPool = dataPool;
    }

    @Override
    public Object load() {
        FacilityData fData = (FacilityData) dataPool.get(FacilityDataLoader.KEY);
        return new ActivityLocationLayer(fData.getAll());
    }
}
