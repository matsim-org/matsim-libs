/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.matrices.postprocess;

import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneGeoJsonIO;
import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.matrix.NumericMatrixXMLReader;
import playground.johannes.synpop.matrix.NumericMatrixXMLWriter;
import playground.johannes.synpop.matrix.ODMatrixOperations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

/**
 * @author johannes
 *
 */
public class Aggregate2Nuts3 {

	private static final String TEMP_ID = "gsv2008";

	private static final String ZONE_KEY = "NO";
	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
//		String matrixFile = "/home/johannes/gsv/fpd/fraunhofer/study/data/matrix/ref/modena.all.xml";
//		String zonesFile = "/home/johannes/gsv/gis/modena/zones.gk3.geojson";
//		String idMappingsFile = "/home/johannes/gsv/matrices/refmatrices/modena2gsv2008.txt";
//		String outFile = "/home/johannes/sge/prj/matsim/run/826/output/matrices-averaged/miv.sym.nuts3.xml";

		String matrixFile = args[0];
		String zonesFile = args[1];
		String idMappingsFile = args[2];
		String outFile = args[3];

		NumericMatrixXMLReader reader = new NumericMatrixXMLReader();
		reader.setValidating(false);
		reader.parse(matrixFile);
		NumericMatrix m = reader.getMatrix();

		ZoneCollection modenaZones = new ZoneCollection(null);
		String data = new String(Files.readAllBytes(Paths.get(zonesFile)));
		modenaZones.addAll(ZoneGeoJsonIO.parseFeatureCollection(data));
		modenaZones.setPrimaryKey(ZONE_KEY);
		data = null;

		Map<String, String> idMap = ZoneIDMappings.modena2gsv2008(idMappingsFile);

		for (Zone zone : modenaZones.getZones()) {
			String id = zone.getAttribute(ZONE_KEY);
			String gsvId = idMap.get(id);
			if (gsvId != null) {
				zone.setAttribute(TEMP_ID, gsvId);
			} else {
				gsvId = id.substring(0, 5);
				zone.setAttribute(TEMP_ID, gsvId);
			}
		}


		m = ODMatrixOperations.aggregate(m, modenaZones, TEMP_ID);

//		MatrixOperations.symmetrize(m);
		NumericMatrixXMLWriter writer = new NumericMatrixXMLWriter();
		writer.write(m, outFile);
	}

}
