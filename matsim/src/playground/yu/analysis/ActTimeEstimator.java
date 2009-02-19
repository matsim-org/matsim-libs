/**
 * 
 */
package playground.yu.analysis;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.basic.v01.BasicPlanImpl.ActIterator;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Act;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Person;
import org.matsim.population.Population;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.utils.io.IOUtils;

/**
 * @author yu
 * 
 */
public class ActTimeEstimator extends AbstractPersonAlgorithm {
	private static class ActTimeCounter {
		private int count = 0;
		private double sum = 0.0, min = 86400.0, max = 0.0;

		public double getMin() {
			return min;
		}

		public double getMax() {
			return max;
		}

		public void add(double time) {
			sum += time;
			count++;
			if (time > max) {
				max = time;
			} else if (time < min) {
				min = time;
			}
		}

		public double getAvg() {
			return sum / (double) count;
		}
	}

	private static class ActDurCounter extends ActTimeCounter {

	}

	private static class ActStartTimeCounter extends ActTimeCounter {

	}

	private static class ActEndTimeCounter extends ActTimeCounter {

	}

	@Override
	public void run(Person person) {
		for (ActIterator ai = person.getSelectedPlan().getIteratorAct(); ai
				.hasNext();) {
			Act a = (Act) ai.next();
			String actType = a.getType();
			ActDurCounter adc = actDurs.get(actType);
			if (adc == null) {
				adc = new ActDurCounter();
				actDurs.put(actType, adc);
			}
			adc.add(a.getDuration());
			ActStartTimeCounter astc = actStarts.get(actType);
			if (astc == null) {
				astc = new ActStartTimeCounter();
				actStarts.put(actType, astc);
			}
			astc.add(a.getStartTime());
			ActEndTimeCounter aetc = actEnds.get(actType);
			if (aetc == null) {
				aetc = new ActEndTimeCounter();
				actEnds.put(actType, aetc);
			}
			aetc.add(a.getEndTime());
		}
	}

	private Map<String, ActDurCounter> actDurs = new HashMap<String, ActDurCounter>();
	private Map<String, ActStartTimeCounter> actStarts = new HashMap<String, ActStartTimeCounter>();
	private Map<String, ActEndTimeCounter> actEnds = new HashMap<String, ActEndTimeCounter>();

	public void write(final String outputFilename) {
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(outputFilename);
			writer
					.write("actType\tactDur\tavg.\tmin\tmax\tactStart\tavg.\tmin\tmax\tactEnd\tavg.\tmin\tmax\n");
			for (String actType : actDurs.keySet()) {
				ActDurCounter adc = actDurs.get(actType);
				ActStartTimeCounter astc = actStarts.get(actType);
				ActEndTimeCounter aetc = actEnds.get(actType);
				writer.write(actType + "\tactDur\t" + adc.getAvg() + "\t"
						+ adc.getMin() + "\t" + adc.getMax() + "\tactStart\t"
						+ astc.getAvg() + "\t" + astc.getMin() + "\t"
						+ astc.getMax() + "\tactEnd\t" + aetc.getAvg() + "\t"
						+ aetc.getMin() + "\t" + aetc.getMax() + "\n");
			}
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Gbl.startMeasurement();

		final String netFilename = "input/Toronto/toronto.xml";
		final String plansFilename = "input/Toronto/fout_chains210.xml.gz";
		final String outputFilename = "output/Toronto/fout_chains210_actDur.txt";

		Gbl.createConfig(null);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		Population population = new Population();

		ActTimeEstimator ade = new ActTimeEstimator();
		population.addAlgorithm(ade);

		System.out.println("-->reading plansfile: " + plansFilename);
		new MatsimPopulationReader(population, network).readFile(plansFilename);

		population.runAlgorithms();

		ade.write(outputFilename);

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}
}
