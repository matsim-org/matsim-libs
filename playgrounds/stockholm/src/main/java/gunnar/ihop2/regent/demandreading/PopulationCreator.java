package gunnar.ihop2.regent.demandreading;

import static gunnar.ihop2.regent.demandreading.RegentPopulationReader.HOMEZONE_ATTRIBUTE;
import static gunnar.ihop2.regent.demandreading.RegentPopulationReader.HOUSINGTYPE_ATTRIBUTE;
import static gunnar.ihop2.regent.demandreading.RegentPopulationReader.OTHERTOURMODE_ATTRIBUTE;
import static gunnar.ihop2.regent.demandreading.RegentPopulationReader.OTHERZONE_ATTRIBUTE;
import static gunnar.ihop2.regent.demandreading.RegentPopulationReader.WORKTOURMODE_ATTRIBUTE;
import static gunnar.ihop2.regent.demandreading.RegentPopulationReader.WORKZONE_ATTRIBUTE;
import static gunnar.ihop2.regent.demandreading.ShapeUtils.drawPointFromGeometry;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.population.algorithms.XY2Links;
import org.matsim.utils.objectattributes.ObjectAttributeUtils2;
import org.matsim.utils.objectattributes.ObjectAttributes;

import patryk.utils.LinksRemover;
import saleem.stockholmscenario.utils.StockholmTransformationFactory;
import floetteroed.utilities.math.Covariance;
import floetteroed.utilities.math.MathHelpers;
import floetteroed.utilities.math.Vector;
import gunnar.ihop2.regent.RegentDictionary;
import gunnar.ihop2.utils.FractionalIterable;

/**
 * 
 * @author Gunnar Flötteröd, based on Patryk Larek
 *
 */
public class PopulationCreator {

	// -------------------- CONSTANTS --------------------

	// TODO make configurable
	private final Random rnd = new Random();

	public static final String HOME = "home";

	public static final String WORK = "work";

	public static final String OTHER = "other";

	public static final String VILLA = "villa";

	public static final String APARTMENT = "apartment";

	// -------------------- MEMBERS --------------------

	private final Scenario scenario;

	private final ZonalSystem zonalSystem;

	private final Map<String, Zone> id2usedZone;

	private double populationSampleFactor = 1.0;

	private String agentHomeXYFileName = null;

	private String agentWorkXYFileName = null;

	private PrintWriter agentHomeXYWriter = null;

	private PrintWriter agentWorkXYWriter = null;

	private Covariance netCoordStats = new Covariance(2, 2);

	private Covariance homeCoordStats = new Covariance(2, 2);

	private Covariance workCoordStats = new Covariance(2, 2);

	// -------------------- CONSTRUCTION --------------------

	public PopulationCreator(final String networkFileName,
			final String zoneShapeFileName, final String zonalCoordinateSystem,
			final String populationFileName) {

		this.scenario = ScenarioUtils
				.createScenario(ConfigUtils.createConfig());
		(new MatsimNetworkReader(this.scenario)).readFile(networkFileName);

		for (Node node : this.scenario.getNetwork().getNodes().values()) {
			final Vector coords = new Vector(node.getCoord().getX(), node
					.getCoord().getY());
			this.netCoordStats.add(coords, coords);
		}

		this.zonalSystem = new ZonalSystem(zoneShapeFileName,
				zonalCoordinateSystem);

		final RegentPopulationReader regentPopulationReader = new RegentPopulationReader(
				populationFileName, this.zonalSystem, this.scenario
						.getPopulation().getPersonAttributes());

		this.id2usedZone = regentPopulationReader.id2usedZone;
	}

	public void setBuildingsFileName(final String buildingShapeFileName) {
		this.zonalSystem.addBuildings(buildingShapeFileName);
	}

	public void setAgentHomeXYFile(final String agentHomeXYFile) {
		this.agentHomeXYFileName = agentHomeXYFile;
	}

	public void setAgentWorkXYFile(final String agentWorkXYFileName) {
		this.agentWorkXYFileName = agentWorkXYFileName;
	}

