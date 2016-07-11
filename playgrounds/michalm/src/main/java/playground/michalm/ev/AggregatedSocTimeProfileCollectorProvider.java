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

package playground.michalm.ev;

import org.matsim.contrib.taxi.util.stats.*;
import org.matsim.contrib.taxi.util.stats.TimeProfileCollector.ProfileCalculator;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;

import com.google.inject.*;

import playground.michalm.ev.data.EvData;


public class AggregatedSocTimeProfileCollectorProvider
    implements Provider<MobsimListener>
{
    private final EvData evData;
    private final MatsimServices matsimServices;


    @Inject
    public AggregatedSocTimeProfileCollectorProvider(EvData evData, MatsimServices matsimServices)
    {
        this.evData = evData;
        this.matsimServices = matsimServices;
    }


    @Override
    public MobsimListener get()
    {
        ProfileCalculator calc = TimeProfiles.combineProfileCalculators(
                EvTimeProfiles.createMeanSocCalculator(evData),
                EvTimeProfiles.createUnderchargedVehiclesCounter(evData, 0.3));
        return new TimeProfileCollector(calc, 300, "ev_agg_soc_time_profiles.txt", matsimServices);
    }
}
