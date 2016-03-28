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

package playground.johannes.gsv.matrices.analysis;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.contrib.common.gis.CRSUtils;
import org.matsim.contrib.common.gis.WGS84DistanceCalculator;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.wololo.geojson.Feature;
import org.wololo.geojson.GeoJSON;
import org.wololo.geojson.GeoJSONFactory;
import org.wololo.geojson.Geometry;
import org.wololo.jts2geojson.GeoJSONWriter;
import playground.johannes.gsv.sim.cadyts.ODUtils;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneGeoJsonIO;
import playground.johannes.synpop.matrix.MatrixOperations;
import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.matrix.NumericMatrixXMLReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * @author johannes
 * 
 */
public class RelationsDiffPlot {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		String runId = "827";
		String simFile = String.format("/home/johannes/gsv/matrices/simmatrices/miv.%s.xml", runId);
//		String simFile = "/home/johannes/gsv/matrices/simmatrices/avr/763/miv2.sym.nuts3.xml";
		String tomtomFile = "/home/johannes/gsv/matrices/refmatrices/tomtom.de.xml";
		String itpFile = "/home/johannes/gsv/matrices/refmatrices/itp.xml";
		/*
		 * load ref matrix
		 */
		NumericMatrixXMLReader reader = new NumericMatrixXMLReader();
		reader.setValidating(false);
		reader.parse(itpFile);
		NumericMatrix itp = reader.getMatrix();

		reader.parse(tomtomFile);
		NumericMatrix tomtom = reader.getMatrix();

		reader.parse(simFile);
		NumericMatrix simulation = reader.getMatrix();
//		MatrixOperations.symmetrize(simulation);
//		MatrixOperations.multiply(simulation, 11.8);
		/*
		 * load zones
		 */
		ZoneCollection zones = new ZoneCollection(null);
		String data = new String(Files.readAllBytes(Paths.get("/home/johannes/gsv/gis/nuts/de.nuts3.json")));
		zones.addAll(ZoneGeoJsonIO.parseFeatureCollection(data));
		zones.setPrimaryKey("gsvId");
		data = null;
		
		MatrixOperations.applyFactor(itp, 1 / 365.0);

		ODUtils.cleanDistances(tomtom, zones, 100000, new WGS84DistanceCalculator());
		ODUtils.cleanVolumes(tomtom, zones, 1000);
		double c = ODUtils.calcNormalization(tomtom, simulation);
		MatrixOperations.applyFactor(tomtom, c);
		MatrixOperations.symmetrize(tomtom);

