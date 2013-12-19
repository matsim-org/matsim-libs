package playground.dziemke.analysis.location;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationReaderMatsimV5;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

/**
 * @author dziemke
 * based on "playground.dziemke.pots.analysis.disaggregatedPotsdamDisaggregatedAnalysis"
 */
public class ActivityTypeLocationAnalyzer {
	// Parameters
	static String runId = "run_132";
	static int iteration = 150;
	
	// Input file and output directory
	static String inputPlansFile = "D:/Workspace/container/demand/output/" + runId + "/ITERS/it." + iteration 
			+ "/" + runId + "." + iteration + ".plans.xml.gz";
	private static String outputDirectory = "D:/Workspace/container/demand/output/" + runId + "/ITERS/it." + iteration + "/";
	
	// Other objects
	private static PointFeatureFactory pointFeatureFactory;
	
	
	public static void main(String[] args) {
		Map <Id, Coord> homeCoords = new HashMap <Id, Coord>();

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		PopulationReaderMatsimV5 reader = new PopulationReaderMatsimV5(scenario);
		reader.readFile(inputPlansFile);
		Population population = scenario.getPopulation();
		
		int selectedStayHomePlans = 0;
		
		for (Person person : population.getPersons().values()) {
			Plan selectedPlan = person.getSelectedPlan();
			int numberOfPlanElements = selectedPlan.getPlanElements().size();
			
			if (numberOfPlanElements == 1) {
				selectedStayHomePlans++;
				//double score = selectedPlan.getScore();
				//System.out.println("Score of stay-home plan is " + score);
				
				Id id = person.getId();
				//TODO Now using 0th activity as home activity. Change it to what is specifically needed...
				Activity activity = (Activity) selectedPlan.getPlanElements().get(0);
				
				homeCoords.put(id, activity.getCoord());
			}
		}
		writeShapeFilePoints(outputDirectory + "stayHome.shp", homeCoords);		
		System.out.println("Number of selected stay-home plans is " + selectedStayHomePlans);
	}
	
	
	private static void writeShapeFilePoints(String outputShapeFile, Map <Id,Coord> coords) {
		if (coords.isEmpty()==true) {
			System.out.println("Map ist leer!");
		} else {
			initFeatureType();
			Collection <SimpleFeature> features = createFeatures(coords);
			ShapeFileWriter.writeGeometries(features, outputShapeFile);
			System.out.println("ShapeFile with points wrtitten to " + outputShapeFile);
		}
	}

	
	private static void initFeatureType() {
		// Before single feature can be created, the type has to be initialized here
		
		// NOTE: "TransformationFactory.DHDN_GK4" does not transform anything, but just returns the string "DHDN_GK4"
		// Essentially, one could also just fill in the string, but then there is no check if one spelled it correctly.
		
		// Via "addAttribute" a attributes of the feature type can be added and its name specified.
		// The value for this attribute can then be filled in when a single attribute of this type is created.
		
		// The effect of "setName" could not be retrieved yet.
		
		pointFeatureFactory = new PointFeatureFactory.Builder().
		setCrs(MGC.getCRS(TransformationFactory.DHDN_GK4)).
		//setCrs(MGC.getCRS("DHDN_GK4")).
		setName("points").
		addAttribute("Agent_ID", String.class).
		//addAttribute("Attribute2", String.class).
		create();
	}	
	
	
	private static Collection <SimpleFeature> createFeatures(Map<Id,Coord> coords) {
		List <SimpleFeature> features = new ArrayList <SimpleFeature>();
		for (Id id : coords.keySet()){
			Coord coord = coords.get(id);
			//features.add(getFeature(coords.get(i), i));
			Object[] attributes = new Object[]{id};
			//Object[] attributes = new Object[2];
			//attributes[1] = i;
			SimpleFeature feature = pointFeatureFactory.createPoint(coord, attributes, null);
			features.add(feature);
		}
		return features;
	}	
}
