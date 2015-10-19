package gunnar.ihop2.regent.demandreading;

import static gunnar.ihop2.regent.demandreading.RegentPopulationReader.HOMEZONE_ATTRIBUTE;
import static gunnar.ihop2.regent.demandreading.RegentPopulationReader.HOUSINGTYPE_ATTRIBUTE;
import static gunnar.ihop2.regent.demandreading.RegentPopulationReader.WORKTOURMODE_ATTRIBUTE;
import static gunnar.ihop2.regent.demandreading.RegentPopulationReader.WORKZONE_ATTRIBUTE;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.LinkedHashSet;
import java.util.List;
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
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.population.algorithms.XY2Links;
import org.matsim.utils.objectattributes.ObjectAttributeUtils2;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import patryk.popgen2.Building;
import patryk.utils.LinksRemover;
import saleem.stockholmscenario.utils.StockholmTransformationFactory;

import com.vividsolutions.jts.geom.Geometry;

import floetteroed.utilities.math.Covariance;
import floetteroed.utilities.math.MathHelpers;
import floetteroed.utilities.math.Vector;
import gunnar.ihop2.integration.MATSimDummy;
import gunnar.ihop2.regent.RegentDictionary;

/**
 * 
 * @author Gunnar Flötteröd, based on Patryk Larek
 *
 */
public class PopulationCreator {

	// -------------------- CONSTANTS --------------------

	public static final String HOME = "home";

	public static final String WORK = "work";

	public static final String VILLA = "villa";

	public static final String APARTMENT = "apartment";

	// -------------------- MEMBERS --------------------

	private final Scenario scenario;

	private final ZonalSystem zonalSystem;

	private final Map<String, Zone> id2usedZone;

	private double populationSampleFactor = 1.0;

	// private String zonesBoundaryShapeFileName = null;

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

	// public void setZonesBoundaryShapeFileName(
	// final String zonesBoundaryShapeFileName) {
	// this.zonesBoundaryShapeFileName = zonesBoundaryShapeFileName;
	// }

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

	// public void setNodeAttributeFileName(final String nodeAttributeFileName)
	// {
	// final ObjectAttributes nodeAttributes = new ObjectAttributes();
	// for (Map.Entry<Id<Node>, ? extends Node> id2node : this.scenario
	// .getNetwork().getNodes().entrySet()) {
	//
	// }
	// final ObjectAttributesXmlWriter nodeAttributesWriter = new
	// ObjectAttributesXmlWriter(
	// nodeAttributes);
	// nodeAttributesWriter.writeFile(nodeAttributeFileName);
	// }

	// -------------------- INTERNALS --------------------

	private Vector newSizeProportionalProbas(
			final List<? extends Building> buildings) {
		if (buildings == null || buildings.isEmpty()) {
			return null;
		} else {
			final Vector result = new Vector(buildings.size());
			for (int i = 0; i < buildings.size(); i++) {
				result.set(i, buildings.get(i).getBuildingSize());
			}
			result.mult(1.0 / result.sum());
			return result;
		}
	}

	private int drawHomeEndTime_s() {
		return 7 * 3600;
	}

	private int drawWorkEndTime_s() {
		return 16 * 3600;
	}

	// TODO Some redundancy in the parameters; makes sense once people can work
	// at home, which is apparently included in the Regent output !!!
	private Geometry drawGeometry(final Zone zone, final String housingType,
			final String activityType) {

		// TODO this is inefficient
		final Vector apartmentProbas = this.newSizeProportionalProbas(zone
				.getMultiFamilyBuildings());
		final Vector officeProbas = this.newSizeProportionalProbas(zone
				.getWorkBuildings());

		final Random rnd = MatsimRandom.getLocalInstance();
		Building building = null;

		if (HOME.equals(activityType)) {

			if (VILLA.equals(housingType)) {
				if (!zone.getSingleFamilyBuildings().isEmpty()) {
					building = zone.getSingleFamilyBuildings()
							.get(rnd.nextInt(zone.getSingleFamilyBuildings()
									.size()));
				} else {
					Logger.getLogger(MATSimDummy.class.getName()).warning(
							"no villas in zone " + zone.getId());
				}
			} else if (APARTMENT.equals(housingType)) {
				if (!zone.getMultiFamilyBuildings().isEmpty()) {
					building = zone.getMultiFamilyBuildings().get(
							MathHelpers.draw(apartmentProbas, rnd));
				} else {
					Logger.getLogger(MATSimDummy.class.getName()).warning(
							"no apartments in zone " + zone.getId());
				}
			} else {
				Logger.getLogger(MATSimDummy.class.getName()).severe(
						"unknown housing type " + housingType);
				throw new RuntimeException("unknown housing type "
						+ housingType);
			}

		} else if (WORK.equals(activityType)) {

			if (!zone.getWorkBuildings().isEmpty()) {
				building = zone.getWorkBuildings().get(
						MathHelpers.draw(officeProbas, rnd));
			} else {
				Logger.getLogger(MATSimDummy.class.getName()).warning(
						"no work buildings in zone " + zone.getId());
			}

		} else {
			Logger.getLogger(MATSimDummy.class.getName()).severe(
					"unkown activity: " + activityType);
			throw new RuntimeException("unknown activity: " + activityType);
		}

		if (building != null) {
			return building.getGeometry();
		} else {
			return zone.getGeometry();
		}
	}

