package gunnar.tryout;

import gunnar.ihop2.regent.demandreading.ZonalSystem;
import gunnar.ihop2.regent.demandreading.Zone;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;

import saleem.stockholmscenario.utils.StockholmTransformationFactory;
import sergio.AssignmentMatrixColumnInventor;
import sergio.Trip;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class Data2James {

	public static void main(String[] args) throws FileNotFoundException {

		System.out.println("STARTED...");

		System.out.println("	READING NETWORK AND PLANS");
		final Config config = ConfigUtils.createConfig();
		config.setParam("network", "inputNetworkFile",
				"/Nobackup/Profilen/Documents/proposals/2015/IHOP2/showcase/"
						+ "2015-11-23ab_LARGE_RegentMATSim/network.xml");
		config.setParam(
				"plans",
				"inputPlansFile",
				"/Nobackup/Profilen/Documents/proposals/2015/IHOP2/showcase/"
						+ "2015-11-23ab_LARGE_RegentMATSim/2015-11-23b_Toll_large/"
						+ "summary/iteration-3/it.400/400.plans.xml.gz");
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		System.out.println("    READING ZONAL SYSTEM");
		final String zonesShapeFileName = "./ihop2-data/demand-input/sverige_TZ_EPSG3857.shp";
		final String zonalCoordinateSystem = StockholmTransformationFactory.WGS84_EPSG3857;
		final String networkCoordinateSystem = StockholmTransformationFactory.WGS84_SWEREF99;
		final ZonalSystem zonalSystem = new ZonalSystem(zonesShapeFileName,
				zonalCoordinateSystem);
		zonalSystem.addNetwork(scenario.getNetwork(), networkCoordinateSystem);

		final String outputFile = "./test/guidon/2016-03-16_4-10.csv";
		final PrintWriter writer = new PrintWriter(outputFile);

		final int earliestDptTime_s = 4 * 3600;
		final int latestDptTime_s = 10 * 3600;

		final AssignmentMatrixColumnInventor amci = new AssignmentMatrixColumnInventor(
				scenario.getNetwork(), zonalSystem, MatsimRandom.getRandom());

		final Set<Zone> fromZones = new LinkedHashSet<>();
		final Set<Zone> toZones = new LinkedHashSet<>();
		final Set<Id<Link>> routeLinks = new LinkedHashSet<>();
		int routeCnt = 0;

		for (Person person : scenario.getPopulation().getPersons().values()) {
			final Plan plan = person.getSelectedPlan();
			// check if there is a relevant route
			if (plan != null) {

				Double lastDptTime_s = null;
				for (int i = 0; i < plan.getPlanElements().size(); i++) {
					final PlanElement element = plan.getPlanElements().get(i);

					if (element instanceof Activity) {
						final Activity act = (Activity) element;
						lastDptTime_s = act.getEndTime();
					} else if (element instanceof Leg) {
						if ((lastDptTime_s >= earliestDptTime_s)
								&& (lastDptTime_s < latestDptTime_s)) {
							final Leg leg = (Leg) element;
							if ("car".equals(leg.getMode())) {
								final NetworkRoute route = (NetworkRoute) leg
										.getRoute();
								if (route != null) {

									routeCnt++;

									// column 1: from-zone
									final Zone fromZone;
									{
										final Id<Link> fromLinkId = ((Activity) plan
												.getPlanElements().get(i - 1))
												.getLinkId();
										final Link fromLink = scenario
												.getNetwork().getLinks()
												.get(fromLinkId);
										fromZone = zonalSystem.getZone(fromLink
												.getFromNode());
										fromZones.add(fromZone);
										writer.print(fromZone.getId());
										writer.print(",");
									}

									// column 2: to-zone
									final Zone toZone;
									{
										final Id<Link> toLinkId = ((Activity) plan
												.getPlanElements().get(i + 1))
												.getLinkId();
										final Link toLink = scenario
												.getNetwork().getLinks()
												.get(toLinkId);
										toZone = zonalSystem.getZone(toLink
												.getFromNode());
										toZones.add(toZone);
										writer.print(toZone.getId());
									}

									amci.registerUsedOdPair(fromZone, toZone);

									// column 3, ... : links in path
									writer.print(",");
									writer.print(route.getStartLinkId()
											.toString());
									routeLinks.add(route.getStartLinkId());
									for (Id<Link> linkId : route.getLinkIds()) {
										writer.print(",");
										writer.print(linkId);
										routeLinks.add(linkId);
									}
									writer.print(",");
									writer.print(route.getEndLinkId()
											.toString());
									routeLinks.add(route.getEndLinkId());
									writer.println();
								}
							}
						}
					}
				}
			}
		}

		writer.flush();
		writer.close();

		System.out.println("distinct origin zones:      " + fromZones.size());
		System.out.println("distinct destination zones: " + toZones.size());
		System.out.println("distinct routes:            " + routeCnt);
		System.out.println("distinct en-route links:    " + routeLinks.size());

		long cnt = 0;
		for (Iterator<Trip> it = amci.missingRoutesIterator(); it.hasNext();) {
			final Trip trip = it.next();
			if (++cnt % 1000 == 0) {
				System.out.println("unused trip number " + cnt + ": " + trip);
			}
		}

		System.out.println("...DONE");

	}

}
