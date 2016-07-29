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

package playground.michalm.taxi.ev;

import org.matsim.contrib.taxi.util.stats.*;
import org.matsim.contrib.taxi.util.stats.TimeProfileCollector.ProfileCalculator;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;

import com.google.inject.*;

import playground.michalm.ev.data.EvData;


public class ETaxiTimeProfileCollectorProvider
    implements Provider<MobsimListener>
{
    private final EvData evData;
    private final MatsimServices matsimServices;


    @Inject
    public ETaxiTimeProfileCollectorProvider(EvData evData, MatsimServices matsimServices)
    {
        this.evData = evData;
        this.matsimServices = matsimServices;
    }


    @Override
    public MobsimListener get()
    {
        ProfileCalculator calc = ETaxiChargerProfiles.createChargerCalculator(evData);
        return new TimeProfileCollector(calc, 300, "etaxi_time_profiles.txt", matsimServices);
    }
}
