/**
 *
 */
package playground.yu.utils.qgis;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.yu.analysis.PlanModeJudger;
import playground.yu.utils.io.SimpleWriter;

/**
 * @author yu
 *
 */
public class ModeWithNewAttributeTextLayer4QGIS extends ModeTextLayer4QGIS {
	private int count, car6hCount, pt6hCount;
	private double travelTime, am6hTravelTime;

	public ModeWithNewAttributeTextLayer4QGIS(String textFilename,
			String attribute) {
		writer = new SimpleWriter(textFilename);
		writer.writeln("x\ty\tmode\t" + attribute + "\ttravelTime");
		count = 0;
		car6hCount = 0;
		pt6hCount = 0;
		travelTime = 0.0;
		am6hTravelTime = 0.0;
	}

	@Override
	public void run(Plan plan) {
		count++;
		Activity act = ((PlanImpl) plan).getFirstActivity();
		Coord homeLoc = act.getCoord();
		double endTime = act.getEndTime();
//		double travelTime = ((PlanImpl) plan).getNextLeg(act).getTravelTime();
		this.travelTime += travelTime;
		String mode = "";

		if (PlanModeJudger.useCar(plan))
			mode = TransportMode.car;
		else if (PlanModeJudger.usePt(plan))
			mode = TransportMode.pt;
		else if (PlanModeJudger.useWalk(plan))
			mode = TransportMode.walk;

		if (endTime ==
		// 21600.0
		86340.0) {
			writer.writeln(homeLoc.getX() + "\t" + homeLoc.getY() + "\t" + mode
					+ "\t6\t" + travelTime);
			am6hTravelTime += travelTime;
			if (mode.equals(TransportMode.car))
				car6hCount++;
			if (mode.equals(TransportMode.pt))
				pt6hCount++;
		} else {
			writer.writeln(homeLoc.getX() + "\t" + homeLoc.getY() + "\t" + mode
					+ "\t0\t" + travelTime);
		}
	}

	@Override
	public void close() {
		writer.writeln("car6h :\t" + car6hCount + "\t" + (double) car6hCount
				/ (double) count + "%");
		writer.writeln("pt6h :\t" + pt6hCount + "\t" + (double) pt6hCount
				/ (double) count + "%");
		writer.writeln("am6h_travelTime :\t" + am6hTravelTime
				/ (car6hCount + pt6hCount) + "\t[s]/[min]");
		writer.writeln("all_travelTime :\t" + travelTime / count
				+ "\t[s]/[min]");
		super.close();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Gbl.startMeasurement();

		// final String netFilename =
		// "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		// final String plansFilename =
		// "../runs_SVN/run674/it.1000/1000.plans.xml.gz";
		// final String textFilename =
		// "../runs_SVN/run674/it.1000/1000.analysis/mode_1.endTime.txt";
		final String netFilename = "../matsimTests/timeAllocationMutatorTest/network.xml";
		final String plansFilename = "../matsimTests/timeAllocationMutatorTest/it.100/100.plans.xml.gz";
		final String textFilename = "../matsimTests/timeAllocationMutatorTest/it.100/mode_1.endTime.txt";

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(netFilename);

		Population population = scenario.getPopulation();
		new MatsimPopulationReader(scenario).readFile(plansFilename);

		ModeWithNewAttributeTextLayer4QGIS mwnatl = new ModeWithNewAttributeTextLayer4QGIS(
				textFilename, "1.actEndTime");
		mwnatl.run(population);
		mwnatl.close();

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}

}
