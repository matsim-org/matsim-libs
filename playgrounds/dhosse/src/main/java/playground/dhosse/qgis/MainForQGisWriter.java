package playground.dhosse.qgis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.dhosse.qgis.layerTemplates.GraduatedSymbolNoiseRenderer;
import playground.dhosse.qgis.layerTemplates.SingleSymbolRenderer;

public class MainForQGisWriter {
	
	public static void main(String args[]){
		
		String workingDirectory =  "C:/Users/Daniel/Desktop/MATSimQGisIntegration/";
		String qGisProjectFile = "testWithAccessibility.qgs";
		
//		Config config = ConfigUtils.createConfig();
//		ConfigUtils.loadConfig(config, "C:/Users/Daniel/Desktop/noiseTest/config.xml");
//		ConfigUtils.loadConfig(config, "C:/Users/Daniel/Desktop/dvrp/cottbus_scenario/config.xml");
//		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		QGisWriter writer = new QGisWriter(TransformationFactory.WGS84_SA_Albers, workingDirectory);
//		writer.setExtent(NetworkUtils.getBoundingBox(scenario.getNetwork().getNodes().values()));
		double[] extent = {4592179.6404021643102169,5813737.57329507824033499,4604275.3595978356897831,5820664.42670492175966501};
		writer.setExtent(extent);
		// #########################################################
		
		// use case 1: nodes		
//		QGisLayer nodesLayer = new QGisLayer("nodes", workingDirectory + "nodes.shp", QGisConstants.geometryType.Point);
//		nodesLayer.setRenderer(new SingleSymbolRenderer(nodesLayer.getGeometryType()));
//		nodesLayer.addAttribute("id");
//		writer.addLayer(nodesLayer);
//		
//		// use case 2: links
//		QGisLayer linksLayer = new QGisLayer("links", workingDirectory + "links.shp", QGisConstants.geometryType.Line);
//		linksLayer.setRenderer(new SingleSymbolRenderer(linksLayer.getGeometryType()));
//		linksLayer.addAttribute("id");
//		linksLayer.addAttribute("length");
//		linksLayer.addAttribute("freespeed");
//		linksLayer.addAttribute("capacity");
//		linksLayer.addAttribute("nlanes");
//		writer.addLayer(linksLayer);
	
//		// use case 3: noise
//		QGisLayer networkLayer = new QGisLayer("network", "C:/Users/Daniel/Desktop/MATSimQGisIntegration/testFiles/network_detail/network.shp", QGisConstants.geometryType.Line);
//		networkLayer.setRenderer(new SingleSymbolRenderer(networkLayer.getGeometryType()));
//		writer.addLayer(networkLayer);
//		
//		QGisLayer noiseLayer = new QGisLayer("receiverPoints", "C:/Users/Daniel/Desktop/MATSimQGisIntegration/testFiles/baseCase_rpGap25meters/receiverPoints/receiverPoints.csv",
//				QGisConstants.geometryType.Point);
//		noiseLayer.addAttribute("receiverPointId");
//		noiseLayer.addAttribute("xCoord");
//		noiseLayer.addAttribute("yCoord");
//		GraduatedSymbolNoiseRenderer renderer = new GraduatedSymbolNoiseRenderer();
//		renderer.setRenderingAttribute("immissions_3600_Immission 11:00:00");
//		noiseLayer.setRenderer(renderer);
//		writer.addLayer(noiseLayer);
//		
//		QGisLayer joinLayer = new QGisLayer("immissions_3600", "C:/Users/Daniel/Desktop/MATSimQGisIntegration/testFiles/baseCase_rpGap25meters/immissions/100.immission_39600.0.csv",
//				QGisConstants.geometryType.No_geometry);
//		joinLayer.addAttribute("Receiver Point Id");
//		joinLayer.addAttribute("Immission 11:00:00");
//		writer.addLayer(joinLayer);
//
//		noiseLayer.addVectorJoin(joinLayer, "Receiver Point Id", "receiverPointId");
		
		// #########################################################
		
		//use case 4: accessibility
		QGisLayer noiseLayer = new QGisLayer("accessibility", "C:/Users/Daniel/Desktop/MATSimQGisIntegration/testFiles/accessibility/accessibility.csv",
				QGisConstants.geometryType.Point);
		noiseLayer.addAttribute("xCoord"); // required?
		noiseLayer.addAttribute("yCoord"); // required?
		noiseLayer.addAttribute("accessibility"); // required?
		GraduatedSymbolNoiseRenderer renderer = new GraduatedSymbolNoiseRenderer();
		renderer.setRenderingAttribute("accessibility"); // choose column/header to visualize
		noiseLayer.setRenderer(renderer);
		writer.addLayer(noiseLayer);

		writer.write(qGisProjectFile);

	}
	
}