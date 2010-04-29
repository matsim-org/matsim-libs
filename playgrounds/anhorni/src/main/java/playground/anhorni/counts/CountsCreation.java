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
		
		CountsWriter writer = new CountsWriter(converter.getCountsIVTCH());
		writer.write("output/counts/countsIVTCH.xml");
		writer = new CountsWriter(converter.getCountsAre());
		writer.write("output/counts/countsAre.xml");
		writer = new CountsWriter(converter.getCountsTeleatlas());
		writer.write("output/counts/countsTeleatlas.xml");
		writer = new CountsWriter(converter.getCountsNavteq());
		writer.write("output/counts/countsNAVTEQ.xml");

		
		// Summary:
		CountsCompareReader countsCompareReader = new CountsCompareReader(stations);
		countsCompareReader.read();
		
		SummaryWriter summaryWriter = new SummaryWriter();
		summaryWriter.write(stations, "output/counts/");
		
		// comparison with old files
		Counts countsTele = new Counts();
		MatsimCountsReader countsReaderTele = new MatsimCountsReader(countsTele);
		countsReaderTele.readFile("input/counts/original/countsTele.xml");
		CountsWriter countsWriterTele = new CountsWriter(countsTele);
		countsWriterTele.write("output/counts/original/countsTele_original.xml");
		
		Counts countsIVTCH = new Counts();
		MatsimCountsReader countsReaderIVTCH = new MatsimCountsReader(countsIVTCH);
		countsReaderIVTCH.readFile("input/counts/original/countsIVTCH.xml");
		CountsWriter countsWriterIVTCH = new CountsWriter(countsIVTCH);
		countsWriterIVTCH.write("output/counts/original/countsIVTCH_original.xml");	
		
		Counts countsARE = new Counts();
		MatsimCountsReader countsReaderARE = new MatsimCountsReader(countsARE);
		countsReaderARE.readFile("input/counts/original/countsARE.xml");
		CountsWriter countsWriterAre = new CountsWriter(countsARE);
		countsWriterAre.write("output/counts/original/countsAre_original.xml");
		
		Counts countsNAVTEQ = new Counts();
		MatsimCountsReader countsReaderNAVTEQ = new MatsimCountsReader(countsNAVTEQ);
		countsReaderNAVTEQ.readFile("input/counts/original/countsNAVTEQ.xml");
		CountsWriter countsWriterNAVTEQ = new CountsWriter(countsNAVTEQ);
		countsWriterNAVTEQ.write("output/counts/original/countsNavteq_original.xml");
	}
}
