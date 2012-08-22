package playground.acmarmol.Avignon;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.StringUtils;

public class AvignonWriter {

	private String outputDirectory = null;
	private HashMap<Id, String> info;
	private HashMap<Id, String> distanceInfo;
	private BufferedWriter out; 
	final  Logger log = Logger.getLogger(AvignonWriter.class);
	
	public AvignonWriter(LinkedHashMap<Id, String> plansInfo, LinkedHashMap<Id, String> distanceInfo ){
		
		this.info = plansInfo;
		this.distanceInfo = distanceInfo;
	}
	
	public void startWriting() throws IOException{
		log.info("Writing Started");	
		
		this.out.write(String.format("%-8s %-25s %-25s %-25s %-25s %-25s %-25s %-25s %-25s %-25s %-25s %-25s %-25s %-25s %-25s %-25s %-25s %-25s %-25s %-25s %-25s %-25s %-25s %-25s %-25s %-25s %-25s %-25s %-25s %-25s %-25s %-25s",
				"ID;", "Score;", "Dist. Car;", "Dist. Ride;", "Dist. Bike;", "Dist. PT;", "Dist. Walk;", "Dist. TWalk;","HCoord-X;", "HCoord-Y;", "WCoord-X;",  "WCoord-Y;", "ECoord-X;", "ECoord-Y;", "S1Coord-X;", "S1Coord-Y;", "S2Coord-X;", "S2Coord-Y;",
				"S3Coord-X;", "S3Coord-Y;","L1-CoordX;","L1-CoordY;", "L2-CoordX;","L2-CoordY;","L3-CoordX;","L3-CoordY;","L4-CoordX;","L4-CoordY;","DistToW;","DistToS;","DistToL;","DistToE;"));
		this.out.newLine();
		
	}
	
	
	@SuppressWarnings("rawtypes")
	public void write() throws IOException{
		
		if(this.outputDirectory == null){
			log.error("No output Directory setted for Writer: use setOutputDirectory method", new Error());
		}
		
		  startWriting();  
		
		  Iterator<Entry<Id, String>> it = info.entrySet().iterator();
		    while (it.hasNext()) {
		       		    	
		    	Map.Entry pairs = (Map.Entry)it.next();
		    	String allInfo = (String) pairs.getValue();
		    	String distances = (String) distanceInfo.get(pairs.getKey());
		        String[] parts = StringUtils.explode(allInfo.concat(" , " + distances), ',') ;
		        
		        this.out.write(String.format("%-8s", pairs.getKey()));
		      	for(String part: parts){        
		        this.out.write(String.format("%-26s", part));
		      	}	
		        this.out.newLine();
		        it.remove(); // avoids a ConcurrentModificationException
		    }
		
		    finishWriting();
	}
		
	public void finishWriting() throws IOException{
		this.out.close();
		log.info("Writing Finished");
	}

	public void setOutputDirectory(String outputDirectory) {
		this.outputDirectory = outputDirectory;
		this.out = IOUtils.getBufferedWriter(this.outputDirectory);
	}
	
}
