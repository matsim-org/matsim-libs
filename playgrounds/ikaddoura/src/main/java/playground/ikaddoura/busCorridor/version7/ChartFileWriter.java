package playground.ikaddoura.busCorridor.version7;

import java.util.Map;
import org.apache.log4j.Logger;
import org.matsim.core.utils.charts.LineChart;

public class ChartFileWriter {
	private final static Logger log = Logger.getLogger(ChartFileWriter.class);

	public void writeChart_LegModes(String outputExternalIterationDirPath, String xAxesLabel, Map<Integer, Double> iteration2xValue, Map<Integer, Integer> iteration2numberOfCarLegs, Map<Integer, Integer> iteration2numberOfPtLegs) {
		String title1 = "Leg Mode Analysis";
		String yAxesLabel1 = "Legs";

		String[] xValues = new String[iteration2xValue.size()];
		int counter1 = 0;
		for (Double xValue : iteration2xValue.values()){
			xValues[counter1] = xValue.toString();
			counter1++;
		}
	
		LineChart chart = new LineChart(title1, xAxesLabel, yAxesLabel1, xValues);
			double[] yWerteCar = new double[iteration2numberOfCarLegs.size()];
			double[] yWertePt = new double[iteration2numberOfPtLegs.size()];
			
			int counter2 = 0;
			for (Integer iteration : iteration2xValue.keySet()){
				xValues[counter2] = iteration.toString();
				yWerteCar[counter2] = iteration2numberOfCarLegs.get(iteration);
				yWertePt[counter2] = iteration2numberOfPtLegs.get(iteration);
				counter2++;
			}
			chart.addSeries("CarLegs", yWerteCar);
			chart.addSeries("PtLegs", yWertePt);

		String outputFile = outputExternalIterationDirPath+"/LegMode_"+xAxesLabel+".png";
		chart.saveAsPng(outputFile, 2000, 800); //File Export
		log.info("Legs written to "+outputFile);
	}
	
	public void writeChart_UserScores(String outputExternalIterationDirPath, String xAxesLabel, Map<Integer, Double> iteration2xValue, Map<Integer, Double> iteration2score) {
		String yAxesLabel = "User Score (avg. executed)";
		
		String[] xValues = new String[iteration2xValue.size()];
		int counter1 = 0;
		for (Double xValue : iteration2xValue.values()){
			xValues[counter1] = xValue.toString();
			counter1++;
		}
		
		LineChart chart = new LineChart("User score per iteration", xAxesLabel, yAxesLabel);
	    
		double[] yWerte = new double[iteration2score.size()];
		int counter2 = 0;
		for (Integer iteration : iteration2xValue.keySet()){
			yWerte[counter2] = iteration2score.get(iteration);
			counter2++;
		}
		chart.addSeries("User Score", yWerte);
		
		String outputFile = outputExternalIterationDirPath+"/UserScore_"+xAxesLabel+".png";
		chart.saveAsPng(outputFile, 1000, 800); //File Export
		log.info("UserScores written to "+outputFile);
	}
	
	public void writeChart_OperatorScores(String outputExternalIterationDirPath, String xAxesLabel, Map<Integer, Double> iteration2xValue, Map<Integer, Double> iteration2score, Map<Integer, Double> iteration2operatorCosts, Map<Integer, Double> iteration2operatorEarnings) {
		String yAxesLabel = "Euro";
		
		String[] xValues = new String[iteration2xValue.size()];
		int counter1 = 0;
		for (Double xValue : iteration2xValue.values()){
			xValues[counter1] = xValue.toString();
			counter1++;
		}
		
		LineChart chart = new LineChart("Operator score per iteration", xAxesLabel, yAxesLabel);
	    
		double[] yWerte1 = new double[iteration2score.size()];
		int counter2 = 0;
		for (Integer iteration : iteration2xValue.keySet()){
			yWerte1[counter2] = iteration2score.get(iteration);
			counter2++;
		}
		double[] yWerte2 = new double[iteration2operatorCosts.size()];
		int counter3 = 0;
		for (Integer iteration : iteration2xValue.keySet()){
			yWerte2[counter3] = iteration2operatorCosts.get(iteration);
			counter3++;
		}
		double[] yWerte3 = new double[iteration2operatorEarnings.size()];
		int counter4 = 0;
		for (Integer iteration : iteration2xValue.keySet()){
			yWerte3[counter4] = iteration2operatorEarnings.get(iteration);
			counter4++;
		}
		chart.addSeries("Profit", yWerte1);
		chart.addSeries("Costs", yWerte2);
		chart.addSeries("Earnings", yWerte3);
		
		String outputFile = outputExternalIterationDirPath+"/OperatorScore_"+xAxesLabel+".png";
		chart.saveAsPng(outputFile, 1000, 800); //File Export
		log.info("OperatorScores written to "+outputFile);
	}
}