	public void setNetworkNodeXYFile(final String networkNodeXYFile)
			throws FileNotFoundException {
		final PrintWriter writer = new PrintWriter(networkNodeXYFile);
		for (Node node : this.scenario.getNetwork().getNodes().values()) {
			writer.print(node.getCoord().getX());
			writer.print(";");
			writer.println(node.getCoord().getY());
		}
		writer.flush();
		writer.close();
	}

	public void setPopulationSampleFactor(final double populationSampleFactor) {
		this.populationSampleFactor = populationSampleFactor;
	}

	public void setLinkAttributes(final ObjectAttributes linkAttributes) {
		final Set<String> tmLinkIds = new LinkedHashSet<String>(
				ObjectAttributeUtils2.allObjectKeys(linkAttributes));
		final Set<Id<Link>> removeTheseLinkIds = new LinkedHashSet<Id<Link>>();
		for (Id<Link> candidateId : this.scenario.getNetwork().getLinks()
				.keySet()) {
			if (!tmLinkIds.contains(candidateId.toString())) {
				removeTheseLinkIds.add(candidateId);
			}
		}
		for (Id<Link> linkId : removeTheseLinkIds) {
			this.scenario.getNetwork().removeLink(linkId);
		}
	}

	// -------------------- INTERNALS --------------------

	// private Geometry drawGeometry(final Zone zone, final String buildingType)
	// {
	//
	// // TODO this is inefficient
	// final Vector apartmentProbas = this.newSizeProportionalProbas(zone
	// .getMultiFamilyBuildings());
	// final Vector officeProbas = this.newSizeProportionalProbas(zone
	// .getWorkBuildings());
	//
	// final Random rnd = MatsimRandom.getLocalInstance();
	// Building building = null;
	//
	// if (HOME.equals(activityType)) {
	//
	// if (VILLA.equals(buildingType)) {
	// if (!zone.getSingleFamilyBuildings().isEmpty()) {
	// building = zone.getSingleFamilyBuildings()
	// .get(rnd.nextInt(zone.getSingleFamilyBuildings()
	// .size()));
	// } else {
	// Logger.getLogger(MATSimDummy.class.getName()).warning(
	// "no villas in zone " + zone.getId());
	// }
	// } else if (APARTMENT.equals(buildingType)) {
	// if (!zone.getMultiFamilyBuildings().isEmpty()) {
	// building = zone.getMultiFamilyBuildings().get(
	// MathHelpers.draw(apartmentProbas, rnd));
	// } else {
	// Logger.getLogger(MATSimDummy.class.getName()).warning(
	// "no apartments in zone " + zone.getId());
	// }
	// } else {
	// Logger.getLogger(MATSimDummy.class.getName()).severe(
	// "unknown housing type " + buildingType);
	// throw new RuntimeException("unknown housing type "
	// + buildingType);
	// }
	//
	// } else if (WORK.equals(activityType)) {
	//
	// if (!zone.getWorkBuildings().isEmpty()) {
	// building = zone.getWorkBuildings().get(
	// MathHelpers.draw(officeProbas, rnd));
	// } else {
	// Logger.getLogger(MATSimDummy.class.getName()).warning(
	// "no work buildings in zone " + zone.getId());
	// }
	//
	// } else {
	// Logger.getLogger(MATSimDummy.class.getName()).severe(
	// "unkown activity: " + activityType);
	// throw new RuntimeException("unknown activity: " + activityType);
	// }
	//
	// if (building != null) {
	// return building.getGeometry();
	// } else {
	// return zone.getGeometry();
	// }
	// }

	private void addHomeActivity(final Plan plan, final Coord homeCoord,
			final Double endTime_s) {
		final Activity initialHome = this.scenario.getPopulation().getFactory()
				.createActivityFromCoord(HOME, homeCoord);
		if (endTime_s != null) {
			initialHome.setEndTime(endTime_s);
		}
		plan.addActivity(initialHome);
	}

