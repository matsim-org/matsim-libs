package playground.andreas.intersection.zuerich;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;


public class SpurLinkMappingReader implements TabularFileHandler {

	private static final Logger log = Logger.getLogger(SpurLinkMappingReader.class);
	
//	private static final String[] HEADER = {"PersonId", "HomeLocation", "Age", "IsFemale", "Income", "PrimaryActivityType", "PrimaryActivityLocation"};
	
	private TabularFileParserConfig tabFileParserConfig;
		
//	private boolean isFirstLine = true;
	
	private HashMap<Integer,HashMap<Integer,String>> lsaMap = new HashMap<Integer, HashMap<Integer, String>>();

	public void startRow(String[] row) throws IllegalArgumentException {
				
		if(row[0].contains("KNOTENNR")){
			log.info("Found header: " + row.toString());
		} else {
//			log.info("Added: " + row.toString());
			Integer nodeId = Integer.valueOf(row[0]);
			HashMap<Integer, String> laneMap = this.lsaMap.get(nodeId);
			
			if(laneMap == null){
				laneMap = new HashMap<Integer, String>();
				this.lsaMap.put(nodeId, laneMap);
			}
			
			Integer laneId = Integer.valueOf(row[1]);
			String linkId = laneMap.get(laneId);

			if(linkId == null){
				linkId = row[2];
				laneMap.put(Integer.valueOf(row[1]), linkId);
			}

		}
		
	}
	
	
	private HashMap<Integer, HashMap<Integer, String>> readFile(String filename) throws IOException {
		this.tabFileParserConfig = new TabularFileParserConfig();
		this.tabFileParserConfig.setFileName(filename);
		this.tabFileParserConfig.setDelimiterTags(new String[] {" ", "\t"}); // \t
//		this.tabFileParserConfig.setDelimiterTags(new String[] {"D"});
		new TabularFileParser().parse(this.tabFileParserConfig, this);
		return this.lsaMap;
	}
	
	public Map<Integer, Map<Integer, String>> readBasicLightSignalSystemDefinition(String filename){
		try {			
			log.info("Start reading file...");
			this.readFile(filename);
			log.info("...finished.");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return (Map)this.lsaMap;
	}
	
}
