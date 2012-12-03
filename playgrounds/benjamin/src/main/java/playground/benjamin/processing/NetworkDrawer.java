/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkDrawer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.benjamin.processing;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.feature.Feature;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.gicentre.geomap.GeoMap;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.spatialschema.geometry.MismatchedDimensionException;

import processing.core.PApplet;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author benjamin
 *
 */
public class NetworkDrawer extends PApplet {
	private static Logger logger = Logger.getLogger(NetworkDrawer.class);

	GeoMap networkMap;
	GeoMap munichMap;

	@Override
	public void setup(){

		String networkShapeFileName = "/media/data/2_Workspaces/repos/shared-svn/projects/detailedEval/Net/network-86-85-87-84_simplifiedWithStrongLinkMerge---withLanes-Lines.shp";
		String munichShapeFileName = "/media/data/2_Workspaces/repos/shared-svn/projects/detailedEval/Net/shapeFromVISUM/Landkreise_umMuenchen_Umrisse.shp";
		
		size(1000, 740);
		background(255, 255, 255);
		
		Set<Feature> network = readShape(networkShapeFileName);
		Set<Feature> munich = readShape(munichShapeFileName);
		
		Collection<Feature> networkProjected = getProjectedFeatures(network);
		Collection<Feature> munichProjected = getProjectedFeatures(munich);
		
		
//		network = new GeoMap(this);
//		munich = new GeoMap(this);
//		network.readFile(networkShapeFileName);
//		munich.readFile(munichShapeFileName);
		
//		for (int id : munich.getFeatures().keySet()) {
//			Feature feature = munich.getFeatures().get(id);
//			if(network.getFeatures().get(id) != null){
//				logger.warn("feature with id " + id + " in original map is replaced!");
//			}
//			network.getFeatures().put(id, feature);
//		}
	}

	@Override
	public void draw(){
		
//		for (int id : network.getFeatures().keySet()) {
//			Feature feature = (Feature) network.getFeatures().get(id);
//			Feature projectedFeature = getProjectedFeature(feature);
//			((GeoMap) feature).draw();
//		}
//		network.draw();
//		munich.draw();
	}

	Feature getProjectedFeature(Feature feature) {
		Feature projectedFeature = null;
		Exception ex;
		try {
			MathTransform proj = CRS.findMathTransform(CRS.decode("EPSG:31464"), CRS.decode("EPSG:4326"), true);
			Geometry trr = JTS.transform(feature.getDefaultGeometry(), proj);
			feature.setDefaultGeometry(trr);
			return projectedFeature;
		} catch (MismatchedDimensionException e) {
			ex = e;
		} catch (TransformException e) {
			ex = e; 
		} catch (IllegalAttributeException e) {
			ex = e; 
		} catch (NoSuchAuthorityCodeException e) {
			ex = e;
			e.printStackTrace();
		} catch (FactoryException e) {
			ex = e;
			e.printStackTrace();
		}
		throw new RuntimeException(ex);
	}


	Collection<Feature> getProjectedFeatures(Collection<Feature> fts) {
		Collection<Feature> projectedFeatrues = new HashSet<Feature>();
		for (Feature ft : fts) {
			Feature projectedFeature = getProjectedFeature(ft);
			projectedFeatrues.add(projectedFeature);
		}
		return projectedFeatrues;
	}

	Set<Feature> readShape(String shapeFile) {
		final Set<Feature> features;
		features = new ShapeFileReader().readFileAndInitialize(shapeFile);
		return features;
	}

	public static void main(String[] args) {
		PApplet.main(new String[] {"--present", "playground.benjamin.processing.NetworkDrawer"});
	}
}
