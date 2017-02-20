package gunnar.ihop2.regent.demandreading;

import static gunnar.ihop2.regent.RegentDictionary.regent2matsim;
import static gunnar.ihop2.regent.demandreading.RegentPopulationReader.CAR_ATTRIBUTEVALUE;
import static gunnar.ihop2.regent.demandreading.RegentPopulationReader.HOMEZONE_ATTRIBUTE;
import static gunnar.ihop2.regent.demandreading.RegentPopulationReader.HOUSINGTYPE_ATTRIBUTE;
import static gunnar.ihop2.regent.demandreading.RegentPopulationReader.OTHERTOURMODE_ATTRIBUTE;
import static gunnar.ihop2.regent.demandreading.RegentPopulationReader.OTHERZONE_ATTRIBUTE;
import static gunnar.ihop2.regent.demandreading.RegentPopulationReader.WORKTOURMODE_ATTRIBUTE;
import static gunnar.ihop2.regent.demandreading.RegentPopulationReader.WORKZONE_ATTRIBUTE;
import static gunnar.ihop2.regent.demandreading.ShapeUtils.drawPointFromGeometry;
import static org.matsim.utils.objectattributes.ObjectAttributeUtils2.allObjectKeys;
import static saleem.stockholmmodel.utils.StockholmTransformationFactory.WGS84_EPSG3857;
import static saleem.stockholmmodel.utils.StockholmTransformationFactory.WGS84_SWEREF99;
import static saleem.stockholmmodel.utils.StockholmTransformationFactory.getCoordinateTransformation;

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.algorithms.XY2Links;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.utils.objectattributes.ObjectAttributeUtils2;
import org.matsim.utils.objectattributes.ObjectAttributes;

import floetteroed.utilities.math.MathHelpers;
import gunnar.ihop2.utils.FractionalIterable;
import saleem.stockholmmodel.utils.StockholmTransformationFactory;

/**
 * 
 * @author Gunnar Flötteröd, based on Patryk Larek
 *
 */
public class PopulationCreator {

	// -------------------- CONSTANTS --------------------

	private final Random rnd = MatsimRandom.getRandom(); // TODO

	public static final String W1S = "w1s";
	public static final String W1L = "w1l";
	public static final String HgivenW1S = "h|w1s";
	public static final String HgivenW1L = "h|w1l";

	public static final String O1 = "o1";
	public static final String HgivenO1 = "h|o1";

	public static final String W2S = "w2s";
	public static final String W2L = "w2l";
	public static final String HgivenW2S = "h|w2s";
	public static final String HgivenW2L = "h|w2l";
	// public static final String H2givenW2S = "h2|w2s";
	// public static final String H2givenW2L = "h2|w2l";
	public static final String O2givenW2S = "o2|w2s";
	public static final String O2givenW2L = "o2|w2l";

	// public static final String HOME = "home";
	// public static final String WORK = "work";
	// public static final String OTHER = "other";

	// anything not completely nonsensical, just to get started
	final double workDuration_s = 8.0 * 3600.0;
	final double intermediateHomeDuration_s = 1.0 * 3600.0;
	final double otherDuration_s = 1.0 * 3600.0;
	final double tripDuration_s = 0.5 * 3600;

	// -------------------- MEMBERS --------------------

	private final Scenario scenario;

	private final ZonalSystem zonalSystem;

	private double populationSampleFactor = 1.0;

	// -------------------- CONSTRUCTION --------------------

	/*
	 * TODO When adding the link attributes, the zonal system is not affected,
	 * meaning that zones may keep pointers at nodes that have been removed. The
	 * difference is probably not so large but still this is inconsistent.
	 * 
	 * Better pass on the link attributes (file name) into this constructor and
	 * first reduce the network and only then add it to the zonal system.
	 */
	public PopulationCreator(final String networkFileName,
			final String zoneShapeFileName, final String zonalCoordinateSystem,
			final String populationFileName) {
		this.scenario = ScenarioUtils
				.createScenario(ConfigUtils.createConfig());
		(new MatsimNetworkReader(this.scenario.getNetwork())).readFile(networkFileName);
		this.zonalSystem = new ZonalSystem(zoneShapeFileName,
				zonalCoordinateSystem);
		this.zonalSystem.addNetwork(this.scenario.getNetwork(),
				StockholmTransformationFactory.WGS84_SWEREF99);
		Logger.getLogger(this.getClass().getName()).info(
				"number of zones in zonal system is "
						+ this.zonalSystem.getId2zoneView().size());
		new RegentPopulationReader(populationFileName, this.zonalSystem,
				this.scenario.getPopulation().getPersonAttributes());
	}

	public void setBuildingsFileName(final String buildingShapeFileName) {
		this.zonalSystem.addBuildings(buildingShapeFileName);
	}

	public void setPopulationSampleFactor(final double populationSampleFactor) {
		this.populationSampleFactor = populationSampleFactor;
	}

