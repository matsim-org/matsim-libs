/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.agarwalamit.berlin.berlinBVG09;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.scenario.ScenarioUtils;
import playground.agarwalamit.utils.FileUtils;

/**
 *
 * Created by amit on 09.06.17.
 */


public class BerlinEmissionAnalyzer {
    /*
     * generated from network file: repos/shared-svn/projects/bvg_3_bln_inputdata/rev554B-bvg00-0.1sample/network/network.final.xml.gz
     *                  plans file: runs-svn/berlin-bvg09/bvg.run189.10pct/ITERS/it.100/bvg.run189.10pct.100.plans.filtered.selected.xml.gz
     */
    private final String networkFile = FileUtils.RUNS_SVN+"/berlin-bvg09/bvg.run189.10pct/emissionsRelatedFiles/rev554B-bvg00-0.1sample.network_withRoadTypes.xml";

    private final String vehiclesFile = FileUtils.RUNS_SVN+"/berlin-bvg09/bvg.run189.10pct/emissionsRelatedFiles/bvg.run189.10pct.100.emissionVehicle.xml.gz";
    private final String roadTypeMappingFile = FileUtils.RUNS_SVN+"/berlin-bvg09/bvg.run189.10pct/emissionsRelatedFiles/rev554B-bvg00-0.1sample.roadTypeMapping.txt";
    private final String eventsFile = FileUtils.RUNS_SVN+"/berlin-bvg09/bvg.run189.10pct/emissionsRelatedFiles/bvg.run189.10pct.100.events.filtered.converted.xml.gz";

    private final String averageFleetColdEmissionFactorsFile = FileUtils.SHARED_SVN+"/projects/detailedEval/matsim-input-files/hbefa-files/v3.1/EFA_ColdStart_vehcat_2005average.txt";
    private final String averageFleetWarmEmissionFactorsFile = FileUtils.SHARED_SVN+"/projects/detailedEval/matsim-input-files/hbefa-files/v3.1/EFA_HOT_vehcat_2005average.txt";

    private final String eventsFileWithEmissionEvents = FileUtils.RUNS_SVN+"/berlin-bvg09/bvg.run189.10pct/emissionsRelatedFiles/bvg.run189.10pct.100.eventsWithEmissionEvents.xml.gz";

    public static void main(String[] args) {
        new BerlinEmissionAnalyzer().run();
    }

    private void run(){
        Config config = ConfigUtils.createConfig();
        config.network().setInputFile(networkFile); // need to find network file
        config.vehicles().setVehiclesFile(vehiclesFile); // need to create vehicles file

        EmissionsConfigGroup emissionsConfigGroup  = ConfigUtils.addOrGetModule(config, EmissionsConfigGroup.class);
        emissionsConfigGroup.setUsingDetailedEmissionCalculation(false);
        emissionsConfigGroup.setEmissionRoadTypeMappingFile(roadTypeMappingFile);
        emissionsConfigGroup.setAverageColdEmissionFactorsFile(averageFleetColdEmissionFactorsFile);
        emissionsConfigGroup.setAverageWarmEmissionFactorsFile(averageFleetWarmEmissionFactorsFile);
        emissionsConfigGroup.setUsingVehicleTypeIdAsVehicleDescription(false);

        Scenario scenario = ScenarioUtils.loadScenario(config);

        EventsManager eventsManager = EventsUtils.createEventsManager();
        EmissionModule emissionModule = new EmissionModule(scenario,eventsManager);

        EventWriterXML emissionEventWriter = new EventWriterXML(eventsFileWithEmissionEvents);
        emissionModule.getEmissionEventsManager().addHandler(emissionEventWriter);

        MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
        matsimEventsReader.readFile(eventsFile);

        emissionEventWriter.closeFile();

        emissionModule.writeEmissionInformation();



        // BerlinTransitEmissionVehicleType --> will help to identify BUS/TRAIN

    }
}