	private Person newPerson(final String personId, final XY2Links xy2links,
			final CoordinateTransformation coordinateTransform) {

		// create a new person with an empty plan

		final Person person = this.scenario.getPopulation().getFactory()
				.createPerson(Id.createPersonId(personId));
		final Plan plan = this.scenario.getPopulation().getFactory()
				.createPlan();
		person.addPlan(plan);

		// home in the morning

		final Zone homeZone = this.id2usedZone.get(this.scenario
				.getPopulation().getPersonAttributes()
				.getAttribute(personId, HOMEZONE_ATTRIBUTE));
		final String homeBuildingType = (String) this.scenario.getPopulation()
				.getPersonAttributes()
				.getAttribute(personId, HOUSINGTYPE_ATTRIBUTE);
		final Coord homeCoord = coordinateTransform.transform(ShapeUtils
				.drawPointFromGeometry(this.drawGeometry(homeZone,
						homeBuildingType, HOME)));
		final Activity homeMorning = this.scenario.getPopulation().getFactory()
				.createActivityFromCoord(HOME, homeCoord);
		homeMorning.setEndTime(this.drawHomeEndTime_s());
		plan.addActivity(homeMorning);

		// travel to work

		final String workTourMode = RegentDictionary.regent2matsim
				.get((String) this.scenario.getPopulation()
						.getPersonAttributes()
						.getAttribute(personId, WORKTOURMODE_ATTRIBUTE));

		final Leg homeToWork = this.scenario.getPopulation().getFactory()
				.createLeg(workTourMode);
		plan.addLeg(homeToWork);

		// at work during the day

		final Zone workZone = this.id2usedZone.get(this.scenario
				.getPopulation().getPersonAttributes()
				.getAttribute(personId, WORKZONE_ATTRIBUTE));

		// ((PersonImpl) person).setSex((String) this.scenario.getPopulation()
		// .getPersonAttributes()
		// .getAttribute(personId, RegentPopulationReader.SEX_ATTRIBUTE));
		// PersonUtils.setEmployed(person, workZone != null);
		// PersonUtils.setAge(
		// person,
		// 2015 - (Integer) this.scenario.getPopulation()
		// .getPersonAttributes()
		// .getAttribute(personId, BIRTHYEAR_ATTRIBUTE));

		// if (this.scenario
		// .getPopulation().getPersonAttributes()
		// .getAttribute(personId, WORKZONE_ATTRIBUTE) == null) {
		// System.out.println(personId + " has no work zone");
		// System.exit(-1);
		// }
		final String workBuildingType = (String) this.scenario
				.getPopulation()
				.getPersonAttributes()
				.getAttribute(personId,
						RegentPopulationReader.HOUSINGTYPE_ATTRIBUTE);
		final Coord workCoord = coordinateTransform.transform(ShapeUtils
				.drawPointFromGeometry(this.drawGeometry(workZone,
						workBuildingType, WORK)));
		final Activity work = this.scenario.getPopulation().getFactory()
				.createActivityFromCoord(WORK, workCoord);
		work.setEndTime(this.drawWorkEndTime_s());
		plan.addActivity(work);

		// travel back home

		final Leg workToHome = this.scenario.getPopulation().getFactory()
				.createLeg(workTourMode);
		plan.addLeg(workToHome);

		// home in the evening

		final Activity homeEvening = this.scenario.getPopulation().getFactory()
				.createActivityFromCoord(HOME, homeCoord);
		plan.addActivity(homeEvening);

		// assign activity coordinates to links

		xy2links.run(person);

		// do coordinate logging if needed

		this.homeCoordStats.add(new Vector(homeCoord.getX(), homeCoord.getY()),
				new Vector(homeCoord.getX(), homeCoord.getY()));
		this.workCoordStats.add(new Vector(workCoord.getX(), workCoord.getY()),
				new Vector(workCoord.getX(), workCoord.getY()));

		if (this.agentHomeXYWriter != null) {
			this.agentHomeXYWriter.print(homeCoord.getX());
			this.agentHomeXYWriter.print(";");
			this.agentHomeXYWriter.println(homeCoord.getY());
		}
		if (this.agentWorkXYWriter != null) {
			this.agentWorkXYWriter.print(workCoord.getX());
			this.agentWorkXYWriter.print(";");
			this.agentWorkXYWriter.println(workCoord.getY());
		}

		return person;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void run(final String initialPlansFile) throws FileNotFoundException {

		int processedPersons = 0;
		int everyXthPerson = (int) (1.0 / this.populationSampleFactor);

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

		for (String personId : ObjectAttributeUtils2
				.allObjectKeys(personAttributes)) {

			final String homeZone = (String) personAttributes.getAttribute(
					personId, HOMEZONE_ATTRIBUTE);
			final String workZone = (String) personAttributes.getAttribute(
					personId, WORKZONE_ATTRIBUTE);
			final String workTourMode = (String) personAttributes.getAttribute(
					personId, WORKTOURMODE_ATTRIBUTE);

			if (this.zonalSystem.getId2zoneView().keySet().contains(homeZone)
					&& this.zonalSystem.getId2zoneView().keySet()
							.contains(workZone) && (
					// RegentPopulationReader.PT_ATTRIBUTEVALUE
					// .equals(workTourMode) ||
					RegentPopulationReader.CAR_ATTRIBUTEVALUE
							.equals(workTourMode))) {
				if (processedPersons % everyXthPerson == 0) {
					Logger.getLogger(MATSimDummy.class.getName()).info(
							"Person " + personId + ": homeZone = " + homeZone
									+ ", workZone = " + workZone
									+ ", workTourMode = " + workTourMode
									+ "; this is the " + processedPersons
									+ "th agent.");
					final Person person = this.newPerson(personId, xy2links,
							coordinateTransform);
					this.scenario.getPopulation().addPerson(person);

				}
				processedPersons++;
			}
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

		final String zonesShapeFileName = "./data/shapes/sverige_TZ_EPSG3857.shp";
		final String buildingShapeFileName = "./data/shapes/by_full_EPSG3857_2.shp";
		final String populationFileName = "./data/synthetic_population/150911_trips.xml";

		final String networkFileName = "./data/run/network-expanded.xml";
		final String linkAttributesFileName = "./data/run/link-attributes.xml";
		final String initialPlansFile = "./data/run/initial-plans.xml";

		final ObjectAttributes linkAttributes = new ObjectAttributes();
		final ObjectAttributesXmlReader reader = new ObjectAttributesXmlReader(
				linkAttributes);
		reader.parse(linkAttributesFileName);

		final PopulationCreator pc = new PopulationCreator(networkFileName,
				zonesShapeFileName,
				StockholmTransformationFactory.WGS84_EPSG3857,
				populationFileName);
		pc.setBuildingsFileName(buildingShapeFileName);
		pc.setAgentHomeXYFile("./data/demand_output/agenthomeXY_v03.txt");
		pc.setAgentWorkXYFile("./data/demand_output/agentWorkXY_v03.txt");
		pc.setNetworkNodeXYFile("./data/demand_output/nodeXY_v03.txt");
		// pc.setZonesBoundaryShapeFileName("./data/shapes/limit_EPSG3857.shp");
		pc.setPopulationSampleFactor(0.05);
		pc.setLinkAttributes(linkAttributes);
		pc.run(initialPlansFile);

		System.out.println("NETWORK NODE COORDINATE STATISTICS");
		System.out.println("center point: " + pc.netCoordStats.getMeanX()
				+ ", " + pc.netCoordStats.getMeanY());
		System.out.println("standard dev: "
				+ Math.sqrt(pc.netCoordStats.getCovariance().get(0, 0)) + ", "
				+ Math.sqrt(pc.netCoordStats.getCovariance().get(1, 1)));
		System.out.println();
		System.out.println("POPULATION HOME COORDINATE STATISTICS");
		System.out.println("center point: " + pc.homeCoordStats.getMeanX());
		System.out.println("standard dev: "
				+ Math.sqrt(pc.homeCoordStats.getCovariance().get(0, 0)) + ", "
				+ Math.sqrt(pc.homeCoordStats.getCovariance().get(1, 1)));

		System.out.println("... DONE");
	}
}
