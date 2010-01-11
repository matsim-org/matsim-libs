/**
 * 
 */
package playground.yu.analysis;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
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
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;
import org.xml.sax.SAXException;

import playground.yu.utils.TollTools;
import playground.yu.utils.charts.PieChart;
import playground.yu.utils.io.SimpleWriter;

/**
 * @author yu
 * 
 */
public class ModeSplit extends AbstractPersonAlgorithm implements PlanAlgorithm {
	private int carLegs = 0, ptLegs = 0, wlkLegs = 0, bikeLegs = 0,
			othersLegs = 0, tollCarLegs = 0, tollPtLegs = 0, tollWlkLegs = 0,
			tollBikeLegs = 0, tollOthersLegs = 0;
	private RoadPricingScheme toll = null;

	public ModeSplit(final RoadPricingScheme toll) {
		this.toll = toll;
	}

	@Override
	public void run(final Person person) {
		run(person.getSelectedPlan());
	}

	public void run(final Plan plan) {
		boolean inRange = false;
		if (toll != null)
			inRange = TollTools.isInRange(((PlanImpl) plan).getFirstActivity().getLinkId(), toll);
		for (PlanElement pe : plan.getPlanElements())
			if (pe instanceof LegImpl) {
				TransportMode m = ((LegImpl) pe).getMode();
				switch (m) {
				case car:
					carLegs++;
					if (inRange)
						tollCarLegs++;
					break;
				case pt:
					ptLegs++;
					if (inRange)
						tollPtLegs++;
					break;
				case walk:
					wlkLegs++;
					if (inRange)
						tollWlkLegs++;
					break;
				case bike:
					bikeLegs++;
					if (inRange)
						tollBikeLegs++;
					break;
				default:
					othersLegs++;
					if (inRange)
						tollOthersLegs++;
					break;
				}
			}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("(total)mode\tnumber\tfraction[%]\n");
		double sum = carLegs + ptLegs + wlkLegs + bikeLegs + othersLegs;
		sb.append("car\t" + carLegs + "\t" + carLegs / sum * 100.0 + "\n");
		sb.append("pt\t" + ptLegs + "\t" + ptLegs / sum * 100.0 + "\n");
		sb.append("walk\t" + wlkLegs + "\t" + wlkLegs / sum * 100.0 + "\n");
		sb.append("bike\t" + bikeLegs + "\t" + bikeLegs / sum * 100.0 + "\n");
		sb.append("others\t" + othersLegs + "\t" + othersLegs / sum * 100.0
				+ "\n");

		if (toll != null) {
			sum = tollCarLegs + tollPtLegs + tollWlkLegs + tollBikeLegs
					+ tollOthersLegs;
			sb.append("(toll area)mode\tnumber\tfraction[%]\n");
			sb.append("car\t" + tollCarLegs + "\t" + tollCarLegs / sum * 100.0
					+ "\n");
			sb.append("pt\t" + tollPtLegs + "\t" + tollPtLegs / sum * 100.0
					+ "\n");
			sb.append("walk\t" + tollWlkLegs + "\t" + tollWlkLegs / sum * 100.0
					+ "\n");
			sb.append("bike\t" + tollBikeLegs + "\t" + tollBikeLegs / sum
					* 100.0 + "\n");
			sb.append("other\t" + tollOthersLegs + "\t" + tollOthersLegs / sum
					* 100.0 + "\n");
		}

		return sb.toString();
	}

	public void write(final String outputPath) {
		SimpleWriter sw = new SimpleWriter(outputPath + "modalSplitLegs.txt");
		sw.write(toString());
		sw.close();
		PieChart chart = new PieChart("ModalSplit -- Legs");
		chart
				.addSeries(
						new String[] { "car", "pt", "walk", "bike", "other" },
						new double[] { carLegs, ptLegs, wlkLegs, bikeLegs,
								othersLegs });
		chart.saveAsPng(outputPath + "modalSplitLegs.png", 800, 600);
		if (toll != null) {
			PieChart chart2 = new PieChart(
					"ModalSplit Center (toll area) -- Legs");
			chart2.addSeries(new String[] { "car", "pt", "walk", "bike",
					"other" }, new double[] { tollCarLegs, tollPtLegs,
					tollWlkLegs, tollBikeLegs, tollOthersLegs });
			chart2.saveAsPng(outputPath + "modalSplitTollLegs.png", 800, 600);
		}
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		Gbl.startMeasurement();

		final String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		final String plansFilename = "../runs-svn/run712/it.1000/1000.plans.xml.gz";
		final String tollFilename = "../matsimTests/toll/KantonZurichToll.xml";
		final String outputPath = "../matsimTests/run712/";
		// final String netFilename = "../matsimTests/scoringTest/network.xml";
		// final String plansFilename =
		// "../matsimTests/scoringTest/output/ITERS/it.100/100.plans.xml.gz";
		// final String textFilename =
		// "../matsimTests/scoringTest/output/ITERS/it.100/mode.txt";

		ScenarioImpl scenario = new ScenarioImpl();
		NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(network).readFile(netFilename);

		PopulationImpl population = scenario.getPopulation();
		new MatsimPopulationReader(scenario).readFile(plansFilename);

		RoadPricingReaderXMLv1 tollReader = new RoadPricingReaderXMLv1();
		try {
			tollReader.parse(tollFilename);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		ModeSplit ms = new ModeSplit(null);
		ms.run(population);
		ms.write(outputPath);

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}
}
