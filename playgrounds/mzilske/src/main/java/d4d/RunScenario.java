package d4d;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

public class RunScenario {
	
	private ScenarioImpl scenario;

	public Scenario readScenario(Config config) {
		scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
		readPosts();
		readSample();
		return scenario;
	}
	
	private CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:3395");
	
	private void readPosts() {
		TabularFileParser tfp = new TabularFileParser();
		TabularFileParserConfig tabularFileParserConfig = new TabularFileParserConfig();
		tabularFileParserConfig.setFileName("/Users/zilske/d4d/ANT_POS.TSV");
		tabularFileParserConfig.setDelimiterRegex("\t");
		tfp.parse(tabularFileParserConfig, new TabularFileHandler() {
			
			@Override
			public void startRow(String[] row) {
				Coord coord = ct.transform(new CoordImpl(Double.parseDouble(row[1]), Double.parseDouble(row[2])));
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

	private void readSample() {

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
					Activity activity = scenario.getPopulation().getFactory().createActivityFromCoord("sighting", facility.getCoord());
					((ActivityImpl) activity).setLinkId(facility.getLinkId());
					activity.setEndTime((dateTimeFormat.parseDateTime(row[1]).getMillis() - beginning.getMillis()) / 1000);
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
	}

}
