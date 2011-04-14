package playground.anhorni.PLOC.analysis.postprocessing;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import org.apache.log4j.Logger;

import playground.anhorni.PLOC.analysis.Run;
import playground.anhorni.PLOC.analysis.RunsEnsemble;

public class LegHistoBox {
	
	private RunsEnsemble runsEnsemble;
	private final static Logger log = Logger.getLogger(LegHistoBox.class);
	
	public static void main(String[] args) {
		if (args.length < 2) {
			log.error("Please specify numberOfRuns and path");
			return;
		}
		LegHistoBox legHistoBoxCreator = new LegHistoBox();
		legHistoBoxCreator.run(Integer.parseInt(args[0]), args[1]);
	}
	
	public void run(int numberOfRuns, String pathStub) {
		this.runsEnsemble = new RunsEnsemble(0, pathStub);
		for (int i = 0; i < numberOfRuns; i++) {
			Run randomRun = new Run(i, 9);
			runsEnsemble.addRandomRun(randomRun);
			for (int day = 0; day < 5; day++) {
				String p = pathStub +  "/run" + i + "/day" + day +"/matsim/ITERS/it.100/" + "R" + i + "D" + day + "." + 100 + ".legHistogram.txt";
				this.readLegHistogramFile(p, day, randomRun);
			}
		}
		try {
			this.runsEnsemble.writeLegHistogramFile("arrivals");
			this.runsEnsemble.writeLegHistogramFile("departures");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void readLegHistogramFile(String file, int day, Run run) {
		try {
	          BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
	          bufferedReader.readLine(); //skip header
	          String line;
	          while ((line = bufferedReader.readLine()) != null) {
	        	  String parts[] = line.split("\t"); 
	        	  int time = Integer.parseInt(parts[1]);
	        	  double deps = Double.parseDouble(parts[2]);
	        	  run.addDepartures(day, time, deps);
	        	  double arrs = Double.parseDouble(parts[3]);
	        	  run.addArrivals(day, time, arrs);
	          }
	        } // end try
        catch (IOException e) {
        	e.printStackTrace();
        }	
	}
}
