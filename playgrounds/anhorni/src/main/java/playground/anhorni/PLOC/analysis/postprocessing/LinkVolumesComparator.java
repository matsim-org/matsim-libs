package playground.anhorni.PLOC.analysis.postprocessing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.IOUtils;

public class LinkVolumesComparator {
	
	private String path;
	private int numberOfAnalyses;
	private String outpath;
	private Network network;
	private TreeMap<Id, EnsemblePerLink> ensembles = new TreeMap<Id, EnsemblePerLink>();
	
	private final static Logger log = Logger.getLogger(LinkVolumesComparator.class);
	
	public LinkVolumesComparator(int numberOfAnalyses, String path, String outpath, Network network) {
		this.numberOfAnalyses = numberOfAnalyses;
		this.path = path;
		this.outpath = outpath;	
		this.network = network;
	}
	
	public void run() {
		this.readVolumes("inter");
		this.write("stdDevInter");
		
		this.ensembles.clear();

		this.readVolumes("interAWTV");
		this.write("stdDevInterAWTV");
		
		this.ensembles.clear();
		
		this.readVolumes("intra");
		this.write("stdDevIntra");
		
		this.ensembles.clear();
		
		this.readVolumes("intra");
		this.write("stdDevIntraAWTV");	
	}
		
	private void readVolumes(String type) {
		for (int i = 0; i < this.numberOfAnalyses; i++) {
			String p ="";
			if (type.equals("inter")) {
				p = this.path + "/" + i + "/zh10PctEps.200.countscompare.txt";
				this.readCountsCompare(p);
			}
			else if (type.equals("interAWTV")) {
				p = this.path + "/" + i + "/zh10PctEps.200.countscompareAWTV.txt";
				this.readAWTV(p);
			}
			else if (type.equals("intra")) {
				int cnt = 191 + i;
				if (cnt >= 200) return;
				p = this.path + "/intrarun/zh10PctEps." + cnt + ".countscompare.txt";
				this.readCountsCompare(p);
			}
			else if (type.equals("intraAWTV")) {
				int cnt = 191 + i;
				if (cnt >= 200) return;
				p = this.path + "/intrarun/zh10PctEps." + cnt + ".countscompareAWTV.txt";
				this.readCountsCompare(p);
			}
			log.info("reading " + p);
		}
	}
	
	private void readAWTV(String p) {
		try {
	          BufferedReader bufferedReader = new BufferedReader(new FileReader(p));
	          String line = bufferedReader.readLine(); //skip header
	          while ((line = bufferedReader.readLine()) != null) {
	        	  String parts[] = line.split("\t");
	        	  Id id = new IdImpl(parts[0]);
	        	  double simVal = Double.parseDouble(parts[1]);
	        	  for (int i = 0; i < 24; i++) {
	        		  this.addSimVal(id, i, simVal);
	        	  }
	          }	          
	        } // end try
		catch (IOException e) {
      	e.printStackTrace();
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
	
	private void write(String name) {
		try {
			new File(this.outpath).mkdirs();
			BufferedWriter out = IOUtils.getBufferedWriter(this.outpath + "/" + name + ".txt");
			log.info("files written to: " + this.outpath);
			String header = "Station\tLinkId";
			for (int hour = 0; hour < 24; hour++) {
				header += "\tHour" + hour;
			}
			out.write(header);
			out.newLine();
			List<LinkWInfo> links = new Vector<LinkWInfo>();
			for (EnsemblePerLink ensemble : this.ensembles.values()) {
				Coord coord = this.network.getLinks().get(ensemble.getId()).getCoord();
				LinkWInfo link = new LinkWInfo(ensemble.getId(), coord);
				links.add(link);
				String line = "n.a.\t" + ensemble.getId();
				for (int hour = 0; hour < 24; hour++) {
					double stdDevPct = 100.0 * ensemble.getStandardDev_s(hour) / ensemble.getAverageVolume(hour);
					line += "\t" + stdDevPct;
					link.setStdDev(hour, stdDevPct);
					link.setAvgVolume(hour, ensemble.getAverageVolume(hour));
				}
				out.write(line); out.newLine();
			}
			out.flush();		
			out.close();
			
			LinkVolumesShapeFileWriter shapeFileWriter = new LinkVolumesShapeFileWriter();
			new File(this.outpath + "/" + name).mkdirs();
			shapeFileWriter.writeLinkVolumesAtCountStations(this.outpath + "/" + name, links, 7);
			shapeFileWriter.writeLinkVolumesAtCountStations(this.outpath + "/" + name, links, 11);
			shapeFileWriter.writeLinkVolumesAtCountStations(this.outpath + "/" + name, links, 17);
			
		} // end try
		catch (IOException e) {
      	e.printStackTrace();
      }		
	}
}
