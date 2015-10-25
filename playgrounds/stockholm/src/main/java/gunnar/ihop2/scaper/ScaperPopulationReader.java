package gunnar.ihop2.scaper;

import floetteroed.utilities.Time;
import gunnar.ihop2.regent.demandreading.ShapeUtils;
import gunnar.ihop2.regent.demandreading.ZonalSystem;
import gunnar.ihop2.regent.demandreading.Zone;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkReaderMatsimV1;
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

	private final Scenario scenario;

	private final ZonalSystem zonalSystem;

	private final CoordinateTransformation coordinateTransformation;

	private Plan plan = null;
	private Person person = null;
	private static ArrayList<String> zeroNodesZones = new ArrayList<String>();

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
			// person is global now, and the previously read person is added, so that we can assess whether to add a person or not after all activities have been read for a person.
			// person is null if it a person hasn't been read fully yet, or has an activity in zones with zero nodes 
			if(person!=null){
				//To remove the end times of last activity
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
			final Coord coord = this.coordinateTransformation
					.transform(ShapeUtils.drawPointFromGeometry(zone
							.getGeometry()));
			final Activity act = this.scenario
					.getPopulation()
					.getFactory()
					.createActivityFromCoord(ScaperToMatsimDictionary.scaper2matsim.get(attrs.getValue(TYPE_ATTRIBUTE)),coord);
//			Uncomment the following lines if you want to allocate starting time to the activities
//			String activityduration = attrs.getValue(ACTIVITY_DURATION);
//			if(activityduration.contains("E")){
//				int activitydurationsecs = 60*(int) (Double.parseDouble(activityduration.substring(0,activityduration.indexOf('E')))
//						*Math.pow(10, Double.parseDouble(activityduration.substring(activityduration.indexOf('+'),activityduration.length()))));
//				act.setStartTime(Time.secFromStr(attrs.getValue(ENDTIME_ATTRIBUTE))-activitydurationsecs);
//			}else{
//				int activitydurationsecs = (int)(60*Double.parseDouble(activityduration));
//				act.setStartTime(Time.secFromStr(attrs.getValue(ENDTIME_ATTRIBUTE))-activitydurationsecs);
//			}
			
			act.setEndTime(Time.secFromStr(attrs.getValue(ENDTIME_ATTRIBUTE)));
			if(zeroNodesZones.contains(zone.getId().toString())){
				person=null;//Exclude this person as it has atleast one activity in a Zone with zero nodes
			}
			this.plan.addActivity(act);

		} else if (LEG_ELEMENT.equals(qName)) {

			final String mode = attrs.getValue(MODE_ATTRIBUTE);
			if (!"".equals(mode)) {
				final Leg leg = this.scenario.getPopulation().getFactory()
						.createLeg(ScaperToMatsimDictionary.scaper2matsim.get(mode));
				this.plan.addLeg(leg);
			}
		}
	}
	public static void removeLastActivityEndTimes(Person person){
			Activity activity = (Activity)person.getSelectedPlan().getPlanElements().get(person.getSelectedPlan().getPlanElements().size()-1);
			activity.setEndTime(org.matsim.core.utils.misc.Time.UNDEFINED_TIME);
	}
	// -------------------- MAIN-FUNCTION, ONLY FOR TESTIN --------------------

	public static void main(String[] args) {

		System.out.println("STARTED ...");

		final String zonesShapeFileName = "./data/shapes/sverige_TZ_EPSG3857.shp";
		final String populationFileName = "./data/scaper/151014_trips.xml";
		final String plansFileName = "./data/scaper/initial_plans.xml";
		final String networkFileName = "./data/run/network-plain.xml";
		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils
				.createConfig());
		//Reading network
		NetworkReaderMatsimV1 networkreader = new NetworkReaderMatsimV1(scenario);
		networkreader.parse(networkFileName);
		final ZonalSystem zonalSystem = new ZonalSystem(zonesShapeFileName,
				StockholmTransformationFactory.WGS84_EPSG3857);
		zonalSystem.addNetwork(scenario.getNetwork(), StockholmTransformationFactory.WGS84_SWEREF99);
		final CoordinateTransformation coordinateTransform = StockholmTransformationFactory
				.getCoordinateTransformation(
						StockholmTransformationFactory.WGS84_EPSG3857,
						StockholmTransformationFactory.WGS84_SWEREF99);
		//To identify zones with zero nodes
		Map<String, Zone> zones = zonalSystem.getId2zoneView();
		Iterator iter = zones.keySet().iterator();
		while(iter.hasNext()){
			Zone zone = zones.get(iter.next());
			if(zonalSystem.getNodes(zone.getId()).size()==0){
				zeroNodesZones.add(zone.getId().toString());
			}
		}
		final ScaperPopulationReader reader = new ScaperPopulationReader(
				scenario, zonalSystem, coordinateTransform, populationFileName);
		PopulationWriter popwriter = new PopulationWriter(
				scenario.getPopulation(), null);
		popwriter.write(plansFileName);

		System.out.println("... DONE");
	}
}