	private void addTour(final Plan plan, final String type,
			final Coord actCoord, final String mode, final Double endTime_s) {
		// leg to activity
		final Leg homeToWork = this.scenario.getPopulation().getFactory()
				.createLeg(mode);
		plan.addLeg(homeToWork);
		// activity itself
		final Activity work = this.scenario.getPopulation().getFactory()
				.createActivityFromCoord(type, actCoord);
		if (endTime_s != null) {
			work.setEndTime(endTime_s);
		}
		plan.addActivity(work);
		// leg back home
		final Leg workToHome = this.scenario.getPopulation().getFactory()
				.createLeg(mode);
		plan.addLeg(workToHome);
	}

	private String attr(final String who, final String what) {
		return (String) this.scenario.getPopulation().getPersonAttributes()
				.getAttribute(who, what);
	}

	private void addPlan(final Person person,
			final CoordinateTransformation coordinateTransform) {

		/*
		 * (0) Every person has an id and lives somewhere.
		 */
		final String personId = person.getId().toString();
		final Zone homeZone = this.id2usedZone.get(this.attr(personId,
				HOMEZONE_ATTRIBUTE));
		final String homeBuildingType = (String) this.attr(personId,
				HOUSINGTYPE_ATTRIBUTE);
		final Coord homeCoord = coordinateTransform
				.transform(drawPointFromGeometry(homeZone
						.drawHomeGeometry(homeBuildingType)));

		// TODO CONTINUE HERE

		/*
		 * (1) Give the person an empty plan.
		 */

		final Plan plan = this.scenario.getPopulation().getFactory()
				.createPlan();
		person.addPlan(plan);

		/*
		 * (2) Identify what (if at all) tours are made.
		 */

		final Zone workZone = this.id2usedZone.get(this.scenario
				.getPopulation().getPersonAttributes()
				.getAttribute(personId, WORKZONE_ATTRIBUTE));
		final Zone otherZone = this.id2usedZone.get(this.scenario
				.getPopulation().getPersonAttributes()
				.getAttribute(personId, OTHERZONE_ATTRIBUTE));

		/*
		 * (3) Construct plan depending on what tours are there.
		 */

		final double workDuration_s = 9.0 * 3600.0;
		final double intermediateHomeDuration_s = 0.5 * 3600.0;
		final double otherDuration_s = 1.5 * 3600.0;
		final double tripDuration_s = 0.5 * 3600;

		if ((workZone != null) && (otherZone == null)) {

			/*
			 * HOME - WORK - HOME
			 */

			final double initialHomeEndTime_s = MathHelpers.draw(6.0, 8.0,
					this.rnd) * 3600.0;
			final double workEndTime_s = initialHomeEndTime_s + tripDuration_s
					+ workDuration_s;

			this.addHomeActivity(plan, homeCoord, initialHomeEndTime_s);

			final String workTourMode = RegentDictionary.regent2matsim
					.get((String) this.scenario.getPopulation()
							.getPersonAttributes()
							.getAttribute(personId, WORKTOURMODE_ATTRIBUTE));
			final Coord workCoord = coordinateTransform
					.transform(drawPointFromGeometry(workZone
							.drawWorkGeometry()));
			this.addTour(plan, WORK, workCoord, workTourMode, workEndTime_s);

		} else if ((workZone == null) && (otherZone != null)) {

			final double initialHomeEndTime_s = MathHelpers.draw(6.0, 21.0,
					this.rnd) * 3600.0;
			final double otherEndTime_s = initialHomeEndTime_s + tripDuration_s
					+ otherDuration_s;

			/*
			 * HOME - OTHER - HOME
			 */
			this.addHomeActivity(plan, homeCoord, initialHomeEndTime_s);

			final String otherTourMode = RegentDictionary.regent2matsim
					.get((String) this.scenario.getPopulation()
							.getPersonAttributes()
							.getAttribute(personId, OTHERTOURMODE_ATTRIBUTE));
			final Coord otherCoord = coordinateTransform
					.transform(drawPointFromGeometry(otherZone
							.drawOtherGeometry()));
			this.addTour(plan, OTHER, otherCoord, otherTourMode, otherEndTime_s);

		} else if ((homeZone != null) && (workZone != null)) {

			final double initialHomeEndTime_s = MathHelpers.draw(6.0, 8.0,
					this.rnd) * 3600.0;
			final double workEndTime_s = initialHomeEndTime_s + tripDuration_s
					+ workDuration_s;
			final double intermediateHomeEndTime_s = workEndTime_s
					+ tripDuration_s + intermediateHomeDuration_s;
			final Double otherEndTime_s = intermediateHomeEndTime_s
					+ tripDuration_s + otherDuration_s;

			/*
			 * HOME - WORK - HOME - OTHER - HOME
			 */
			this.addHomeActivity(plan, homeCoord, initialHomeEndTime_s);

			final String workTourMode = RegentDictionary.regent2matsim
					.get((String) this.scenario.getPopulation()
							.getPersonAttributes()
							.getAttribute(personId, WORKTOURMODE_ATTRIBUTE));
			final Coord workCoord = coordinateTransform.transform(ShapeUtils
					.drawPointFromGeometry(workZone.drawWorkGeometry()));
			this.addTour(plan, WORK, workCoord, workTourMode, workEndTime_s);

			this.addHomeActivity(plan, homeCoord, intermediateHomeEndTime_s);

			final String otherTourMode = RegentDictionary.regent2matsim
					.get((String) this.scenario.getPopulation()
							.getPersonAttributes()
							.getAttribute(personId, OTHERTOURMODE_ATTRIBUTE));
			final Coord otherCoord = coordinateTransform
					.transform(drawPointFromGeometry(otherZone
							.drawOtherGeometry()));
			this.addTour(plan, OTHER, otherCoord, otherTourMode, otherEndTime_s);
		}

		/*
		 * Last (open-end) home activity of the day.
		 */
		this.addHomeActivity(plan, homeCoord, null);
	}

