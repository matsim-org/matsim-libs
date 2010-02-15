package playground.andreas.bln.ana.events2timespacechart;

import java.io.IOException;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;


public class ReadStopIDNamesMap implements TabularFileHandler {

	private static final Logger log = Logger.getLogger(ReadStopIDNamesMap.class);
	
	private TabularFileParserConfig tabFileParserConfig;
	private HashMap<Id, String> stopIDNameMap = new HashMap<Id, String>();

	public void startRow(String[] row) throws IllegalArgumentException {
				
		if(row[0].contains("$")){
			//ignore
			StringBuffer tempBuffer = new StringBuffer();
			for (String string : row) {
				tempBuffer.append(string);
				tempBuffer.append(", ");
			}
			log.info("Ignoring: " + tempBuffer);
		} else if(row.length == 12){			
			this.stopIDNameMap.put(new IdImpl(row[0]), row[3]);			
		} else {
			StringBuffer tempBuffer = new StringBuffer();
			for (String string : row) {
				tempBuffer.append(string);
				tempBuffer.append(", ");
			}
			log.warn("Could not interpretate row: " + tempBuffer);
		}
		
	}
	
	public static HashMap<Id, String> readFile(String filename) throws IOException {
		
		ReadStopIDNamesMap stopNameMapReader = new ReadStopIDNamesMap();
		
		stopNameMapReader.tabFileParserConfig = new TabularFileParserConfig();
		stopNameMapReader.tabFileParserConfig.setFileName(filename);
		stopNameMapReader.tabFileParserConfig.setDelimiterTags(new String[] {";"}); // \t
//		this.tabFileParserConfig.setDelimiterTags(new String[] {"D"});
		new TabularFileParser().parse(stopNameMapReader.tabFileParserConfig, stopNameMapReader);
		return stopNameMapReader.stopIDNameMap;
	}	
	
}