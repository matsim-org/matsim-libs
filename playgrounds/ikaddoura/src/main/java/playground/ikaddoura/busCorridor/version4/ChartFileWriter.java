package playground.ikaddoura.busCorridor.version4;

import java.util.Map;
import org.apache.log4j.Logger;
import org.matsim.core.utils.charts.LineChart;

public class ChartFileWriter {
	private final static Logger log = Logger.getLogger(ChartFileWriter.class);

	public void writeLineChartFile(String outputExternalIterationDirPath, Map<Integer, Integer> iteration2numberOfBuses, Map<Integer, Integer> iteration2numberOfCarLegs, Map<Integer, Integer> iteration2numberOfPtLegs) {
		String title1 = "Leg Mode Analysis";

		String xAxesLabel = "Number of Buses";
		String yAxesLabel1 = "Legs";

		String[] numberOfBuses = new String[iteration2numberOfBuses.size()];
		int counter1 = 0;
		for (Integer busNumber : iteration2numberOfBuses.values()){
			numberOfBuses[counter1] = busNumber.toString();
			counter1++;
		}
	
		LineChart chart = new LineChart(title1, xAxesLabel, yAxesLabel1, numberOfBuses);
			double[] yWerteCar = new double[iteration2numberOfCarLegs.size()];
			double[] yWertePt = new double[iteration2numberOfPtLegs.size()];
			
			int counter2 = 0;
			for (Integer iteration : iteration2numberOfBuses.keySet()){
				numberOfBuses[counter2] = iteration.toString();
				yWerteCar[counter2] = iteration2numberOfCarLegs.get(iteration);
				yWertePt[counter2] = iteration2numberOfPtLegs.get(iteration);
				counter2++;
			}
			chart.addSeries("CarLegs", yWerteCar);
			chart.addSeries("PtLegs", yWertePt);

		String outputFile = outputExternalIterationDirPath+"/LegsPerMode.png";
		chart.saveAsPng(outputFile, 2000, 800); //File Export
		log.info("Legs written to "+outputFile);
	}
	
	public void writeLineChartScores(String outputExternalIterationDirPath, String name, Map<Integer, Integer> iteration2numberOfBuses, Map<Integer, Double> iteration2score) {
		String xAxesLabel = "Number of Buses";
		String yAxesLabel = name+"Score";
		
		String[] numberOfBuses = new String[iteration2numberOfBuses.size()];
		int counter1 = 0;
		for (Integer busNumber : iteration2numberOfBuses.values()){
			numberOfBuses[counter1] = busNumber.toString();
			counter1++;
		}
		
		LineChart chart = new LineChart(name+" Score Analysis", xAxesLabel, yAxesLabel);
	    
		double[] yWerte = new double[iteration2score.size()];
		int counter2 = 0;
		for (Integer iteration : iteration2numberOfBuses.keySet()){
			yWerte[counter2] = iteration2score.get(iteration);
			counter2++;
		}
		chart.addSeries(name, yWerte);
		
		String outputFile = outputExternalIterationDirPath+"/"+name+"Scores.png";
		chart.saveAsPng(outputFile, 1000, 800); //File Export
		log.info(name+"Scores written to "+outputFile);
	}
}
