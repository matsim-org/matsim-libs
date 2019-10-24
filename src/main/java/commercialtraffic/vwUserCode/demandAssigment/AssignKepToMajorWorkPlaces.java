package commercialtraffic.vwUserCode.demandAssigment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;

public class AssignKepToMajorWorkPlaces {
	static Map<String, String> mapWorkPlaceToLinks = new HashMap<>();
	
	public static void main(String[] args) {
		long nr = 1896;
		Random r = MatsimRandom.getRandom();
		r.setSeed(nr);
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		String input="D:\\Thiel\\Programme\\WVModell\\02_MatSimOutput\\vw280_CT_0.1_output\\";
		new PopulationReader(scenario).readFile(input+"\\2019-10-22_15-59-18__vw280_CT_0.1.output_plans.xml.gz");
		new MatsimNetworkReader(scenario.getNetwork()).readFile(
				"D:\\Thiel\\Programme\\MatSim\\01_HannoverModel_2.0\\Network\\00_Final_Network\\network_editedPt.xml.gz");

		Network network = scenario.getNetwork();

		mapWorkPlaceToLinks.put("vw", "105858");
		mapWorkPlaceToLinks.put("conti", "105986");
		mapWorkPlaceToLinks.put("klinikum", "18056");
		mapWorkPlaceToLinks.put("mhh", "143958");
		mapWorkPlaceToLinks.put("db", "324469");
		mapWorkPlaceToLinks.put("wabco", "135896");

		Map<String, Geometry> grid = new HashMap<>();
		Map<Person, Integer> workers2workPlaceMap = new HashMap<>();

		GeometryFactory gf = new GeometryFactory();

		for (String workPlace : mapWorkPlaceToLinks.keySet()) {

			Link link = network.getLinks().get(Id.createLinkId(mapWorkPlaceToLinks.get(workPlace)));
			Coord linkCoord = link.getCoord();
			Double linkCoordXmaxCoord = linkCoord.getX() + 300;
			Double linkCoordYmaxCoord = linkCoord.getY() + 300;
			Double linkCoordXminCoord = linkCoord.getY() - 300;
			Double linkCoordYminCoord = linkCoord.getY() - 300;
			Coordinate p1 = new Coordinate(linkCoordXmaxCoord, linkCoordYmaxCoord);
			Coordinate p2 = new Coordinate(linkCoordXminCoord, linkCoordYmaxCoord);
			Coordinate p3 = new Coordinate(linkCoordXminCoord, linkCoordYminCoord);
			Coordinate p4 = new Coordinate(linkCoordXmaxCoord, linkCoordYminCoord);
			Coordinate[] ca = { p1, p2, p3, p4, p1 };
			Polygon p = new Polygon(gf.createLinearRing(ca), null, gf);
			grid.put(workPlace + "", p);

		}

		for (Person p : scenario.getPopulation().getPersons().values()) {
			int peIdx=0;
			Plan plan = p.getSelectedPlan();
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					if (((Activity) pe).getType().startsWith("work")) {
						Coord workCoord = ((Activity) pe).getCoord();
						for (Entry<String, Geometry> e : grid.entrySet()) {

							if (e.getValue().intersects(MGC.coord2Point(workCoord))) {
								workers2workPlaceMap.put(p, peIdx);

							}
						}
					}
				}
				peIdx++;
			}
		}
		int changeDeliveryCounter=0;
		for (Person p : scenario.getPopulation().getPersons().values()) {

			Plan plan = p.getSelectedPlan();
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					if (!(pe.getAttributes().isEmpty())) {
						Map<String, Object> attMap = pe.getAttributes().getAsMap();
						for (Entry<String, Object> attr : attMap.entrySet()) {
							if (attr.getValue().toString().contains("KEP")) {
								if (workers2workPlaceMap.containsKey(p)&&r.nextDouble()<=0.5) {
									String[] att =attr.getValue().toString().split(";");
									String newAtt=att[0]+";"+att[1]+";"+"23400"+";"+"64800"+";"+"15";
									
									pe.getAttributes().removeAttribute(attr.getKey());
									plan.getPlanElements().get(workers2workPlaceMap.get(p)).getAttributes().putAttribute(attr.getKey(), newAtt);				
									
									changeDeliveryCounter++;
								}

							}
						}

					}
				}
			}
		}
		System.out.println(changeDeliveryCounter);
		writePopulation(scenario,input);
	}
	
	public static void writePopulation(Scenario scenario, String matsimInput) {
		String filename = matsimInput + "KepToMajorWorkLocations_0.5_plans_0.1.xml.gz";
		PopulationWriter writer = new PopulationWriter(scenario.getPopulation());
		writer.write(filename);
	}

}
