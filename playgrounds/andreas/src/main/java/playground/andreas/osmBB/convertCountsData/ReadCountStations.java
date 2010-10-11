package playground.andreas.osmBB.convertCountsData;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

public class ReadCountStations implements TabularFileHandler{
	
	private static final Logger log = Logger.getLogger(ReadCountStations.class);
	
	private TabularFileParserConfig tabFileParserConfig;
	private List<CountStationDataBox> list = new LinkedList<CountStationDataBox>();

	public static List<CountStationDataBox> readCountStations(String filename) {
		ReadCountStations countStationsReader = new ReadCountStations();		
		countStationsReader.tabFileParserConfig = new TabularFileParserConfig();
		countStationsReader.tabFileParserConfig.setFileName(filename);
		countStationsReader.tabFileParserConfig.setDelimiterTags(new String[] {";"}); // \t
//		this.tabFileParserConfig.setDelimiterTags(new String[] {"D"});
		try {
			new TabularFileParser().parse(countStationsReader.tabFileParserConfig, countStationsReader);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		return countStationsReader.list;
	}
	
	public void startRow(String[] row) throws IllegalArgumentException {
		
		if(row[0].contains("SHORT")){
			//ignore
			StringBuffer tempBuffer = new StringBuffer();
			for (String string : row) {
				tempBuffer.append(string);
				tempBuffer.append(", ");
			}
			log.info("Ignoring: " + tempBuffer);
		} else if(row.length == 14){
			CountStationDataBox cS = new CountStationDataBox();
			cS.setShortName(row[0]);
			cS.setUnitName(row[1]);
			cS.setCoord(new CoordImpl(row[2].replace(',', '.'), row[3].replace(',', '.')));
			cS.setPosition(row[4]);
			cS.setPositionDetail(row[5]);
			cS.setDirection(row[6]);
			cS.setOrientation(row[7]);
			cS.setNumberOfLanesDetected(Integer.parseInt(row[8]));
			cS.setErrorCodeFromMapping(Integer.parseInt(row[10]));			
			this.list.add(cS);
		} else {
			StringBuffer tempBuffer = new StringBuffer();
			for (String string : row) {
				tempBuffer.append(string);
				tempBuffer.append(", ");
			}
			log.warn("Could not interpretate row: " + tempBuffer);
		}
		
	}

}
