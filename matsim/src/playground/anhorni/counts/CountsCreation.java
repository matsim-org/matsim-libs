package playground.anhorni.counts;

import java.util.Iterator;
import java.util.TreeMap;
import java.util.Vector;
import org.matsim.core.gbl.Gbl;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.matsim.counts.MatsimCountsReader;
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
		
		CountsWriter writer = new CountsWriter(converter.getCountsIVTCH(), "output/counts/countsIVTCH.xml");
		writer.write();
		writer = new CountsWriter(converter.getCountsAre(), "output/counts/countsAre.xml");
		writer.write();
		writer = new CountsWriter(converter.getCountsTeleatlas(), "output/counts/countsTeleatlas.xml");
		writer.write();
		writer = new CountsWriter(converter.getCountsNavteq(), "output/counts/countsNAVTEQ.xml");
		writer.write();

		
		// Summary:
		CountsCompareReader countsCompareReader = new CountsCompareReader(stations);
		countsCompareReader.read();
		
		SummaryWriter summaryWriter = new SummaryWriter();
		summaryWriter.write(stations, "output/counts/");
		
		// comparison with old files
		Counts countsTele = new Counts();
		MatsimCountsReader countsReaderTele = new MatsimCountsReader(countsTele);
		countsReaderTele.readFile("input/counts/original/countsTele.xml");
		CountsWriter countsWriterTele = new CountsWriter(countsTele, "output/counts/original/countsTele_original.xml");
		countsWriterTele.write();
		
		Counts countsIVTCH = new Counts();
		MatsimCountsReader countsReaderIVTCH = new MatsimCountsReader(countsIVTCH);
		countsReaderIVTCH.readFile("input/counts/original/countsIVTCH.xml");
		CountsWriter countsWriterIVTCH = new CountsWriter(countsIVTCH, "output/counts/original/countsIVTCH_original.xml");
		countsWriterIVTCH.write();	
		
		Counts countsARE = new Counts();
		MatsimCountsReader countsReaderARE = new MatsimCountsReader(countsARE);
		countsReaderARE.readFile("input/counts/original/countsARE.xml");
		CountsWriter countsWriterAre = new CountsWriter(countsARE, "output/counts/original/countsAre_original.xml");
		countsWriterAre.write();
		
		Counts countsNAVTEQ = new Counts();
		MatsimCountsReader countsReaderNAVTEQ = new MatsimCountsReader(countsNAVTEQ);
		countsReaderNAVTEQ.readFile("input/counts/original/countsNAVTEQ.xml");
		CountsWriter countsWriterNAVTEQ = new CountsWriter(countsNAVTEQ, "output/counts/original/countsNavteq_original.xml");
		countsWriterNAVTEQ.write();
	}
}
