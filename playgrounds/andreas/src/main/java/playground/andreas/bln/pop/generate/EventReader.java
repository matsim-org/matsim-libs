package playground.andreas.bln.pop.generate;

import java.io.IOException;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;


/**
 *
 * @author aneumann
 *
 */
public class EventReader implements TabularFileHandler {

	private static final Logger log = Logger.getLogger(EventReader.class);
	
	private TabularFileParserConfig tabFileParserConfig;
	private TreeSet<Integer> agentIds = new TreeSet<Integer>();

	public void startRow(String[] row) throws IllegalArgumentException {
				
		if(row[0].contains("T_GBL")){
			StringBuffer tempBuffer = new StringBuffer();
			for (String string : row) {
				tempBuffer.append(string);
				tempBuffer.append(", ");
			}
			log.info("Ignoring: " + tempBuffer);
		} else {
			if(Integer.parseInt(row[0]) > 86400){
				try {
					this.agentIds.add(Integer.valueOf((row[1])));
				} catch (Exception e) {
					e.printStackTrace();
				}				
			}		
	
		}
		
	}
	
	
	public static TreeSet<Integer> readFile(String filename) throws IOException {
		
		EventReader personReader = new EventReader();
		
		personReader.tabFileParserConfig = new TabularFileParserConfig();
		personReader.tabFileParserConfig.setFileName(filename);
		personReader.tabFileParserConfig.setDelimiterTags(new String[] {"\t"}); // \t
//		this.tabFileParserConfig.setDelimiterTags(new String[] {"D"});
		new TabularFileParser().parse(personReader.tabFileParserConfig, personReader);
		return personReader.agentIds;
	}	
	
}