package playground.andreas.intersection.zuerich;

import java.io.IOException;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.utils.io.tabularFileParser.TabularFileParserConfig;


public class LaneLinkMappingReader implements TabularFileHandler {

	private static final Logger log = Logger.getLogger(LaneLinkMappingReader.class);
	
//	private static final String[] HEADER = {"PersonId", "HomeLocation", "Age", "IsFemale", "Income", "PrimaryActivityType", "PrimaryActivityLocation"};
	
	private TabularFileParserConfig tabFileParserConfig;
		
//	private boolean isFirstLine = true;
	
	private HashMap<Integer, HashMap<Integer, String>> lsaMap = new HashMap<Integer, HashMap<Integer, String>>();

	public void startRow(String[] row) throws IllegalArgumentException {
				
		if(row[0].contains("KNOTENNR")){
			log.info("Found header: " + row.toString());
		} else {
//			log.info("Added: " + row.toString());
			
			HashMap<Integer, String> laneMap = this.lsaMap.get(Integer.valueOf(row[0]));
			
			if(laneMap == null){
				laneMap = new HashMap<Integer, String>();
				this.lsaMap.put(Integer.valueOf(row[0]), laneMap);
			}
			
			String sgLink = laneMap.get(Integer.valueOf(row[1]));

			if(sgLink == null){
				sgLink = row[2];
				laneMap.put(Integer.valueOf(row[1]), sgLink);
			}

		}
		
	}
	
	
	public HashMap<Integer, HashMap<Integer, String>> readFile(String filename) throws IOException {
		this.tabFileParserConfig = new TabularFileParserConfig();
		this.tabFileParserConfig.setFileName(filename);
		this.tabFileParserConfig.setDelimiterTags(new String[] {" ", "\t"}); // \t
//		this.tabFileParserConfig.setDelimiterTags(new String[] {"D"});
		new TabularFileParser().parse(this.tabFileParserConfig, this);
		return this.lsaMap;
	}
	
	public static HashMap<Integer, HashMap<Integer, String>> readBasicLightSignalSystemDefinition(String filename){
		
		LaneLinkMappingReader myLSAFileParser = new LaneLinkMappingReader();
		try {			
			log.info("Start reading file...");
			myLSAFileParser.readFile(filename);
			log.info("...finished.");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return myLSAFileParser.lsaMap;
	}
	
}
