/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.polettif.publicTransitMapping.tools.shp;

import com.opencsv.CSVReader;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import playground.polettif.publicTransitMapping.gtfs.GTFSReader;
import playground.polettif.publicTransitMapping.gtfs.containers.GTFSDefinitions;
import playground.polettif.publicTransitMapping.gtfs.containers.Shape;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Gtfs2ShapeFile {

	private CoordinateTransformation transformation = new IdentityTransformation();
	private Map<String, Shape> gtfsShapes;
	private Collection<SimpleFeature> features;

	public Gtfs2ShapeFile() {
		features = new ArrayList<>();
	}

	public static void main(String[] arg) {
		String[] args = new String[2];
		args[0] = "C:/Users/polettif/Desktop/data/gtfs/zvv/shapes.txt";
		args[1] = "C:/Users/polettif/Desktop/output/results_2016-05-04/shp/gtfs.shp";

		Gtfs2ShapeFile converter = new Gtfs2ShapeFile();
		converter.setTransformation(TransformationFactory.getCoordinateTransformation("WGS84", "CH1903_LV03_Plus"));

		try {
			converter.readShapes(args[0]);
		} catch (IOException e) {
			e.printStackTrace();
		}
		converter.convert(args[1]);
	}

	public void readShapes(String filePath) throws IOException {
		gtfsShapes = new HashMap<>();
		CSVReader reader = new CSVReader(new FileReader(filePath));

		String[] header = reader.readNext();
		Map<String, Integer> col = GTFSReader.getIndices(header, GTFSDefinitions.SHAPES.columns);

		String[] line = reader.readNext();
		while(line != null) {
			Shape actual = gtfsShapes.get(line[col.get("shape_id")]);
			if(actual == null) {
				actual = new Shape(line[col.get("shape_id")]);
				gtfsShapes.put(line[col.get("shape_id")], actual);
			}
			Coord point;
			point = new Coord(Double.parseDouble(line[col.get("shape_pt_lon")]), Double.parseDouble(line[col.get("shape_pt_lat")]));
			actual.addPoint(transformation.transform(point), Integer.parseInt(line[col.get("shape_pt_sequence")]));

			line = reader.readNext();
		}
		reader.close();
	}

	public void convert(String outFile) {
		PolylineFeatureFactory ff = new PolylineFeatureFactory.Builder()
				.setName("schedule_zurich")
				.setCrs(MGC.getCRS("EPSG:2056"))
				.addAttribute("id", String.class)
				.create();

		for(Map.Entry<String, Shape> entry : gtfsShapes.entrySet()) {
				SimpleFeature f = ff.createPolyline(entry.getValue().getCoordinates());
				f.setAttribute("id", entry.getKey());
				features.add(f);
		}

		ShapeFileWriter.writeGeometries(features, outFile);
	}

	public void setTransformation(CoordinateTransformation transformation) {
		this.transformation = transformation;
	}
}
