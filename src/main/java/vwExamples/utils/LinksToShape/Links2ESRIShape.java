/* *********************************************************************** *
 * project: org.matsim.*
 * Links2ESRIShape.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package vwExamples.utils.LinksToShape;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.utils.gis.matsim2esri.network.*;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Simple class to convert the links of MATSim network files to ESRI shape files. The network can be written either
 * as line strings or as polygons. Furthermore the width of the links could be calculated according to
 * freespeed, lanes or capacity. For some basic examples please have a look at the <code>main</code> method.
 * Can also be called as Links2ESRIShape inputNetwork.xml outputAsLines.shp outputAsPolygons.shp .
 *
 * <p> <strong>Keywords:</strong> converter, network, links, esri, shp, matsim </p>
 *
 * @author laemmel
 */
public class Links2ESRIShape {

    private static Logger log = Logger.getLogger(Links2ESRIShape.class);

    private final FeatureGenerator featureGenerator;
    private final Network network;
    private final String filename;


    public Links2ESRIShape(final Network network, final String filename, final String coordinateSystem) {
        this(network, filename, new FeatureGeneratorBuilderImpl(network, coordinateSystem));
    }

    public Links2ESRIShape(final Network network, final String filename, final FeatureGeneratorBuilder builder) {
        this.network = network;
        this.filename = filename;
        this.featureGenerator = builder.createFeatureGenerator();

    }

    public void write() {
        log.info("creating features...");
        Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
        for (Link link : NetworkUtils.getSortedLinks(this.network)) {
            features.add(this.featureGenerator.getFeature(link));
        }
        log.info("writing features to shape file... " + this.filename);
        ShapeFileWriter.writeGeometries(features, this.filename);
        log.info("done writing shape file.");
    }

    public static void main(final String[] args) {
        String netfile = null;
        String outputFileLs = null;
        String outputFileP = null;
        String defaultCRS = "DHDN_GK4";
        boolean commonWealth = false; //to render Commonwealth networks correctly (e.g. drive on left-hand side of the road)
        if (args.length == 0) {
            netfile = "D:/Axer/MatsimDataStore/BaseCases/VW205/vw205.1.0.output_network_mod.xml.gz";
//		String netfile = "./test/scenarios/berlin/network.xml.gz";

            outputFileLs = "D:/Axer/MatsimDataStore/BaseCases/VW205/networkLs.shp";
            outputFileP = "D:/Axer/MatsimDataStore/BaseCases/VW205/networkP.shp";
        } else if (args.length == 3) {
            netfile = args[0];
            outputFileLs = args[1];
            outputFileP = args[2];
        } else if (args.length == 4) {
            netfile = args[0];
            outputFileLs = args[1];
            outputFileP = args[2];
            defaultCRS = args[3];
        } else if (args.length == 5) {
            netfile = args[0];
            outputFileLs = args[1];
            outputFileP = args[2];
            defaultCRS = args[3];
            commonWealth = Boolean.parseBoolean(args[4]);
        } else {
            log.error("Arguments cannot be interpreted.  Aborting ...");
            System.exit(-1);
        }

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        scenario.getConfig().global().setCoordinateSystem(defaultCRS);

        log.info("loading network from " + netfile);
        final Network network = scenario.getNetwork();
        new MatsimNetworkReader(scenario.getNetwork()).readFile(netfile);
        log.info("done.");

        FeatureGeneratorBuilderImpl builder = new FeatureGeneratorBuilderImpl(network, defaultCRS);
        builder.setFeatureGeneratorPrototype(LineStringBasedFeatureGenerator.class);
        builder.setWidthCoefficient(0.5);
        builder.setWidthCalculatorPrototype(LanesBasedWidthCalculator.class);
        new Links2ESRIShape(network,outputFileLs, builder).write();

        CoordinateReferenceSystem crs = MGC.getCRS(defaultCRS);
        builder.setWidthCoefficient((commonWealth ? -1 : 1) * 0.003);
        builder.setFeatureGeneratorPrototype(PolygonFeatureGenerator.class);
        builder.setWidthCalculatorPrototype(CapacityBasedWidthCalculator.class);
        builder.setCoordinateReferenceSystem(crs);
        new Links2ESRIShape(network, outputFileP, builder).write();

    }

}