package playground.dziemke.utils;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.facilities.ActivityFacility;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * @author dziemke
 * 
 * This class takes a given population/plans file and matches all activities of all plans of all persons to a given
 * shapefile. The feature keys, i.e. zone ids are stored for each activity as facility id (sic!) in a modified plans
 * file (=the output of this class).
 * Note: No "real" facilities are used. The facility id field is simply used to get the information on zones into
 * the plans file.
 * 
 */

public class AddZoneInformationToPlan {
	private static final Logger log = Logger.getLogger(AddZoneInformationToPlan.class);
	private final static GeometryFactory geometryFactory = new GeometryFactory();
	
	// parameters
	static String runId = "run_145f";
	static int iteration = 150;
	
	// input and output files
//	static String inputPlansFile = "D:/Workspace/data/cemdapMatsimCadyts/output/" + runId + "/ITERS/it." + iteration
//			+ "/" + runId + "." + iteration + ".plans.xml.gz";
//	static String outputPlansFile = "D:/Workspace/data/cemdapMatsimCadyts/output/" + runId + "/ITERS/it." + iteration
//			+ "/" + runId + "." + iteration + ".plansWithZonesAsFacilities.xml.gz";
	static String inputPlansFile = "D:/Workspace/runs-svn/cemdapCadyts/" + runId + "/ITERS/it." + iteration
			+ "/" + runId + "." + iteration + ".plans.xml.gz";
	static String outputPlansFile = "D:/Workspace/runs-svn/cemdapCadyts/" + runId + "/ITERS/it." + iteration
			+ "/" + runId + "." + iteration + ".plansWithZonesAsFacilities2.xml.gz";
	static String combinedShapeFile = "D:/Workspace/data/cemdapMatsimCadyts/input/shapefiles/gemeindenLOR_DHDN_GK4.shp";
	
	
	public static void main(String[] args) {
		// write all (geographic) features of planning area to a map
		Map<Id<ActivityFacility>, SimpleFeature> combinedFeatures = new HashMap<Id<ActivityFacility>, SimpleFeature>();
		for (SimpleFeature feature: ShapeFileReader.getAllFeatures(combinedShapeFile)) {
			Integer featureKey = Integer.parseInt((String) feature.getAttribute("NR"));
			
			// "abuse" facitiyId field to store zone
			Id<ActivityFacility> zoneId = Id.create(featureKey, ActivityFacility.class);
			
			combinedFeatures.put(zoneId,feature);
		}
					
		// load output plans/population
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		PopulationReader reader = new PopulationReader(scenario);
		reader.readFile(inputPlansFile);
		Population population = scenario.getPopulation();
		
		// new scenario with new/modified population
		Scenario modifiedScenario = ScenarioUtils.createScenario(config);
		Population modifiedPopulation = modifiedScenario.getPopulation();
			
		for (Person person : population.getPersons().values()) {
			Id<Person> personId = person.getId();
			Person modifiedPerson = population.getFactory().createPerson(personId);
						
			for(int i=0; i<person.getPlans().size(); i++) {
				Plan plan = person.getPlans().get(i);
				Plan modifiedPlan = population.getFactory().createPlan();
									
				for (int j=0; j<plan.getPlanElements().size(); j++) {
					PlanElement planElement = plan.getPlanElements().get(j);
					
					if (planElement instanceof Activity) {
						Activity activity = (Activity) planElement;
						
						double x = activity.getCoord().getX();
						double y = activity.getCoord().getY();
						
						Point activityCoordAsPoint = geometryFactory.createPoint(new Coordinate(x,y));
						
						Activity modifiedActivity = PopulationUtils.createActivity(activity);
						
						for(Id<ActivityFacility> id : combinedFeatures.keySet()) {
							SimpleFeature feature = combinedFeatures.get(id);
							Geometry geometry = (Geometry) feature.getDefaultGeometry();
						
							if (activityCoordAsPoint.within(geometry)) {
								if (modifiedActivity.getFacilityId() != null) {
									new RuntimeException("Faciliy ID should be empty since each activity can only" +
											"fall within one zone!");
								}
								modifiedActivity.setFacilityId(id);
							}							
						}
						
						if (modifiedActivity.getFacilityId() == null) {
							new RuntimeException("Modified activity must have a facility/zone id!");
						}
						
						modifiedPlan.addActivity(modifiedActivity);
					} else if (planElement instanceof Leg) {
						Leg leg = (Leg) planElement;
						modifiedPlan.addLeg(leg);
					} else {
						new RuntimeException("A plan element must either be an activity or a leg!");
					}
				}
				
				modifiedPerson.addPlan(modifiedPlan);		
			}
			
			modifiedPopulation.addPerson(modifiedPerson);		
		}
		
		// write population file
		new PopulationWriter(modifiedScenario.getPopulation(), null).write(outputPlansFile);
		
	log.info("Modified plans file " + outputPlansFile + " written.");
	}
}