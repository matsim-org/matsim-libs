/**
 * 
 */
package playground.qiuhan.sa;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.yu.utils.LegBeelineDistanceCalculator;
import playground.yu.utils.io.DistributionCreator;

/**
 * @author Q. SUN
 * 
 */
public class BeelineDistanceHistogram extends AbstractPersonAlgorithm implements
		PlanAlgorithm {
	private final Network network;
	private final List<Double> beelineDistances;

	public BeelineDistanceHistogram(Network network) {
		this.network = network;
		this.beelineDistances = new ArrayList<Double>();
	}

	public List<Double> getBeelineDistances() {
		return beelineDistances;
	}

	@Override
	public void run(Plan plan) {
		Activity preAct = null, nextAct = null;
		for (PlanElement planElement : plan.getPlanElements()) {
			if (planElement instanceof Activity) {
				if (preAct == null)/* first act */{
					preAct = (Activity) planElement;
					continue;
				}
				nextAct = (Activity) planElement;
				double dist = LegBeelineDistanceCalculator
						.getBeelineDistance_coord(network, preAct, nextAct);
				if (dist == 0d) {
					dist = 1d;
				}
				beelineDistances.add(dist);
				preAct = nextAct;
			}
		}

	}

	@Override
	public void run(Person person) {
		run(person.getSelectedPlan());
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String networkFile = "output/matsimNetwork/network.multimodalCombi2.xml"//

		, PopulationFile = "input/A_NM/plans.xml.gz"//

		// , PopulationFile =
		// "output/population/popRoutedOevModellCombi2_10pct.xml.gz"//

		, outputFileBase = "output/comparison/";

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils
				.createConfig());
		new MatsimNetworkReader(scenario).readFile(networkFile);
		new MatsimPopulationReader(scenario).readFile(PopulationFile);

		Network network = scenario.getNetwork();
		Population population = scenario.getPopulation();

		BeelineDistanceHistogram bdh = new BeelineDistanceHistogram(network);
		bdh.run(population);

		DistributionCreator dc = new DistributionCreator(
				bdh.getBeelineDistances(), 50d);

		dc.write(outputFileBase + "AN_beelineDistance_abs.log");
		dc.writePercent(outputFileBase + "AN_beelineDistance_pct.log");
		dc.createChart(outputFileBase + "AN_beelineDistance_abs.png",
				"AN_beelineDistance_abs", "distance [m]",
				"number of beeline distances", true);
		dc.createChartPercent(outputFileBase + "AN_beelineDistance_pct.png",
				"AN_beelineDistance_pct", "distance [m]",
				"fraction of beeline distances", true);

		// dc.write(outputFileBase + "QS_beelineDistance_abs.log");
		// dc.writePercent(outputFileBase + "QS_beelineDistance_pct.log");
		// dc.createChart(outputFileBase + "QS_beelineDistance_abs.png",
		// "QS_beelineDistance_abs", "distance [m]",
		// "number of beeline distances", true);
		// dc.createChartPercent(outputFileBase + "QS_beelineDistance_pct.png",
		// "QS_beelineDistance_pct", "distance [m]",
		// "fraction of beeline distances", true);
	}
}
