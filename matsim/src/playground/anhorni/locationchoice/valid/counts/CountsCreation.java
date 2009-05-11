package playground.anhorni.locationchoice.valid.counts;

import java.util.Iterator;
import java.util.TreeMap;
import java.util.Vector;
import org.matsim.core.gbl.Gbl;
import org.matsim.counts.CountsWriter;
 import org.apache.log4j.Logger;

public class CountsCreation {
	
	private final static Logger log = Logger.getLogger(CountsCreation.class);
	private String pathsFile = "./input/counts/datasets.txt";
	private String networkMappingFile = "./input/counts/networkMapping.txt";
	
	public static void main(final String[] args) {

		Gbl.startMeasurement();
		CountsCreation creator = new CountsCreation();
		creator.run();
		
		Gbl.printElapsedTime();
	}
	
	
	public void run() {
		CountsReaderYear reader = new CountsReaderYear();
		reader.read(this.pathsFile);
		
		TreeMap<String, Vector<RawCount>> rawCounts = reader.getRawCounts();
		
		NetworkMapper mapper = new NetworkMapper();
		mapper.map(rawCounts, this.networkMappingFile);

		
		Stations stations = new Stations();
		stations.setCountStations(mapper.getCountStations());
		log.info("Number of stations: " + stations.getCountStations().size());
			
		DateFilter filter = new DateFilter();
		Iterator<CountStation> station_it = stations.getCountStations().iterator();
		while (station_it.hasNext()) {
			CountStation station = station_it.next();
			station.filter(filter);	
			station.mapCounts();
			station.aggregate();
		}		
		
		Converter converter = new Converter();
		converter.convert(stations.getCountStations());
				
		CountsWriter writer = new CountsWriter(converter.getCountsAre(), "output/counts/countsAre.xml");
		writer.write();
		writer = new CountsWriter(converter.getCountsTeleatlas(), "output/counts/countsTeleatlas.xml");
		writer.write();
		writer = new CountsWriter(converter.getCountsNavteq(), "output/counts/countsNAVTEQ.xml");
		writer.write();
		writer = new CountsWriter(converter.getCountsIVTCH(), "output/counts/countsIVTCH.xml");
		writer.write();	
		
		// Summary:
		CountsCompareReader countsCompareReader = new CountsCompareReader(stations);
		countsCompareReader.read();
		
		SummaryWriter summaryWriter = new SummaryWriter();
		summaryWriter.write(stations, "output/counts/");
		
	}
}
