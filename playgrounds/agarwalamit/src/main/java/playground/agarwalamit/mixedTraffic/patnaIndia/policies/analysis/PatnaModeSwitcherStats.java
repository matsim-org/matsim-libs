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

package playground.agarwalamit.mixedTraffic.patnaIndia.policies.analysis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import playground.agarwalamit.analysis.modeSwitcherRetainer.ModeSwitchersTripDistance;
import playground.agarwalamit.analysis.modeSwitcherRetainer.ModeSwitchersTripTime;
import playground.agarwalamit.analysis.tripDistance.TripDistanceType;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.utils.FileUtils;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.agarwalamit.utils.PersonFilter;

/**
 * Created by amit on 24/11/2016.
 */


public class PatnaModeSwitcherStats {

    private final String dir = FileUtils.RUNS_SVN+"patnaIndia/run108/jointDemand/policies/0.15pcu/BT-b/";

    private final PersonFilter pf = new PatnaPersonFilter();
    private final String userGroup = PatnaPersonFilter.PatnaUserGroup.urban.toString();
    private final int firstIteration = 1200;
    private final int lastIteration = 1400;

    public static void main(String[] args) {
        new PatnaModeSwitcherStats().run();
    }

    private void run(){
        Scenario scenario = LoadMyScenarios.loadScenarioFromNetworkAndConfig(dir+"/output_network.xml.gz",dir+"/output_config.xml.gz");
        scenario.getConfig().controler().setOutputDirectory(dir);

        ModeSwitchersTripTime mstt = new ModeSwitchersTripTime(userGroup, pf);
        mstt.processEventsFiles(scenario);
        mstt.writeResults(dir+"/analysis/");

        {
            ModeSwitchersTripDistance mstd = new ModeSwitchersTripDistance(userGroup, pf, TripDistanceType.BEELINE_DISTANCE);
            mstd.processEventsFiles(scenario);
            mstd.writeResults(dir+"/analysis/");
        }

        {
            ModeSwitchersTripDistance mstd = new ModeSwitchersTripDistance(userGroup, pf, TripDistanceType.ROUTE_DISTANCE);

            updateBikeTrackLinkLength(scenario);

            mstd.processEventsFiles(scenario);
            mstd.writeResults(dir+"/analysis/correctedBikeLinksLength_");
        }
    }

    private void updateBikeTrackLinkLength (final  Scenario scenario) {

        for(Link l : scenario.getNetwork().getLinks().values()) {
            String linkId = l.getId().toString();
            if (linkId.startsWith(PatnaUtils.BIKE_TRACK_PREFIX) ||linkId.startsWith(PatnaUtils.BIKE_TRACK_CONNECTOR_PREFIX)) {
                l.setLength(l.getLength() * PatnaUtils.BIKE_TRACK_LEGNTH_REDUCTION_FACTOR );
            }
        }
    }
}
