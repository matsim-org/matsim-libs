package playground.mzilske.pt.queuesim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
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

	ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

	@Test
	public void testGreedyUmlaufBuilder() {
		Config config = this.scenario.getConfig();
		config.scenario().setUseTransit(true);
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
		TransitStopFacility stop1 = builder.createTransitStopFacility(Id.create(1, TransitStopFacility.class), this.scenario.createCoord(-100, -50), false);
		TransitStopFacility stop2 = builder.createTransitStopFacility(Id.create(2, TransitStopFacility.class), this.scenario.createCoord(-100, 850), false);
		TransitStopFacility stop3 = builder.createTransitStopFacility(Id.create(3, TransitStopFacility.class), this.scenario.createCoord(1400, 450), false);
		TransitStopFacility stop4 = builder.createTransitStopFacility(Id.create(4, TransitStopFacility.class), this.scenario.createCoord(3400, 450), false);
		TransitStopFacility stop5 = builder.createTransitStopFacility(Id.create(5, TransitStopFacility.class), this.scenario.createCoord(3900, 50), false);
		TransitStopFacility stop6 = builder.createTransitStopFacility(Id.create(6, TransitStopFacility.class), this.scenario.createCoord(3900, 850), false);

		TransitStopFacility stop7 = builder.createTransitStopFacility(Id.create(7, TransitStopFacility.class), this.scenario.createCoord(2600, 550), false);
		TransitStopFacility stop8 = builder.createTransitStopFacility(Id.create(8, TransitStopFacility.class), this.scenario.createCoord( 600, 550), false);

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
		NetworkImpl network = (NetworkImpl) this.scenario.getNetwork();
		network.setCapacityPeriod(3600.0);
		Node node1 = network.createAndAddNode(Id.create(1, Node.class), this.scenario.createCoord(-2000, 0));
		Node node2 = network.createAndAddNode(Id.create(2, Node.class), this.scenario.createCoord(-2000, 1000));
		Node node3 = network.createAndAddNode(Id.create(3, Node.class), this.scenario.createCoord(-1000, 0));
		Node node4 = network.createAndAddNode(Id.create(4, Node.class), this.scenario.createCoord(-1000, 1000));
		Node node5 = network.createAndAddNode(Id.create(5, Node.class), this.scenario.createCoord(0, 0));
		Node node6 = network.createAndAddNode(Id.create(6, Node.class), this.scenario.createCoord(0, 1000));
		Node node7 = network.createAndAddNode(Id.create(7, Node.class), this.scenario.createCoord(500, 500));
		Node node8 = network.createAndAddNode(Id.create(8, Node.class), this.scenario.createCoord(1500, 500));
		Node node9 = network.createAndAddNode(Id.create(9, Node.class), this.scenario.createCoord(2500, 500));
		Node node10 = network.createAndAddNode(Id.create(10, Node.class), this.scenario.createCoord(3500, 500));
		Node node11 = network.createAndAddNode(Id.create(11, Node.class), this.scenario.createCoord(4000, 0));
		Node node12 = network.createAndAddNode(Id.create(12, Node.class), this.scenario.createCoord(4000, 1000));
		Node node13 = network.createAndAddNode(Id.create(13, Node.class), this.scenario.createCoord(5000, 0));
		Node node14 = network.createAndAddNode(Id.create(14, Node.class), this.scenario.createCoord(5000, 1000));

		network.createAndAddLink(Id.create( 1, Link.class), node1, node3, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(Id.create( 2, Link.class), node2, node4, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(Id.create( 3, Link.class), node3, node5, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(Id.create( 4, Link.class), node4, node6, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(Id.create( 5, Link.class), node5, node7, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(Id.create( 6, Link.class), node6, node7, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(Id.create( 7, Link.class), node7, node8, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(Id.create( 8, Link.class), node8, node9, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(Id.create( 9, Link.class), node9, node10, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(Id.create(10, Link.class), node10, node12, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(Id.create(11, Link.class), node10, node11, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(Id.create(12, Link.class), node12, node14, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(Id.create(13, Link.class), node11, node13, 1000.0, 10.0, 3600.0, 1);

		network.createAndAddLink(Id.create(14, Link.class), node10, node9, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(Id.create(15, Link.class), node9, node8, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(Id.create(16, Link.class), node8, node7, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(Id.create(17, Link.class), node14, node12, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(Id.create(18, Link.class), node12, node10, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(Id.create(19, Link.class), node7, node6, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(Id.create(20, Link.class), node6, node4, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(Id.create(21, Link.class), node4, node2, 1000.0, 10.0, 3600.0, 1);
	}

}
