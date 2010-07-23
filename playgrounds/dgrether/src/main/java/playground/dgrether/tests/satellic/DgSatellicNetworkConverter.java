/* *********************************************************************** *
 * project: org.matsim.*
 * DgSatellicNetworkConverter
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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.utils.gis.matsim2esri.network.FeatureGenerator;
import org.matsim.utils.gis.matsim2esri.network.FeatureGeneratorBuilder;
import org.matsim.utils.gis.matsim2esri.network.LineStringBasedFeatureGenerator;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.matsim.utils.gis.matsim2esri.network.WidthCalculator;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.DgPaths;
import playground.dgrether.visualization.KmlNetworkVisualizer;

/**
 * @author dgrether
 * 
 */
public class DgSatellicNetworkConverter {

	
	private static final Logger log = Logger.getLogger(DgSatellicNetworkConverter.class);
	
	public static void main(String[] args) {
		String net = DgPaths.SHAREDSVN + "studies/countries/de/prognose_2025/demand/network_ab.xml";
//		String netOut = DgPaths.SHAREDSVN + "studies/countries/de/prognose_2025/demand/network_ab.shp";
		String netOut = "/home/dgrether/Desktop/prognoseNetz/network_ab.shp";

		Scenario sc = new ScenarioImpl();
		MatsimNetworkReader reader = new MatsimNetworkReader(sc);
		reader.readFile(net);

		final WidthCalculator wc = new WidthCalculator() {

			@Override
			public double getWidth(Link link) {
				return 1.0;
			}
		};

//		String epsgCode = "epsg:3035";
//		String epsgCode = "GEOGCS[\"GCS_ETRS_1989\",DATUM[\"D_ETRS_1989\",SPHEROID[\"GRS_1980\",6378137.0,298.257222101]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]]";
		String epsgCode = /*"PROJCS[\"ETRS_1989_LAEA\",*/"GEOGCS[\"GCS_ETRS_1989\",DATUM[\"D_ETRS_1989\",SPHEROID[\"GRS_1980\",6378137.0,298.257222101]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Lambert_Azimuthal_Equal_Area\"]," +
				"PARAMETER[\"False_Easting\",4321000.0],PARAMETER[\"False_Northing\",3210000.0],PARAMETER[\"Central_Meridian\",10.0],PARAMETER[\"Latitude_Of_Origin\",52.0],UNIT[\"Meter\",1.0]]";
		
		final CoordinateReferenceSystem crs = MGC.getCRS(epsgCode);
//		final CoordinateReferenceSystem crs = MGC.getCRS(TransformationFactory.WGS84_UTM33N);
		
		FeatureGeneratorBuilder builder = new FeatureGeneratorBuilder() {

			@Override
			public FeatureGenerator createFeatureGenerator() {
				FeatureGenerator fg = new LineStringBasedFeatureGenerator(wc, crs);
				return fg;
			}
		};

		Links2ESRIShape linksToEsri = new Links2ESRIShape(sc.getNetwork(), netOut, builder);
		linksToEsri.write();
		
		KmlNetworkVisualizer kmlwriter = new KmlNetworkVisualizer(sc.getNetwork());
		kmlwriter.write(netOut + ".kmz", new GeotoolsTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84));
//		NetworkWriteAsTable tableWriter = new NetworkWriteAsTable(netOut);
//		tableWriter.run(sc.getNetwork());

	}

}
