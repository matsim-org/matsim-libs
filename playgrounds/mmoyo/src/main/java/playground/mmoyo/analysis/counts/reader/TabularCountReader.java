package playground.mmoyo.analysis.counts.reader;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.matsim.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.transitSchedule.TransitScheduleReaderV1;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitScheduleFactory;
import org.xml.sax.SAXException;

/**Reads a tabular text with pt counts saved as ANSI. **/
public class TabularCountReader implements TabularFileHandler {
	private static final Logger log = Logger.getLogger(TabularCountReader.class);
	private static final String[] HEADER = {"id", "Haltestelle", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23"};
	private final TabularFileParserConfig tabFileParserConfig;
	private boolean isFirstLine = true;
	private int rowNum;
	private Counts counts = new Counts();

	final String EMPTY = "--";
	final String ZERO = "0.0";
	final String POINT = ".";
	
	TransitSchedule transitSchedule;
	
	public TabularCountReader(final String countName, final String countLayer) {
		this.tabFileParserConfig = new TabularFileParserConfig();
		this.tabFileParserConfig.setDelimiterTags(new String[] {"\t"});
		counts.setName(countName);
		counts.setLayer(countLayer);
		counts.setDescription("counts values from BVG 09.2009");	
		counts.setYear(2009);		
	}

	private void setTransitSchedule(final String transitScheddulePath, final String ptNetworkPath){
		ScenarioImpl scenario = new ScenarioImpl();
		NetworkImpl network = scenario.getNetwork();
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		this.transitSchedule = builder.createTransitSchedule();

		/* **************reads the transitSchedule file********* */
		new MatsimNetworkReader(scenario).readFile(ptNetworkPath);
		try {
			new TransitScheduleReaderV1(this.transitSchedule, network).readFile(transitScheddulePath);
		} catch (SAXException e){
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		}
	}
	
	public void startRow(final String[] row) throws IllegalArgumentException {
		if (this.isFirstLine) {
			boolean equalsHeader = true;
			int i = 0;
			for (String s : row) {
				if (!s.equalsIgnoreCase(HEADER[i])){
					equalsHeader = false;
					break;
				}
				i++;
			}
			if (!equalsHeader) {
				log.warn("the structure does not match. The header should be:  ");
				for (String g : HEADER) {
					System.out.print(g + " ");
				}
				System.out.println();
			}
			this.isFirstLine = false;
		}else{
			//for each station
			Id id =  new IdImpl(row[0]);   				
			counts.createCount(id, row[1]); //id of station  with index 0     ,   //name of station with index 1
			Count count = counts.getCount(id);
			count.setCoord(this.transitSchedule.getFacilities().get(new IdImpl(row[0])).getCoord());  // look up the coordinate of the stop facility and set it to the count 
			
			int col= 0;
			for (String s : row) {
				if (col>1){  
					if (s.equals(EMPTY)){s= ZERO;}
					count.createVolume(col-1, Double.parseDouble(s));	
				}
				col++;
			}
			rowNum++;
		}
	}
	
	public void writeCounts (final String outputFile){
		new CountsWriter(this.counts).write(outputFile);
	}
	
	public void readFile(final String filename) throws IOException {
		this.tabFileParserConfig.setFileName(filename);
		new TabularFileParser().parse(this.tabFileParserConfig, this);
	}
		
	public static void main(String[] args) throws IOException {
		String tabularFile = "../playgrounds/mmoyo/output/@counts/1.txt";           //<-change
		TabularCountReader countReader = new TabularCountReader("occupancy counts", "layer0") ;   //<-set name
		countReader.setTransitSchedule("../shared-svn/studies/countries/de/berlin-bvg09/pt/nullfall_berlin_brandenburg/input/pt_transitSchedule.xml.gz", "../shared-svn/studies/countries/de/berlin-bvg09/pt/nullfall_berlin_brandenburg/input/pt_network.xml.gz");
		countReader.readFile(tabularFile);
		countReader.writeCounts("../playgrounds/mmoyo/output/@counts/counts.xml");   //<-output file name
		System.out.println("done.");
	}
}