	public void removeExpandedLinks(final ObjectAttributes linkAttributes) {
		final Set<String> tmLinkIds = new LinkedHashSet<String>(
				ObjectAttributeUtils2.allObjectKeys(linkAttributes));
		final Set<Id<Link>> removeTheseLinkIds = new LinkedHashSet<Id<Link>>();
		for (Id<Link> candidateId : this.scenario.getNetwork().getLinks()
				.keySet()) {
			if (!tmLinkIds.contains(candidateId.toString())) {
				removeTheseLinkIds.add(candidateId);
			}
		}
		Logger.getLogger(this.getClass().getName()).info(
				"Excluding " + removeTheseLinkIds.size()
						+ " expanded links from being activity locations.");
		for (Id<Link> linkId : removeTheseLinkIds) {
			this.scenario.getNetwork().removeLink(linkId);
		}
	}

	// -------------------- INTERNALS --------------------

	private Coord drawHomeCoord(final String personId,
			final CoordinateTransformation coordinateTransform) {
		final Zone homeZone = this.zonalSystem.getZone(this.attr(personId,
				HOMEZONE_ATTRIBUTE));
		if (!this.zonalSystem.getNodes(homeZone).isEmpty()) {
			final String homeBuildingType = (String) this.attr(personId,
					HOUSINGTYPE_ATTRIBUTE);
			return coordinateTransform.transform(drawPointFromGeometry(homeZone
					.drawHomeGeometry(homeBuildingType)));
		}
		return null;
	}

	private Coord drawWorkCoordinate(final String personId,
			final CoordinateTransformation coordinateTransform) {
		if (CAR_ATTRIBUTEVALUE.equals(this.attr(personId,
				WORKTOURMODE_ATTRIBUTE))) {
			final Zone workZone = this.zonalSystem.getZone(this.attr(personId,
					WORKZONE_ATTRIBUTE));
			if (!this.zonalSystem.getNodes(workZone).isEmpty()) {
				return coordinateTransform
						.transform(drawPointFromGeometry(workZone
								.drawWorkGeometry()));
			}
		}
		return null;
	}

	private Coord drawOtherCoordinate(final String personId,
			final CoordinateTransformation coordinateTransform) {
		if (CAR_ATTRIBUTEVALUE.equals(this.attr(personId,
				OTHERTOURMODE_ATTRIBUTE))) {
			final Zone otherZone = this.zonalSystem.getZone(this.attr(personId,
					OTHERZONE_ATTRIBUTE));
			if (!this.zonalSystem.getNodes(otherZone).isEmpty()) {
				return coordinateTransform
						.transform(drawPointFromGeometry(otherZone
								.drawWorkGeometry()));
			}
		}
		return null;
	}

	private void addHomeActivity(final Plan plan, final Coord homeCoord,
			final Double endTime_s, final String type) {
		final Activity home = this.scenario.getPopulation().getFactory()
				.createActivityFromCoord(type, homeCoord);
		if (endTime_s != null) {
			home.setEndTime(endTime_s);
		}
		plan.addActivity(home);
	}

	private void addTour(final Plan plan, final String type,
			final Coord actCoord, final String mode, final Double endTime_s) {

		// leg to activity
		final Leg homeToAct = this.scenario.getPopulation().getFactory()
				.createLeg(mode);
		plan.addLeg(homeToAct);

		// activity itself
		final Activity act = this.scenario.getPopulation().getFactory()
				.createActivityFromCoord(type, actCoord);
		if (endTime_s != null) {
			act.setEndTime(endTime_s);
		}
		plan.addActivity(act);

		// leg back home
		final Leg actToHome = this.scenario.getPopulation().getFactory()
				.createLeg(mode);
		plan.addLeg(actToHome);
	}

	private String attr(final String who, final String what) {
		return (String) this.scenario.getPopulation().getPersonAttributes()
				.getAttribute(who, what);
	}

