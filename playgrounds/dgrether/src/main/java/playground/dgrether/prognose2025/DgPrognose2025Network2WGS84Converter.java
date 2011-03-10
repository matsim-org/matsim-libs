/* *********************************************************************** *
 * project: org.matsim.*
 * DgSatellicNetwork2WGS84Converter
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
package playground.dgrether.prognose2025;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.utils.gis.matsim2esri.network.FeatureGenerator;
import org.matsim.utils.gis.matsim2esri.network.FeatureGeneratorBuilder;
import org.matsim.utils.gis.matsim2esri.network.LineStringBasedFeatureGenerator;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.matsim.utils.gis.matsim2esri.network.WidthCalculator;

import playground.dgrether.DgPaths;
import playground.dgrether.visualization.KmlNetworkVisualizer;
import playground.gregor.gis.coordinatetransform.ApproximatelyCoordianteTransformation;


public class DgPrognose2025Network2WGS84Converter {
	
	public static void main(String[] args) {
		
//		String netbase = DgPaths.SHAREDSVN + "studies/countries/de/prognose_2025/demand/network_ab";
		String netbase = DgPaths.SHAREDSVN + "studies/countries/de/prognose_2025/demand/network_pv_cleaned";
		String net = netbase + ".xml.gz";
		String f = DgPaths.REPOS + "shared-svn/studies/countries/de/prognose_2025/orig/netze/coordinateTransformationLookupTable.csv";
		ApproximatelyCoordianteTransformation transform = new ApproximatelyCoordianteTransformation(f);

		String netOut = netbase + "_wgs84.xml.gz";

		Scenario sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader reader = new MatsimNetworkReader(sc);
		reader.readFile(net);

		Coord c = null;
		for (Node n : sc.getNetwork().getNodes().values()){
			c = transform.getTransformed(n.getCoord());
			n.getCoord().setXY(c.getX(), c.getY());
		}
		
		NetworkWriter netWriter = new NetworkWriter(sc.getNetwork());
		netWriter.write(netOut);
		
		KmlNetworkVisualizer kmlwriter = new KmlNetworkVisualizer(sc.getNetwork());
		kmlwriter.write(netbase + ".kmz", new GeotoolsTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84));
		
		//write shape file
		final WidthCalculator wc = new WidthCalculator() {
			@Override
			public double getWidth(Link link) {
				return 1.0;
			}
		};
		
		FeatureGeneratorBuilder builder = new FeatureGeneratorBuilder() {
			@Override
			public FeatureGenerator createFeatureGenerator() {
				FeatureGenerator fg = new LineStringBasedFeatureGenerator(wc, MGC.getCRS(TransformationFactory.WGS84));
				return fg;
			}
		};
		Links2ESRIShape linksToEsri = new Links2ESRIShape(sc.getNetwork(), netbase + "_wgs84.shp", builder);
		linksToEsri.write();

		
		
	}
	
}
