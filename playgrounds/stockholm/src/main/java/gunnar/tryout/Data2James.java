package gunnar.tryout;

import gunnar.ihop2.regent.demandreading.RegentPopulationReader;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class Data2James {

	public Data2James() {
	}

	public static void main(String[] args) throws FileNotFoundException {

		System.out.println("STARTED ...");

		System.out.println("	READING PERSON ATTRIBUTES");
		final String regentOutputFileName = "./data/synthetic_population/150615_trips.xml";
		final ObjectAttributes personAttributes = new ObjectAttributes();
		(new ObjectAttributesXmlReader(personAttributes))
				.parse(regentOutputFileName);

		System.out.println("	READING NETWORK AND PLANS");
		final Config config = ConfigUtils.createConfig();
		config.setParam("network", "inputNetworkFile",
				"./data/transmodeler/network.xml");
		config.setParam("plans", "inputPlansFile",
				"./data/saleem/10.plans.xml.gz");
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		 final String outputFile = "./data/2015-09-15_toJames.csv";
		 final PrintWriter writer = new PrintWriter(outputFile);

		for (Person person : scenario.getPopulation().getPersons().values()) {
			final Plan plan = person.getSelectedPlan();
			NetworkRoute route = null;
			// check if there is a relevant route
			if (plan != null) {
				for (PlanElement element : plan.getPlanElements()) {
					if (element instanceof Leg) {
						final Leg leg = (Leg) element;
						if ("car".equals(leg.getMode())) {
							route = (NetworkRoute) leg.getRoute();
							break; // because we only want the to-work trip
						}
					}
				}
			}
			// in case there is one, write out a row
			if (route != null) {
				// // column 1: from-zone
				writer
						.print(personAttributes.getAttribute(person.getId()
								.toString(),
								RegentPopulationReader.HOMEZONE_ATTRIBUTE));
				writer.print(",");
				// // column 2: to-zone
				writer
						.print(personAttributes.getAttribute(person.getId()
								.toString(),
								RegentPopulationReader.WORKZONE_ATTRIBUTE));
				// // column 3, ... : links in path
				writer.print(",");
				writer.print(route.getStartLinkId().toString());
				for (Id<Link> linkId : route.getLinkIds()) {
					writer.print(",");
					writer.print(linkId);
				}
				writer.print(",");
				writer.print(route.getEndLinkId().toString());
				writer.println();
			}
		}

		writer.flush();
		writer.close();
		
		System.out.println("...DONE");

	}

}
