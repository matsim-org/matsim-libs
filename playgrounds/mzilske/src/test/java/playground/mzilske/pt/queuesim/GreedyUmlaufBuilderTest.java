package playground.mzilske.pt.queuesim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.Umlauf;
import org.matsim.pt.UmlaufBuilder;
import org.matsim.pt.UmlaufInterpolator;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitScheduleFactory;
import org.matsim.transitSchedule.api.TransitStopFacility;

public class GreedyUmlaufBuilderTest {

	ScenarioImpl scenario = new ScenarioImpl();

	private final Id[] ids = new Id[25];

	private void createIds() {
		for (int i = 0; i < this.ids.length; i++) {
			this.ids[i] = this.scenario.createId(Integer.toString(i));
		}
	}

	@Test
	public void testGreedyUmlaufBuilder() {
		createIds();
		Config config = this.scenario.getConfig();
		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		config.getQSimConfigGroup().setSnapshotStyle("queue");
		config.getQSimConfigGroup().setEndTime(24.0*3600);
		createNetwork();
		setupSchedule();
		Collection<TransitLine> transitLines = scenario.getTransitSchedule().getTransitLines().values();
		UmlaufBuilder umlaufBuilder = new GreedyUmlaufBuilderImpl(new UmlaufInterpolator(scenario.getNetwork(), scenario.getConfig().charyparNagelScoring()), transitLines);
		Collection<Umlauf> umlaeufe = umlaufBuilder.build();
	}

