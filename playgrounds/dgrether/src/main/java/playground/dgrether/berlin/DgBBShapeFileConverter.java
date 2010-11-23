/* *********************************************************************** *
 * project: org.matsim.*
 * DgBBDemandFilter
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
package playground.dgrether.berlin;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.geotools.factory.FactoryConfigurationError;
import org.geotools.feature.AttributeType;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.DgPaths;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;


/**
 * @author dgrether
 *
 */
public class DgBBShapeFileConverter {

	private static final String polygon = DgPaths.REPOS + "shared-svn/studies/countries/de/osm_berlinbrandenburg/urdaten/brandenburg.poly";
	private static final String outfile = DgPaths.REPOS + "shared-svn/studies/countries/de/osm_berlinbrandenburg/urdaten/brandenburg.shp";
	
	private final GeometryFactory geoFac = new GeometryFactory();
	
	private void convertTxt2Shp(String polygonFile, String shapeFile) throws FileNotFoundException, IOException, FactoryConfigurationError, SchemaException, IllegalAttributeException {
		//read feature from polygon file
		List<Coordinate> coordsList = this.readCoordinates(polygonFile);
		Coordinate[] coordinates = new Coordinate[coordsList.size()];
		for (int i = 0; i < coordsList.size(); i++){
			coordinates[i] = coordsList.get(i);
		}
		LinearRing linearRing = geoFac.createLinearRing(coordinates);
		
		CoordinateReferenceSystem crs = MGC.getCRS(TransformationFactory.WGS84);
		final AttributeType[] attribLineString = new AttributeType[1];
		attribLineString[0] = DefaultAttributeTypeFactory.newAttributeType("LinearRing",LinearRing.class, true, null, null, crs);
		FeatureType ftLineString  = FeatureTypeBuilder.newFeatureType(attribLineString, "linearRingFeatureType");
		Object[] objectArray = {linearRing};
		Feature feature = ftLineString.create(objectArray);
		Set<Feature> featureSet = new HashSet<Feature>();
		featureSet.add(feature);
		
		ShapeFileWriter.writeGeometries(featureSet, shapeFile);
		
	}
	
	private List<Coordinate> readCoordinates(final String polygonFile) throws FileNotFoundException, IOException {
		BufferedReader reader = IOUtils.getBufferedReader(polygonFile);
		List<Coordinate> coordsList = new ArrayList<Coordinate>();
		//skip header
		String line = reader.readLine();
		line = reader.readLine();
		//read file
		line = reader.readLine();
		while (line != null && "END".compareTo(line) != 0){
			line = line.trim();
			String[] coords = line.split("   ");
			double x = Double.parseDouble(coords[0]);
			double y = Double.parseDouble(coords[1]);
			Coordinate c = new Coordinate(x, y);
			coordsList.add(c);
			line = reader.readLine();
		}
		return coordsList;
	}

	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws Exception {
		if (args == null || args.length == 0){
			new DgBBShapeFileConverter().convertTxt2Shp(polygon, outfile);
		}
		else {
			
		}
	}
}
