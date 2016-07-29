package playground.artemc.scenarioTools;

import java.io.IOException;
import java.util.HashMap;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitScheduleImpl;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

public class PopulationTransitStopDistanceCalculator {

	private static HashMap<String, double[]> stops = new HashMap<String, double[]>();
	private static ObjectAttributes distances = new ObjectAttributes();

	public static void main(String[] args) throws IOException {

		String populationPath = args[0];
		String transitSchedulePath = args[1];
		String distanceAttributesPath = args[2];

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().transit().setUseTransit(true);
		new PopulationReader(scenario).readFile(populationPath);
		new TransitScheduleReaderV1(scenario).readFile(transitSchedulePath);

		Population population = (Population) scenario.getPopulation();
		TransitScheduleImpl schedule = (TransitScheduleImpl) scenario.getTransitSchedule();
		
		for(TransitLine line:schedule.getTransitLines().values()){
			for(TransitRoute route:line.getRoutes().values()){
				for(TransitRouteStop stop:route.getStops()){
					double[] coord = new double[2];
					coord[0] = stop.getStopFacility().getCoord().getX();
					coord[1] = stop.getStopFacility().getCoord().getY();
					stops.put(stop.getStopFacility().getId().toString(), coord);
				}
			}
		}
		
		
		for(Person person:population.getPersons().values()){
				Activity firstActivity = (Activity) person.getSelectedPlan().getPlanElements().get(0);

				
				double x = firstActivity.getCoord().getX();
				double y = firstActivity.getCoord().getY();
				
				double shortestDistance = 999999.0;
				String closestStop = "";
				
				for(String stopId:stops.keySet()){
					double distance = Math.sqrt((stops.get(stopId)[0] - x)*(stops.get(stopId)[0] - x)+(stops.get(stopId)[1] - y)*(stops.get(stopId)[1] - y));
					if(distance<shortestDistance){
						shortestDistance = distance;
						closestStop = stopId;
					}
				}
				
				distances.putAttribute(person.getId().toString(), "stopDistance", shortestDistance);
				System.out.println(person.getId().toString()+"   "+closestStop+"  "+shortestDistance);
				
		}
		
		ObjectAttributesXmlWriter attributesWriter = new ObjectAttributesXmlWriter(distances);
		attributesWriter.writeFile(distanceAttributesPath);

		System.out.println("Done!");
		
	}
}
