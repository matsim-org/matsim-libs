package playground.ikaddoura.busCorridorPaper.busCorridorWelfareAnalysis;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.utils.charts.LineChart;

public class ChartFileWriter {
	private final static Logger log = Logger.getLogger(ChartFileWriter.class);
	private String outputExternalIterationDirPath;
	
	public ChartFileWriter(String outputExternalIterationDirPath) {
		this.outputExternalIterationDirPath = outputExternalIterationDirPath;
	}

	public void writeMapToChart(Map<Integer, Double> key2value, String title, String x, String y, String fileName) {
		
		String[] xValues = new String[key2value.size()];
		int counter1 = 0;
		for (Integer xValue : key2value.keySet()){
			xValues[counter1] = xValue.toString();
			counter1++;
		}
		
		LineChart chart = new LineChart(title, x, y, xValues);
	    
		double[] yWerte = new double[key2value.size()];
		int counter2 = 0;
		for (Integer iteration : key2value.keySet()){
			yWerte[counter2] = key2value.get(iteration);
			counter2++;
		}
		chart.addSeries(y, yWerte);
		
		String outputFile = this.outputExternalIterationDirPath+"/"+fileName;
		chart.saveAsPng(outputFile, 1000, 800); //File Export
		log.info(y +" written to "+ outputFile);
	}
	
	public void writeChart_LegModes(Map<Integer, Integer> iteration2numberOfCarLegs, Map<Integer, Integer> iteration2numberOfPtLegs) {

		String[] xValues = new String[iteration2numberOfCarLegs.size()];
		int counter1 = 0;
		for (Integer xValue : iteration2numberOfCarLegs.keySet()){
			xValues[counter1] = xValue.toString();
			counter1++;
		}
	
		LineChart chart = new LineChart("Leg Mode Analysis", "Iteration", "Legs", xValues);
			double[] yWerteCar = new double[iteration2numberOfCarLegs.size()];
			double[] yWertePt = new double[iteration2numberOfPtLegs.size()];
			
			int counter2 = 0;
			for (Integer iteration : iteration2numberOfCarLegs.keySet()){
				xValues[counter2] = iteration.toString();
				yWerteCar[counter2] = iteration2numberOfCarLegs.get(iteration);
				yWertePt[counter2] = iteration2numberOfPtLegs.get(iteration);
				counter2++;
			}
			chart.addSeries("CarLegs", yWerteCar);
			chart.addSeries("PtLegs", yWertePt);

		String outputFile = this.outputExternalIterationDirPath+"/LegMode.png";
		chart.saveAsPng(outputFile, 2000, 800); //File Export
		log.info("Legs written to "+outputFile);
	}
	
	public void writeChart_OperatorScores(Map<Integer, Double> iteration2score, Map<Integer, Double> iteration2operatorCosts, Map<Integer, Double> iteration2operatorRevenue) {
		
		String[] xValues = new String[iteration2score.size()];
		int counter1 = 0;
		for (Integer xValue : iteration2score.keySet()){
			xValues[counter1] = xValue.toString();
			counter1++;
		}
		
		LineChart chart = new LineChart("Operator score per iteration", "Iteration", "AUD", xValues);
	    
		double[] yWerte1 = new double[iteration2score.size()];
		int counter2 = 0;
		for (Integer iteration : iteration2score.keySet()){
			yWerte1[counter2] = iteration2score.get(iteration);
			counter2++;
		}
		double[] yWerte2 = new double[iteration2operatorCosts.size()];
		int counter3 = 0;
		for (Integer iteration : iteration2operatorCosts.keySet()){
			yWerte2[counter3] = iteration2operatorCosts.get(iteration);
			counter3++;
		}
		double[] yWerte3 = new double[iteration2operatorRevenue.size()];
		int counter4 = 0;
		for (Integer iteration : iteration2operatorRevenue.keySet()){
			yWerte3[counter4] = iteration2operatorRevenue.get(iteration);
			counter4++;
		}
		chart.addSeries("Profit", yWerte1);
		chart.addSeries("Costs", yWerte2);
		chart.addSeries("Revenue", yWerte3);
		
		String outputFile = this.outputExternalIterationDirPath+"/OperatorScore.png";
		chart.saveAsPng(outputFile, 1000, 800); //File Export
		log.info("OperatorScores written to "+outputFile);
	}

