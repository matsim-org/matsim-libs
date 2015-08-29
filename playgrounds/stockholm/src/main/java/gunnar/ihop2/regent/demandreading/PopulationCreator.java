package gunnar.ihop2.regent.demandreading;

import static gunnar.ihop2.regent.demandreading.RegentPopulationReader.BIRTHYEAR_ATTRIBUTE;
import static gunnar.ihop2.regent.demandreading.RegentPopulationReader.HOMEZONE_ATTRIBUTE;
import static gunnar.ihop2.regent.demandreading.RegentPopulationReader.HOUSINGTYPE_ATTRIBUTE;
import static gunnar.ihop2.regent.demandreading.RegentPopulationReader.SEX_ATTRIBUTE;
import static gunnar.ihop2.regent.demandreading.RegentPopulationReader.WORKTOURMODE_ATTRIBUTE;
import static gunnar.ihop2.regent.demandreading.RegentPopulationReader.WORKZONE_ATTRIBUTE;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.population.algorithms.XY2Links;
import org.matsim.utils.objectattributes.ObjectAttributeUtils2;
import org.matsim.utils.objectattributes.ObjectAttributes;

import patryk.popgen2.Building;
import patryk.utils.LinksRemover;

import com.vividsolutions.jts.geom.Geometry;

import floetteroed.utilities.math.Covariance;
import floetteroed.utilities.math.MathHelpers;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd, based on Patryk Larek
 *
 */
public class PopulationCreator {

	// -------------------- CONSTANTS --------------------

	static final String HOME = "home";

	static final String WORK = "work";

	static final String VILLA = "villa";

	static final String APARTMENT = "apartment";

	// -------------------- MEMBERS --------------------

	private final Scenario scenario;

	private final ZonalSystem zonalSystem;

	private final Map<String, Zone> id2usedZone;

	private double populationSampleFactor = 1.0;

	private String zonesBoundaryShapeFileName = null;

	private String agentHomeXYFileName = null;

	private String agentWorkXYFileName = null;

	private PrintWriter agentHomeXYWriter = null;

	private PrintWriter agentWorkXYWriter = null;

	private Covariance netCoordStats = new Covariance(2, 2);

	private Covariance homeCoordStats = new Covariance(2, 2);

	private Covariance workCoordStats = new Covariance(2, 2);

	// -------------------- CONSTRUCTION --------------------

