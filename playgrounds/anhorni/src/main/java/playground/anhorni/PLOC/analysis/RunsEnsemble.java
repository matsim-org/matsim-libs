package playground.anhorni.PLOC.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Vector;

import org.apache.log4j.Logger;
import playground.anhorni.PLOC.MultiplerunsControler;

public class RunsEnsemble {
	
	private int id = -1;
	private String outpath;
	
	private Vector<Run> randomRuns = new Vector<Run>();
	private final static Logger log = Logger.getLogger(RunsEnsemble.class);
	
	public RunsEnsemble(int id, String outpath) {
		this.id = id;
		this.outpath = outpath;
	}
	
	public void addRandomRun(Run randomRun) {
		this.randomRuns.add(randomRun);
	}
	
	private double getAvgRuns_PerLocationPerDayPerHour(int locIndex, int day, int hour) {
		double mean = 0.0;   
    	for (int runIndex = 0; runIndex < randomRuns.size(); runIndex++) {
			mean += this.randomRuns.get(runIndex).getTotalExpenditure(locIndex, day, hour);
        }
    	return mean /= randomRuns.size();	
	}
	
	public double getAvgRunsDays_PerLocationPerHour(int locIndex, int hour) {
		double mean = 0.0;
		for (int day = 0; day < 5; day++) {   
				mean += getAvgRuns_PerLocationPerDayPerHour(locIndex, day, hour);
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
		DecimalFormat formatter = new DecimalFormat("0.00");
		
		for (int facIndex = 0; facIndex < MultiplerunsControler.shoppingFacilities.length; facIndex++) {
			String outputFolder = this.outpath + "/facility" + MultiplerunsControler.shoppingFacilities[facIndex];
			new File(outputFolder).mkdirs();
			RunsEnsembleBoxPlot boxPlot = new RunsEnsembleBoxPlot("Facility " + MultiplerunsControler.shoppingFacilities[facIndex] + ": " + this.randomRuns.size() + " Runs");				
			
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(this.outpath + "/facility" + MultiplerunsControler.shoppingFacilities[facIndex] + "/SingleEnsemble_facility" + 
					MultiplerunsControler.shoppingFacilities[facIndex] + ".txt")); 
			bufferedWriter.write("Hour\t");
			for (int i = 0; i < this.randomRuns.size(); i++) {
				bufferedWriter.write("run" + i + "\t");
			}			
			bufferedWriter.newLine();
			for (int hour = 0; hour < 24; hour++) {
				ArrayList<Double> expenditures = new ArrayList<Double>();
				int runIndex = 0;
				bufferedWriter.write(hour + "\t");
				for (Run run : this.randomRuns) {
					expenditures.add(run.getAvgDays_ExpendituresPerHourPerFacility(facIndex, hour));
					runIndex++;
					bufferedWriter.write(formatter.format(run.getAvgDays_ExpendituresPerHourPerFacility(facIndex, hour)) + "\t");
				}
				boxPlot.addHourlySeries(expenditures, hour);
				bufferedWriter.newLine();
			}
			
			boxPlot.createChart();
			boxPlot.saveAsPng(this.outpath + "/facility" + MultiplerunsControler.shoppingFacilities[facIndex] + "/SingleEnsemble_facility" + 
					MultiplerunsControler.shoppingFacilities[facIndex] + ".png", 1000, 500);
			
			bufferedWriter.flush();
			bufferedWriter.close();
		}
	}
		
	public void writeLegHistogramFile() throws IOException {
		DecimalFormat formatter = new DecimalFormat("0.00");
			new File(outpath).mkdirs();			
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(this.outpath +  "/legHistogram.txt")); 
			bufferedWriter.write("Run.Day \\ Hour\t");
			
			for (int hour = 0; hour < 24; hour++) {
				bufferedWriter.write("h" + hour + "\t");
			}			
			bufferedWriter.newLine();
			
			for (int day = 0; day < 5; day++) {
				for (Run run : this.randomRuns) {
					bufferedWriter.write("R" + run.getId() + "D" + day + "\t");
					for (int hour = 0; hour < 24; hour++) {
						bufferedWriter.write(formatter.format(run.getArrivals()[day][hour]) + "\t");
					}
					bufferedWriter.newLine();
				}
			}
			bufferedWriter.flush();
			bufferedWriter.close();
			log.info("legHistogram file written");
		}
}
