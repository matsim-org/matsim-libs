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
package org.matsim.contrib.accessibility.utils;

import com.vividsolutions.jts.geom.Envelope;
import org.apache.log4j.Logger;
import org.matsim.contrib.accessibility.Labels;
import org.matsim.contrib.analysis.vsp.qgis.*;
import org.matsim.contrib.analysis.vsp.qgis.layerTemplates.AccessibilityXmlRenderer;
import org.matsim.contrib.analysis.vsp.qgis.utils.ColorRangeUtils;
import org.matsim.core.utils.misc.ExeRunner;

/**
 * @author nagel, dziemke
 */
public class VisualizationUtils {
	public static final Logger log = Logger.getLogger(VisualizationUtils.class);
	private VisualizationUtils(){} // do not instantiate

	
	
	public static void createQGisOutputGraduatedStandardColorRange(String actType, String mode, Envelope envelope,
			String workingDirectory, String crs, boolean includeDensityLayer,
			Double lowerBound, Double upperBound, Integer range,
			int symbolSize, int populationThreshold) {
	    createQGisOutputGraduated(actType, mode, envelope,
	    		workingDirectory, crs, includeDensityLayer, lowerBound, upperBound,
                range, ColorRangeUtils.ColorRange.DEFAULT_RED_TO_BLUE, 
                symbolSize, populationThreshold);
    }
	
    public static void createQGisOutputGraduated(String actType, String mode, Envelope envelope,
    		String workingDirectory, String crs, boolean includeDensityLayer,
    		Double lowerBound, Double upperBound, Integer range,
    		ColorRangeUtils.ColorRange colorRange, int symbolSize, int populationThreshold) {

    	// Create Mapnik file that is needed for OSM layer
		QGisMapnikFileCreator.writeMapnikFile(workingDirectory + "osm_mapnik.xml");

		// Write QGis project file
		QGisWriter writer = new QGisWriter(crs, workingDirectory);
		String qGisProjectFile = "QGisProjectFile_" + mode + ".qgs";
		writer.setEnvelope(envelope);

		// Osm raster layer
		writer.changeWorkingDirectory(workingDirectory); // has to be the storage location of the file

		RasterLayer mapnikLayer = new RasterLayer("osm_mapnik_xml", workingDirectory + "/osm_mapnik.xml");
		new AccessibilityXmlRenderer(mapnikLayer);
		mapnikLayer.setSrs("WGS84_Pseudo_Mercator");
		writer.addLayer(0,mapnikLayer);

		String actSpecificWorkingDirectory =  workingDirectory + actType + "/";  // has to be the storage location of the file
		writer.changeWorkingDirectory(actSpecificWorkingDirectory);

		if (includeDensityLayer) {
			VectorLayer densityLayer = new VectorLayer(
					"density", actSpecificWorkingDirectory + "accessibilities.csv", QGisConstants.geometryType.Point, true);
			densityLayer.setXField(Labels.X_COORDINATE);
			densityLayer.setYField(Labels.Y_COORDINATE);
			GraduatedSymbolRenderer dRenderer = RendererFactory.createDensitiesRenderer(densityLayer, populationThreshold, symbolSize);
			dRenderer.setRenderingAttribute(Labels.POPULATION_DENSITIY);
			writer.addLayer(densityLayer);
		}

		VectorLayer accessibilityLayer = new VectorLayer(
				"accessibility", actSpecificWorkingDirectory + "accessibilities.csv", QGisConstants.geometryType.Point, true);
		// there are two ways to set x and y fields for csv geometry files
		// 1) if there is a header, you can set the members xField and yField to the name of the column headers
		// 2) if there is no header, you can write the column index into the member (e.g. field_1, field_2,...), but works also if there is a header
		accessibilityLayer.setXField(Labels.X_COORDINATE);
		accessibilityLayer.setYField(Labels.Y_COORDINATE);
//		AccessibilityRenderer renderer = new AccessibilityRenderer(accessibilityLayer, lowerBound, upperBound,
//				range, symbolSize);
		GraduatedSymbolRenderer renderer = new GraduatedSymbolRenderer(accessibilityLayer, lowerBound, upperBound,
				range, symbolSize, colorRange);
			renderer.setRenderingAttribute(mode.toString() + "_accessibility");
		writer.addLayer(accessibilityLayer);

		writer.write(qGisProjectFile);
	}
	
    
    public static void createQGisOutputRuleBasedStandardColorRange(String actType, String mode, Envelope envelope,
			String workingDirectory, String crs, boolean includeDensityLayer,
			Double lowerBound, Double upperBound, Integer range,
			int symbolSize, int populationThreshold) {
    	createQGisOutputRuleBased(actType, mode, envelope,
	    		workingDirectory, crs, includeDensityLayer, lowerBound, upperBound,
                range, ColorRangeUtils.ColorRange.DEFAULT_RED_TO_BLUE, 
                symbolSize, populationThreshold);
    }
	
    
	public static void createQGisOutputRuleBased(String actType, String mode, Envelope envelope,
			String workingDirectory, String crs, boolean includeDensityLayer,
			Double lowerBound, Double upperBound, Integer range,
			ColorRangeUtils.ColorRange colorRange, int symbolSize, int populationThreshold) {

		// Create Mapnik file that is needed for OSM layer
		QGisMapnikFileCreator.writeMapnikFile(workingDirectory + "osm_mapnik.xml");

		// Write QGis project file
		QGisWriter writer = new QGisWriter(crs, workingDirectory);
		String qGisProjectFile = "QGisProjectFile_" + mode + ".qgs";
		writer.setEnvelope(envelope);

		// Osm raster layer
		writer.changeWorkingDirectory(workingDirectory); // has to be the storage location of the file

		RasterLayer mapnikLayer = new RasterLayer("osm_mapnik_xml", workingDirectory + "/osm_mapnik.xml");
		new AccessibilityXmlRenderer(mapnikLayer);
		mapnikLayer.setSrs("WGS84_Pseudo_Mercator");
		writer.addLayer(0,mapnikLayer);

		String actSpecificWorkingDirectory =  workingDirectory + actType + "/";  // has to be the storage location of the file
		writer.changeWorkingDirectory(actSpecificWorkingDirectory);

//		VectorLayer accessibilityLayer = new VectorLayer(
//				"accessibility", actSpecificWorkingDirectory + "accessibilities.csv", QGisConstants.geometryType.Point, true);
//		// there are two ways to set x and y fields for csv geometry files
//		// 1) if there is a header, you can set the members xField and yField to the name of the column headers
//		// 2) if there is no header, you can write the column index into the member (e.g. field_1, field_2,...), but works also if there is a header
//		accessibilityLayer.setXField(Labels.X_COORDINATE);
//		accessibilityLayer.setYField(Labels.Y_COORDINATE);
//		RuleBasedRenderer renderer = new RuleBasedRenderer(accessibilityLayer, lowerBound, upperBound, range,
//				symbolSize, colorRange, mode.toString() + "_accessibility",
//				Labels.POPULATION_DENSITIY, populationThreshold);
//		renderer.setRenderingAttribute(mode.toString() + "_accessibility");
//		writer.addLayer(accessibilityLayer);

		if (includeDensityLayer) {
			VectorLayer densityLayer = new VectorLayer(
					"density", actSpecificWorkingDirectory + "accessibilities.csv", QGisConstants.geometryType.Point, true);
			densityLayer.setXField(Labels.X_COORDINATE);
			densityLayer.setYField(Labels.Y_COORDINATE);
			GraduatedSymbolRenderer dRenderer = RendererFactory.createDensitiesRenderer(densityLayer, populationThreshold, symbolSize);
			dRenderer.setRenderingAttribute(Labels.POPULATION_DENSITIY);
			writer.addLayer(0,densityLayer);
		}

		VectorLayer accessibilityLayer = new VectorLayer(
				"accessibility", actSpecificWorkingDirectory + "../../voronoi_test/voronoi_test.shp", QGisConstants.geometryType.Polygon, false);
		// there are two ways to set x and y fields for csv geometry files
		// 1) if there is a header, you can set the members xField and yField to the name of the column headers
		// 2) if there is no header, you can write the column index into the member (e.g. field_1, field_2,...), but works also if there is a header
		accessibilityLayer.setXField(Integer.parseInt(Labels.X_COORDINATE));
		accessibilityLayer.setYField(Integer.parseInt(Labels.Y_COORDINATE));
		PolygonLayerRenderer renderer = new PolygonLayerRenderer(accessibilityLayer, lowerBound, upperBound, range,
				colorRange, mode,
				Labels.POPULATION_DENSITIY, populationThreshold);
		renderer.setRenderingAttribute(mode);
//		renderer.setRenderingAttribute(mode.toString() + "_accessibility");
		accessibilityLayer.setLayerClass(QGisConstants.layerClass.SimpleFill);
		accessibilityLayer.setSrs("EPSG:4326");
		writer.addLayer(0,accessibilityLayer);

		writer.write(qGisProjectFile);
	}


