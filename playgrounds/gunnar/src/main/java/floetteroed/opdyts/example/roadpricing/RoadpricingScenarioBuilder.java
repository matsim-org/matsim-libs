package floetteroed.opdyts.example.roadpricing;

import static floetteroed.utilities.math.MathHelpers.draw;
import static org.matsim.core.gbl.MatsimRandom.getRandom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.RandomizedCharyparNagelScoringFunctionFactory;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.roadpricing.ControlerDefaultsWithRoadPricingModule;
import org.matsim.roadpricing.RoadPricingConfigGroup;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import floetteroed.utilities.Units;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class RoadpricingScenarioBuilder {

	private final Config config;

	private final double linkLength;

	private final int outerPopSize;

	private final int innerPopSize;

	private final int otherPopSize;

	private final boolean split;

	private final int detourOffset;

	private Network net;

	private Population population;

	RoadpricingScenarioBuilder(final Config config, final double linkLength,
			final int outerPopSize, final int innerPopSize,
			final int otherPopSize, final boolean split, final int detourOffset) {
		this.config = config;
		this.linkLength = linkLength;
		this.outerPopSize = outerPopSize;
		this.innerPopSize = innerPopSize;
		this.otherPopSize = otherPopSize;
		this.split = split;
		this.detourOffset = detourOffset;
	}

	private Node addNode(final String id, final double x, final double y) {
		final NodeImpl node = new NodeImpl(Id.createNodeId(id));
		node.setCoord(new Coord(x * this.linkLength, y * this.linkLength));
		this.net.addNode(node);
		return node;
	}

	private Link addLink(final Node fromNode, final Node toNode) {
		final Link link = this.net.getFactory().createLink(
				Id.createLinkId(fromNode.getId().toString() + "_"
						+ toNode.getId().toString()), fromNode, toNode);
		link.setLength(CoordUtils.calcEuclideanDistance(fromNode.getCoord(),
				toNode.getCoord()));
		link.setNumberOfLanes(1);
		link.setCapacity(1200);
		link.setFreespeed(Units.M_S_PER_KM_H * 60);
		this.net.addLink(link);
		return link;

	}

	private Person newPerson(final Link homeLink, final Link actLink,
			final String actType) {
		final Person person = this.population.getFactory().createPerson(
				Id.createPersonId(Integer.toString(this.population.getPersons()
						.size() + 1)));
		this.population.addPerson(person);

		final Plan plan = this.population.getFactory().createPlan();
		person.addPlan(plan);

		final Activity firstHome = this.population.getFactory()
				.createActivityFromLinkId("home", homeLink.getId());
		firstHome.setEndTime((8.0 + 4.0 * getRandom().nextDouble()) * 3600.0);
		plan.addActivity(firstHome);

		plan.addLeg(this.population.getFactory().createLeg("car"));

		final Activity work = this.population.getFactory()
				.createActivityFromLinkId(actType, actLink.getId());
		work.setEndTime((14.0 + 4.0 * getRandom().nextDouble()) * 3600.0);
		plan.addActivity(work);

		plan.addLeg(this.population.getFactory().createLeg("car"));

		plan.addActivity((this.population.getFactory()
				.createActivityFromLinkId("home", homeLink.getId())));

		this.population.getPersonAttributes()
				.putAttribute(
						person.getId().toString(),
						"workduration_s",
						new Double(
								Units.S_PER_H
										* (2.0 + MatsimRandom.getRandom()
												.nextDouble() * 10.0)));

		return person;
	}

	public void build() {

		this.net = NetworkUtils.createNetwork();

		// NODES

		final Node n1 = this.addNode("1", 0, 1);
		final Node n2 = this.addNode("2", 1, 1);
		final Node n3 = this.addNode("3", 2, 1);
		final Node n4 = this.addNode("4", 3, 1);
		final Node n5 = this.addNode("5", 4, 1);
		final Node n6 = this.addNode("6", 5, 1);
		final Node n7 = this.addNode("7", 2, 0);
		final Node n8 = this.addNode("8", 3, 0);
		final Node n9 = this.addNode("9", 1, 0.5 * this.detourOffset);
		final Node n10 = this.addNode("10", 4, 0.5 * this.detourOffset);

		// LINKS

		final Link l1_2 = this.addLink(n1, n2);
		final Link l2_1 = this.addLink(n2, n1);

		final Link l2_3;
		final Link l3_2;
		if (this.split) {
			l2_3 = null;
			l3_2 = null;
		} else {
			l2_3 = this.addLink(n2, n3);
			l3_2 = this.addLink(n3, n2);
		}

		final Link l3_4 = this.addLink(n3, n4);
		final Link l4_3 = this.addLink(n4, n3);

		final Link l4_5;
		final Link l5_4;
		if (this.split) {
			l4_5 = null;
			l5_4 = null;
		} else {
			l4_5 = this.addLink(n4, n5);
			l5_4 = this.addLink(n5, n4);
		}

		final Link l5_6 = this.addLink(n5, n6);
		final Link l6_5 = this.addLink(n6, n5);

		final Link l3_7 = this.addLink(n3, n7);
		final Link l7_3 = this.addLink(n7, n3);

		final Link l4_8 = this.addLink(n4, n8);
		final Link l8_4 = this.addLink(n8, n4);

		final Link l2_9 = this.addLink(n2, n9);
		final Link l9_2 = this.addLink(n9, n2);

		final Link l9_10 = this.addLink(n9, n10);
		final Link l10_9 = this.addLink(n10, n9);

		final Link l5_10 = this.addLink(n5, n10);
		final Link l10_5 = this.addLink(n10, n5);

		final List<Link> allLinks = new ArrayList<>();
		allLinks.addAll(Arrays.asList(l1_2, l2_1, l3_4, l4_3, l5_6, l6_5, l3_7,
				l7_3, l4_8, l8_4, l2_9, l9_2, l9_10, l10_9, l5_10, l10_5));
		if (!this.split) {
			allLinks.addAll(Arrays.asList(l2_3, l3_2, l4_5, l5_4));
		}

		// POPULATION
		this.population = PopulationUtils.createPopulation(config);
		for (int i = 0; i < this.outerPopSize; i++) {
			this.newPerson(l1_2, l5_6, "work");
		}
		for (int i = 0; i < this.innerPopSize; i++) {
			this.newPerson(l7_3, l4_8, "work");
		}
		for (int i = 0; i < this.otherPopSize; i++) {
			this.newPerson(draw(allLinks, MatsimRandom.getRandom()),
					draw(allLinks, MatsimRandom.getRandom()), "other");
		}

	}

	public Network getNetwork() {
		return this.net;
	}

	public Population getPopulation() {
		return this.population;
	}

	public static void main(String[] args) {

		// >>> TODO >>>
		args = new String[] { "false", "8", "5000", "2000" };
		// <<< TODO <<<

		boolean split = false;
		int detourOffset = 0;
		double linkLength = 0;
		int outerPopSize = 0;
		int innerPopSize = 0;
		try {
			split = Boolean.parseBoolean(args[0]);
			detourOffset = Integer.parseInt(args[1]);
			outerPopSize = Integer.parseInt(args[2]);
			innerPopSize = outerPopSize;
			linkLength = Double.parseDouble(args[3]);
		} catch (Exception e) {
			System.err
					.println("EXPECTING 4 PARAMETERS: split detour-offset popsize linklength");
			System.exit(-1);
		}

		final Config config = ConfigUtils.loadConfig(
				"./input/roadpricing/config.xml", new RoadPricingConfigGroup());

		final RoadpricingScenarioBuilder builder = new RoadpricingScenarioBuilder(
				config, linkLength, outerPopSize, innerPopSize, 0, split,
				detourOffset);
		builder.build();

		final NetworkWriter netWriter = new NetworkWriter(builder.getNetwork());
		netWriter.write("./input/roadpricing/network.xml");

		final PopulationWriter popWriter = new PopulationWriter(
				builder.getPopulation(), builder.getNetwork());
		popWriter.write("./input/roadpricing/plans.xml");

		final ObjectAttributesXmlWriter popAttrWriter = new ObjectAttributesXmlWriter(
				builder.getPopulation().getPersonAttributes());
		popAttrWriter
				.writeFile("./input/roadpricing/population-attributes.xml");

		final Scenario scenario = ScenarioUtils.loadScenario(config);
		final Controler controler = new Controler(scenario);
		controler
				.setScoringFunctionFactory(new RandomizedCharyparNagelScoringFunctionFactory(
						scenario));

		controler.setModules(new ControlerDefaultsWithRoadPricingModule());

		controler.run();

		// final OccupancyAnalyzer occupAnalyzer = new OccupancyAnalyzer(
		// new TimeDiscretization(0, 1800, 48), scenario.getNetwork()
		// .getLinks().keySet());
		// final EventsManager events = EventsUtils.createEventsManager(config);
		// events.addHandler(occupAnalyzer);
		// final MatsimEventsReader reader = new MatsimEventsReader(events);
		// reader.readFile(config.getModule("controler").getValue(
		// "outputDirectory")
		// + "ITERS/it."
		// + config.getModule("controler").getValue("lastIteration")
		// + "/"
		// + config.getModule("controler").getValue("lastIteration")
		// + ".events.xml.gz");
		// System.out.println();
		// for (Id<Link> linkId : scenario.getNetwork().getLinks().keySet()) {
		// System.out.print(linkId);
		// for (int bin = 0; bin < 48; bin++) {
		// System.out.print("\t");
		// final Link link = scenario.getNetwork().getLinks().get(linkId);
		// final double relOccup = occupAnalyzer.getOccupancy_veh(linkId,
		// bin)
		// / (link.getLength() * link.getNumberOfLanes() / 7.5);
		// // TODO no getter
		// System.out.print(relOccup);
		// }
		// System.out.println();
		// }
	}
}
