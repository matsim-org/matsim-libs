/**
 * 
 */
package playground.yu.analysis;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;
import org.xml.sax.SAXException;

import playground.yu.utils.TollTools;
import playground.yu.utils.io.SimpleWriter;

/**
 * @author yu
 * 
 */
public class LinearDistanceExtractor extends AbstractPersonAlgorithm implements
		PlanAlgorithm {
	private RoadPricingScheme toll = null;
	private SimpleWriter sw;
	private double w2hLinearDist = 0.0, legLinearDist = 0.0;
	private int w2hCnt = 0, legCnt = 0, personCnt = 0;

	public LinearDistanceExtractor(RoadPricingScheme toll, String outputFilename) {
		this.toll = toll;
		sw = new SimpleWriter(outputFilename);
		sw.writeln("linearDistance\tlinearDistance [m]\tlinearDistance [km]");
	}

	@Override
	public void run(Person person) {
		Plan plan = person.getSelectedPlan();
		if (toll == null) {
			personCnt++;
			run(plan);
		} else if (TollTools.isInRange(((PlanImpl) plan).getFirstActivity().getLink(), toll)) {
			personCnt++;
			run(plan);
		}
	}

	public void run(Plan p) {
		PlanImpl plan = (PlanImpl) p;
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Leg) {
				Activity previousAct = plan.getPreviousActivity((Leg) pe);
				Activity nextAct = plan.getNextActivity((LegImpl) pe);
				double legLD = CoordUtils.calcDistance(previousAct.getCoord(),
						nextAct.getCoord());
				if (previousAct.getType().startsWith("w")
						&& nextAct.getType().startsWith("h")) {
					w2hCnt++;
					w2hLinearDist += legLD;
				}
				legCnt++;
				legLinearDist += legLD;
			}
		}
	}

	public void write() {
		double avgW2hLD = w2hLinearDist / (double) w2hCnt;
		double avgLegLD = legLinearDist / (double) legCnt;
		sw.writeln("avg. Work2home\t" + avgW2hLD + "\t" + avgW2hLD / 1000.0);
		sw.writeln("avg. Leg\t" + avgLegLD + "\t" + avgLegLD / 1000.0);
		sw.writeln("\npersons :\t" + personCnt + "\tlegs :\t" + legCnt
				+ "\twork2home legs :\t" + w2hCnt);
		sw.close();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		final String plansFilename = "../runs-svn/run684/it.1000/1000.plans.xml.gz";
		final String outputFilename = "../matsimTests/LinearDistance/linearDistanceKanton.txt";
		String tollFilename = "../schweiz-ivtch-SVN/baseCase/roadpricing/KantonZurich.xml";

		Gbl.startMeasurement();

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		PopulationImpl population = new PopulationImpl();
		System.out.println("-->reading plansfile: " + plansFilename);
		new MatsimPopulationReader(population, network).readFile(plansFilename);

		RoadPricingReaderXMLv1 tollReader = new RoadPricingReaderXMLv1(network);
		try {
			tollReader.parse(tollFilename);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		LinearDistanceExtractor lde = new LinearDistanceExtractor(tollReader
				.getScheme(), outputFilename);
		lde.run(population);
		lde.write();

		System.out.println("--> Done!");

		Gbl.printElapsedTime();
		System.exit(0);
	}
}
