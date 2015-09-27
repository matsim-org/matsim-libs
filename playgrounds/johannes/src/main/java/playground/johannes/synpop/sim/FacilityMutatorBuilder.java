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
package playground.johannes.synpop.sim;

import playground.johannes.gsv.synPop.data.FacilityData;
import playground.johannes.gsv.synPop.data.FacilityDataLoader;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.gis.DataPool;
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

    public FacilityMutatorBuilder(DataPool dataPool, Random random) {
        this.facilityData = (FacilityData) dataPool.get(FacilityDataLoader.KEY);
        this.random = random;
        blacklist = new ArrayList<>();
    }

    public void addToBlacklist(String type) {
        blacklist.add(type);
    }

    public void setListener(AttributeChangeListener listener) {
        this.listener = listener;
    }

    @Override
    public Mutator build() {
        Object dataKey = Converters.register(CommonKeys.ACTIVITY_FACILITY, ActivityFacilityConverter.getInstance(facilityData));

        RandomFacilityGenerator generator = new RandomFacilityGenerator(facilityData);
        for(String type : blacklist) {
            generator.addToBlacklist(type);
        }

        AttributeMutator attMutator = new AttributeMutator(dataKey, generator, listener);
        RandomActMutator actMutator = new RandomActMutator(attMutator, random);

        return actMutator;
    }

}
