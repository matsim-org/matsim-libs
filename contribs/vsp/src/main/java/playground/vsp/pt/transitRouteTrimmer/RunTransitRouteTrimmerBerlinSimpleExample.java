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
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.pt.utils.TransitScheduleValidator;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.Vehicles;
import org.osgeo.proj4j.CoordinateTransformFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class RunTransitRouteTrimmerBerlinSimpleExample {
    public static void main(String[] args) throws IOException, SchemaException {

        final String inScheduleFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-transit-schedule.xml.gz";
        final String inVehiclesFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-transit-vehicles.xml.gz";
        final String inNetworkFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz";
        final String zoneShpFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/shp-files/shp-inner-city-area/inner-city-area.shp";
        String outputPath = "output/";
        final String scenarioCRS = "EPSG:31468";
        final String shapeFileCRS = "EPSG:31468";


        Config config = ConfigUtils.createConfig();
        config.global().setCoordinateSystem(scenarioCRS);
        config.transit().setTransitScheduleFile(inScheduleFile);
        config.network().setInputFile(inNetworkFile);
        config.vehicles().setVehiclesFile(inVehiclesFile);

        MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);

        TransitSchedule transitSchedule = scenario.getTransitSchedule();


        Set<String> modes2Trim = new HashSet<>();
        modes2Trim.add("bus");
        Set<Id<TransitLine>> linesToModify = TransitRouteTrimmerUtils
                .filterTransitLinesForMode(transitSchedule.getTransitLines().values(), modes2Trim);


        Set<Id<TransitLine>> linesX = transitSchedule.getTransitLines().values().stream()
                .filter(v -> v.getId().toString().contains("X"))
                .map(v -> v.getId())
                .collect(Collectors.toSet()
                );

        linesToModify.removeAll(linesX);

        CoordinateTransformation transformer = TransformationFactory.getCoordinateTransformation(scenarioCRS, shapeFileCRS);
        Set<Id<TransitStopFacility>> stopsInZone = TransitRouteTrimmerUtils.getStopsInZone(scenario.getTransitSchedule(), IOUtils.resolveFileOrResource(zoneShpFile), transformer);
        Pair<TransitSchedule, Vehicles> results = TransitRouteTrimmer.splitRoute(scenario.getTransitSchedule(), scenario.getVehicles(), stopsInZone,
                linesToModify, true, modes2Trim, 2, true, false, false, 0);


        TransitSchedule transitScheduleNew = results.getKey();
        Vehicles vehiclesNew = results.getValue();

        TransitScheduleValidator.ValidationResult validationResult = TransitScheduleValidator.validateAll(transitScheduleNew, scenario.getNetwork());
        System.out.println(validationResult.getErrors());

        outputPath = outputPath.endsWith("/") ? outputPath : outputPath.concat("/");
        new File(outputPath).mkdirs();
        TransitRouteTrimmerUtils.transitSchedule2ShapeFile(transitScheduleNew, outputPath + "trimmed-transitRoutes.shp", scenarioCRS.substring(scenarioCRS.lastIndexOf(":") + 1));
        new TransitScheduleWriter(transitScheduleNew).writeFile(outputPath + "trimmed-transitSchedule.xml.gz");
        new MatsimVehicleWriter(vehiclesNew).writeFile(outputPath + "vehiclesNew.xml.gz");
        //copy network (SimWrapper needs it next to the schedule)
        new NetworkWriter(scenario.getNetwork()).write(outputPath + inNetworkFile.substring(inNetworkFile.lastIndexOf("/") + 1));
    }
}
