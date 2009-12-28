package playground.andreas.intersection.zuerich;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;


public class RemoveDuplicates implements TabularFileHandler {

	private static final Logger log = Logger.getLogger(RemoveDuplicates.class);
	
	private TabularFileParserConfig tabFileParserConfig;
	private String lastEntry = null;
	private String secondLastEntry = null;
	private String thirdLastEntry = null;
	private String fourthLastEntry = null;
	private BufferedWriter writer;
	
//	private boolean isFirstLine = true;
	
	public RemoveDuplicates(String outFileName) {
		try {
			this.writer = new BufferedWriter(new FileWriter(new File(outFileName)));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	public void startRow(String[] row) throws IllegalArgumentException {
				
		if(row[0].equalsIgnoreCase(this.lastEntry) || row[0].equalsIgnoreCase(this.secondLastEntry) || row[0].equalsIgnoreCase(this.thirdLastEntry) || row[0].equalsIgnoreCase(this.fourthLastEntry)){
			log.info("Found duplicate: " + row[0]);
		} else {
			try {
				this.writer.write(row[0]);
				this.writer.newLine();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			this.fourthLastEntry = this.thirdLastEntry;
			this.thirdLastEntry = this.secondLastEntry;
			this.secondLastEntry = this.lastEntry;
			this.lastEntry = row[0];
//			log.info("Added: " + row.toString());			
		}
		
	}
	
	
	public void readFile(String filename) throws IOException {
		this.tabFileParserConfig = new TabularFileParserConfig();
		this.tabFileParserConfig.setFileName(filename);
		this.tabFileParserConfig.setDelimiterTags(new String[] {";"}); // \t
//		this.tabFileParserConfig.setDelimiterTags(new String[] {"D"});
		new TabularFileParser().parse(this.tabFileParserConfig, this);
		this.writer.flush();
		this.writer.close();
	}
	
	public static void readBasicLightSignalSystemDefinition(String filename){
		
		RemoveDuplicates myLSAFileParser = new RemoveDuplicates(filename + ".new.xml");
		try {			
			log.info("Start reading file...");
			myLSAFileParser.readFile(filename);
			log.info("...finished.");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
}