	private void setupSchedule() {
		TransitSchedule schedule = this.scenario.getTransitSchedule();
		TransitScheduleFactory builder = schedule.getFactory();
		TransitStopFacility stop1 = builder.createTransitStopFacility(this.ids[1], this.scenario.createCoord(-100, -50), false);
		TransitStopFacility stop2 = builder.createTransitStopFacility(this.ids[2], this.scenario.createCoord(-100, 850), false);
		TransitStopFacility stop3 = builder.createTransitStopFacility(this.ids[3], this.scenario.createCoord(1400, 450), false);
		TransitStopFacility stop4 = builder.createTransitStopFacility(this.ids[4], this.scenario.createCoord(3400, 450), false);
		TransitStopFacility stop5 = builder.createTransitStopFacility(this.ids[5], this.scenario.createCoord(3900, 50), false);
		TransitStopFacility stop6 = builder.createTransitStopFacility(this.ids[6], this.scenario.createCoord(3900, 850), false);

		TransitStopFacility stop7 = builder.createTransitStopFacility(this.ids[7], this.scenario.createCoord(2600, 550), false);
		TransitStopFacility stop8 = builder.createTransitStopFacility(this.ids[8], this.scenario.createCoord( 600, 550), false);

		LinkImpl link1 = this.scenario.getNetwork().getLinks().get(this.ids[1]);
		LinkImpl link2 = this.scenario.getNetwork().getLinks().get(this.ids[2]);
		LinkImpl link3 = this.scenario.getNetwork().getLinks().get(this.ids[3]);
		LinkImpl link4 = this.scenario.getNetwork().getLinks().get(this.ids[4]);
		LinkImpl link5 = this.scenario.getNetwork().getLinks().get(this.ids[5]);
		LinkImpl link6 = this.scenario.getNetwork().getLinks().get(this.ids[6]);
		LinkImpl link7 = this.scenario.getNetwork().getLinks().get(this.ids[7]);
		LinkImpl link8 = this.scenario.getNetwork().getLinks().get(this.ids[8]);
		LinkImpl link9 = this.scenario.getNetwork().getLinks().get(this.ids[9]);
		LinkImpl link10 = this.scenario.getNetwork().getLinks().get(this.ids[10]);
		LinkImpl link11 = this.scenario.getNetwork().getLinks().get(this.ids[11]);
		LinkImpl link12 = this.scenario.getNetwork().getLinks().get(this.ids[12]);
		LinkImpl link13 = this.scenario.getNetwork().getLinks().get(this.ids[13]);
		LinkImpl link14 = this.scenario.getNetwork().getLinks().get(this.ids[14]);
		LinkImpl link15 = this.scenario.getNetwork().getLinks().get(this.ids[15]);
		LinkImpl link16 = this.scenario.getNetwork().getLinks().get(this.ids[16]);
		LinkImpl link17 = this.scenario.getNetwork().getLinks().get(this.ids[17]);
		LinkImpl link18 = this.scenario.getNetwork().getLinks().get(this.ids[18]);
		LinkImpl link19 = this.scenario.getNetwork().getLinks().get(this.ids[19]);

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

		TransitLine tLine2 = builder.createTransitLine(this.ids[2]);
		NetworkRouteWRefs networkRoute = (NetworkRouteWRefs) this.scenario.getNetwork().getFactory().createRoute(TransportMode.car, link2.getId(), link12.getId());
		ArrayList<Id> linkIdList = new ArrayList<Id>(6);
		Collections.addAll(linkIdList, link4.getId(), link6.getId(), link7.getId(), link8.getId(), link9.getId(), link10.getId());
		networkRoute.setLinkIds(link2.getId(), linkIdList, link12.getId());
		ArrayList<TransitRouteStop> stopList = new ArrayList<TransitRouteStop>(4);
		stopList.add(builder.createTransitRouteStop(stop2, 0, 0));
		stopList.add(builder.createTransitRouteStop(stop3, 90, 100));
		stopList.add(builder.createTransitRouteStop(stop4, 290, 300));
		stopList.add(builder.createTransitRouteStop(stop6, 390, Time.UNDEFINED_TIME));
		TransitRoute tRoute2 = builder.createTransitRoute(this.ids[1], networkRoute, stopList, TransportMode.bus);
		tLine2.addRoute(tRoute2);
		tRoute2.addDeparture(builder.createDeparture(this.ids[1], Time.parseTime("07:02:00")));
		tRoute2.addDeparture(builder.createDeparture(this.ids[2], Time.parseTime("07:12:00")));
		tRoute2.addDeparture(builder.createDeparture(this.ids[3], Time.parseTime("07:22:00")));

		networkRoute = (NetworkRouteWRefs) this.scenario.getNetwork().getFactory().createRoute(TransportMode.car, link17.getId(), link19.getId());
		linkIdList = new ArrayList<Id>(6);
		Collections.addAll(linkIdList, link18.getId(), link14.getId(), link15.getId(), link16.getId());
		networkRoute.setLinkIds(link17.getId(), linkIdList, link19.getId());
		stopList = new ArrayList<TransitRouteStop>(4);
		stopList.add(builder.createTransitRouteStop(stop6, 0, 0));
		stopList.add(builder.createTransitRouteStop(stop4, 100, 110));
		stopList.add(builder.createTransitRouteStop(stop3, 300, 310));
		stopList.add(builder.createTransitRouteStop(stop2, 390, Time.UNDEFINED_TIME));
		TransitRoute tRoute2a = builder.createTransitRoute(this.ids[2], networkRoute, stopList, TransportMode.bus);
		tLine2.addRoute(tRoute2a);
		tRoute2a.addDeparture(builder.createDeparture(this.ids[1], Time.parseTime("07:18:00")));
		tRoute2a.addDeparture(builder.createDeparture(this.ids[2], Time.parseTime("07:28:00")));
		tRoute2a.addDeparture(builder.createDeparture(this.ids[3], Time.parseTime("07:38:00")));

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
		NetworkLayer network = this.scenario.getNetwork();
		network.setCapacityPeriod(3600.0);
		Node node1 = network.createAndAddNode(this.ids[1], this.scenario.createCoord(-2000, 0));
		Node node2 = network.createAndAddNode(this.ids[2], this.scenario.createCoord(-2000, 1000));
		Node node3 = network.createAndAddNode(this.ids[3], this.scenario.createCoord(-1000, 0));
		Node node4 = network.createAndAddNode(this.ids[4], this.scenario.createCoord(-1000, 1000));
		Node node5 = network.createAndAddNode(this.ids[5], this.scenario.createCoord(0, 0));
		Node node6 = network.createAndAddNode(this.ids[6], this.scenario.createCoord(0, 1000));
		Node node7 = network.createAndAddNode(this.ids[7], this.scenario.createCoord(500, 500));
		Node node8 = network.createAndAddNode(this.ids[8], this.scenario.createCoord(1500, 500));
		Node node9 = network.createAndAddNode(this.ids[9], this.scenario.createCoord(2500, 500));
		Node node10 = network.createAndAddNode(this.ids[10], this.scenario.createCoord(3500, 500));
		Node node11 = network.createAndAddNode(this.ids[11], this.scenario.createCoord(4000, 0));
		Node node12 = network.createAndAddNode(this.ids[12], this.scenario.createCoord(4000, 1000));
		Node node13 = network.createAndAddNode(this.ids[13], this.scenario.createCoord(5000, 0));
		Node node14 = network.createAndAddNode(this.ids[14], this.scenario.createCoord(5000, 1000));

		network.createAndAddLink(this.ids[1], node1, node3, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(this.ids[2], node2, node4, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(this.ids[3], node3, node5, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(this.ids[4], node4, node6, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(this.ids[5], node5, node7, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(this.ids[6], node6, node7, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(this.ids[7], node7, node8, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(this.ids[8], node8, node9, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(this.ids[9], node9, node10, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(this.ids[10], node10, node12, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(this.ids[11], node10, node11, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(this.ids[12], node12, node14, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(this.ids[13], node11, node13, 1000.0, 10.0, 3600.0, 1);

		network.createAndAddLink(this.ids[14], node10, node9, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(this.ids[15], node9, node8, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(this.ids[16], node8, node7, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(this.ids[17], node14, node12, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(this.ids[18], node12, node10, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(this.ids[19], node7, node6, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(this.ids[20], node6, node4, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(this.ids[21], node4, node2, 1000.0, 10.0, 3600.0, 1);
	}

}
