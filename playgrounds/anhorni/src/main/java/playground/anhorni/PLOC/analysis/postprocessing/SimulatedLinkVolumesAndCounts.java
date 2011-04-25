package playground.anhorni.PLOC.analysis.postprocessing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.IOUtils;

public class SimulatedLinkVolumesAndCounts {
	
	private String path;
	private int numberOfAnalyses;
	private String outpath;
	private TreeMap<Id, EnsemblePerLink> ensembles = new TreeMap<Id, EnsemblePerLink>();
	
	public SimulatedLinkVolumesAndCounts(int numberOfAnalyses, String path, String outpath) {
		this.numberOfAnalyses = numberOfAnalyses;
		this.path = path;
		this.outpath = outpath;		
	}
	
	public void run() {
		this.readVolumes();
		this.write();
	}
		
	private void readVolumes() {
		for (int i = 0; i < this.numberOfAnalyses; i++) {
			String p = this.path + "/" + i + "/countspost/countscompare.txt";
			this.readCountsCompare(p);
		}
	}
	
	private void readCountsCompare(String p) {
		try {
	          BufferedReader bufferedReader = new BufferedReader(new FileReader(p));
	          String line = bufferedReader.readLine(); //skip header
	          while ((line = bufferedReader.readLine()) != null) {
	        	  String parts[] = line.split("\t");
	        	  Id id = new IdImpl(parts[0]);
	        	  int hour = Integer.parseInt(parts[1]) - 1;
	        	  double simVal = Double.parseDouble(parts[2]);
	        	  this.addSimVal(id, hour, simVal);
	          }	          
	        } // end try
		catch (IOException e) {
      	e.printStackTrace();
      }		
	}
	
	private void addSimVal(Id id, int hour, double volume) {
		if (this.ensembles.get(id) == null) this.ensembles.put(id, new EnsemblePerLink(id));
		this.ensembles.get(id).addVolume(hour, volume);
	}
	
	private void write() {
		try {
			new File(this.outpath).mkdirs();
			BufferedWriter out = IOUtils.getBufferedWriter(this.outpath + "/stdDevsScaled0-23.txt");
			String header = "Station\tLinkId";
			for (int hour = 0; hour < 24; hour++) {
				header += "\tHour" + hour;
			}
			out.write(header);
			out.newLine();
			for (EnsemblePerLink ensemble : this.ensembles.values()) {
				String line = "n.a.\t" + ensemble.getId();
				for (int hour = 0; hour < 24; hour++) {
					line += "\t" + 100.0 * ensemble.getStandardDev_population(hour) / ensemble.getAverageVolume(hour);
				}
				out.write(line); out.newLine();
			}
			out.flush();		
			out.close();
		} // end try
		catch (IOException e) {
      	e.printStackTrace();
      }		
	}
}
