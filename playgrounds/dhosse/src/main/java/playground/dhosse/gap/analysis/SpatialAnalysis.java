package playground.dhosse.gap.analysis;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.PointFeatureFactory.Builder;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.counts.Counts;
import org.opengis.feature.simple.SimpleFeature;

import playground.dhosse.gap.scenario.GAPMain;
import playground.michalm.util.postprocess.LinkStatsReader;
import playground.michalm.util.postprocess.LinkStatsReader.LinkStats;

public class SpatialAnalysis {

	public static void writePopulationToShape(String plansFile, String outputShapefile){
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(scenario).parse(plansFile);

		Builder builder = new Builder();
		builder.setCrs(MGC.getCRS(GAPMain.toCrs));
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
		
		ShapeFileWriter.writeGeometries(features, "/home/dhosse/Dokumente/01_eGAP/MATSim_input/pop.shp");
		
	}
	
	public static void createLinkWithVolumes(String linkStatsFile, String outputShapefile){
		
		List<? extends LinkStats> linkStatsList = LinkStatsReader.readLinkStats(linkStatsFile);
		
		for(LinkStats linkStats : linkStatsList){
			//TODO
		}
		
	}
	
}
