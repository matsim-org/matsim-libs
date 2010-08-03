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
package playground.dgrether.tests.satellic;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.dgrether.DgPaths;
import playground.dgrether.visualization.KmlNetworkVisualizer;
import playground.gregor.gis.coordinatetransform.ApproximatelyCoordianteTransformation;


public class DgSatellicNetwork2WGS84Converter {
	
	public static void main(String[] args) {
		
		String netbase = DgPaths.SHAREDSVN + "studies/countries/de/prognose_2025/demand/network_ab";
		String net = netbase + ".xml";
		String f = DgPaths.SCMWORKSPACE + "shared-svn/studies/countries/de/prognose_2025/orig/netze/coordinateTransformationLookupTable.csv";
		ApproximatelyCoordianteTransformation transform = new ApproximatelyCoordianteTransformation(f);

		String netOut = netbase + "_wgs84.xml";

		Scenario sc = new ScenarioImpl();
		MatsimNetworkReader reader = new MatsimNetworkReader(sc);
		reader.readFile(net);

		Coord c;
		for (Node n : sc.getNetwork().getNodes().values()){
			c = transform.getTransformed(n.getCoord());
			n.getCoord().setXY(c.getX(), c.getY());
		}
		
		KmlNetworkVisualizer kmlwriter = new KmlNetworkVisualizer(sc.getNetwork());
		kmlwriter.write(netbase + ".kmz", new GeotoolsTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84));
		
		NetworkWriter netWriter = new NetworkWriter(sc.getNetwork());
		netWriter.write(netOut);
	}
	
}
