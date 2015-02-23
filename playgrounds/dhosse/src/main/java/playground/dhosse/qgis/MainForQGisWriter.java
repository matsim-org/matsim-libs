package playground.dhosse.qgis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.dhosse.qgis.layerTemplates.networkSimple.SingleSymbolRenderer;
import playground.dhosse.qgis.layerTemplates.noise.GraduatedSymbolNoiseRenderer;

public class MainForQGisWriter {
	
	public static void main(String args[]){
		
		String workingDirectory =  "C:/Users/Daniel/Desktop/MATSimQGisIntegration/";
		String qGisProjectFile = "test.qgs";
		
//		Config config = ConfigUtils.createConfig();
//		ConfigUtils.loadConfig(config, "C:/Users/Daniel/Desktop/noiseTest/config.xml");
//		ConfigUtils.loadConfig(config, "C:/Users/Daniel/Desktop/dvrp/cottbus_scenario/config.xml");
//		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		QGisWriter writer = new QGisWriter(TransformationFactory.DHDN_GK4, workingDirectory);
//		writer.setExtent(NetworkUtils.getBoundingBox(scenario.getNetwork().getNodes().values()));
		double[] extent = {0,0,5000,5000};
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
	
		// use case 3: noise
		QGisLayer noiseLayer = new QGisLayer("receiverPoints", "C:/Users/Daniel/Desktop/MATSimQGisIntegration/testFiles/baseCase_rpGap25meters/receiverPoints/receiverPoints.csv",
				QGisConstants.geometryType.Point);
		noiseLayer.addAttribute("receiverPointId");
		noiseLayer.addAttribute("xCoord");
		noiseLayer.addAttribute("yCorod");
		writer.addLayer(noiseLayer);
		
		QGisLayer joinLayer = new QGisLayer("immissions_3600", "C:/Users/Daniel/Desktop/MATSimQGisIntegration/testFiles/baseCase_rpGap25meters/immissions/100.immission_39600.0.csv",
				QGisConstants.geometryType.No_geometry);
		joinLayer.addAttribute("Receiver Point Id");
		joinLayer.addAttribute("Immission 11:00:00");
		writer.addLayer(joinLayer);

		noiseLayer.addVectorJoin(joinLayer, "Receiver Point Id", "receiverPointId");
		
		GraduatedSymbolNoiseRenderer renderer = new GraduatedSymbolNoiseRenderer();
		renderer.setRenderingAttribute("immissions_3600_Immission 11:00:00");
		noiseLayer.setRenderer(renderer);

		// #########################################################

		writer.write(qGisProjectFile);

	}
	
}