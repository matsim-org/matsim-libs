package tutorial;

import java.util.HashMap;
import java.util.Map;

import org.matsim.controler.events.IterationEndsEvent;
import org.matsim.controler.events.ShutdownEvent;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.IterationEndsListener;
import org.matsim.controler.listener.ShutdownListener;
import org.matsim.controler.listener.StartupListener;
import org.matsim.utils.charts.XYLineChart;

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
			iters[k] = k;
			times[k] = this.timePerIterationMap.get(k);
		}
		//write the chart
		chart.addSeries("tt", iters, times);
		chart.saveAsPng("./output/travelTimes.png", 800, 600);
	}



}
