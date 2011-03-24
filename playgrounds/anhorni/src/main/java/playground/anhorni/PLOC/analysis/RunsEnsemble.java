package playground.anhorni.PLOC.analysis;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

public class RunsEnsemble {
	
	private int id = -1;
	private String outpath;
	private int numberOfLocations = -1;
	private int shoppingFacilities[] = {1, 2, 5, 6, 7};
	
	private Vector<Run> randomRuns = new Vector<Run>();
	
	public RunsEnsemble(int id, String outpath, int numberOfLocations) {
		this.id = id;
		this.numberOfLocations = numberOfLocations;
		this.outpath = outpath;
	}
	
	public void addRandomRun(Run randomRun) {
		this.randomRuns.add(randomRun);
	}
	
	private double getMean(int locIndex, int day, int hour) {
		double mean = 0.0;   
    	for (int runIndex = 0; runIndex < randomRuns.size(); runIndex++) {
			mean += this.randomRuns.get(runIndex).getTotalExpenditure(locIndex, day, hour);
        }
    	return mean /= randomRuns.size();	
	}
	
	public double getMean(int locIndex, int hour) {
		double mean = 0.0;
		for (int day = 0; day < 5; day++) {   
				mean += getMean(locIndex, day, hour);
		}
    	return mean /= 5.0;	
	}
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	
	public void write() throws IOException {			
		for (int facIndex = 0; facIndex < this.numberOfLocations; facIndex++) {
			RunsEnsembleBoxPlot boxPlot = new RunsEnsembleBoxPlot("Facility " + shoppingFacilities[facIndex] + " " + this.randomRuns.size() + " Runs");				
			for (int hour = 0; hour < 24; hour++) {
				ArrayList<Double> expenditures = new ArrayList<Double>();
				int runIndex = 0;
				for (Run run : this.randomRuns) {
					expenditures.add(run.getAvgDays_ExpendituresPerHourPerFacility(facIndex, hour));
					runIndex++;
				}
				boxPlot.addHourlySeries(expenditures, hour);
			}
			String outputFolder = this.outpath + "/facility" + shoppingFacilities[facIndex];
			new File(outputFolder).mkdirs();
			boxPlot.createChart();
			boxPlot.saveAsPng(this.outpath + "/facility" + shoppingFacilities[facIndex] + "/SingleEnsemble_facility" + shoppingFacilities[facIndex] + ".png", 1000, 500);
		}
    }
}
