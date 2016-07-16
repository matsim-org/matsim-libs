/* *********************************************************************** *
 * project: org.matsim.*
 * DgCottbusNet2Shape
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.signalsystems.cottbus.scripts;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.utils.gis.matsim2esri.network.CapacityBasedWidthCalculator;
import org.matsim.utils.gis.matsim2esri.network.FeatureGeneratorBuilderImpl;
import org.matsim.utils.gis.matsim2esri.network.FreespeedBasedWidthCalculator;
import org.matsim.utils.gis.matsim2esri.network.LanesBasedWidthCalculator;
import org.matsim.utils.gis.matsim2esri.network.LineStringBasedFeatureGenerator;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.matsim.utils.gis.matsim2esri.network.PolygonFeatureGenerator;
import org.matsim.utils.gis.matsim2esri.network.WidthCalculator;

import playground.dgrether.visualization.KmlNetworkVisualizer;


/**
 * @author dgrether
 *
 */
public class DgCottbusMSNet2Shape {

	public static final class SimpleWidthCalculator implements WidthCalculator{
		
		public SimpleWidthCalculator(Network n, Double d) {}
		
		@Override
		public double getWidth(Link link) {
			return 1.0;
		}
	}
	
	public static void main(String[] args) {
		String netFile = "/media/data/work/repos/shared-svn/studies/dgrether/cottbus/originaldaten/network.xml";
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(netFile);

//		NetworkCleaner nc = new NetworkCleaner();
//		nc.run(net);
//		NetworkWriter writer = new NetworkWriter(net);
//		writer.write(netFile);
		
		String outputDirectory = "/media/data/work/repos/shared-svn/studies/dgrether/cottbus/originaldaten/shape_files/shape_network_wgs84_utm33n/";
		String outputFile = outputDirectory + "network_wgs84_utm33n_lanebasedwidth.shp";
		String crsString = TransformationFactory.WGS84_UTM33N;
		
		FeatureGeneratorBuilderImpl builder = new FeatureGeneratorBuilderImpl(network, crsString);
		builder.setFeatureGeneratorPrototype(PolygonFeatureGenerator.class);
		builder.setWidthCoefficient(1.5);
		builder.setWidthCalculatorPrototype(LanesBasedWidthCalculator.class);
		new Links2ESRIShape(network,outputFile, builder).write();

		
		outputFile = outputDirectory + "network_wgs84_utm33n_capacitybasedwidth.shp";
		builder = new FeatureGeneratorBuilderImpl(network, crsString);
		builder.setWidthCoefficient(0.003);
		builder.setFeatureGeneratorPrototype(PolygonFeatureGenerator.class);
		builder.setWidthCalculatorPrototype(CapacityBasedWidthCalculator.class);
		new Links2ESRIShape(network,outputFile, builder).write();


		outputFile = outputDirectory + "network_wgs84_utm33n_freespeedbasedwidth.shp";
		builder = new FeatureGeneratorBuilderImpl(network, crsString);
		builder.setWidthCoefficient(0.003);
		builder.setFeatureGeneratorPrototype(PolygonFeatureGenerator.class);
		builder.setWidthCalculatorPrototype(FreespeedBasedWidthCalculator.class);
		new Links2ESRIShape(network,outputFile, builder).write();

		outputFile = outputDirectory + "network_wgs84_utm33n_linestrings.shp";
		builder = new FeatureGeneratorBuilderImpl(network, crsString);
		builder.setWidthCoefficient(1.0);
		builder.setFeatureGeneratorPrototype(LineStringBasedFeatureGenerator.class);
		builder.setWidthCalculatorPrototype(SimpleWidthCalculator.class);
		new Links2ESRIShape(network,outputFile, builder).write();

		
		
		
		CoordinateTransformation transform = TransformationFactory.getCoordinateTransformation(crsString, TransformationFactory.WGS84);
		new KmlNetworkVisualizer(network).write(outputDirectory + "network_wgs84_utm33n.kml", transform);
		
		
		
	}

}
