package playground.dhosse.gap.scenario.roadpricing;

import java.util.Collection;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingSchemeImpl;
import org.matsim.roadpricing.RoadPricingWriterXMLv1;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;

import playground.dhosse.gap.Global;

public class CreateRoadpricingScheme {
	
	public static void main(String args[]){
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(Global.runInputDir + "merged-networkV2_20150929.xml");
		
		CreateRoadpricingScheme.run(scenario);
		
	}
	
	public static void run(Scenario scenario){
		
		RoadPricingSchemeImpl rsp = new RoadPricingSchemeImpl();
		rsp.setName("austria roadpricing");
		rsp.setType(RoadPricingScheme.TOLL_TYPE_DISTANCE);
		
		double amountPerMeter = 0.000038;
		
		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures("/home/dhosse/Downloads/austria/AUT_adm/AUT_adm0.shp");
		Geometry boundary = null;
		
		for(SimpleFeature feature : features){
			boundary = (Geometry)feature.getDefaultGeometry();
		}
		
		for(Link link : scenario.getNetwork().getLinks().values()){
			
			Coord coord = Global.reverseCt.transform(link.getCoord());
			Coord from = Global.reverseCt.transform(link.getFromNode().getCoord());
			Coord to = Global.reverseCt.transform(link.getToNode().getCoord());
			
			if(/*boundary.contains(MGC.coord2Point(coord)) && */(boundary.contains(MGC.coord2Point(from))|| boundary.contains(MGC.coord2Point(to)))){
				
				rsp.addLink(link.getId());
				rsp.addLinkCost(link.getId(), 0.0, 30.0 * 3600, amountPerMeter);
				
			}
			
		}
		
		new RoadPricingWriterXMLv1(rsp).writeFile("/home/dhosse/roadpricing.xml");
		
	}
	
}
