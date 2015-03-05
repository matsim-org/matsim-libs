package org.matsim.contrib.analysis.vsp.qgis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.analysis.vsp.qgis.layerTemplates.AccessibilityDensitiesRenderer;
import org.matsim.contrib.analysis.vsp.qgis.layerTemplates.AccessibilityRenderer;
import org.matsim.contrib.analysis.vsp.qgis.layerTemplates.AccessibilityXmlRenderer;
import org.matsim.contrib.analysis.vsp.qgis.layerTemplates.NoiseRenderer;
import org.matsim.contrib.analysis.vsp.qgis.layerTemplates.SimpleNetworkRenderer;
import org.matsim.contrib.analysis.vsp.qgis.rendering.QGisRasterRenderer;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

public class MainForQGisWriter {
	
	public static void main(String args[]){
		
		String workingDirectory =  "C:/Users/Daniel/Desktop/MATSimQGisIntegration/";
		String qGisProjectFile = "testWithMergedImmissionsCSV.qgs";
		
		QGisWriter writer = new QGisWriter(TransformationFactory.DHDN_GK4, workingDirectory);

// ################################################################################################################################################
		
		// use case 1: nodes		
//		VecorLayer nodesLayer = new VectorLayer("nodes", workingDirectory + "nodes.shp", QGisConstants.geometryType.Point);
//		nodesLayer.setRenderer(new SingleSymbolRenderer(nodesLayer.getGeometryType()));
//		nodesLayer.addAttribute("id");
//		writer.addLayer(nodesLayer);
		
// ################################################################################################################################################
		
//		// use case 2: links
//		VectorLayer linksLayer = new VecorLayer("links", workingDirectory + "links.shp", QGisConstants.geometryType.Line);
//		linksLayer.setRenderer(new SingleSymbolRenderer(linksLayer.getGeometryType()));
//		writer.addLayer(linksLayer);
	
// ################################################################################################################################################
		
		// use case 3: noise
//		double[] extent = {4582770.625,5807267.875,4608784.375,5825459.125};
//		writer.setExtent(extent);
//		
//		VectorLayer networkLayer = new VectorLayer("network", workingDirectory + "testFiles/network_detail/network.shp", QGisConstants.geometryType.Line);
//		networkLayer.setRenderer(new SimpleNetworkRenderer(networkLayer.getGeometryType()));
//		writer.addLayer(networkLayer);
//		
//		VectorLayer noiseLayer = new VectorLayer("receiverPoints", workingDirectory + "testFiles/baseCase_rpGap25meters/receiverPoints/receiverPoints.csv",
//				QGisConstants.geometryType.Point);
//		noiseLayer.setDelimiter(";");
//		noiseLayer.setXField("xCoord");
//		noiseLayer.setYField("yCoord");
//		NoiseRenderer renderer = new NoiseRenderer(noiseLayer.isUsingHeader());
//		renderer.setRenderingAttribute("immissions_3600_Immission 11:00:00");
//		noiseLayer.setRenderer(renderer);
//		writer.addLayer(noiseLayer);
//		
//		VectorLayer joinLayer = new VectorLayer("immissions_3600", workingDirectory + "testFiles/baseCase_rpGap25meters/immissions/100.immission_39600.0.csv",
//				QGisConstants.geometryType.No_geometry);
//		writer.addLayer(joinLayer);
//
//		noiseLayer.addVectorJoin(joinLayer, "Receiver Point Id", "receiverPointId");
		
// ################################################################################################################################################
		
		//use case 4: accessibility
//		double[] extent = {100000,-3720000,180000,-3675000};
//		writer.setExtent(extent);
//		
//		//example for adding a raster layer
//		RasterLayer mapnikLayer = new RasterLayer("osm_mapnik_xml", workingDirectory + "testfiles/accessibility/osm_mapnik.xml");
//		mapnikLayer.setRenderer(new AccessibilityXmlRenderer());
//		mapnikLayer.setSrs("WGS84_Pseudo_Mercator");
//		writer.addLayer(0,mapnikLayer);
//
//		VectorLayer densityLayer = new VectorLayer("density", workingDirectory + "testFiles/accessibility/accessibilities.csv", QGisConstants.geometryType.Point);
//		densityLayer.setXField(1);
//		densityLayer.setYField(2);
//		AccessibilityDensitiesRenderer dRenderer = new AccessibilityDensitiesRenderer(densityLayer.isUsingHeader());
//		dRenderer.setRenderingAttribute(8);
//		densityLayer.setRenderer(dRenderer);
//		writer.addLayer(1,densityLayer);
//		
//		VectorLayer accessibilityLayer = new VectorLayer("accessibility", workingDirectory + "testFiles/accessibility/accessibilities.csv",
//				QGisConstants.geometryType.Point);
//		//there are two ways to set x and y fields for csv geometry files
//		//1) if there is a header, you can set the members xField and yField to the name of the column headers
//		//2) if there is no header, you can write the column index into the member (e.g. field_1, field_2,...), but works also if there is a header
//		accessibilityLayer.setXField(1);
//		accessibilityLayer.setYField(2);
//		AccessibilityRenderer renderer = new AccessibilityRenderer(densityLayer.isUsingHeader());
//		renderer.setRenderingAttribute(3); // choose column/header to visualize
//		accessibilityLayer.setRenderer(renderer);
//		writer.addLayer(2,accessibilityLayer);

// ################################################################################################################################################
		
		double[] extent = {4582770.625,5807267.875,4608784.375,5825459.125};
		writer.setExtent(extent);
		
		VectorLayer layer = new VectorLayer("immissions", workingDirectory + "/testFiles/noise/immission_merged.csv",
				QGisConstants.geometryType.Point);
		layer.setXField("xCoord");
		layer.setYField("yCoord");
		layer.setDelimiter(",");
		NoiseRenderer renderer = new NoiseRenderer(layer.isUsingHeader());
		renderer.setRenderingAttribute("immission_16:00:00");
		layer.setRenderer(renderer);
		
		writer.addLayer(layer);
		
		writer.write(qGisProjectFile);

	}
	
}