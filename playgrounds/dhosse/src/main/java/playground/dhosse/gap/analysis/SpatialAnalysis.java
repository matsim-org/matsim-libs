package playground.dhosse.gap.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.PointFeatureFactory.Builder;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsReaderMatsimV1;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;

import playground.dhosse.gap.Global;

public class SpatialAnalysis {

	public static void writePopulationToShape(String plansFile, String outputShapefile){
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(scenario).parse(plansFile);

		Builder builder = new Builder();
		builder.setCrs(MGC.getCRS(Global.toCrs));
		builder.addAttribute("personId", String.class);
		builder.addAttribute("actType", String.class);
		PointFeatureFactory pff = builder.create();
		
		List<SimpleFeature> features = new ArrayList<>();
		
		for(Person person : scenario.getPopulation().getPersons().values()){
			
			for(PlanElement pe : person.getSelectedPlan().getPlanElements()){
				
				if(pe instanceof Activity){
					
					Activity act = (Activity)pe;
					
					features.add(pff.createPoint(MGC.coord2Coordinate(act.getCoord()), new String[]{person.getId().toString(), act.getType()}, null));
					
				}
				
			}
			
		}
		
		ShapeFileWriter.writeGeometries(features, outputShapefile);
		
	}
	
	public static void writeNetworkToShape(String networkFile, String outputShapefile){
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		Network network = scenario.getNetwork();
		
		new MatsimNetworkReader(scenario).readFile(networkFile);
		
		org.matsim.core.utils.gis.PolylineFeatureFactory.Builder builder = new PolylineFeatureFactory.Builder();
		builder.setCrs(MGC.getCRS(Global.toCrs));
		builder.addAttribute("id", String.class);
		builder.addAttribute("length", Double.class);
		builder.addAttribute("capacity", Double.class);
		builder.addAttribute("freespeed", Double.class);
		builder.addAttribute("nLanes", Double.class);
		PolylineFeatureFactory factory = builder.create();
		
		List<SimpleFeature> features = new ArrayList<>();
		
		for(Link link : network.getLinks().values()){
			
			Map<String, Object> atts = new HashMap<>();
			atts.put("id", link.getId().toString());
			atts.put("length", link.getLength());
			atts.put("capacity", link.getCapacity());
			atts.put("freespeed", link.getFreespeed());
			atts.put("nLanes", link.getNumberOfLanes());
			
			features.add(factory.createPolyline(new Coordinate[]{MGC.coord2Coordinate(link.getFromNode().getCoord()),
					MGC.coord2Coordinate(link.getToNode().getCoord())}, atts, link.getId().toString()));
			
		}
		
		ShapeFileWriter.writeGeometries(features, outputShapefile);
		
	}
	
	public static void writeCountsToShape(String countsFile, String outputShapefile){
		
		Counts<Link> counts = new Counts();
		
		new CountsReaderMatsimV1(counts).parse(countsFile);
		
		Builder builder = new Builder();
		builder.setCrs(MGC.getCRS(Global.toCrs));
		builder.addAttribute("Id", String.class);
		builder.addAttribute("Name", String.class);
		PointFeatureFactory pff = builder.create();
		
		List<SimpleFeature> features = new ArrayList<>();
		
		for(Count count : counts.getCounts().values()){
			
			features.add(pff.createPoint(MGC.coord2Coordinate(count.getCoord())));
			
		}
		
		ShapeFileWriter.writeGeometries(features, outputShapefile);
		
	}
	
}
