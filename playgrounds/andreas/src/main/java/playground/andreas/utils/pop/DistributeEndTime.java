package playground.andreas.utils.pop;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.core.utils.misc.Time;

/**
 * Create numberOfcopies additional persons for a given plan.
 *
 * @author aneumann
 *
 */
public class DistributeEndTime extends NewPopulation {

	private final double startTime;
	private final double endTime;

	public DistributeEndTime(Network network, Population plans, String filename, double startTime, double endTime) {
		super(network, plans, filename);
		this.startTime = startTime;
		this.endTime = endTime;
	}

	@Override
	public void run(Person person) {
		PlanElement pE = person.getSelectedPlan().getPlanElements().get(0);
		Activity act = (Activity) pE;
		act.setEndTime(this.startTime + MatsimRandom.getRandom().nextDouble() * (this.endTime - this.startTime));
		this.popWriter.writePerson(person);
	}

	public static void main(final String[] args) {
		Gbl.startMeasurement();

		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		String networkFile = "F:/p/network_real.xml";
		String inPlansFile = "F:/p/population_ABBACDDC_1000x.xml.gz";
		String outPlansFile = "F:/p/population_ABBACDDC_1000x_time.xml.gz";
		double startTime = Time.parseTime("06:00:00");
		double endTime = Time.parseTime("10:00:00");

		NetworkImpl net = sc.getNetwork();
		new MatsimNetworkReader(sc).readFile(networkFile);

		Population inPop = sc.getPopulation();
		PopulationReader popReader = new MatsimPopulationReader(sc);
		popReader.readFile(inPlansFile);

		DistributeEndTime dp = new DistributeEndTime(net, inPop, outPlansFile, startTime, endTime);
		dp.run(inPop);
		dp.writeEndPlans();

		Gbl.printElapsedTime();
	}
}
