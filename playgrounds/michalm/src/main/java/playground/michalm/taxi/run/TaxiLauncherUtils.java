/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils.*;
import org.matsim.contrib.dvrp.util.time.TimeDiscretizer;

import playground.michalm.taxi.data.TaxiData;
import playground.michalm.taxi.data.file.*;


public class TaxiLauncherUtils
{
    public static TaxiData initTaxiData(Scenario scenario, String taxisFileName,
            String ranksFileName)
    {
        TaxiData taxiData = new TaxiData();

        new ElectricVehicleReader(scenario, taxiData).parse(taxisFileName);
        new TaxiRankReader(scenario, taxiData).parse(ranksFileName);

        return taxiData;
    }
    
    
    public static TimeDiscretizer getTimeDiscretizer(Scenario scenario,
            TravelTimeSource ttimeSource, TravelDisutilitySource tdisSource)
    {
        if (tdisSource == TravelDisutilitySource.DISTANCE) {
            return TimeDiscretizer.CYCLIC_24_HOURS;
        }

        //else if TravelDisutilitySource.Time:
        if (ttimeSource == TravelTimeSource.FREE_FLOW_SPEED && //
                !scenario.getConfig().network().isTimeVariantNetwork()) {
            return TimeDiscretizer.CYCLIC_24_HOURS;
        }

        return TimeDiscretizer.CYCLIC_15_MIN;
    }
}
