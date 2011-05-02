package herbie.running.analysis;

import java.io.*;
import java.util.*;

import org.matsim.core.controler.events.*;
import org.matsim.core.controler.listener.*;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;

import tutorial.programming.example06EventsHandling.MyEventHandler2;
import utils.Bins;


/**
 * 
 * @author bvitins, anhorni
 *
 */
public class ControlerListenerForStandardAnalysis implements StartupListener, IterationEndsListener, ShutdownListener {


	private StandardAnalysisEventHandler eventHandler; 
	private Map<Integer, Double> timePerIterationMap = new HashMap<Integer, Double>();
	private Map<Integer, Double> distancePerIterationMap = new HashMap<Integer, Double>();
	private String currentDir;
	
	
	public ControlerListenerForStandardAnalysis(String currentDir) {
		this.currentDir = currentDir;
	}


	@Override
	public void notifyStartup(StartupEvent event) {
		// after the controler is started create and add the event handler for events of the mobility simulation
		this.eventHandler = new StandardAnalysisEventHandler();
		event.getControler().getEvents().addHandler(this.eventHandler);
	}


	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		
		// for travelDistanceDistribution
		
		// for averageTravelTime
		this.timePerIterationMap.put(event.getIteration(), this.eventHandler.getAverageOverallTripDuration());
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		
		createChartForAverageTravelTime();
		
		createChartsForTravelTimeDistributionByMode();
	}


	private void createChartsForTravelTimeDistributionByMode() {
		TreeMap<String, Bins> travelTimeDistributionByMode = this.eventHandler.getTravelTimeDistributionByMode(10);
		
		for (String mode : travelTimeDistributionByMode.keySet()) {
			Bins bin = travelTimeDistributionByMode.get(mode);
			bin.plotBinnedDistribution(currentDir, "travel time", "min");
		}
	}


	private void createChartForAverageTravelTime() {
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
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < iters.length; i++) {
			buffer.append(iters[i]);
			buffer.append("\t");
			buffer.append(times[i]);
			buffer.append("\n");
		}
		//write the chart
		chart.addSeries("tt", iters, times);
		chart.saveAsPng(currentDir+"travelTimes.png", 800, 600);
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(currentDir+"/travelTimes.txt");
			writer.write(buffer.toString());
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}
