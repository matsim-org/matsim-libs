package playground.andreas.bln.ana;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;


/**
 * Compare two Linkstats files
 *
 * @author aneumann
 *
 */
public class EvaluateLinkstats implements TabularFileHandler {

	private static final Logger log = Logger.getLogger(EvaluateLinkstats.class);
	
	private TabularFileParserConfig tabFileParserConfig;
	private HashMap<String, ArrayList<Double>> linkMapWithCounts = new HashMap<String, ArrayList<Double>>();

	public void startRow(String[] row) throws IllegalArgumentException {
				
		if(row[0].contains("LINK")){
			StringBuffer tempBuffer = new StringBuffer();
			for (String string : row) {
				tempBuffer.append(string);
				tempBuffer.append(", ");
			}
			log.info("Ignoring: " + tempBuffer);
		} else {
			
			if(this.linkMapWithCounts.get(row[0]) == null){
				this.linkMapWithCounts.put(row[0], new ArrayList<Double>());
			}						
				
			ArrayList<Double> tempList = this.linkMapWithCounts.get(row[0]);
			
			// Put all average values in the array
			// Header looks like
			// LINK	ORIG_ID	FROM	TO	LENGTH	FREESPEED	CAPACITY	HRS0-1min	HRS0-1avg	HRS0-1max	HRS1-2min	HRS1-2avg	HRS1-2max	HRS2-3min	HRS2-3avg
			// 0	1		2		3	4		5			6			7			8			9			10			11			12			13			14
			// So start with number 8 and jump forward three steps at a time until the 
			for (int entry = 8; entry < 80; entry += 3) {
				tempList.add(Double.valueOf(row[entry]));
			}
	
		}
		
	}
	
	
	public static HashMap<String, ArrayList<Double>> readFile(String filename) throws IOException {
		
		EvaluateLinkstats personReader = new EvaluateLinkstats();
		
		personReader.tabFileParserConfig = new TabularFileParserConfig();
		personReader.tabFileParserConfig.setFileName(filename);
		personReader.tabFileParserConfig.setDelimiterTags(new String[] {"\t"}); // \t
//		this.tabFileParserConfig.setDelimiterTags(new String[] {"D"});
		new TabularFileParser().parse(personReader.tabFileParserConfig, personReader);
		return personReader.linkMapWithCounts;
	}
	
	public static HashMap<String, ArrayList<Double>> compareLinkstatFiles(String filename1, String filename2) {
		HashMap<String, ArrayList<Double>> outputEvent1 = null;
		HashMap<String, ArrayList<Double>> outputEvent2 = null;
		
		HashMap<String, ArrayList<Double>> compareMap = new HashMap<String, ArrayList<Double>>(); 

		try {
			outputEvent1 = EvaluateLinkstats.readFile(filename1);
			outputEvent2 = EvaluateLinkstats.readFile(filename2);
			
			for (Entry<String, ArrayList<Double>> entry : outputEvent1.entrySet()) {
				
				ArrayList<Double> tempList = new ArrayList<Double>();
				
				if(outputEvent2.containsKey(entry.getKey())){
					for (int i = 0; i < outputEvent2.get(entry.getKey()).size(); i++) {
						tempList.add(Double.valueOf(entry.getValue().get(i).doubleValue() - (outputEvent2.get(entry.getKey()).get(i)).doubleValue()));
					}
					compareMap.put(entry.getKey(), tempList);
				} else {
					for (int i = 0; i < entry.getValue().size(); i++) {
						tempList.add(entry.getValue().get(i));
					}
					compareMap.put(entry.getKey(), tempList);
				}
										
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
		return compareMap;
	}
	
}