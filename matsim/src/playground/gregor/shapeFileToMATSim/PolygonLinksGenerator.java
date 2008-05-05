/* *********************************************************************** *
 * project: org.matsim.*
 * PolygonLinksGenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.gregor.shapeFileToMATSim;

import org.apache.log4j.Logger;
import org.geotools.feature.Feature;
import org.matsim.utils.collections.QuadTree;

public class PolygonLinksGenerator {

	private static final Logger log = Logger.getLogger(PolygonNodesGenerator.class);
	
	private PolygonGeneratorII pg;
	private String file;
	private boolean saveToFile = false;

	
	
	public PolygonLinksGenerator(PolygonGeneratorII pg) {
		this.pg = pg;
	}
	public PolygonLinksGenerator(PolygonGeneratorII pg, String file){
		this.pg = pg;
		this.saveToFile  = true;
		this.file = file;
	}
	public void createPolygonLinks(QuadTree<Feature> nodeFeatures) {
		//TODO polygons zurechtschneiden - minmale breite ermittlen - flaeche ermitteln und in eine Datei schreiben ...
		
	}
	
	
	
	


}
