package playground.andreas.fggeoinfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.transitSchedule.TransitScheduleReaderV1;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.xml.sax.SAXException;

import playground.andreas.bln.pop.generate.TabReader;


public class ReadBBIDemand implements TabularFileHandler{

	private static final Logger log = Logger.getLogger(ReadBBIDemand.class);
	
	private TabularFileParserConfig tabFileParserConfig;
	private ArrayList<DemandBox> demandList = new ArrayList<DemandBox>();
	
	public static List<DemandBox> readBBIDemand(String filename) throws IOException {
		
		ReadBBIDemand reader = new ReadBBIDemand();
		
		reader.tabFileParserConfig = new TabularFileParserConfig();
		reader.tabFileParserConfig.setFileName(filename);
		reader.tabFileParserConfig.setDelimiterTags(new String[] {";"}); // \t
//		this.tabFileParserConfig.setDelimiterTags(new String[] {"D"});
		new TabularFileParser().parse(reader.tabFileParserConfig, reader);

		return reader.demandList;		
	}	
	
	
	protected static class DemandBox {
		private String street;
		private String description;
		private int numberOfAgents;
		private Coord location;
		
		public DemandBox(String street, String description, int numberOfAgents, double xCoord, double yCoord) {
			this.street = street;
			this.description = description;
			this.numberOfAgents = numberOfAgents;
			this.location = new CoordImpl(xCoord, yCoord);
			

		}
		
		public String getNameBySourceAndDescription(){
				return this.street + "_" + this.description;
		}
		
		public Coord getCoord(){
			return this.location;
		}
		
		public int numberOfPassengers(){
			return this.numberOfAgents;
		}
	}

	@Override
	public void startRow(String[] row) {
		if(row[0].contains("#")){
			StringBuffer tempBuffer = new StringBuffer();
			for (String string : row) {
				tempBuffer.append(string);
				tempBuffer.append(", ");
			}
			log.info("Ignoring: " + tempBuffer);
		} else {
			this.demandList.add(new DemandBox(row[0], row[1], Integer.parseInt(row[2]), Double.parseDouble(row[3]), Double.parseDouble(row[4])));			
		}
		
	}
	
}