/**
 * 
 */
package playground.yu.newPlan;

import java.util.ArrayList;
import java.util.List;

import org.matsim.basic.v01.IdImpl;
import org.matsim.basic.v01.BasicLeg.Mode;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.PersonImpl;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.PopulationWriter;
import org.matsim.population.routes.CarRoute;
import org.matsim.population.routes.NodeCarRoute;
import org.matsim.utils.misc.Time;

/**
 * @author yu
 * 
 */
public class PtPlansFileCreator {
	private int personCount = 0;
	private Population pop;

	public void setPop(Population pop) {
		this.pop = pop;
	}

	public void setNetwork(NetworkLayer network) {
		this.network = network;
	}

	public Population getPop() {
		return pop;
	}

	private NetworkLayer network;

	public NetworkLayer getNetwork() {
		return network;
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

	public List<Node> getSrcRoute(int[] nodes) {
		List<Node> srcRoute = new ArrayList<Node>();
		for (int i = 0; i < nodes.length; i++)
			srcRoute.add(network.getNode(new IdImpl(nodes[i])));
		return srcRoute;
	}

	protected void createSouthBus245Person(String endTime) {
		int[] southNodes = { 2857, 2856, 2855, 1000142, 2850, 1000134, 2848,
				6575, 2787, 2805, 1000544, 6362, 1000540, 2768, 1000580, 2765,
				2763, 2727, 2728, 1915, 1918, 1917, 1867, 1872, 1869, 1865,
				1000447, 1866, 1854, 1853, 1852, 1863, 1000448, 1859, 1861,
				1849, 1892, 1893, 1000454, 1906 };
		createPtPerson("7622", endTime, "1982", getSrcRoute(southNodes));
	}

	protected void createNorthBus245Person(String endTime) {
		int[] northNodes = { 1906, 1000453, 1892, 1849, 1861, 1859, 1000448,
				1863, 1856, 1854, 1866, 1000447, 1865, 1869, 1872, 1867, 1917,
				1918, 1915, 2728, 2727, 2763, 2765, 1000580, 2768, 1000540,
				6362, 1000544, 2805, 2787, 6575, 2848, 1000134, 2850, 1000142,
				2855, 2856, 2857 };
		createPtPerson("1981", endTime, "7621", getSrcRoute(northNodes));
	}

	private void createPtPerson(String startLinkId, String endTime,
			String endLinkId, List<Node> srcRoute) {

		Person p = new PersonImpl(new IdImpl("245-" + personCount));
		try {
			Plan pl = new Plan(p);
			p.addPlan(pl);
			Link link = this.network.getLink(new IdImpl(startLinkId));
			Act a = pl.createAct("h", link);
			a.setEndTime(Time.parseTime(endTime));
			Leg leg = pl.createLeg(Mode.car);
			leg.setDepartureTime(Time.parseTime(endTime));
			CarRoute route = new NodeCarRoute();
			leg.setRoute(route);
			route.setNodes(srcRoute);
			link = this.network.getLink(new IdImpl(endLinkId));
			pl.createAct("w", link);
			pop.addPerson(p);
			personCount++;
			System.out.println("i have " + pop.getPersons().size()
					+ " persons\t" + startLinkId + "\t" + endTime + "\t"
					+ endLinkId);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String newPlansFilename = "output/bvg/245.xml.gz";
		String netFilename = "test/scenarios/berlin/network.xml.gz";

		PtPlansFileCreator pfc = new PtPlansFileCreator();

		Gbl.createConfig(null);
		pfc.setNetwork(new NetworkLayer());
		new MatsimNetworkReader(pfc.getNetwork()).readFile(netFilename);
		Gbl.getWorld().setNetworkLayer(pfc.getNetwork());
		Gbl.getWorld().complete();

		pfc.setPop(new Population());
		PopulationWriter writer = new PopulationWriter(pfc.getPop(),
				newPlansFilename, "v4", 1.0);
		writer.writeStartPlans();
		// TODO
		pfc.createPersons();
		writer.writePersons();
		writer.writeEndPlans();
		System.out.println("done.");
	}
}
