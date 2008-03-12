package tutorial;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.controler.events.IterationEndsEvent;
import org.matsim.controler.events.ShutdownEvent;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.IterationEndsListener;
import org.matsim.controler.listener.ShutdownListener;
import org.matsim.controler.listener.StartupListener;
import org.matsim.utils.charts.XYLineChart;
import org.matsim.utils.io.IOUtils;

/**
 * This class collects the average travel time per agent measured by the
 * MyEventHandler instance after each iteration. When the Controler is shut down
 * the travel times are written to a file, i.e. a JFreeChart plot.
 * @author dgrether
 *
 */
public class MyControlerListener implements StartupListener, IterationEndsListener, ShutdownListener {

	private MyEventHandler eventHandler;

	private Map<Integer, Double> timePerIterationMap = new HashMap<Integer, Double>();


	public void notifyStartup(StartupEvent event) {
		// after the controler is started create and add the event handler for events of the mobility simulation
		this.eventHandler = new MyEventHandler(event.getControler().getPopulation().getPersons().size());
		event.getControler().getEvents().addHandler(this.eventHandler);
	}


	public void notifyIterationEnds(IterationEndsEvent event) {
		System.out.println("Average travel time in iteration " + event.getIteration() + " is: "
				+ this.eventHandler.getAverageTravelTime());
		this.timePerIterationMap.put(event.getIteration(), this.eventHandler.getAverageTravelTime());
	}

	public void notifyShutdown(ShutdownEvent event) {
		//create the chart with the appropriate helper class of org.matsim.utils.charts
		XYLineChart chart = new XYLineChart("Average travel times per iteration",
				"Iterations", "ttimes");
		chart.addMatsimLogo();
		//create the arrays needed for chart creation
		double[] iters = new double[this.timePerIterationMap.size()];
		double[] times = new double[this.timePerIterationMap.size()];
		//unfortunately we have to do this as...
		for (Integer k : this.timePerIterationMap.keySet()) {
			iters[k-1] = k;
			times[k-1] = this.timePerIterationMap.get(k);
		}
		//write to file doing this not in the loop above is cause we
		//would like to have a sorted output.
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < iters.length; i++) {
			buffer.append(iters[i]);
			buffer.append("\t");
			buffer.append(times[i]);
			buffer.append("\n");
		}
		//write the chart
		chart.addSeries("tt", iters, times);
		chart.saveAsPng("./output/travelTimes.png", 800, 600);
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter("./output/travelTimes.txt");
			writer.write(buffer.toString());
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}



}
