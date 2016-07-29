package playground.mzilske.pt.queuesim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.Umlauf;
import org.matsim.pt.UmlaufBuilder;
import org.matsim.pt.UmlaufInterpolator;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class GreedyUmlaufBuilderTest {

	MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());

	@Test
	public void testGreedyUmlaufBuilder() {
		Config config = this.scenario.getConfig();
		config.transit().setUseTransit(true);
		config.qsim().setSnapshotStyle( SnapshotStyle.queue ) ;;
		config.qsim().setEndTime(24.0*3600);
		createNetwork();
		setupSchedule();
		Collection<TransitLine> transitLines = scenario.getTransitSchedule().getTransitLines().values();
		UmlaufBuilder umlaufBuilder = new GreedyUmlaufBuilderImpl(new UmlaufInterpolator(scenario.getNetwork(), scenario.getConfig().planCalcScore()), transitLines);
		Collection<Umlauf> umlaeufe = umlaufBuilder.build();
	}

	private void setupSchedule() {
		TransitSchedule schedule = this.scenario.getTransitSchedule();
		TransitScheduleFactory builder = schedule.getFactory();
		double x1 = -100;
		double y = -50;
		TransitStopFacility stop1 = builder.createTransitStopFacility(Id.create(1, TransitStopFacility.class), new Coord(x1, y), false);
		double x = -100;
		TransitStopFacility stop2 = builder.createTransitStopFacility(Id.create(2, TransitStopFacility.class), new Coord(x, (double) 850), false);
		TransitStopFacility stop3 = builder.createTransitStopFacility(Id.create(3, TransitStopFacility.class), new Coord((double) 1400, (double) 450), false);
		TransitStopFacility stop4 = builder.createTransitStopFacility(Id.create(4, TransitStopFacility.class), new Coord((double) 3400, (double) 450), false);
		TransitStopFacility stop5 = builder.createTransitStopFacility(Id.create(5, TransitStopFacility.class), new Coord((double) 3900, (double) 50), false);
		TransitStopFacility stop6 = builder.createTransitStopFacility(Id.create(6, TransitStopFacility.class), new Coord((double) 3900, (double) 850), false);

		TransitStopFacility stop7 = builder.createTransitStopFacility(Id.create(7, TransitStopFacility.class), new Coord((double) 2600, (double) 550), false);
		TransitStopFacility stop8 = builder.createTransitStopFacility(Id.create(8, TransitStopFacility.class), new Coord((double) 600, (double) 550), false);

		Link link1 = this.scenario.getNetwork().getLinks().get(Id.create(1, Link.class));
		Link link2 = this.scenario.getNetwork().getLinks().get(Id.create(2, Link.class));
		Link link3 = this.scenario.getNetwork().getLinks().get(Id.create(3, Link.class));
		Link link4 = this.scenario.getNetwork().getLinks().get(Id.create(4, Link.class));
		Link link5 = this.scenario.getNetwork().getLinks().get(Id.create(5, Link.class));
		Link link6 = this.scenario.getNetwork().getLinks().get(Id.create(6, Link.class));
		Link link7 = this.scenario.getNetwork().getLinks().get(Id.create(7, Link.class));
		Link link8 = this.scenario.getNetwork().getLinks().get(Id.create(8, Link.class));
		Link link9 = this.scenario.getNetwork().getLinks().get(Id.create(9, Link.class));
		Link link10 = this.scenario.getNetwork().getLinks().get(Id.create(10, Link.class));
		Link link11 = this.scenario.getNetwork().getLinks().get(Id.create(11, Link.class));
		Link link12 = this.scenario.getNetwork().getLinks().get(Id.create(12, Link.class));
		Link link13 = this.scenario.getNetwork().getLinks().get(Id.create(13, Link.class));
		Link link14 = this.scenario.getNetwork().getLinks().get(Id.create(14, Link.class));
		Link link15 = this.scenario.getNetwork().getLinks().get(Id.create(15, Link.class));
		Link link16 = this.scenario.getNetwork().getLinks().get(Id.create(16, Link.class));
		Link link17 = this.scenario.getNetwork().getLinks().get(Id.create(17, Link.class));
		Link link18 = this.scenario.getNetwork().getLinks().get(Id.create(18, Link.class));
		Link link19 = this.scenario.getNetwork().getLinks().get(Id.create(19, Link.class));

		stop1.setLinkId(link3.getId());
		stop2.setLinkId(link4.getId());
		stop3.setLinkId(link7.getId());
		stop4.setLinkId(link9.getId());
		stop5.setLinkId(link11.getId());
		stop6.setLinkId(link10.getId());
		stop7.setLinkId(link14.getId());
		stop8.setLinkId(link16.getId());

		schedule.addStopFacility(stop1);
		schedule.addStopFacility(stop2);
		schedule.addStopFacility(stop3);
		schedule.addStopFacility(stop4);
		schedule.addStopFacility(stop5);
		schedule.addStopFacility(stop6);
		schedule.addStopFacility(stop7);
		schedule.addStopFacility(stop8);

		TransitLine tLine2 = builder.createTransitLine(Id.create(2, TransitLine.class));
		NetworkRoute networkRoute = new LinkNetworkRouteImpl(link2.getId(), link12.getId());
		ArrayList<Id<Link>> linkIdList = new ArrayList<Id<Link>>(6);
		Collections.addAll(linkIdList, link4.getId(), link6.getId(), link7.getId(), link8.getId(), link9.getId(), link10.getId());
		networkRoute.setLinkIds(link2.getId(), linkIdList, link12.getId());
		ArrayList<TransitRouteStop> stopList = new ArrayList<TransitRouteStop>(4);
		stopList.add(builder.createTransitRouteStop(stop2, 0, 0));
		stopList.add(builder.createTransitRouteStop(stop3, 90, 100));
		stopList.add(builder.createTransitRouteStop(stop4, 290, 300));
		stopList.add(builder.createTransitRouteStop(stop6, 390, Time.UNDEFINED_TIME));
		TransitRoute tRoute2 = builder.createTransitRoute(Id.create(1, TransitRoute.class), networkRoute, stopList, "bus");
		tLine2.addRoute(tRoute2);
		tRoute2.addDeparture(builder.createDeparture(Id.create(1, Departure.class), Time.parseTime("07:02:00")));
		tRoute2.addDeparture(builder.createDeparture(Id.create(2, Departure.class), Time.parseTime("07:12:00")));
		tRoute2.addDeparture(builder.createDeparture(Id.create(3, Departure.class), Time.parseTime("07:22:00")));

		networkRoute = new LinkNetworkRouteImpl(link17.getId(), link19.getId());
		linkIdList = new ArrayList<Id<Link>>(6);
		Collections.addAll(linkIdList, link18.getId(), link14.getId(), link15.getId(), link16.getId());
		networkRoute.setLinkIds(link17.getId(), linkIdList, link19.getId());
		stopList = new ArrayList<TransitRouteStop>(4);
		stopList.add(builder.createTransitRouteStop(stop6, 0, 0));
		stopList.add(builder.createTransitRouteStop(stop4, 100, 110));
		stopList.add(builder.createTransitRouteStop(stop3, 300, 310));
		stopList.add(builder.createTransitRouteStop(stop2, 390, Time.UNDEFINED_TIME));
		TransitRoute tRoute2a = builder.createTransitRoute(Id.create(2, TransitRoute.class), networkRoute, stopList, "bus");
		tLine2.addRoute(tRoute2a);
		tRoute2a.addDeparture(builder.createDeparture(Id.create(1, Departure.class), Time.parseTime("07:18:00")));
		tRoute2a.addDeparture(builder.createDeparture(Id.create(2, Departure.class), Time.parseTime("07:28:00")));
		tRoute2a.addDeparture(builder.createDeparture(Id.create(3, Departure.class), Time.parseTime("07:38:00")));

		schedule.addTransitLine(tLine2);
	}

	private void createNetwork() {
		/*
		 * (2)-2 21--(4)-4 20--(6)                                               (12)---12-17--(14)
		 *                    o  \                                               /o
		 *                    2   \                                             /  6
		 *                         6 19                                       10 18
		 *                          \   8                         7           /
		 *                           \  o                         o          /
		 *                           (7)---7- 16--(8)---8- 15--(9)---9 14--(10)
		 *                           /        o                           o  \
		 *                          /         3                           4   \
		 *                         5                                          11
		 *                        /                                             \
		 *                       /                                              o\
		 * (1)---1---(3)---3---(5)                                             5 (11)---13---(13)
		 *                    o
		 *                    1
		 *
		 */
		Network network = (Network) this.scenario.getNetwork();
		network.setCapacityPeriod(3600.0);
		double x3 = -2000;
		Node node1 = NetworkUtils.createAndAddNode(network, Id.create(1, Node.class), new Coord(x3, (double) 0));
		double x2 = -2000;
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create(2, Node.class), new Coord(x2, (double) 1000));
		double x1 = -1000;
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create(3, Node.class), new Coord(x1, (double) 0));
		double x = -1000;
		Node node4 = NetworkUtils.createAndAddNode(network, Id.create(4, Node.class), new Coord(x, (double) 1000));
		Node node5 = NetworkUtils.createAndAddNode(network, Id.create(5, Node.class), new Coord((double) 0, (double) 0));
		Node node6 = NetworkUtils.createAndAddNode(network, Id.create(6, Node.class), new Coord((double) 0, (double) 1000));
		Node node7 = NetworkUtils.createAndAddNode(network, Id.create(7, Node.class), new Coord((double) 500, (double) 500));
		Node node8 = NetworkUtils.createAndAddNode(network, Id.create(8, Node.class), new Coord((double) 1500, (double) 500));
		Node node9 = NetworkUtils.createAndAddNode(network, Id.create(9, Node.class), new Coord((double) 2500, (double) 500));
		Node node10 = NetworkUtils.createAndAddNode(network, Id.create(10, Node.class), new Coord((double) 3500, (double) 500));
		Node node11 = NetworkUtils.createAndAddNode(network, Id.create(11, Node.class), new Coord((double) 4000, (double) 0));
		Node node12 = NetworkUtils.createAndAddNode(network, Id.create(12, Node.class), new Coord((double) 4000, (double) 1000));
		Node node13 = NetworkUtils.createAndAddNode(network, Id.create(13, Node.class), new Coord((double) 5000, (double) 0));
		Node node14 = NetworkUtils.createAndAddNode(network, Id.create(14, Node.class), new Coord((double) 5000, (double) 1000));
		final Node fromNode = node1;
		final Node toNode = node3;

		NetworkUtils.createAndAddLink(network,Id.create( 1, Link.class), fromNode, toNode, 1000.0, 10.0, 3600.0, (double) 1 );
		final Node fromNode1 = node2;
		final Node toNode1 = node4;
		NetworkUtils.createAndAddLink(network,Id.create( 2, Link.class), fromNode1, toNode1, 1000.0, 10.0, 3600.0, (double) 1 );
		final Node fromNode2 = node3;
		final Node toNode2 = node5;
		NetworkUtils.createAndAddLink(network,Id.create( 3, Link.class), fromNode2, toNode2, 1000.0, 10.0, 3600.0, (double) 1 );
		final Node fromNode3 = node4;
		final Node toNode3 = node6;
		NetworkUtils.createAndAddLink(network,Id.create( 4, Link.class), fromNode3, toNode3, 1000.0, 10.0, 3600.0, (double) 1 );
		final Node fromNode4 = node5;
		final Node toNode4 = node7;
		NetworkUtils.createAndAddLink(network,Id.create( 5, Link.class), fromNode4, toNode4, 1000.0, 10.0, 3600.0, (double) 1 );
		final Node fromNode5 = node6;
		final Node toNode5 = node7;
		NetworkUtils.createAndAddLink(network,Id.create( 6, Link.class), fromNode5, toNode5, 1000.0, 10.0, 3600.0, (double) 1 );
		final Node fromNode6 = node7;
		final Node toNode6 = node8;
		NetworkUtils.createAndAddLink(network,Id.create( 7, Link.class), fromNode6, toNode6, 1000.0, 10.0, 3600.0, (double) 1 );
		final Node fromNode7 = node8;
		final Node toNode7 = node9;
		NetworkUtils.createAndAddLink(network,Id.create( 8, Link.class), fromNode7, toNode7, 1000.0, 10.0, 3600.0, (double) 1 );
		final Node fromNode8 = node9;
		final Node toNode8 = node10;
		NetworkUtils.createAndAddLink(network,Id.create( 9, Link.class), fromNode8, toNode8, 1000.0, 10.0, 3600.0, (double) 1 );
		final Node fromNode9 = node10;
		final Node toNode9 = node12;
		NetworkUtils.createAndAddLink(network,Id.create(10, Link.class), fromNode9, toNode9, 1000.0, 10.0, 3600.0, (double) 1 );
		final Node fromNode10 = node10;
		final Node toNode10 = node11;
		NetworkUtils.createAndAddLink(network,Id.create(11, Link.class), fromNode10, toNode10, 1000.0, 10.0, 3600.0, (double) 1 );
		final Node fromNode11 = node12;
		final Node toNode11 = node14;
		NetworkUtils.createAndAddLink(network,Id.create(12, Link.class), fromNode11, toNode11, 1000.0, 10.0, 3600.0, (double) 1 );
		final Node fromNode12 = node11;
		final Node toNode12 = node13;
		NetworkUtils.createAndAddLink(network,Id.create(13, Link.class), fromNode12, toNode12, 1000.0, 10.0, 3600.0, (double) 1 );
		final Node fromNode13 = node10;
		final Node toNode13 = node9;

		NetworkUtils.createAndAddLink(network,Id.create(14, Link.class), fromNode13, toNode13, 1000.0, 10.0, 3600.0, (double) 1 );
		final Node fromNode14 = node9;
		final Node toNode14 = node8;
		NetworkUtils.createAndAddLink(network,Id.create(15, Link.class), fromNode14, toNode14, 1000.0, 10.0, 3600.0, (double) 1 );
		final Node fromNode15 = node8;
		final Node toNode15 = node7;
		NetworkUtils.createAndAddLink(network,Id.create(16, Link.class), fromNode15, toNode15, 1000.0, 10.0, 3600.0, (double) 1 );
		final Node fromNode16 = node14;
		final Node toNode16 = node12;
		NetworkUtils.createAndAddLink(network,Id.create(17, Link.class), fromNode16, toNode16, 1000.0, 10.0, 3600.0, (double) 1 );
		final Node fromNode17 = node12;
		final Node toNode17 = node10;
		NetworkUtils.createAndAddLink(network,Id.create(18, Link.class), fromNode17, toNode17, 1000.0, 10.0, 3600.0, (double) 1 );
		final Node fromNode18 = node7;
		final Node toNode18 = node6;
		NetworkUtils.createAndAddLink(network,Id.create(19, Link.class), fromNode18, toNode18, 1000.0, 10.0, 3600.0, (double) 1 );
		final Node fromNode19 = node6;
		final Node toNode19 = node4;
		NetworkUtils.createAndAddLink(network,Id.create(20, Link.class), fromNode19, toNode19, 1000.0, 10.0, 3600.0, (double) 1 );
		final Node fromNode20 = node4;
		final Node toNode20 = node2;
		NetworkUtils.createAndAddLink(network,Id.create(21, Link.class), fromNode20, toNode20, 1000.0, 10.0, 3600.0, (double) 1 );
	}

}
