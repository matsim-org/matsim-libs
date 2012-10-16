package d4d;

import java.io.FileNotFoundException;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

public class RunScenario {
	
	double minLong = -4.265;
	double maxLong = -3.671;
	double minLat = 5.175;
	double maxLat = 5.53;
	
	
	boolean filter = true;
	
	private ScenarioImpl scenario;

	public Scenario readScenario(Config config)  {
		scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
		readPosts();
		try {
			readSample();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return scenario;
	}
	
	private CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:3395");
	

	Coord min = ct.transform(new CoordImpl(minLong, minLat));
	Coord max = ct.transform(new CoordImpl(minLat, maxLat));
	
	private void readPosts() {
		TabularFileParser tfp = new TabularFileParser();
		TabularFileParserConfig tabularFileParserConfig = new TabularFileParserConfig();
		tabularFileParserConfig.setFileName("/Users/zilske/d4d/ANT_POS.TSV");
		tabularFileParserConfig.setDelimiterRegex("\t");
		tfp.parse(tabularFileParserConfig, new TabularFileHandler() {
			
			@Override
			public void startRow(String[] row) {
				CoordImpl longLat = new CoordImpl(Double.parseDouble(row[1]), Double.parseDouble(row[2]));
				Coord coord = ct.transform(longLat);
				if (Double.isNaN(coord.getX()) || Double.isNaN(coord.getY())) {
					throw new RuntimeException("Bad latlong: " + coord);
				}
				IdImpl linkId = new IdImpl(row[0]);
				IdImpl nodeId = linkId;
				IdImpl facilityId = linkId;
				Node node = scenario.getNetwork().getFactory().createNode(nodeId, coord);
				scenario.getNetwork().addNode(node);
				scenario.getNetwork().addLink(scenario.getNetwork().getFactory().createLink(linkId, node, node));
				ActivityFacilityImpl facility = scenario.getActivityFacilities().createFacility(facilityId, coord);
				facility.setLinkId(linkId);
			}
		});
	}
	

	private void readSample() throws FileNotFoundException {

		final DateTimeFormatter dateTimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
		final DateTime beginning = dateTimeFormat.parseDateTime("2011-12-06 00:00:00");
		TabularFileParser tfp = new TabularFileParser();
		TabularFileParserConfig tabularFileParserConfig = new TabularFileParserConfig();
		tabularFileParserConfig.setFileName("/Users/zilske/d4d/POS_SAMPLE_0.TSV");
		tabularFileParserConfig.setDelimiterRegex("\t");
		tfp.parse(tabularFileParserConfig, new TabularFileHandler() {
			
			@Override
			public void startRow(String[] row) {
				String cellTowerId = row[2];
				if (!cellTowerId.equals("-1")) {
					Facility facility = scenario.getActivityFacilities().getFacilities().get(new IdImpl(cellTowerId));
					Coord coord = facility.getCoord();
					Activity activity = scenario.getPopulation().getFactory().createActivityFromCoord("sighting", coord);
					((ActivityImpl) activity).setLinkId(facility.getLinkId());
					DateTime sighting = dateTimeFormat.parseDateTime(row[1]);
					activity.setEndTime((sighting.getMillis() - beginning.getMillis()) / 1000);
					IdImpl personId = new IdImpl(row[0]);
					Person person = scenario.getPopulation().getPersons().get(personId);
					if (person == null) {
						person = scenario.getPopulation().getFactory().createPerson(personId);
						person.addPlan(scenario.getPopulation().getFactory().createPlan());
						scenario.getPopulation().addPerson(person);
					} else {
						
						Leg leg = scenario.getPopulation().getFactory().createLeg("unknown");
						person.getSelectedPlan().addLeg(leg);
						
						
					}
					person.getSelectedPlan().addActivity(activity);
				}
			}
		});

		System.out.println(scenario.getPopulation().getPersons().size());
		new InitialStatistics("").run(scenario.getPopulation());
		
		Population cityPopulation = new PopulationImpl(scenario);
		Population nonCityPopulation = new PopulationImpl(scenario);
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			if (planContainsActivityInCity(plan)) {
				cityPopulation.addPerson(person);
			} else {
				nonCityPopulation.addPerson(person);
			}
			
		}
		System.out.println(cityPopulation.getPersons().size());
		new InitialStatistics("-capital-only").run(cityPopulation);
		System.out.println(nonCityPopulation.getPersons().size());
		new InitialStatistics("-countryside-only").run(nonCityPopulation);
	}


	private boolean planContainsActivityInCity(Plan plan) {
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				Activity activity = (Activity) pe;
				Coord coord = activity.getCoord();
				if (coord.getX() >= min.getX() && coord.getX() < max.getX() && coord.getY() >= min.getY() && coord.getY() < max.getY()) {
					return true;
				}
			}
		}
		return false;
	}

}
