/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.integration.daily.accessibility;

import java.util.List;

import org.matsim.contrib.accessibility.Labels;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.analysis.vsp.qgis.QGisConstants;
import org.matsim.contrib.analysis.vsp.qgis.QGisConstants.geometryType;
import org.matsim.contrib.analysis.vsp.qgis.QGisMapnikFileCreator;
import org.matsim.contrib.analysis.vsp.qgis.QGisWriter;
import org.matsim.contrib.analysis.vsp.qgis.RasterLayer;
import org.matsim.contrib.analysis.vsp.qgis.VectorLayer;
import org.matsim.contrib.analysis.vsp.qgis.layerTemplates.AccessibilityDensitiesRenderer;
import org.matsim.contrib.analysis.vsp.qgis.layerTemplates.AccessibilityRenderer;
import org.matsim.contrib.analysis.vsp.qgis.layerTemplates.AccessibilityXmlRenderer;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * @author nagel
 *
 */
class Utils {
	private Utils(){} // do not instantiate

	/**
		 * Create QGis project files
		 * 
		 * @param activityTypes
		 * @param mapViewExtent
		 * @param workingDirectory
		 */
		static void createQGisOutput(List<String> activityTypes, double[] mapViewExtent, String workingDirectory) {
			// create Mapnik file that is needed to have OSM layer in QGis project
			QGisMapnikFileCreator.writeMapnikFile(workingDirectory + "osm_mapnik.xml");
			
			// loop over activity types to produce one QGIs project file for each combination
			for (String actType : activityTypes) {
				for ( Modes4Accessibility mode : Modes4Accessibility.values()) {
					if ( !actType.equals("w") ) {
						AccessibilityRunTest.log.error("skipping everything except work for debugging purposes; remove in production code. kai, feb'14") ;
						continue ;
					}
	//				if ( !mode.equals(Modes4Accessibility.freeSpeed)) {
	//					log.error("skipping everything except freespeed and pt for debugging purposes; remove in production code. dz, nov'14") ;
	//					continue ;
	//				}
			
					// Write QGis project file
					QGisWriter writer = new QGisWriter(TransformationFactory.WGS84_SA_Albers, workingDirectory);
					String qGisProjectFile = "QGisProjectFile_" + mode + ".qgs";
					writer.setExtent(mapViewExtent);
						
					// osm raster layer
					// working directory needs to be the storage location of the file
					writer.changeWorkingDirectory(workingDirectory);
					
					RasterLayer mapnikLayer = new RasterLayer("osm_mapnik_xml", workingDirectory + "/osm_mapnik.xml");
					new AccessibilityXmlRenderer(mapnikLayer);
					mapnikLayer.setSrs("WGS84_Pseudo_Mercator");
					writer.addLayer(0,mapnikLayer);
	
					// working directory needs to be the storage location of the file
					String actSpecificWorkingDirectory =  workingDirectory + actType + "/";
					writer.changeWorkingDirectory(actSpecificWorkingDirectory);
					
					// density layer
					VectorLayer densityLayer = new VectorLayer(
							"density", actSpecificWorkingDirectory + "accessibilities.csv", QGisConstants.geometryType.Point, true);
					densityLayer.setXField(Labels.X_COORDINATE);
					densityLayer.setYField(Labels.Y_COORDINATE);
					AccessibilityDensitiesRenderer dRenderer = new AccessibilityDensitiesRenderer(densityLayer);
					dRenderer.setRenderingAttribute(Labels.POPULATION_DENSITIY);
					writer.addLayer(densityLayer);
					
					// accessibility layer
					VectorLayer accessibilityLayer = new VectorLayer(
							"accessibility", actSpecificWorkingDirectory + "accessibilities.csv", QGisConstants.geometryType.Point, true);
					// there are two ways to set x and y fields for csv geometry files
					// 1) if there is a header, you can set the members xField and yField to the name of the column headers
					// 2) if there is no header, you can write the column index into the member (e.g. field_1, field_2,...), but works also if there is a header
					accessibilityLayer.setXField(Labels.X_COORDINATE);
					accessibilityLayer.setYField(Labels.Y_COORDINATE);
					AccessibilityRenderer renderer = new AccessibilityRenderer(accessibilityLayer);
					if (mode.equals(Modes4Accessibility.freeSpeed)) {
						renderer.setRenderingAttribute(Labels.ACCESSIBILITY_BY_FREESPEED); // choose column/header to visualize
					} else if (mode.equals(Modes4Accessibility.car)) {
						renderer.setRenderingAttribute(Labels.ACCESSIBILITY_BY_CAR); // choose column/header to visualize
					} else if (mode.equals(Modes4Accessibility.bike)) {
						renderer.setRenderingAttribute(Labels.ACCESSIBILITY_BY_BIKE); // choose column/header to visualize
					} else if (mode.equals(Modes4Accessibility.walk)) {
						renderer.setRenderingAttribute(Labels.ACCESSIBILITY_BY_WALK); // choose column/header to visualize
					} else if (mode.equals(Modes4Accessibility.pt)) {
						renderer.setRenderingAttribute(Labels.ACCESSIBILITY_BY_PT); // choose column/header to visualize
					} else {
						throw new RuntimeException("Other modes not yet considered!");
					}
					writer.addLayer(accessibilityLayer);
	
					// write the project file
					writer.write(qGisProjectFile);
				
					String osName = System.getProperty("os.name");
					AccessibilityRunTest.createSnapshot(actSpecificWorkingDirectory, mode, osName);
				}
			}		
		}

}
