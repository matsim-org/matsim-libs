/**
 *
 */
package playground.yu.newPlans;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.population.routes.NodeNetworkRouteImpl;
import org.matsim.core.utils.misc.Time;

/**
 * @author yu
 * 
 */
public class PtPlansFileCreator {
	private int personCount = 0;
	private PopulationImpl pop = null;

	public void setPop(final PopulationImpl pop) {
		this.pop = pop;
	}

	public void setNetwork(final NetworkLayer network) {
		this.network = network;
	}

	public PopulationImpl getPop() {
		return this.pop;
	}

	private NetworkLayer network = null;

	public NetworkLayer getNetwork() {
		return this.network;
	}

	/**
	 *
	 */
	public PtPlansFileCreator() {
	}

	public void createPersons() {
		// north
		createNorthBus245Person("04:27");
		createNorthBus245Person("04:47");
		createNorthBus245Person("05:07");
		createNorthBus245Person("05:27");
		createNorthBus245Person("05:45");
		createNorthBus245Person("05:52");
		createNorthBus245Person("05:58");
		for (int i = 6; i < 20; i++) {
			for (int j = 0; j < 6; j++) {
				createNorthBus245Person(((i < 10) ? ("0" + i) : i) + ":" + j
						+ "8");
			}
		}
		for (int i = 20; i < 24; i++) {
			for (int j = 0; j < 5; j += 2) {
				createNorthBus245Person(i + ":" + j + "7");
			}
		}
		createNorthBus245Person("00:07");
		// south
		createSouthBus245Person("04:58");
		createSouthBus245Person("05:18");
		createSouthBus245Person("05:38");
		createSouthBus245Person("05:48");
		createSouthBus245Person("05:58");

		for (int j = 1; j < 6; j++) {
			createSouthBus245Person("06:" + j + "0");
		}

		for (int i = 7; i < 19; i++) {
			for (int j = 0; j < 6; j++) {
				createSouthBus245Person(((i < 10) ? ("0" + i) : i) + ":" + j
						+ "0");
			}
		}

		for (int j = 0; j < 5; j++) {
			createSouthBus245Person("19:" + j + "0");
		}

		createSouthBus245Person("20:00");

		for (int i = 20; i < 24; i++) {
			for (int j = 1; j < 6; j += 2) {
				createSouthBus245Person(i + ":" + j + "8");
			}
		}

		createSouthBus245Person("00:18");
	}

	public List<Node> getSrcRoute(final int[] nodes) {
		List<Node> srcRoute = new ArrayList<Node>();
		for (int i = 0; i < nodes.length; i++)
			srcRoute.add(this.network.getNode(new IdImpl(nodes[i])));
		return srcRoute;
	}

	protected void createSouthBus245Person(final String endTime) {
		int[] southNodes = { 2857, 2856, 2855, 1000142, 2850, 1000134, 2848,
				6575, 2787, 2805, 1000544, 6362, 1000540, 2768, 1000580, 2765,
				2763, 2727, 2728, 1915, 1918, 1917, 1867, 1872, 1869, 1865,
				1000447, 1866, 1854, 1853, 1852, 1863, 1000448, 1859, 1861,
				1849, 1892, 1893, 1000454, 1906 };
		createPtPerson("7622", endTime, "1982", getSrcRoute(southNodes));
	}

	protected void createNorthBus245Person(final String endTime) {
		int[] northNodes = { 1906, 1000453, 1892, 1849, 1861, 1859, 1000448,
				1863, 1856, 1854, 1866, 1000447, 1865, 1869, 1872, 1867, 1917,
				1918, 1915, 2728, 2727, 2763, 2765, 1000580, 2768, 1000540,
				6362, 1000544, 2805, 2787, 6575, 2848, 1000134, 2850, 1000142,
				2855, 2856, 2857 };
		createPtPerson("1981", endTime, "7621", getSrcRoute(northNodes));
	}

	@SuppressWarnings("deprecation")
	private void createPtPerson(final String startLinkId, final String endTime,
			final String endLinkId, final List<Node> srcRoute) {

		PersonImpl p = new PersonImpl(new IdImpl("245-" + this.personCount));
		try {
			PlanImpl pl = new org.matsim.core.population.PlanImpl(p);
			p.addPlan(pl);
			LinkImpl startLink = this.network.getLink(new IdImpl(startLinkId));
			ActivityImpl a = pl.createAndAddActivity("h", startLink);
			a.setEndTime(Time.parseTime(endTime));
			LegImpl leg = pl.createAndAddLeg(TransportMode.car);
			leg.setDepartureTime(Time.parseTime(endTime));
			NetworkRouteWRefs route = new NodeNetworkRouteImpl();
			leg.setRoute(route);
			LinkImpl endLink = this.network.getLink(new IdImpl(endLinkId));
			pl.createAndAddActivity("w", endLink);
			route.setNodes(startLink, srcRoute, endLink);
			this.pop.addPerson(p);
			this.personCount++;
			System.out.println("i have " + this.pop.getPersons().size()
					+ " persons\t" + startLinkId + "\t" + endTime + "\t"
					+ endLinkId);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		String newPlansFilename = "output/bvg/245.xml.gz";
		String netFilename = "test/scenarios/berlin/network.xml.gz";

		PtPlansFileCreator pfc = new PtPlansFileCreator();
		pfc.setNetwork(new NetworkLayer());
		new MatsimNetworkReader(pfc.getNetwork()).readFile(netFilename);

		pfc.setPop(new PopulationImpl());
		pfc.createPersons();
		new PopulationWriter(pfc.getPop()).writeFile(newPlansFilename);
		System.out.println("done.");
	}
}
