package gunnar.ihop2.scaper;

import floetteroed.utilities.Time;
import gunnar.ihop2.regent.demandreading.ShapeUtils;
import gunnar.ihop2.regent.demandreading.Zone;

import java.io.File;
import java.util.logging.Logger;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.xml.sax.Attributes;
import org.xml.sax.XMLReader;

import saleem.stockholmmodel.utils.StockholmTransformationFactory;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class ScaperPopulationCreator extends AbstractPopulationCreator {

	// -------------------- CONSTANTS --------------------

	// SOCIO-DEMOGRAPHICS

	static final String PERSON_ELEMENT = "person";

	static final String ID_ATTRIBUTE = "id";

	static final String AGE_ATTRIBUTE = "age";

	static final String EMPLOYED_ATTRIBUTE = "employed";

	// ACTIVITIES

	static final String ACTIVITY_ELEMENT = "act";

	static final String TYPE_ATTRIBUTE = "type";

	static final String ZONE_ATTRIBUTE = "zone";

	static final String ENDTIME_ATTRIBUTE = "end_time";

	static final String ACTIVITY_DURATION = "act_dur";

	// LEGS

	static final String LEG_ELEMENT = "leg";

	static final String MODE_ATTRIBUTE = "mode";

	static final String HOME_ACTIVITY = "h";

	static final String WORK_ACTIVITY = "w";

	static final String SHOPPING_ACTIVITY = "s";

	static final String LEISURE_ACTIVITY = "l";

	static final String OTHER_ACTIVITY = "o";

	static final String MODE_BICYCLE = "bicycle";

	static final String MODE_CAR = "car";

	static final String MODE_BUS = "bus";

	static final String MODE_TRAIN = "train";

	static final String MODE_TRAM = "tram";

	static final String MODE_RAIL = "rail";

	static final String MODE_PT = "pt";

	static final String MODE_WALK = "walk";

	static final String MODE_SUBWAY = "subway";

	// -------------------- MEMBERS --------------------

	private Plan plan = null;

	private Person person = null;

	private final CoordinateTransformation zone2popCoordTrafo = StockholmTransformationFactory
			.getCoordinateTransformation(
					StockholmTransformationFactory.WGS84_EPSG3857,
					StockholmTransformationFactory.WGS84_SWEREF99);

	// -------------------- CONSTRUCTION --------------------

	public ScaperPopulationCreator(final String networkFileName,
			final String zoneShapeFileName, final String zonalCoordinateSystem,
			final String populationFileNamePrefix) {
		super(networkFileName, zoneShapeFileName, zonalCoordinateSystem);

		final File populationFolder = new File(populationFileNamePrefix);
		for (File file : populationFolder.listFiles()) {
			if (file.getName().startsWith("population")) {
				final String populationFileName = file.getAbsolutePath();
				Logger.getLogger(this.getClass().getName()).info(
						"Loading population file " + populationFileName);
				try {
					final SAXParserFactory factory = SAXParserFactory
							.newInstance();
					factory.setValidating(false);
					factory.setNamespaceAware(false);
					final SAXParser parser = factory.newSAXParser();
					final XMLReader reader = parser.getXMLReader();
					reader.setContentHandler(this);
					reader.parse(populationFileName);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				Logger.getLogger(this.getClass().getName()).info(
						"Total number of persons loaded: "
								+ this.scenario.getPopulation().getPersons()
										.size());
			}
		}

	}

	// -------------------- OVERRIDING OF DefaultHandler --------------------

	@Override
	public void startElement(final String uri, final String lName,
			final String qName, final Attributes attrs) {

		if (PERSON_ELEMENT.equals(qName)) {
			// person is global now, and the previously read person is added, so
			// that we can assess whether to add a person or not after all
			// activities have been read for a person.
			// person is null if it a person hasn't been read fully yet, or has
			// an activity in zones with zero nodes
			if (person != null) {
				// To remove the end times of last activity
				removeLastActivityEndTimes(person);
				this.scenario.getPopulation().addPerson(person);
			}
			person = this.scenario
					.getPopulation()
					.getFactory()
					.createPerson(
							Id.createPersonId(attrs.getValue(ID_ATTRIBUTE)));
			PersonUtils.setEmployed(person,
					Boolean.parseBoolean(attrs.getValue(EMPLOYED_ATTRIBUTE)));
			PersonUtils.setAge(person,
					Integer.parseInt(attrs.getValue(AGE_ATTRIBUTE)));
			this.plan = this.scenario.getPopulation().getFactory().createPlan();
			person.addPlan(this.plan);
			System.out.println("reading person " + person.getId());

		} else if (ACTIVITY_ELEMENT.equals(qName)) {

			final Zone zone = this.zonalSystem.getZone(attrs
					.getValue(ZONE_ATTRIBUTE));
			final Coord coord = this.zone2popCoordTrafo.transform(ShapeUtils
					.drawPointFromGeometry(zone.getGeometry()));
			final Activity act = this.scenario
					.getPopulation()
					.getFactory()
					.createActivityFromCoord(
							ScaperToMatsimDictionary.scaper2matsim.get(attrs
									.getValue(TYPE_ATTRIBUTE)), coord);

			act.setEndTime(Time.secFromStr(attrs.getValue(ENDTIME_ATTRIBUTE)));
			// if (zeroNodesZones.contains(zone.getId().toString())) {
			if (this.zonalSystem.getNodes(attrs.getValue(ZONE_ATTRIBUTE))
					.isEmpty()) {
				person = null;// Exclude this person as it has atleast one
				// activity in a Zone with zero nodes
			}
			this.plan.addActivity(act);

		} else if (LEG_ELEMENT.equals(qName)) {

			final String mode = attrs.getValue(MODE_ATTRIBUTE);
			if (!"".equals(mode)) {
				final Leg leg = this.scenario
						.getPopulation()
						.getFactory()
						.createLeg(
								ScaperToMatsimDictionary.scaper2matsim
										.get(mode));
				this.plan.addLeg(leg);
			}
		}
	}

	public static void removeLastActivityEndTimes(Person person) {
		Activity activity = (Activity) person.getSelectedPlan()
				.getPlanElements()
				.get(person.getSelectedPlan().getPlanElements().size() - 1);
		activity.setEndTime(org.matsim.core.utils.misc.Time.UNDEFINED_TIME);
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTIN --------------------

	public static void main(String[] args) {

		System.out.println("STARTED ...");

		final String path = "./test/regentmatsim/";
		final String zonesShapeFileName = path
				+ "input/sverige_TZ_EPSG3857.shp";
		final String populationFileName = path + "exchange/scaper-sample.xml";
		final String plansFileName = path + "input/initial_plans.xml";
		final String networkFileName = path + "input/network-plain.xml";

		final ScaperPopulationCreator reader = new ScaperPopulationCreator(
				networkFileName, zonesShapeFileName, "WGS84_EPSG3857",
				populationFileName);

		PopulationWriter popwriter = new PopulationWriter(
				reader.scenario.getPopulation(), null);
		popwriter.write(plansFileName);

		System.out.println("... DONE");
	}
}