		/*
		 * compare
		 */
		Collection<Zone> studyZones = urbanZones(zones.getZones());
		data = writeGeoJSON(zones, studyZones, simulation, itp, tomtom);
		Files.write(Paths.get("/home/johannes/gsv/matrices/analysis/relations/matrixdiff."+runId+".json"), data.getBytes(), StandardOpenOption.CREATE);
		/*
		 * write centroids
		 */
		data = writeStudyZones(studyZones);
		Files.write(Paths.get(String.format("/home/johannes/gsv/matrices/analysis/relations/zones.%s.json", runId)), data.getBytes(), StandardOpenOption.CREATE);
		
	}

	private static String writeStudyZones(Collection<Zone> zones) {
		GeoJSONWriter jsonWriter = new GeoJSONWriter();
		List<Feature> features = new ArrayList<>();
		for(Zone zone : zones) {
			GeoJSON json = jsonWriter.write(zone.getGeometry().getCentroid());
			Geometry geom = (Geometry) GeoJSONFactory.create(json.toString());

			Map<String, Object> atts = new HashMap<>();
			atts.put("name", zone.getAttribute("nuts3_name"));
			features.add(new Feature(geom, atts));
		}
		
		return jsonWriter.write(features).toString();
	}
	
	private static String writeGeoJSON(ZoneCollection zones, Collection<Zone> relationZones, NumericMatrix m1, NumericMatrix m2, NumericMatrix m3) {
		StringBuilder builder = new StringBuilder();
		/*
		 * write zone polygons
		 */
		//builder.append(Zone2GeoJSON.toJson(zones.getZones()));
		/*
		 * create a line string for each relation
		 */
		GeometryFactory factory = JTSFactoryFinder.getGeometryFactory(null);
		GeoJSONWriter jsonWriter = new GeoJSONWriter();
		List<Feature> features = new ArrayList<>();

		double sum2 = 0;
		double sum3 = 0;
		
		MathTransform transform = null;
		
		try {
			transform = CRS.findMathTransform(DefaultGeographicCRS.WGS84, CRSUtils.getCRS(31467));
		} catch (FactoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		List<Zone> zoneList = new ArrayList<>(relationZones);
//		for (Zone i : relationZones) {
//			for (Zone j : relationZones) {
		for(int idxI = 0; idxI < zoneList.size(); idxI++) {
			for(int idxJ = (idxI + 1); idxJ < zoneList.size(); idxJ++) {
				Zone i = zoneList.get(idxI);
				Zone j = zoneList.get(idxJ);
				
				if (i != j) {
					Double val1 = m1.get(i.getAttribute("gsvId"), j.getAttribute("gsvId"));
					if (val1 == null)
						val1 = 0.0;
					Double val2 = m2.get(i.getAttribute("gsvId"), j.getAttribute("gsvId"));
					if (val2 == null)
						val2 = 0.0;

					Double val3 = m3.get(i.getAttribute("gsvId"), j.getAttribute("gsvId"));
					if (val3 == null)
						val3 = 0.0;
					
					sum2 += val2;
					sum3 += val3;
					
					double err1 = (val1 - val2) / val2;
					double err2 = (val1 - val3) / val3;

					Point start = i.getGeometry().getCentroid();
					Point end = j.getGeometry().getCentroid();

					Coordinate[] coords = new Coordinate[2];
					coords[0] = start.getCoordinate();
					coords[1] = end.getCoordinate();
					
					double deltaX = coords[1].x - coords[0].x;
					double deltaY = coords[1].y - coords[0].y;
					
//					coords[1].x = coords[0].x + deltaX/2.0;
//					coords[1].y = coords[0].y + deltaY/2.0;
					
//					double labelX = coords[0].x + deltaX/4.0;
//					double labelY = coords[0].y + deltaY/4.0;
					
					double labelX = coords[0].x + deltaX/2;
					double labelY = coords[0].y + deltaY/2;
					
					LineString line = factory.createLineString(coords);

					
					GeoJSON json = jsonWriter.write(line);
					Geometry geom = (Geometry) GeoJSONFactory.create(json.toString());

					CRSUtils.transformCoordinate(coords[0], transform);
					CRSUtils.transformCoordinate(coords[1], transform);
					deltaX = coords[1].x - coords[0].x;
					deltaY = coords[1].y - coords[0].y;
					double rotation = Math.atan(deltaY/deltaX) * 180/Math.PI;
					
					Map<String, Object> atts = new HashMap<>();
					atts.put("volume", val1);
					atts.put("error1", err1);
					atts.put("error2", err2);
					atts.put("labelX", labelX);
					atts.put("labelY", labelY);
					atts.put("rotation", rotation);
					features.add(new Feature(geom, atts));
				}
			}
		}

		builder.append(jsonWriter.write(features).toString());

		System.out.println(String.format("sum2 = %s, sum3 = %s, f = %s", sum2, sum3, sum3/sum2));
		return builder.toString();
	}

	private static Collection<Zone> urbanZones(Collection<Zone> zones) {
		final double threshold = 600000;

		Set<Zone> urbanZones = new HashSet<>();

		for (Zone zone : zones) {
			double pop = Double.parseDouble(zone.getAttribute("inhabitants"));
//			double a = zone.getGeometry().getArea();

//			double rho = pop / a;

			if (pop > threshold) {
				urbanZones.add(zone);
			}
		}

		return urbanZones;
	}
}