	private Person newPerson(final String personId, final XY2Links xy2links,
			final CoordinateTransformation coordinateTransform) {

		// create a new person with an empty plan

		final Person person = this.scenario.getPopulation().getFactory()
				.createPerson(Id.createPersonId(personId));
		this.addPlan(person, coordinateTransform);

		// final Plan plan = this.scenario.getPopulation().getFactory()
		// .createPlan();
		// person.addPlan(plan);
		//
		// // identify all zones
		//
		// /*
		// * Starting at home.
		// */
		//
		// final Zone homeZone = this.id2usedZone.get(this.scenario
		// .getPopulation().getPersonAttributes()
		// .getAttribute(personId, HOMEZONE_ATTRIBUTE));
		// final String homeBuildingType = (String)
		// this.scenario.getPopulation()
		// .getPersonAttributes()
		// .getAttribute(personId, HOUSINGTYPE_ATTRIBUTE);
		// final Coord homeCoord = coordinateTransform.transform(ShapeUtils
		// .drawPointFromGeometry(this.drawGeometry(homeZone,
		// homeBuildingType)));
		// final Activity home = this.scenario.getPopulation().getFactory()
		// .createActivityFromCoord(HOME, homeCoord);
		// home.setEndTime(7 * 3600);
		// plan.addActivity(home);
		//
		// /*
		// * Work tour.
		// */
		//
		// final Zone workZone = this.id2usedZone.get(this.scenario
		// .getPopulation().getPersonAttributes()
		// .getAttribute(personId, WORKZONE_ATTRIBUTE));
		//
		// final String workTourMode = RegentDictionary.regent2matsim
		// .get((String) this.scenario.getPopulation()
		// .getPersonAttributes()
		// .getAttribute(personId, WORKTOURMODE_ATTRIBUTE));
		//
		// if (workZone != null && "car".equals(workTourMode)) {
		//
		// // travel to work
		//
		// final Leg homeToWork = this.scenario.getPopulation().getFactory()
		// .createLeg(workTourMode);
		// plan.addLeg(homeToWork);
		//
		// // work
		//
		// final String workBuildingType = (String) this.scenario
		// .getPopulation().getPersonAttributes()
		// .getAttribute(personId, // TODO why do they work in houses?
		// RegentPopulationReader.HOUSINGTYPE_ATTRIBUTE);
		// final Coord workCoord = coordinateTransform.transform(ShapeUtils
		// .drawPointFromGeometry(this.drawGeometry(workZone,
		// workBuildingType)));
		// final Activity work = this.scenario.getPopulation().getFactory()
		// .createActivityFromCoord(WORK, workCoord);
		// work.setEndTime(17 * 3600);
		// ;
		// plan.addActivity(work);
		//
		// // travel back home
		//
		// final Leg workToHome = this.scenario.getPopulation().getFactory()
		// .createLeg(workTourMode);
		// plan.addLeg(workToHome);
		//
		// final Activity home2 = this.scenario.getPopulation().getFactory()
		// .createActivityFromCoord(HOME, homeCoord);
		// home2.setEndTime(19 * 3600);
		// plan.addActivity(home2);
		//
		// }
		//
		// /*
		// * Other tour.
		// */
		//
		// final Zone otherZone = this.id2usedZone.get(this.scenario
		// .getPopulation().getPersonAttributes()
		// .getAttribute(personId, OTHERZONE_ATTRIBUTE));
		//
		// final String otherTourMode = RegentDictionary.regent2matsim
		// .get((String) this.scenario.getPopulation()
		// .getPersonAttributes()
		// .getAttribute(personId, OTHERTOURMODE_ATTRIBUTE));
		//
		// if (otherZone != null && "car".equals(otherTourMode)) {
		//
		// // travel to work
		//
		// final Leg homeToOther = this.scenario.getPopulation().getFactory()
		// .createLeg(otherTourMode);
		// plan.addLeg(homeToOther);
		//
		// // other
		//
		// final String otherBuildingType = (String) this.scenario
		// .getPopulation().getPersonAttributes()
		// .getAttribute(personId, // TODO why happens "other" in
		// // houses?
		// RegentPopulationReader.HOUSINGTYPE_ATTRIBUTE);
		// final Coord otherCoord = coordinateTransform.transform(ShapeUtils
		// .drawPointFromGeometry(this.drawGeometry(otherZone,
		// otherBuildingType)));
		// final Activity other = this.scenario.getPopulation().getFactory()
		// .createActivityFromCoord(OTHER, otherCoord);
		// other.setEndTime(21 * 3600);
		// plan.addActivity(other);
		//
		// // travel back home
		//
		// final Leg otherToHome = this.scenario.getPopulation().getFactory()
		// .createLeg(otherTourMode);
		// plan.addLeg(otherToHome);
		//
		// // at home
		//
		// final Activity home3 = this.scenario.getPopulation().getFactory()
		// .createActivityFromCoord(HOME, homeCoord);
		// plan.addActivity(home3);
		//
		// }
		//
		// // final Activity homeEvening =
		// // this.scenario.getPopulation().getFactory()
		// // .createActivityFromCoord(HOME, homeCoord);
		// // plan.addActivity(homeEvening);
		//
		// // socio-demographics
		//
		// // ((PersonImpl) person).setSex((String)
		// this.scenario.getPopulation()
		// // .getPersonAttributes()
		// // .getAttribute(personId, RegentPopulationReader.SEX_ATTRIBUTE));
		// // PersonUtils.setEmployed(person, workZone != null);
		// // PersonUtils.setAge(
		// // person,
		// // 2015 - (Integer) this.scenario.getPopulation()
		// // .getPersonAttributes()
		// // .getAttribute(personId, BIRTHYEAR_ATTRIBUTE));
		//
		// // if (this.scenario
		// // .getPopulation().getPersonAttributes()
		// // .getAttribute(personId, WORKZONE_ATTRIBUTE) == null) {
		// // System.out.println(personId + " has no work zone");
		// // System.exit(-1);
		// // }
		//
		// // assign activity coordinates to links
		//
		// xy2links.run(person);
		//
		// // do coordinate logging if needed
		//
		// // this.homeCoordStats.add(new Vector(homeCoord.getX(),
		// // homeCoord.getY()),
		// // new Vector(homeCoord.getX(), homeCoord.getY()));
		// // this.workCoordStats.add(new Vector(workCoord.getX(),
		// // workCoord.getY()),
		// // new Vector(workCoord.getX(), workCoord.getY()));
		// //
		// // if (this.agentHomeXYWriter != null) {
		// // this.agentHomeXYWriter.print(homeCoord.getX());
		// // this.agentHomeXYWriter.print(";");
		// // this.agentHomeXYWriter.println(homeCoord.getY());
		// // }
		// // if (this.agentWorkXYWriter != null) {
		// // this.agentWorkXYWriter.print(workCoord.getX());
		// // this.agentWorkXYWriter.print(";");
		// // this.agentWorkXYWriter.println(workCoord.getY());
		// // }

		return person;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void run(final String initialPlansFile) throws FileNotFoundException {

		// int processedPersons = 0;
		// int everyXthPerson = (int) (1.0 / this.populationSampleFactor);

		// >>>>> TODO remove links where we do not want activities >>>>>
		final LinksRemover linksRem = new LinksRemover(
				this.scenario.getNetwork());
		linksRem.run();
		// <<<<< TODO <<<<<

		final XY2Links xy2links = new XY2Links(this.scenario);
		final CoordinateTransformation coordinateTransform = StockholmTransformationFactory
				.getCoordinateTransformation(
						StockholmTransformationFactory.WGS84_EPSG3857,
						StockholmTransformationFactory.WGS84_SWEREF99);

		// final Map<String, Zone> id2clippedZone;
		// if (this.zonesBoundaryShapeFileName != null) {
		// id2clippedZone = this.zonalSystem
		// .getZonesInsideBoundary(this.zonesBoundaryShapeFileName);
		// } else {
		// id2clippedZone = this.zonalSystem.getId2zoneView();
		// }

		final ObjectAttributes personAttributes = this.scenario.getPopulation()
				.getPersonAttributes();

		if (this.agentHomeXYFileName != null) {
			this.agentHomeXYWriter = new PrintWriter(this.agentHomeXYFileName);
		}
		if (this.agentWorkXYFileName != null) {
			this.agentWorkXYWriter = new PrintWriter(this.agentWorkXYFileName);
		}

		// for (String personId : ObjectAttributeUtils2
		// .allObjectKeys(personAttributes)) {
		for (String personId : new FractionalIterable<>(
				ObjectAttributeUtils2.allObjectKeys(personAttributes),
				this.populationSampleFactor)) {

			final String homeZone = (String) personAttributes.getAttribute(
					personId, HOMEZONE_ATTRIBUTE);

			final String workZone = (String) personAttributes.getAttribute(
					personId, WORKZONE_ATTRIBUTE);
			// final String workTourMode = (String)
			// personAttributes.getAttribute(
			// personId, WORKTOURMODE_ATTRIBUTE);

			final String otherZone = (String) personAttributes.getAttribute(
					personId, OTHERZONE_ATTRIBUTE);
			// final String otherTourMode = (String) personAttributes
			// .getAttribute(personId, OTHERTOURMODE_ATTRIBUTE);

			if (this.zonalSystem.getId2zoneView().keySet().contains(homeZone)
					&& ((workZone == null) || this.zonalSystem.getId2zoneView()
							.keySet().contains(workZone))
					&& ((otherZone == null) || this.zonalSystem
							.getId2zoneView().keySet().contains(otherZone))) {
				// if (processedPersons % everyXthPerson == 0) {
				Logger.getLogger(this.getClass().getName()).info(
						"creating person " + personId);
				final Person person = this.newPerson(personId, xy2links,
						coordinateTransform);
				this.scenario.getPopulation().addPerson(person);
				// }
				// processedPersons++;
			}
			// if (this.zonalSystem.getId2zoneView().keySet().contains(homeZone)
			// && this.zonalSystem.getId2zoneView().keySet()
			// .contains(workZone) && (
			// // RegentPopulationReader.PT_ATTRIBUTEVALUE
			// // .equals(workTourMode) ||
			// RegentPopulationReader.CAR_ATTRIBUTEVALUE
			// .equals(workTourMode))) {
			// if (processedPersons % everyXthPerson == 0) {
			// Logger.getLogger(MATSimDummy.class.getName()).info(
			// "Person " + personId + ": homeZone = " + homeZone
			// + ", workZone = " + workZone
			// + ", workTourMode = " + workTourMode
			// + "; this is the " + processedPersons
			// + "th agent.");
			// final Person person = this.newPerson(personId, xy2links,
			// coordinateTransform);
			// this.scenario.getPopulation().addPerson(person);
			//
			// }
			// processedPersons++;
			// }
		}

		PopulationWriter popwriter = new PopulationWriter(
				scenario.getPopulation(), this.scenario.getNetwork());
		popwriter.write(initialPlansFile);

		if (this.agentHomeXYWriter != null) {
			this.agentHomeXYWriter.flush();
			this.agentHomeXYWriter.close();
			this.agentHomeXYWriter = null;
		}
		if (this.agentWorkXYWriter != null) {
			this.agentWorkXYWriter.flush();
			this.agentWorkXYWriter.close();
			this.agentWorkXYWriter = null;
		}

	}

	// ========== TODO TO BE REARRANGED ==========

	// MAIN-FUNCTION

	public static void main(String[] args) throws FileNotFoundException {

		System.out.println("STARTED ...");

		final String zonesShapeFileName = "./data_ZZZ/shapes/sverige_TZ_EPSG3857.shp";
		final String buildingShapeFileName = "./data_ZZZ/shapes/by_full_EPSG3857_2.shp";
		final String populationFileName = "./data_ZZZ/synthetic_population/151008_trips.xml";

		final String networkFileName = "./data_ZZZ/run/network-plain.xml";
		// final String linkAttributesFileName =
		// "./data/run/link-attributes.xml";
		final String initialPlansFile = "./data_ZZZ/run/initial-plans-FULL.xml";

		// final ObjectAttributes linkAttributes = new ObjectAttributes();
		// final ObjectAttributesXmlReader reader = new
		// ObjectAttributesXmlReader(
		// linkAttributes);
		// reader.parse(linkAttributesFileName);

		final PopulationCreator pc = new PopulationCreator(networkFileName,
				zonesShapeFileName,
				StockholmTransformationFactory.WGS84_EPSG3857,
				populationFileName);
		pc.setBuildingsFileName(buildingShapeFileName);
		// pc.setAgentHomeXYFile("./data/demand_output/agenthomeXY_v03.txt");
		// pc.setAgentWorkXYFile("./data/demand_output/agentWorkXY_v03.txt");
		// pc.setNetworkNodeXYFile("./data/demand_output/nodeXY_v03.txt");
		// pc.setZonesBoundaryShapeFileName("./data/shapes/limit_EPSG3857.shp");
		pc.setPopulationSampleFactor(0.01);
		// pc.setLinkAttributes(linkAttributes);
		pc.run(initialPlansFile);

		// System.out.println("NETWORK NODE COORDINATE STATISTICS");
		// System.out.println("center point: " + pc.netCoordStats.getMeanX()
		// + ", " + pc.netCoordStats.getMeanY());
		// System.out.println("standard dev: "
		// + Math.sqrt(pc.netCoordStats.getCovariance().get(0, 0)) + ", "
		// + Math.sqrt(pc.netCoordStats.getCovariance().get(1, 1)));
		// System.out.println();
		// System.out.println("POPULATION HOME COORDINATE STATISTICS");
		// System.out.println("center point: " + pc.homeCoordStats.getMeanX());
		// System.out.println("standard dev: "
		// + Math.sqrt(pc.homeCoordStats.getCovariance().get(0, 0)) + ", "
		// + Math.sqrt(pc.homeCoordStats.getCovariance().get(1, 1)));

		System.out.println("... DONE");
	}
}
