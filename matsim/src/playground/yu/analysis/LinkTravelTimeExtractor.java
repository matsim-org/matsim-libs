/**
 *
 */
package playground.yu.analysis;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author yu
 * 
 */
public class LinkTravelTimeExtractor {
	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		int timeBin = 60;
		final String netFilename = "../psrc/network/psrc-wo-3212.xml.gz";
		final String plansFilename = "../runs/run668/it.2000/2000.plans.xml.gz";
		final String eventsFilename = "../runs/run668/it.2000/2000.analysis/6760.txt";
		final String outFilename = "../runs/run668/it.2000/2000.analysis/6760.travelTime.";

		Gbl.startMeasurement();

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		PopulationImpl population = new PopulationImpl();
		System.out.println("-->reading plansfile: " + plansFilename);
		new MatsimPopulationReader(population, network).readFile(plansFilename);

		EventsManagerImpl events = new EventsManagerImpl();

		Config config = new ScenarioImpl().getConfig();
		config.travelTimeCalculator().setTraveltimeBinSize(timeBin);
		
		TravelTimeCalculator ttc = new TravelTimeCalculator(network, config.travelTimeCalculator());
		events.addHandler(ttc);

		System.out.println("-->reading evetsfile: " + eventsFilename);
		new MatsimEventsReader(events).readFile(eventsFilename);

		BufferedWriter writer;
		double[] xs = new double[(24 * 3600 + 1) / timeBin];
		double[] ys = new double[(24 * 3600 + 1) / timeBin];
		int index;
		try {
			writer = IOUtils.getBufferedWriter(outFilename + timeBin + ".txt");
			writer
					.write("TimeBin\tLinkTravelTime\t[s]\tLinkTravelTime\t[m]\tLinkTravelTime\t[h]\n");
			for (int anI = 0; anI < 24 * 3600; anI = anI + timeBin) {
				index = (anI) / timeBin;
				ys[index] = ttc.getLinkTravelTime(network.getLink("6760"), anI);
				writer.write(anI + "\t" + ys[index] + "\t[s]\t" + ys[index]
						/ 60.0 + "\t[m]\t" + ys[index] / 3600.0 + "\t[h]\n");
				ys[index] /= 60.0;
				xs[index] = (anI) / 3600.0;
				writer.flush();
			}
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		XYLineChart travelTimeChart = new XYLineChart(
				"TravelTimes of Link 6760 of psrc network", "time [h]",
				"TravelTime [min]");
		travelTimeChart.addSeries("with timeBin " + timeBin / 60 + " min.", xs,
				ys);
		travelTimeChart.saveAsPng(outFilename + timeBin + ".png", 1024, 768);

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}

}
