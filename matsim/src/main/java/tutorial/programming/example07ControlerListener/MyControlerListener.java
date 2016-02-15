package tutorial.programming.example07ControlerListener;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;


/**
 * This class collects the average travel time per agent measured by the
 * MyEventHandler instance after each iteration. When the Controler is shut down
 * the travel times are written to a file, i.e. a JFreeChart plot.
 * @author dgrether
 *
 */
public class MyControlerListener implements StartupListener, IterationEndsListener, ShutdownListener {

	
	@Inject
	private MyEventHandler eventHandler; 
	
	@Inject
	private EventsManager events ;

	private Map<Integer, Double> timePerIterationMap = new HashMap<Integer, Double>();


	@Override
	public void notifyStartup(StartupEvent event) {
		// after the controler is started create and add the event handler for events of the mobility simulation
		this.events.addHandler(this.eventHandler);
	}


	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		System.out.println("Average travel time in iteration " + event.getIteration() + " is: "
				+ this.eventHandler.getAverageTravelTime());
		this.timePerIterationMap.put(event.getIteration(), this.eventHandler.getAverageTravelTime());
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		//create the chart with the appropriate helper class of org.matsim.utils.charts
		XYLineChart chart = new XYLineChart("Average travel times per iteration",
				"Iterations", "ttimes");
		chart.addMatsimLogo();
		//create the arrays needed for chart creation
		double[] iters = new double[this.timePerIterationMap.size()];
		double[] times = new double[this.timePerIterationMap.size()];
		int decrement = 1;
		if (this.timePerIterationMap.containsKey(Integer.valueOf(0))) {
			decrement = 0;
		}
		//unfortunately we have to do this as...
		for (Integer k : this.timePerIterationMap.keySet()) {
			iters[k - decrement] = k;
			times[k - decrement] = this.timePerIterationMap.get(k);
		}
		//write to file doing this not in the loop above is cause we
		//would like to have a sorted output.
		StringBuilder buffer = new StringBuilder();
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
		} catch (IOException e) {
			e.printStackTrace();
		}
	}



}
