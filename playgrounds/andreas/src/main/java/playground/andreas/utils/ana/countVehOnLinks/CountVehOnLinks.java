package playground.andreas.utils.ana.countVehOnLinks;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;


/**
 * Counts number of agents passing a link during the simulation (normally 24 or 30 hours)
 *
 * @author aneumann
 *
 */
public class CountVehOnLinks implements TabularFileHandler {

	private static final Logger log = Logger.getLogger(CountVehOnLinks.class);
	
	private TabularFileParserConfig tabFileParserConfig;
	private HashMap<String, Integer> linkMapWithCounts = new HashMap<String, Integer>();

	public void startRow(String[] row) throws IllegalArgumentException {
				
		if(row[0].contains("T_GBL")){
			StringBuffer tempBuffer = new StringBuffer();
			for (String string : row) {
				tempBuffer.append(string);
				tempBuffer.append(", ");
			}
			log.info("Ignoring: " + tempBuffer);
		} else {
			if(row[5].equalsIgnoreCase("entered link")){

				if(this.linkMapWithCounts.get(row[2]) == null){
					this.linkMapWithCounts.put(row[2], Integer.valueOf(0));
				}						
				
				this.linkMapWithCounts.put(row[2], Integer.valueOf(this.linkMapWithCounts.get(row[2]).intValue() + 1));

			}		
		}
		
	}
	
	public static HashMap<String, Integer> readFile(String filename) throws IOException {
		
		CountVehOnLinks personReader = new CountVehOnLinks();
		
		personReader.tabFileParserConfig = new TabularFileParserConfig();
		personReader.tabFileParserConfig.setFileName(filename);
		personReader.tabFileParserConfig.setDelimiterTags(new String[] {"\t"}); // \t
//		this.tabFileParserConfig.setDelimiterTags(new String[] {"D"});
		new TabularFileParser().parse(personReader.tabFileParserConfig, personReader);
		return personReader.linkMapWithCounts;
	}
	
	public static HashMap<String, Integer> compareEventFiles(String filename1, String filename2) {
		HashMap<String, Integer> outputEvent1 = null;
		HashMap<String, Integer> outputEvent2 = null;
		
		HashMap<String, Integer> compareMap = new HashMap<String, Integer>(); 

		try {
			outputEvent1 = CountVehOnLinks.readFile(filename1);
			outputEvent2 = CountVehOnLinks.readFile(filename2);
			
			for (Entry<String, Integer> entry : outputEvent1.entrySet()) {
				
				if(outputEvent2.containsKey(entry.getKey())){
					compareMap.put(entry.getKey(), Integer.valueOf(entry.getValue().intValue() - (outputEvent2.get(entry.getKey())).intValue()));					
				} else {
					compareMap.put(entry.getKey(), Integer.valueOf(entry.getValue().intValue()));
				}
										
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
		return compareMap;
	}
	
	public static void main(String[] args) {
		
		String event1Filename = "c:\\Users\\aneumann\\Documents\\VSP_Extern\\Berlin\\berlin-sharedsvn\\network\\A100\\1000.events.txt"; 
		String event2Filename = "c:\\Users\\aneumann\\Documents\\VSP_Extern\\Berlin\\berlin-sharedsvn\\network\\A100\\1000.events.txt";
		String outFilename = "c:\\Users\\aneumann\\Documents\\VSP_Extern\\Berlin\\berlin-sharedsvn\\network\\A100\\count.txt";

		HashMap<String, Integer> outputEvent1 = null;
		HashMap<String, Integer> outputEvent2 = null;
		
		HashMap<String, Integer> compareMap = new HashMap<String, Integer>(); 

		BufferedWriter writer;
		try {

			outputEvent1 = CountVehOnLinks.readFile(event1Filename);
			outputEvent2 = CountVehOnLinks.readFile(event2Filename);
			
			for (Entry<String, Integer> entry : outputEvent1.entrySet()) {
				
				if(outputEvent2.containsKey(entry.getKey())){
					compareMap.put(entry.getKey(), Integer.valueOf(entry.getValue().intValue() - (outputEvent2.get(entry.getKey())).intValue()));					
				}
									
			}			
			
			writer = new BufferedWriter(new FileWriter(new File(outFilename)));

			writer.write("link Id, 24hcount");
			writer.newLine();

			for (String string : compareMap.keySet()) {
				writer.write(string + ", " + compareMap.get(string).intValue());
				writer.newLine();				
			}

			writer.flush();
			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		//			log.info("Finished writing trips to " + filename);

		System.out.print("Wait");
	}
	
}