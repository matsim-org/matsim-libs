/* *********************************************************************** *
 * project: org.matsim.*
 * EditRoutesTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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

package playground.vsp.pt.transitRouteTrimmer;

import javafx.util.Pair;
import org.geotools.feature.SchemaException;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.pt.utils.TransitScheduleValidator;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.Vehicles;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class RunTransitRouteTrimmerGladbeckExample {
    public static void main(String[] args) throws IOException, SchemaException {

        final String inScheduleFile = "../shared-svn/projects/avoev/matsim-input-files/gladbeck_umland/v0/optimizedSchedule.xml.gz";
        final String inVehiclesFile = "../shared-svn/projects/avoev/matsim-input-files/gladbeck_umland/v0/optimizedVehicles.xml.gz";
        final String inNetworkFile = "../shared-svn/projects/avoev/matsim-input-files/gladbeck_umland/v0/optimizedNetwork.xml.gz";
        final String zoneShpFile = "../shared-svn/projects/avoev/matsim-input-files/gladbeck_umland/v1/shp-files/Gladbeck_area_b_en_detail_bus_hubs_Schnellbus_cut_out.shp";
        final String outputPath = "output/";
        final String epsgCode = "25832";


        Config config = ConfigUtils.createConfig();
        config.global().setCoordinateSystem("EPSG:" + epsgCode);
        config.transit().setTransitScheduleFile(inScheduleFile);
        config.network().setInputFile(inNetworkFile);
        config.vehicles().setVehiclesFile(inVehiclesFile);

        MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);

        TransitSchedule transitSchedule = scenario.getTransitSchedule();


        Set<String> modes2Trim = new HashSet<>();
        modes2Trim.add("bus");
        Set<Id<TransitLine>> linesToModify = TransitRouteTrimmerUtils
                .filterTransitLinesForMode(transitSchedule.getTransitLines().values(), modes2Trim);


        Set<Id<TransitLine>> linesSB = transitSchedule.getTransitLines().values().stream()
                .filter(v -> v.getId().toString().contains("SB"))
                .map(v -> v.getId())
                .collect(Collectors.toSet()
                );

        linesToModify.removeAll(linesSB);


        Set<Id<TransitStopFacility>> stopsInZone = TransitRouteTrimmerUtils.getStopsInZone(scenario.getTransitSchedule(), new File(zoneShpFile).toURI().toURL());
        Pair<TransitSchedule, Vehicles> results = TransitRouteTrimmer.splitRoute(scenario.getTransitSchedule(), scenario.getVehicles(), stopsInZone,
                linesToModify, true, modes2Trim, 2, true, false, false, 0);


        TransitSchedule transitScheduleNew = results.getKey();
        Vehicles vehiclesNew = results.getValue();

        TransitScheduleValidator.ValidationResult validationResult = TransitScheduleValidator.validateAll(transitScheduleNew, scenario.getNetwork());
        System.out.println(validationResult.getErrors());


        TransitRouteTrimmerUtils.transitSchedule2ShapeFile(transitScheduleNew, outputPath + "output-trimmed-routes.shp",epsgCode);
        new TransitScheduleWriter(transitScheduleNew).writeFile(outputPath + "optimizedSchedule_nonSB-bus-split-at-hubs.xml.gz");
        new MatsimVehicleWriter(vehiclesNew).writeFile(outputPath + "optimizedVehicles_nonSB-bus-split-at-hubs.xml.gz");

    }
}
