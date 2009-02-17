package playground.andreas.intersection.zuerich;

import java.io.IOException;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.basic.signalsystems.BasicSignalSystemDefinition;
import org.matsim.basic.v01.IdImpl;
import org.matsim.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.utils.io.tabularFileParser.TabularFileParserConfig;


public class LSASystemsReader implements TabularFileHandler {

	private static final Logger log = Logger.getLogger(LSASystemsReader.class);
	
//	private static final String[] HEADER = {"PersonId", "HomeLocation", "Age", "IsFemale", "Income", "PrimaryActivityType", "PrimaryActivityLocation"};
	
	private TabularFileParserConfig tabFileParserConfig;
		
//	private boolean isFirstLine = true;
	
	private HashMap<Integer, BasicSignalSystemDefinition> lsaMap = new HashMap<Integer, BasicSignalSystemDefinition>();

	public void startRow(String[] row) throws IllegalArgumentException {
				
		if(row[0].contains("LSAID")){
			log.info("Found header: " + row.toString());
		} else {
//			log.info("Added: " + row.toString());
			
			BasicSignalSystemDefinition lsa = this.lsaMap.get(Integer.valueOf(row[0]));
			
			if(lsa == null){
				lsa = new BasicSignalSystemDefinition(new IdImpl(Integer.parseInt(row[0])));
				this.lsaMap.put(Integer.valueOf(row[0]), lsa);
			}
			
			lsa.setDefaultCirculationTime(Double.parseDouble(row[1]));
			lsa.setDefaultInterimTime(3.0);
			lsa.setDefaultSyncronizationOffset(0.0);
		}
		
	}
	
	
	public HashMap<Integer, BasicSignalSystemDefinition> readFile(String filename) throws IOException {
		this.tabFileParserConfig = new TabularFileParserConfig();
		this.tabFileParserConfig.setFileName(filename);
		this.tabFileParserConfig.setDelimiterTags(new String[] {" ", "\t"}); // \t
//		this.tabFileParserConfig.setDelimiterTags(new String[] {"D"});
		new TabularFileParser().parse(this.tabFileParserConfig, this);
		return this.lsaMap;
	}
	
	public static HashMap<Integer, BasicSignalSystemDefinition> readBasicLightSignalSystemDefinition(String filename){
		
		LSASystemsReader myLSAFileParser = new LSASystemsReader();
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