	public PopulationCreator(final String networkFileName,
			final String zoneShapeFileName, final String populationFileName) {

		this.scenario = ScenarioUtils
				.createScenario(ConfigUtils.createConfig());
		(new MatsimNetworkReader(this.scenario)).readFile(networkFileName);

		for (Node node : this.scenario.getNetwork().getNodes().values()) {
			final Vector coords = new Vector(node.getCoord().getX(), node
					.getCoord().getY());
			this.netCoordStats.add(coords, coords);
		}

		this.zonalSystem = new ZonalSystem(zoneShapeFileName);

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

	public void setZonesBoundaryShapeFileName(
			final String zonesBoundaryShapeFileName) {
		this.zonesBoundaryShapeFileName = zonesBoundaryShapeFileName;
	}

	public void setPopulationSampleFactor(final double populationSampleFactor) {
		this.populationSampleFactor = populationSampleFactor;
	}

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
					System.err.println("no villas in zone " + zone.getId());
				}
			} else if (APARTMENT.equals(housingType)) {
				if (!zone.getMultiFamilyBuildings().isEmpty()) {
					building = zone.getMultiFamilyBuildings().get(
							MathHelpers.draw(apartmentProbas, rnd));
				} else {
					System.err.println("no apartments in zone " + zone.getId());
				}
			} else {
				throw new RuntimeException("unknown housing type "
						+ housingType);
			}

		} else if (WORK.equals(activityType)) {

			if (!zone.getWorkBuildings().isEmpty()) {
				building = zone.getWorkBuildings().get(
						MathHelpers.draw(officeProbas, rnd));
			} else {
				System.err.println("no work buildings in zone " + zone.getId());
			}

		} else {
			throw new RuntimeException("unknown activity: " + activityType);
		}

		if (building != null) {
			return building.getGeometry();
		} else {
			return zone.getGeometry();
		}
	}

	private Person newPerson(final String personId, final XY2Links xy2links) {

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
		final Coord homeCoord = ShapeUtils.drawPointFromGeometry(this
				.drawGeometry(homeZone, homeBuildingType, HOME));
		final Activity homeMorning = this.scenario.getPopulation().getFactory()
				.createActivityFromCoord(HOME, homeCoord);
		homeMorning.setEndTime(this.drawHomeEndTime_s());
		plan.addActivity(homeMorning);

		// travel to work

		final String workTourMode = (String) this.scenario.getPopulation()
				.getPersonAttributes()
				.getAttribute(personId, WORKTOURMODE_ATTRIBUTE);

		final Leg homeToWork = this.scenario.getPopulation().getFactory()
				.createLeg(workTourMode);
		plan.addLeg(homeToWork);

		// at work during the day

		final Zone workZone = this.id2usedZone.get(this.scenario
				.getPopulation().getPersonAttributes()
				.getAttribute(personId, WORKZONE_ATTRIBUTE));

		((PersonImpl) person).setSex((String) this.scenario.getPopulation()
				.getPersonAttributes().getAttribute(personId, SEX_ATTRIBUTE));
		((PersonImpl) person).setEmployed(workZone != null);
		((PersonImpl) person).setAge(2015 - Integer
				.parseInt((String) this.scenario.getPopulation()
						.getPersonAttributes()
						.getAttribute(personId, BIRTHYEAR_ATTRIBUTE)));

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
		final Coord workCoord = ShapeUtils.drawPointFromGeometry(this
				.drawGeometry(workZone, workBuildingType, WORK));
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

		int processedCarDrivers = 0;
		int everyXthPerson = (int) (1 / this.populationSampleFactor);

		// >>>>> TODO remove links where we do not want activities >>>>>
		final LinksRemover linksRem = new LinksRemover(
				this.scenario.getNetwork());
		linksRem.run();
		// <<<<< TODO <<<<<

		XY2Links xy2links = new XY2Links(this.scenario);

		final Map<String, Zone> id2clippedZone = this.zonalSystem
				.getZonesInsideBoundary(this.zonesBoundaryShapeFileName);

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

			// TODO ONLY PT WORK TRIPS !!!

			if (id2clippedZone.keySet().contains(homeZone)
					&& id2clippedZone.keySet().contains(workZone)
					&& RegentPopulationReader.PT_ATTRIBUTEVALUE
							.equals(workTourMode)
			// && CAR_ATTRIBUTEVALUE.equals(workTourMode)
			) {
				if (processedCarDrivers % everyXthPerson == 0) {
					System.out.print("Person " + personId + ": homeZone = "
							+ homeZone + ", workZone = " + workZone
							+ ", workTourMode = " + workTourMode);
					System.out.println("; this is the " + processedCarDrivers
							+ "th agent.");
					final Person person = this.newPerson(personId, xy2links);
					this.scenario.getPopulation().addPerson(person);

				}
				processedCarDrivers++;
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

		// final String networkFileName =
		// "./data/network/network_v12_utan_forbifart.xml";
		final String networkFileName = "./data/transmodeler/network.xml";
		final String zonesShapeFileName = "./data/shapes/sverige_TZ_EPSG3857.shp";

		final String buildingShapeFileName = "./data/shapes/by_full_EPSG3857_2.shp";
		// final String buildingShapeFileName = null;

		// final String populationFileName = "./150410_worktrips_small.xml";
		// final String populationFileName =
		// "./data/synthetic_population/150410_worktrips.xml";
		final String populationFileName = "./data/synthetic_population/150615_trips.xml";

		final String initialPlansFile = "./data/demand_output/initial_plans_v03.xml";

		final PopulationCreator pc = new PopulationCreator(networkFileName,
				zonesShapeFileName, populationFileName);
		pc.setBuildingsFileName(buildingShapeFileName);
		pc.setAgentHomeXYFile("./data/demand_output/agenthomeXY_v03.txt");
		pc.setAgentWorkXYFile("./data/demand_output/agentWorkXY_v03.txt");
		pc.setZonesBoundaryShapeFileName("./data/shapes/limit_EPSG3857.shp");
		
		pc.setPopulationSampleFactor(1.0);

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