	/**
	 * This method creates a snapshot of the accessibility map that is held in the created QGis file.
	 * The syntax within the method is different dependent on the operating system.
	 * 
	 * @param workingDirectory The directory where the QGisProjectFile (the data source of the to-be-created snapshot) is stored
	 * @param mode
	 * @param osName
	 */
	public static void createSnapshot(String workingDirectory, String mode, String osName) {
		// if OS is Windows
		// example (daniel r) // os.arch=amd64 // os.name=Windows 7 // os.version=6.1
		if ( osName.contains("Win") || osName.contains("win")) {
			// On Windows, the PATH variables need to be set correctly to be able to call "qgis.bat" on the command line
			// This needs to be done manually. It does not seem to be set automatically when installing QGis
			String cmd = "qgis.bat " + workingDirectory + "QGisProjectFile_" + mode + ".qgs" +
					" --snapshot " + workingDirectory + "snapshot_" + mode + ".png";

			String stdoutFileName = workingDirectory + "snapshot_" + mode + ".log";
			int timeout = 99999;

			ExeRunner.run(cmd, stdoutFileName, timeout);

			// if OS is Macintosh
			// example (dominik) // os.arch=x86_64 // os.name=Mac OS X // os.version=10.10.2
		} else if ( osName.contains("Mac") || osName.contains("mac") ) {
			String cmd = "/Applications/QGIS.app/Contents/MacOS/QGIS " + workingDirectory + "QGisProjectFile_" + mode + ".qgs" +
					" --snapshot " + workingDirectory + "snapshot_" + mode + ".png";

			String stdoutFileName = workingDirectory + "snapshot_" + mode + ".log";

			int timeout = 99999;

			ExeRunner.run(cmd, stdoutFileName, timeout);

			// if OS is Linux
			// example (benjamin) // os.arch=amd64 // os.name=Linux	// os.version=3.13.0-45-generic
			//} else if ( osName.contains("Lin") || osName.contains("lin") ) {
			// TODO for linux

			// if OS is other
		} else {
			log.warn("generating png files not implemented for os.arch=" + System.getProperty("os.arch") );
		}
	}

}