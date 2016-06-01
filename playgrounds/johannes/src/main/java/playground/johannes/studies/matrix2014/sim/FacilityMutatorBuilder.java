/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,       *
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
package playground.johannes.studies.matrix2014.sim;

import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.gis.DataPool;
import playground.johannes.synpop.gis.FacilityData;
import playground.johannes.synpop.gis.FacilityDataLoader;
import playground.johannes.synpop.sim.*;
import playground.johannes.synpop.sim.data.ActivityFacilityConverter;
import playground.johannes.synpop.sim.data.Converters;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author jillenberger
 */
public class FacilityMutatorBuilder implements MutatorBuilder {

    private final Random random;

    private final FacilityData facilityData;

    private final List<String> blacklist;

    private AttributeChangeListener listener;

    private DataPool dataPool;

    private double proximityProba = 0.5;

    public FacilityMutatorBuilder(DataPool dataPool, Random random) {
        this.facilityData = (FacilityData) dataPool.get(FacilityDataLoader.KEY);
        this.random = random;
        this.dataPool = dataPool;
        blacklist = new ArrayList<>();
    }

    public void addToBlacklist(String type) {
        blacklist.add(type);
    }

    public void setListener(AttributeChangeListener listener) {
        this.listener = listener;
    }

    public void setProximityProbability(double proba) {
        this.proximityProba = proba;
    }

    @Override
    public Mutator build() {
        Object dataKey = Converters.register(CommonKeys.ACTIVITY_FACILITY, ActivityFacilityConverter.getInstance(facilityData));

//        RandomFacilityGenerator generator = new RandomFacilityGenerator(facilityData);
//        LocalFacilityGenerator generator = new LocalFacilityGenerator(facilityData, random);

//        ZoneData zoneData = (ZoneData) dataPool.get(ZoneDataLoader.KEY);
//        ZoneCollection zones = zoneData.getLayer("nuts3");
//        ProximityFacilityGenerator generator = new ProximityFacilityGenerator(facilityData, zones, proximityProba,
//                random);

        SegmentedFacilityGenerator generator = new SegmentedFacilityGenerator(dataPool, "modena", random);
        generator.setLocalSegmentProbability(proximityProba);
        for(String type : blacklist) {
            generator.addToBlacklist(type);
        }

        AttributeMutator attMutator = new AttributeMutator(dataKey, generator, listener);
        RandomActMutator actMutator = new RandomActMutator(attMutator, random);

        return actMutator;
    }

}
