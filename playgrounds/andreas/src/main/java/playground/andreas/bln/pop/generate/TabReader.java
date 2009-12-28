package playground.andreas.bln.pop.generate;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;


/**
 *
 * @author aneumann
 *
 */
public class TabReader implements TabularFileHandler {

	private static final Logger log = Logger.getLogger(TabReader.class);
	
	private TabularFileParserConfig tabFileParserConfig;
	private ArrayList<String[]> personList = new ArrayList<String[]>();

	public void startRow(String[] row) throws IllegalArgumentException {
				
		if(row[0].contains("ORDNRPER") || row[0].contains("STARTZEIT") || row[0].contains("Ordnr2")){
			StringBuffer tempBuffer = new StringBuffer();
			for (String string : row) {
				tempBuffer.append(string);
				tempBuffer.append(", ");
			}
			log.info("Ignoring: " + tempBuffer);
		} else {
			this.personList.add(row);			
		}
		
	}
	
	
	public static ArrayList<String[]> readFile(String filename) throws IOException {
		
		TabReader personReader = new TabReader();
		
		personReader.tabFileParserConfig = new TabularFileParserConfig();
		personReader.tabFileParserConfig.setFileName(filename);
		personReader.tabFileParserConfig.setDelimiterTags(new String[] {","}); // \t
//		this.tabFileParserConfig.setDelimiterTags(new String[] {"D"});
		new TabularFileParser().parse(personReader.tabFileParserConfig, personReader);
		return personReader.personList;
	}	
	
}