	public void write(SortedMap<Integer, ExtItInformation> extIt2information) {
		
		SortedMap<Integer, Double> iteration2operatorProfit = new TreeMap<Integer, Double>();
		SortedMap<Integer, Double> iteration2operatorCosts = new TreeMap<Integer, Double>();
		SortedMap<Integer, Double> iteration2operatorRevenue = new TreeMap<Integer, Double>();
		SortedMap<Integer, Double> iteration2numberOfBuses = new TreeMap<Integer, Double>();
		SortedMap<Integer, Double> iteration2userScoreSum = new TreeMap<Integer,Double>();
		SortedMap<Integer, Double> iteration2totalScore = new TreeMap<Integer,Double>();
		SortedMap<Integer, Integer> iteration2numberOfCarLegs = new TreeMap<Integer, Integer>();
		SortedMap<Integer, Integer> iteration2numberOfPtLegs = new TreeMap<Integer, Integer>();
		SortedMap<Integer, Integer> iteration2numberOfWalkLegs = new TreeMap<Integer, Integer>();
		SortedMap<Integer, Double> iteration2fare = new TreeMap<Integer, Double>();
		SortedMap<Integer, Double> iteration2capacity = new TreeMap<Integer, Double>();
		SortedMap<Integer, Double> iteration2waitTimeSum = new TreeMap<Integer, Double>();

		 for (Integer iteration : extIt2information.keySet()){
			 iteration2numberOfBuses.put(iteration, extIt2information.get(iteration).getNumberOfBuses());
			 iteration2operatorCosts.put(iteration, extIt2information.get(iteration).getOperatorCosts());
			 iteration2operatorRevenue.put(iteration, extIt2information.get(iteration).getOperatorRevenue());
			 iteration2operatorProfit.put(iteration, extIt2information.get(iteration).getOperatorProfit());
			 iteration2userScoreSum.put(iteration, extIt2information.get(iteration).getUsersLogSum());
			 iteration2totalScore.put(iteration, extIt2information.get(iteration).getWelfare());
			 iteration2numberOfCarLegs.put(iteration, (int)extIt2information.get(iteration).getNumberOfCarLegs());
			 iteration2numberOfPtLegs.put(iteration, (int)extIt2information.get(iteration).getNumberOfPtLegs());
			 iteration2numberOfWalkLegs.put(iteration, (int)extIt2information.get(iteration).getNumberOfWalkLegs());
			 iteration2fare.put(iteration, extIt2information.get(iteration).getFare());
			 iteration2capacity.put(iteration, extIt2information.get(iteration).getCapacity());
			 iteration2waitTimeSum.put(iteration, extIt2information.get(iteration).getSumOfWaitingTimes());
		 }
		
		// Map<Integer, Double> key2value, String title, String x, String y, String fileName 
		writeMapToChart(iteration2numberOfBuses, "Number of buses per iteration", "Iteration", "Number of Buses", "Parameters_NumberOfBuses.png");
		writeMapToChart(iteration2capacity, "Vehicle capacity per iteration", "Iteration", "Capacity (Persons/Vehicle)", "Parameters_Capacity.png");
		writeMapToChart(iteration2fare, "Fare per iteration", "Iteration", "Fare (AUD)", "Parameters_Fare.png");

		writeMapToChart(iteration2userScoreSum, "All Users Logsum per iteration", "Iteration", "All Users Logsum (AUD)", "AllUsersLogsum.png");
		writeMapToChart(iteration2totalScore, "Welfare per iteration", "Iteration", "Welfare (AUD)", "Welfare.png");

		writeChart_LegModes(iteration2numberOfCarLegs, iteration2numberOfPtLegs);
		writeChart_OperatorScores(iteration2operatorProfit, iteration2operatorCosts, iteration2operatorRevenue);
	}
}
