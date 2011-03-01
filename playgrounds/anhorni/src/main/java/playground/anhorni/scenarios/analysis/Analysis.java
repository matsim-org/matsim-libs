package playground.anhorni.scenarios.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Vector;

import org.matsim.core.utils.charts.XYScatterChart;

public class Analysis {
	
	private int id = -1;
	private String outpath;
	private int numberOfLocations = -1;
	
	private Vector<RandomRun> randomRuns = new Vector<RandomRun>();
	
	public Analysis(int id, String outpath, int numberOfLocations) {
		this.id = id;
		this.numberOfLocations = numberOfLocations;
		this.outpath = outpath;
	}
	
	public double getMean(int locIndex, int numberOfRandomRunsUsedForAveraging) {
		double mean = 0.0;
		   
    	for (int j = 0; j < numberOfRandomRunsUsedForAveraging; j++) {
			mean += this.randomRuns.get(j).getExpenditure(locIndex);
        }
    	return mean /= numberOfRandomRunsUsedForAveraging;	
	}	
	
	public void addRandomRun(RandomRun randomRun) {
		this.randomRuns.add(randomRun);
	}

	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	
	public double[] computeSingleLocationAnalysis(int locIndex) {   	
    	double[] meanAll = new double[this.randomRuns.size()];  
    	for (int j = 0; j < this.randomRuns.size(); j++) {
    		double mean = 0;
    		for (int k = 0; k <= j; k++) {
				mean += this.getMean(locIndex, k + 1);
        	}
    		meanAll[j] = mean / (j + 1);
    	}
    	return meanAll;
    }
	
	public double[] computeSingleLocationAnalysisPercentage(int locIndex, double refValue) {   	
    	double[] meanAllPercentage = new double[this.randomRuns.size()];  
    	
    	double mean_n = 0;
		for (int k = 0; k < this.randomRuns.size(); k++) {
			mean_n += this.getMean(locIndex, k + 1);
    	}
		mean_n /= this.randomRuns.size();
    	
    	for (int j = 0; j < this.randomRuns.size(); j++) {
    		double mean = 0;
    		for (int k = 0; k <= j; k++) {
				mean += this.getMean(locIndex, k + 1);
        	}
    		if (refValue > 0.0) {
    			meanAllPercentage[j] = ((mean / (j + 1)) - refValue) / refValue * 100.0;
    		}
    		else {	
    			meanAllPercentage[j] = ((mean / (j + 1)) - mean_n) / mean_n * 100.0;
    		}
    	}
    	return meanAllPercentage;
    }
	
	public void write() {
		DecimalFormat formatter = new DecimalFormat("0.00");
		BufferedWriter bufferedWriter = null;
		
		double x[] = new double[this.randomRuns.size()];
		for (int i = 0; i < this.randomRuns.size(); i++) {
			x[i] = i + 1;
		}
		
		for (int locIndex = 0; locIndex < this.numberOfLocations; locIndex++) {		
			try { 
				double[] singleLocMeanExpenditures = computeSingleLocationAnalysis(locIndex);
				
				XYScatterChart chart = new XYScatterChart("loc_" + locIndex + "_analysis_" + this.id, "Number of sample runs used to build average", "Mean");
				chart.addSeries("", x, singleLocMeanExpenditures);
				
				String dir = this.outpath + "/output/PLOC/3towns/random/" + "loc_" + locIndex + "/";
				new File(dir).mkdir();
				
				chart.saveAsPng(dir + "analysis_" + this.id + ".png" , 1000, 500);
				
			    bufferedWriter = new BufferedWriter(new FileWriter(dir + "analysis_" + this.id + ".txt"));           
	
				for (int i = 0; i < singleLocMeanExpenditures.length; i++) {
						bufferedWriter.append("Number of sample runs used to build average: " + (i + 1)  + "\t " + String.valueOf(formatter.format(singleLocMeanExpenditures[i])));
						bufferedWriter.newLine();
				}
				
			bufferedWriter.flush();
	        bufferedWriter.close();
			} catch (IOException ex) {
			    ex.printStackTrace();
			}
		}
    }	
}
