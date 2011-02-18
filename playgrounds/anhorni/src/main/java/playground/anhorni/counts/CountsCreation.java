package playground.anhorni.counts;

import java.util.Iterator;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.matsim.counts.MatsimCountsReader;

public class CountsCreation {
	
	private final static Logger log = Logger.getLogger(CountsCreation.class);
	private String pathsFile = "../../matsim/input/counts/datasets.txt";
	private String networkMappingFile = "../../matsim/input/counts/networkMapping.txt";
	
	private final static String dayFilter = "MOFRI";
	private final static boolean writeForSpecificArea = false;
	private final static boolean removeOutliers = false;
	
	public static void main(final String[] args) {

		Gbl.startMeasurement();
		CountsCreation creator = new CountsCreation();
		creator.run();
		
		Gbl.printElapsedTime();
		log.info("finished #########################################################################");
	}
	
	
	public void run() {
		CountsReaderYear reader = new CountsReaderYear();
		reader.read(this.pathsFile);
				
		log.info("Do the cleaning ------------------");
		Cleaner cleaner = new Cleaner();
		TreeMap<String, Vector<RawCount>> rawCounts = cleaner.cleanRawCounts(reader.getRawCounts());
				
		NetworkMapper mapper = new NetworkMapper();
		mapper.map(rawCounts, this.networkMappingFile);

		Stations stations = new Stations();
		stations.setCountStations(mapper.getCountStations());

		log.info("Do the filtering ------------------");
		TimeFilter filter = new TimeFilter();
		filter.setDayFilter(dayFilter);
		log.info(" 	day filter: " + dayFilter);
		
		Iterator<CountStation> station_it = stations.getCountStations().iterator();
		while (station_it.hasNext()) {
			CountStation station = station_it.next();
			log.info("	Station " + station.getId() + " number of counts before filtering " + station.getCounts().size() + " i.e. days: " + 
					station.getCounts().size()/ 24.0);
			station.filter(filter);	
			station.mapCounts();
			station.aggregate(removeOutliers);
			log.info("	Station " + station.getId() + " number of counts after filtering: " + station.getCounts().size() + " i.e. days: " + 
					station.getCounts().size()/ 24.0);
			log.info("	--- ");
		}		
		log.info("Do the converting ------------------");
		Converter converter = new Converter();
		converter.convert(stations.getCountStations());
		
		CountsWriter writer = new CountsWriter(converter.getCountsIVTCH());
		writer.write("../../matsim/output/counts/countsIVTCH.xml");
		writer = new CountsWriter(converter.getCountsTeleatlas());
		writer.write("../../matsim/output/counts/countsTeleatlas.xml");
		writer = new CountsWriter(converter.getCountsNavteq());
		writer.write("../../matsim/output/counts/countsNAVTEQ.xml");

		
		// Summary:
		CountsCompareReader countsCompareReader = new CountsCompareReader(stations);
		countsCompareReader.read();
		
		log.info("Writing the summary -------------------");
		log.info("	Number of stations to write: " + stations.getCountStations().size());
		SummaryWriter summaryWriter = new SummaryWriter();
		log.info(" 		write analysis for specific area: " + writeForSpecificArea);
		summaryWriter.write(stations, "../../matsim/output/counts/analysis/", writeForSpecificArea);
		
		log.info("Writing old files  -------------------");
		//for comparison with old files
		Counts countsTele = new Counts();
		MatsimCountsReader countsReaderTele = new MatsimCountsReader(countsTele);
		countsReaderTele.readFile("../../matsim/input/counts/original/countsTeleatlas.xml");
		CountsWriter countsWriterTele = new CountsWriter(countsTele);
		countsWriterTele.write("../../matsim/output/counts/original/countsTeleatlas_original.xml");
		
		Counts countsIVTCH = new Counts();
		MatsimCountsReader countsReaderIVTCH = new MatsimCountsReader(countsIVTCH);
		countsReaderIVTCH.readFile("../../matsim/input/counts/original/countsIVTCH.xml");
		CountsWriter countsWriterIVTCH = new CountsWriter(countsIVTCH);
		countsWriterIVTCH.write("../../matsim/output/counts/original/countsIVTCH_original.xml");	
				
		Counts countsNAVTEQ = new Counts();
		MatsimCountsReader countsReaderNAVTEQ = new MatsimCountsReader(countsNAVTEQ);
		countsReaderNAVTEQ.readFile("../../matsim/input/counts/original/countsNAVTEQ.xml");
		CountsWriter countsWriterNAVTEQ = new CountsWriter(countsNAVTEQ);
		countsWriterNAVTEQ.write("../../matsim/output/counts/original/countsNavteq_original.xml");
	}
}
