/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.michalm.demand.poznan.taxi;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.run.VrpConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.matrices.Matrices;

import playground.michalm.demand.aggregator.DemandAggregator;
import playground.michalm.demand.aggregator.FormatBasedDateDiscretizer;
import playground.michalm.util.matrices.MatricesTxtWriter;
import playground.michalm.util.matrices.MatrixUtils;
import playground.michalm.zone.Zone;
import playground.michalm.zone.ZoneFinder;
import playground.michalm.zone.poznan.PoznanZones;

import com.google.common.base.Function;


public class ServedRequestsAggregator
{
    public static void main(String[] args)
    {
        Scenario scenario = ScenarioUtils.createScenario(VrpConfigUtils.createConfig());

        //Map<Id, Zone> zones = PoznanZones.readTaxiZones(scenario);
        Map<Id<Zone>, Zone> zones = PoznanZones.readVisumZones(scenario);

        Iterable<ServedRequest> requests = PoznanServedRequests.readRequests(2, 3, 4);
        requests = PoznanServedRequests.filterNormalPeriods(requests);
        requests = PoznanServedRequests.filterWorkDaysPeriods(requests);
        requests = PoznanServedRequests.filterRequestsWithinAgglomeration(requests);

        //aggregateRequests
        ZoneFinder zoneFinder = new ZoneFinder(zones, 200);
        final FormatBasedDateDiscretizer hourlyDateDiscretizer = new FormatBasedDateDiscretizer(
                FormatBasedDateDiscretizer.YMDH);
        DemandAggregator demandAggregator = new DemandAggregator(zoneFinder, hourlyDateDiscretizer);

        for (ServedRequest r : requests) {
            demandAggregator.addTrip(r.assigned, r.from, r.to);
        }

        demandAggregator.printCounters();

        //write ODs to file
        String matricesFile = "d:/PP-rad/taxi/poznan-supply/zlecenia_obsluzone/matrices_workdays.txt";
        String header = "year\tmonth\tm_day\tw_day\thour";
        final DateFormat tabDateFormat = new SimpleDateFormat("yy\tMM\tdd\tu\tHH");

        MatricesTxtWriter w1 = new MatricesTxtWriter(demandAggregator.getMatrices());
        w1.setKeyHeader(header);
        w1.setKeyFormatter(new Function<String, String>() {
            @Override
						public String apply(String key)
            {
                Date date = hourlyDateDiscretizer.parseDiscretizedDate(key);
                return tabDateFormat.format(date);
            }
        });
        w1.write(matricesFile);

        Matrices aggregatedMatrices = MatrixUtils.aggregateMatrices(demandAggregator.getMatrices(),
                new Function<String, String>() {
                    @Override
										public String apply(String key)
                    {
                        return StringUtils.leftPad(hourlyDateDiscretizer
                                .parseDiscretizedDate(key).getHours() + "", 2);
                    };
                });

        String aggregatedMatricesFile = "d:/PP-rad/taxi/poznan-supply/zlecenia_obsluzone/matrices_workdays_aggregated.txt";
        MatricesTxtWriter w2 = new MatricesTxtWriter(aggregatedMatrices);
        w2.setKeyHeader("hour");
        w2.write(aggregatedMatricesFile);
    }

}
