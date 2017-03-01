package gunnar.ihop2.transmodeler.run;

import static saleem.stockholmmodel.utils.StockholmTransformationFactory.WGS84_EPSG3857;
import static saleem.stockholmmodel.utils.StockholmTransformationFactory.WGS84_SWEREF99;
import static saleem.stockholmmodel.utils.StockholmTransformationFactory.getCoordinateTransformation;

import java.util.LinkedHashSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.XY2Links;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;

import gunnar.ihop2.regent.demandreading.ActivityLocationSampler;
import gunnar.ihop2.regent.demandreading.ZonalSystem;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class Plans2Trips {

	// -------------------- MEMBERS --------------------

	private final Scenario scenario;

	private final ActivityLocationSampler actLocSampler;

	// -------------------- CONSTRUCTION AND CONFIGURATION --------------------

	public Plans2Trips(final String plansFileName,
			final String personAttributeFileName, final String networkFileName,
			final String zoneShapeFileName, final String zonalCoordinateSystem,
			final String buildingShapeFileName) {

		final Config config = ConfigUtils.createConfig();
		config.getModule("plans").addParam("inputPlansFile", plansFileName);
		config.getModule("plans").addParam("inputPersonAttributesFile",
				personAttributeFileName);
		config.getModule("network").addParam("inputNetworkFile",
				networkFileName);
		this.scenario = ScenarioUtils.loadScenario(config);

		final ZonalSystem zonalSystem = new ZonalSystem(zoneShapeFileName,
				zonalCoordinateSystem);
		zonalSystem.addNetwork(this.scenario.getNetwork(), WGS84_SWEREF99);
		if (buildingShapeFileName != null) {
			zonalSystem.addBuildings(buildingShapeFileName);
		}

		this.actLocSampler = new ActivityLocationSampler(this.scenario
				.getPopulation().getPersonAttributes(), zonalSystem,
				getCoordinateTransformation(WGS84_EPSG3857, WGS84_SWEREF99));

		// TODO NEW
		this.actLocSampler.setZonesMustContainNodes(true);
	}

	// -------------------- IMPLEMENTATION --------------------

	private void resampleLocation(final Person person, final Activity act) {
		act.setLinkId(null);
		final String personIdStr = person.getId().toString();
		// TODO NEW >>>
		// if (HOME.equals(act.getType())) {
		// act.setCoord(this.actLocSampler.drawHomeCoordinate(personIdStr));
		// } else if (WORK.equals(act.getType())) {
		// act.setCoord(this.actLocSampler.drawWorkCoordinate(personIdStr));
		// } else if (OTHER.equals(act.getType())) {
		// act.setCoord(this.actLocSampler.drawOtherCoordinate(personIdStr));
		if (act.getType().toUpperCase().startsWith("H")) {
			act.setCoord(this.actLocSampler.drawHomeCoordinate(personIdStr));
		} else if (act.getType().toUpperCase().startsWith("W")) {
			act.setCoord(this.actLocSampler.drawWorkCoordinate(personIdStr));
		} else if (act.getType().toUpperCase().startsWith("O")) {
			act.setCoord(this.actLocSampler.drawOtherCoordinate(personIdStr));
		} else {
			throw new RuntimeException("unknown activity " + act.getType()
					+ " for person " + personIdStr);
		}
		if (act.getCoord() == null) {
			throw new RuntimeException(
					"Did not suceed to draw a coordinate for person "
							+ personIdStr + "'s " + act.getType()
							+ " activity.");
		}
	}

	public void createTripMakers(final String tripsFileName, final int cloneCnt) {

		final XY2Links xy2links = new XY2Links(this.scenario);

		for (Person person : new LinkedHashSet<>(this.scenario.getPopulation()
				.getPersons().values())) {
			final Plan selectedPlan = person.getSelectedPlan();

			// 1, 3, 5, ...
			for (int tripPlanElementIndex = 1; tripPlanElementIndex < selectedPlan
					.getPlanElements().size(); tripPlanElementIndex += 2) {

				// 1/2 = 0, 3/2 = 1, 5/2 = 2, ...
				final int tripIndex = tripPlanElementIndex / 2;

				for (int cloneIndex = 0; cloneIndex < cloneCnt; cloneIndex++) {

					final Person clone = this.scenario
							.getPopulation()
							.getFactory()
							.createPerson(
									Id.create(person.getId().toString()
											+ "_clone" + cloneIndex + "_trip"
											+ tripIndex, Person.class));
					this.scenario.getPopulation().addPerson(clone);

					final Plan clonePlan = this.scenario.getPopulation()
							.getFactory().createPlan();
					clone.addPlan(clonePlan);

					final Activity newStartAct = PopulationUtils.createActivity((Activity) selectedPlan.getPlanElements().get(
							tripPlanElementIndex - 1));
					clonePlan.addActivity(newStartAct);
					this.resampleLocation(person, newStartAct);

					final Leg newLeg = (Leg) selectedPlan.getPlanElements()
							.get(tripPlanElementIndex);
					clonePlan.addLeg(newLeg);
					newLeg.setRoute(null);

					final Activity newEndAct = PopulationUtils.createActivity((Activity) selectedPlan.getPlanElements().get(
							tripPlanElementIndex + 1));
					clonePlan.addActivity(newEndAct);
					this.resampleLocation(person, newEndAct);
					newEndAct.setEndTime(Time.UNDEFINED_TIME);
					
					xy2links.run(clone);
				}
			}

			this.scenario.getPopulation().getPersons().remove(person.getId());
		}

		PopulationWriter populationWriter = new PopulationWriter(
				this.scenario.getPopulation(), this.scenario.getNetwork());
		populationWriter.write(tripsFileName);
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) {

		final int cloneCnt = 50;

		final String populationFile = "./ihop2-data/demand-input/trips.xml";
		final String networkFile = "./ihop2-data/network-output/network.xml";
		final String zonesFile = "./ihop2-data/demand-input/sverige_TZ_EPSG3857.shp";
		final String buildingsFile = "./ihop2-data/demand-input/by_full_EPSG3857_2.shp";

		final Plans2Trips p2t = new Plans2Trips(
				"./ihop2-data/without-toll/ITERS/it.200/200.plans.xml.gz",
				populationFile, networkFile, zonesFile, WGS84_EPSG3857,
				buildingsFile);
		p2t.createTripMakers("./ihop2-data/without-toll/ITERS/it.200/200.trips-from-plans_0.50.xml",
		// "./ihop2-data/playground/initial-trips.xml",
				cloneCnt);

		// extractFirstTrip(configFile, from, to);
		// extractAllTrips(configFile, from, to);

	}

}
