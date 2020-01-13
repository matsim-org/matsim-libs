/* *********************************************************************** *
 * project: org.matsim.*
 * Landuse
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.vsp.demandde.commuterDemandCottbus;

import java.util.Collection;
import java.util.HashSet;

import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * @author dgrether
 *
 */
public class DgLanduseReader {
	
	public static String BASE_DIR = "../../../shared-svn/studies/countries/de/berlin_brandenburg_corine_landcover/shapefiles/";
	
	private static final String STADTGEFUEGE_KONTINUIERLICH = BASE_DIR + "clc06_c111.shp";
	private static final String STADTGEFUEGE_NICHT_KONTINUIERLICH = BASE_DIR + "clc06_c112.shp";
	private static final String INDUSTRIE_GEWERBEGEBIETE = BASE_DIR + "clc06_c121.shp";
	private static final String STRASSEN_SCHIENENNETZE_GEBAUEDE = BASE_DIR + "clc06_c122.shp";
	private static final String HAFENGEBIET = BASE_DIR + "clc06_c123.shp";
	private static final String FLUGHAEFEN = BASE_DIR + "clc06_c124.shp";
//	private static final String MINERALE_ABBAUSTAETTEN = BASE_DIR + "clc06_c131.shp";
	private static final String DEPONIEN = BASE_DIR + "clc06_c132.shp";
	private static final String BAUSTELLEN = BASE_DIR + "clc06_c133.shp";
	
	
	private static final String[] landuse_files_home = {STADTGEFUEGE_KONTINUIERLICH, STADTGEFUEGE_NICHT_KONTINUIERLICH};
//	private static final String[] landuse_files_home = {STADTGEFUEGE_NICHT_KONTINUIERLICH};
	
	private static final String[] landuse_files_work = {STADTGEFUEGE_KONTINUIERLICH, 
		STADTGEFUEGE_NICHT_KONTINUIERLICH, INDUSTRIE_GEWERBEGEBIETE,
		STRASSEN_SCHIENENNETZE_GEBAUEDE, HAFENGEBIET,
		FLUGHAEFEN, DEPONIEN, BAUSTELLEN
		/* skip mines because they cause disproportional many work places and, thus, to much traffic in their region */ 
//		, MINERALE_ABBAUSTAETTEN
		};
//	private static final String[] landuse_files_work = {INDUSTRIE_GEWERBEGEBIETE};

	
	public Tuple<Collection<SimpleFeature>,CoordinateReferenceSystem> readLanduseDataHome(){
		return this.readLanduseData(landuse_files_home);
	}

	
	public Tuple<Collection<SimpleFeature>,CoordinateReferenceSystem> readLanduseDataWork(){
		return this.readLanduseData(landuse_files_work);
	}

	
	private Tuple<Collection<SimpleFeature>, CoordinateReferenceSystem> readLanduseData(String[] shapefiles){
		ShapeFileReader shapeReader = new ShapeFileReader();
		Collection<SimpleFeature> allFeatures = new HashSet<SimpleFeature>();
		Collection<SimpleFeature> currentFeatures = null;
		for (String filename : shapefiles){
			currentFeatures = shapeReader.readFileAndInitialize(filename);
			allFeatures.addAll(currentFeatures);
		}
		CoordinateReferenceSystem crs = shapeReader.getCoordinateSystem();
		return new Tuple<Collection<SimpleFeature>, CoordinateReferenceSystem>(allFeatures, crs);
	}

}