	private Person newPerson(final String personId, final XY2Links xy2links,
			final CoordinateTransformation coordinateTransform) {

		final Coord homeCoord = this.drawHomeCoord(personId,
				coordinateTransform);
		if (homeCoord == null) {
			return null; // ------------------------------------------------
		}

		/*
		 * (1) Create a person with socio-demographics and an empty single plan.
		 */
		final Person person = this.scenario.getPopulation().getFactory()
				.createPerson(Id.createPersonId(personId));
		// socio-demographics
		// ((PersonImpl) person).setSex((String) this.scenario.getPopulation()
		// .getPersonAttributes()
		// .getAttribute(personId, RegentPopulationReader.SEX_ATTRIBUTE));
		// PersonUtils.setEmployed(person, workZone != null);
		// PersonUtils.setAge(
		// person,
		// 2015 - (Integer) this.scenario.getPopulation()
		// .getPersonAttributes()
		// .getAttribute(personId, BIRTHYEAR_ATTRIBUTE));

		final Plan plan = this.scenario.getPopulation().getFactory()
				.createPlan();
		person.addPlan(plan);

		/*
		 * (2) Construct plan based on what tours are made.
		 */
		final Coord workCoord = this.drawWorkCoordinate(personId,
				coordinateTransform);
		final Coord otherCoord = this.drawOtherCoordinate(personId,
				coordinateTransform);

		if ((workCoord != null) && (otherCoord == null)) {

			/*
			 * HOME - WORK - HOME
			 */

			final boolean shortWork = (this.rnd.nextDouble() < 0.63);

			final double initialHomeEndTime_s = MathHelpers.draw(5.0, 7.0,
					this.rnd) * 3600.0;
			this.addHomeActivity(plan, homeCoord, initialHomeEndTime_s,
					(shortWork ? HgivenW1S : HgivenW1L));

			final double workEndTime_s = initialHomeEndTime_s
					+ this.tripDuration_s + this.workDuration_s;
			final String workTourMode = regent2matsim.get(this.attr(personId,
					WORKTOURMODE_ATTRIBUTE));
			this.addTour(plan, (shortWork ? W1S : W1L), workCoord,
					workTourMode, workEndTime_s);

			this.addHomeActivity(plan, homeCoord, null, (shortWork ? HgivenW1S
					: HgivenW1L));

		} else if ((workCoord == null) && (otherCoord != null)) {

			/*
			 * HOME - OTHER - HOME
			 */
			final double initialHomeEndTime_s = MathHelpers.draw(6.0, 22.0,
					this.rnd) * 3600.0;
			this.addHomeActivity(plan, homeCoord, initialHomeEndTime_s,
					HgivenO1);

			final double otherEndTime_s = initialHomeEndTime_s
					+ this.tripDuration_s + this.otherDuration_s;
			final String otherTourMode = regent2matsim.get(this.attr(personId,
					OTHERTOURMODE_ATTRIBUTE));
			this.addTour(plan, O1, otherCoord, otherTourMode, otherEndTime_s);

			this.addHomeActivity(plan, homeCoord, null, HgivenO1);

		} else if ((homeCoord != null) && (workCoord != null)) {

			/*
			 * HOME - WORK - HOME - OTHER - HOME
			 */

			final boolean shortWork = (this.rnd.nextDouble() < 0.41);

			final double initialHomeEndTime_s = MathHelpers.draw(5.0, 7.0,
					this.rnd) * 3600.0;
			this.addHomeActivity(plan, homeCoord, initialHomeEndTime_s,
					(shortWork ? HgivenW2S : HgivenW2L));

			final double workEndTime_s = initialHomeEndTime_s
					+ this.tripDuration_s + this.workDuration_s;
			final String workTourMode = regent2matsim.get(this.attr(personId,
					WORKTOURMODE_ATTRIBUTE));
			this.addTour(plan, (shortWork ? W2S : W2L), workCoord,
					workTourMode, workEndTime_s);

			final double intermediateHomeEndTime_s = workEndTime_s
					+ this.tripDuration_s + this.intermediateHomeDuration_s;
			this.addHomeActivity(plan, homeCoord, intermediateHomeEndTime_s,
					(shortWork ? HgivenW2S : HgivenW2L));

			final double otherEndTime_s = intermediateHomeEndTime_s
					+ this.tripDuration_s + this.otherDuration_s;
			final String otherTourMode = regent2matsim.get(this.attr(personId,
					OTHERTOURMODE_ATTRIBUTE));
			this.addTour(plan, (shortWork ? O2givenW2S : O2givenW2L),
					otherCoord, otherTourMode, otherEndTime_s);

			this.addHomeActivity(plan, homeCoord, null, (shortWork ? HgivenW2S
					: HgivenW2L));

		} else {

			return null;

		}

		/*
		 * Assign activity coordinates to links.
		 */
		xy2links.run(person);

		return person;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void run(final String initialPlansFile) throws FileNotFoundException {

		final XY2Links xy2links = new XY2Links(this.scenario);
		final CoordinateTransformation coordinateTransform = getCoordinateTransformation(
				WGS84_EPSG3857, WGS84_SWEREF99);

		final List<String> allPersonIds = allObjectKeys(this.scenario
				.getPopulation().getPersonAttributes());
		Collections.shuffle(allPersonIds);

		for (String personId : new FractionalIterable<>(allPersonIds,
				this.populationSampleFactor)) {

			final Person person = this.newPerson(personId, xy2links,
					coordinateTransform);
			if (person != null) {
				// Logger.getLogger(this.getClass().getName()).info(
				// "creating person " + personId);
				this.scenario.getPopulation().addPerson(person);
			}
		}

		PopulationWriter popwriter = new PopulationWriter(
				scenario.getPopulation(), this.scenario.getNetwork());
		popwriter.write(initialPlansFile);
	}

	// MAIN-FUNCTION

	public static void main(String[] args) throws FileNotFoundException {

		System.out.println("STARTED ...");

		final String zonesShapeFileName = "./ihop2/demand-input/sverige_TZ_EPSG3857.shp";
		final String buildingShapeFileName = "./ihop2/demand-input/by_full_EPSG3857_2.shp";

		final String populationFileName = "./ihop2/demand-input/trips.xml";

		final String networkFileName = "./ihop2/network-output/network.xml";
		final String initialPlansFile = "./ihop2/demand-output/initial-plans.xml";
		// final String linkAttributesFileName =
		// "./data/run/link-attributes.xml";

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
