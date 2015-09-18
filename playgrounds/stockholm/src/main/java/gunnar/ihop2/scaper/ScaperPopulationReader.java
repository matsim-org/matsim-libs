package gunnar.ihop2.scaper;

import floetteroed.utilities.Time;
import gunnar.ihop2.regent.demandreading.ShapeUtils;
import gunnar.ihop2.regent.demandreading.ZonalSystem;
import gunnar.ihop2.regent.demandreading.Zone;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.xml.sax.Attributes;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import saleem.stockholmscenario.utils.StockholmTransformationFactory;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class ScaperPopulationReader extends DefaultHandler {

	// -------------------- CONSTANTS --------------------

	// SOCIO-DEMOGRAPHICS

	static final String PERSON_ELEMENT = "person";

	static final String ID_ATTRIBUTE = "id";

	static final String AGE_ATTRIBUTE = "age";

	static final String EMPLOYED_ATTRIBUTE = "employed";

	// static final String WORKSTARTAFTER_ATTRIBUTE = "workstartafter";

	// static final String WORKSTARTBEFORE_ATTRIBUTE = "workstartbefore";

	// static final String WORKENDHOUR_ATTRIBUTE = "workendhour";

	// static final String WORKLENGTHMINUTES_ATTRIBUTE = "worklengthminutes";

	// ACTIVITIES

	static final String ACTIVITY_ELEMENT = "act";

	static final String TYPE_ATTRIBUTE = "type";

	static final String ZONE_ATTRIBUTE = "zone";

	static final String ENDTIME_ATTRIBUTE = "end_time";

	// LEGS

	static final String LEG_ELEMENT = "leg";

	static final String MODE_ATTRIBUTE = "mode";

	// -------------------- MEMBERS --------------------

	private final Scenario scenario;

	private final ZonalSystem zonalSystem;

	private final CoordinateTransformation coordinateTransformation;

	private Plan plan = null;

	// -------------------- CONSTRUCTION --------------------

	ScaperPopulationReader(final Scenario scenario,
			final ZonalSystem zonalSystem,
			final CoordinateTransformation coordinateTransformation,
			final String populationFileName) {
		this.scenario = scenario;
		this.zonalSystem = zonalSystem;
		this.coordinateTransformation = coordinateTransformation;
		try {
			final SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setValidating(false);
			factory.setNamespaceAware(false);
			final SAXParser parser = factory.newSAXParser();
			final XMLReader reader = parser.getXMLReader();
			reader.setContentHandler(this);
			/*
			 * ignore the DTD declaration
			 */
			// reader.setFeature("http://apache.org/xml/features/"
			// + "nonvalidating/load-external-dtd", false);
			// reader.setFeature("http://xml.org/sax/features/" + "validation",
			// false);
			reader.parse(populationFileName);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// -------------------- OVERRIDING OF DefaultHandler --------------------

	@Override
	public void startElement(final String uri, final String lName,
			final String qName, final Attributes attrs) {

		if (PERSON_ELEMENT.equals(qName)) {

			final Person person = this.scenario
					.getPopulation()
					.getFactory()
					.createPerson(
							Id.createPersonId(attrs.getValue(ID_ATTRIBUTE)));
			PersonUtils.setEmployed(person, Boolean.parseBoolean(attrs
					.getValue(EMPLOYED_ATTRIBUTE)));
			PersonUtils.setAge(person, Integer.parseInt(attrs
					.getValue(AGE_ATTRIBUTE)));
			this.scenario.getPopulation().addPerson(person);

			this.plan = this.scenario.getPopulation().getFactory().createPlan();
			person.addPlan(this.plan);
			System.out.println("reading person " + person.getId());

		} else if (ACTIVITY_ELEMENT.equals(qName)) {

			final Zone zone = this.zonalSystem.getZone(attrs
					.getValue(ZONE_ATTRIBUTE));
			final Coord coord = this.coordinateTransformation
					.transform(ShapeUtils.drawPointFromGeometry(zone
							.getGeometry()));
			final Activity act = this.scenario
					.getPopulation()
					.getFactory()
					.createActivityFromCoord(attrs.getValue(TYPE_ATTRIBUTE),
							coord);
			act.setEndTime(Time.secFromStr(attrs.getValue(ENDTIME_ATTRIBUTE)));
			this.plan.addActivity(act);

		} else if (LEG_ELEMENT.equals(qName)) {

			final String mode = attrs.getValue(MODE_ATTRIBUTE);
			if (!"".equals(mode)) {
				final Leg leg = this.scenario.getPopulation().getFactory()
						.createLeg(mode);
				this.plan.addLeg(leg);
			}
		}
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTIN --------------------

	public static void main(String[] args) {

		System.out.println("STARTED ...");

		final String zonesShapeFileName = "./data/shapes/sverige_TZ_EPSG3857.shp";
		final String populationFileName = "./data/scaper/allind.xml";
		final String plansFileName = "./data/scaper/initial_plans.xml";

		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils
				.createConfig());
		final ZonalSystem zonalSystem = new ZonalSystem(zonesShapeFileName);
		final CoordinateTransformation coordinateTransform = StockholmTransformationFactory
				.getCoordinateTransformation(
						StockholmTransformationFactory.WGS84_EPSG3857,
						StockholmTransformationFactory.WGS84_SWEREF99);
		final ScaperPopulationReader reader = new ScaperPopulationReader(
				scenario, zonalSystem, coordinateTransform, populationFileName);

		PopulationWriter popwriter = new PopulationWriter(
				scenario.getPopulation(), null);
		popwriter.write(plansFileName);

		System.out.println("... DONE");
	}
}
