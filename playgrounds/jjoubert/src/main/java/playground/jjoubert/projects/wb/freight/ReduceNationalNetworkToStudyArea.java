/* *********************************************************************** *
 * project: org.matsim.*
 * ReduceNationalNetworkToStudyArea.java
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

/**
 * 
 */
package playground.jjoubert.projects.wb.freight;

import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;

import playground.southafrica.projects.complexNetworks.pathDependence.DigicorePathDependentNetworkReader_v2;
import playground.southafrica.projects.complexNetworks.pathDependence.PathDependentNetwork;
import playground.southafrica.projects.complexNetworks.utils.ComplexNetworkUtils;
import playground.southafrica.utilities.Header;

/**
 * Reading in a national, path-dependent complex network and extracting the
 * network edge list for a specific study area.
 * 
 * @author jwjoubert
 */
public class ReduceNationalNetworkToStudyArea {
	final private static Logger LOG = Logger.getLogger(ReduceNationalNetworkToStudyArea.class);
	private final static String NATIONAL_CRS = TransformationFactory.HARTEBEESTHOEK94_LO29;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(ReduceNationalNetworkToStudyArea.class.toString(), args);
		
		String network = args[0];
		String shapefileFolder = args[1];
		String edgelistFolder = args[2];
		
		/* Parse the path-dependent network. */
		DigicorePathDependentNetworkReader_v2 pdr = new DigicorePathDependentNetworkReader_v2();
		pdr.readFile(network);
		PathDependentNetwork pdn = pdr.getPathDependentNetwork();
		
		reduceToArea(pdn, shapefileFolder, edgelistFolder);
		
		Header.printFooter();
	}
	
	private static void reduceToArea(PathDependentNetwork network, 
			String shapefileFolder, String outputFolder){
		LOG.info("Reducing each study area to a graph edge list.");
		
		outputFolder += outputFolder.endsWith("/") ? "" : "/";

		LOG.info("Cape Town...");
		Geometry gCapeTown = parseStudyAreaGeometry(StudyArea.CAPETOWN, shapefileFolder);
		ComplexNetworkUtils.writeWeightedEdgelistToFile(
				network, 
				ComplexNetworkUtils.cleanupNetwork(network, gCapeTown),
				outputFolder + "capeTown_edgeList.csv.gz", NATIONAL_CRS);
		
		LOG.info("eThekwini...");
		Geometry gEthekwini = parseStudyAreaGeometry(StudyArea.ETHEKWINI, shapefileFolder);
		ComplexNetworkUtils.writeWeightedEdgelistToFile(
				network, 
				ComplexNetworkUtils.cleanupNetwork(network, gEthekwini),
				outputFolder + "eThekwini_edgeList.csv.gz", NATIONAL_CRS);
		
		LOG.info("Done reducing study areas.");

		LOG.info("Gauteng...");
		Geometry gGauteng = parseStudyAreaGeometry(StudyArea.GAUTENG, shapefileFolder);
		ComplexNetworkUtils.writeWeightedEdgelistToFile(
				network, 
				ComplexNetworkUtils.cleanupNetwork(network, gGauteng),
				outputFolder + "gauteng_edgeList.csv.gz", NATIONAL_CRS);
		
		LOG.info("Done reducing study areas.");
	}
	
	private static Geometry parseStudyAreaGeometry(StudyArea area, String path){
		Geometry geometry = null;
		ShapeFileReader sfr = new ShapeFileReader();
		sfr.readFileAndInitialize(area.getAbsolutShapefilePath(path));
		Collection<SimpleFeature> features = sfr.getFeatureSet();
		if(features.size() > 1){
			LOG.warn("Study area " + area.getName() + " has multiple features, only using first.");
		}
		SimpleFeature firstFeature = features.iterator().next();
		Object o = firstFeature.getDefaultGeometry();
		if(o instanceof MultiPolygon){
			geometry = (MultiPolygon) o;
		} else{
			LOG.warn("Feature type of " + area.getName() + " is not a MultiPolygon. Returning null.");
		}
		
		return geometry;
	}
	
	private enum StudyArea{
		CAPETOWN("CapeTown", "CapeTown/zones/CapeTown_MN2011_H94Lo29.shp"),
		ETHEKWINI("eThekwini", "eThekwini/zones/eThekwini_MN2011_H94Lo29_NE.shp"),
		GAUTENG("Gauteng", "Gauteng/zones/Gauteng_PR2011_H94Lo29_NE.shp");
		
		private String name;
		private String shapefileSuffix;
		
		private StudyArea(String name, String shapefileSuffix) {
			this.name = name;
			this.shapefileSuffix = shapefileSuffix;
		}
		
		private String getAbsolutShapefilePath(String path){
			path += path.endsWith("/") ? "" : "/";
			return path + this.shapefileSuffix;
		}
		
		private String getName(){
			return this.name;
		}
	}

}
