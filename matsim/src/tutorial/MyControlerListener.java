package tutorial;

import java.util.HashMap;
import java.util.Map;

import org.matsim.controler.events.IterationEndsEvent;
import org.matsim.controler.events.ShutdownEvent;
import org.matsim.controler.listener.IterationEndsListener;
import org.matsim.controler.listener.ShutdownListener;
import org.matsim.utils.charts.XYLineChart;

/**
 * This class collects the average travel time per agent measured by the
 * MyEventHandler instance after each iteration. When the Controler is shut down
 * the travel times are written to a file, i.e. a JFreeChart plot.
 * @author dgrether
 *
 */
public class MyControlerListener implements IterationEndsListener, ShutdownListener {

	private MyEventHandler eventHandler;

	private Map<Integer, Double> timePerIterationMap = new HashMap<Integer, Double>();


	public MyControlerListener(MyEventHandler eventHandler) {
		this.eventHandler = eventHandler;
	}

	public void notifyIterationEnds(IterationEndsEvent event) {
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
		int i = 0;
		for (Integer k : this.timePerIterationMap.keySet()) {
			iters[i] = i;
			times[i] = this.timePerIterationMap.get(k);
			i++;
		}
		//write the chart
		chart.addSeries("tt", iters, times);
		chart.saveAsPng("./output/travelTimes.png", 800, 600);
	}



}
