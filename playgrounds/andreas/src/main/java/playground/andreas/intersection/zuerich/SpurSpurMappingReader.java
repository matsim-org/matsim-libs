package playground.andreas.intersection.zuerich;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;


public class SpurSpurMappingReader implements TabularFileHandler {

	private static final Logger log = Logger.getLogger(SpurSpurMappingReader.class);
	
//	private static final String[] HEADER = {"PersonId", "HomeLocation", "Age", "IsFemale", "Income", "PrimaryActivityType", "PrimaryActivityLocation"};
	
	private TabularFileParserConfig tabFileParserConfig;
		
//	private boolean isFirstLine = true;
	
	private HashMap<Integer, HashMap<Integer, List<Integer>>> lsaMap = new HashMap<Integer, HashMap<Integer, List<Integer>>>();

	public void startRow(String[] row) throws IllegalArgumentException {
				
		if(row[0].contains("KNOTENNR")){
			log.info("Found header: " + row.toString());
		} else {
//			log.info("Added: " + row.toString());
			
			HashMap<Integer,  List<Integer>> laneMap = this.lsaMap.get(Integer.valueOf(row[0]));
			
			if(laneMap == null){
				laneMap = new HashMap<Integer,  List<Integer>>();
				this.lsaMap.put(Integer.valueOf(row[0]), laneMap);
			}
			
			List<Integer> sgLink = laneMap.get(Integer.valueOf(row[1]));

			if(sgLink == null){
				sgLink =  new ArrayList<Integer>();
//				sgLink.add(Integer.valueOf(row[2]));
				laneMap.put(Integer.valueOf(row[1]), sgLink);
			}
			
			sgLink = laneMap.get(Integer.valueOf(row[1]));
			sgLink.add(Integer.valueOf(row[2]));

		}
		
	}
	
	
	public HashMap<Integer, HashMap<Integer,  List<Integer>>> readFile(String filename) throws IOException {
		this.tabFileParserConfig = new TabularFileParserConfig();
		this.tabFileParserConfig.setFileName(filename);
		this.tabFileParserConfig.setDelimiterTags(new String[] {" ", "\t"}); // \t
//		this.tabFileParserConfig.setDelimiterTags(new String[] {"D"});
		new TabularFileParser().parse(this.tabFileParserConfig, this);
		return this.lsaMap;
	}
	
	public static Map<Integer, Map<Integer,  List<Integer>>> readBasicLightSignalSystemDefinition(String filename){
		
		SpurSpurMappingReader myLSAFileParser = new SpurSpurMappingReader();
		try {			
			log.info("Start reading file...");
			myLSAFileParser.readFile(filename);
			log.info("...finished.");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return (Map)myLSAFileParser.lsaMap;
	}
	
}
