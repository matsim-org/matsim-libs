package playground.vsp.andreas.osmBB.convertCountsData;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.counts.Count;

public class ReadCountDataForWeek implements TabularFileHandler{
	
private static final Logger log = LogManager.getLogger(ReadCountDataForWeek.class);
	
	private TabularFileParserConfig tabFileParserConfig;
	private Count count;
	private String startTag;
	

	public static void readCountDataForWeek(String filename, Count count, String startTag) {
		ReadCountDataForWeek countDataReader = new ReadCountDataForWeek();		
		countDataReader.tabFileParserConfig = new TabularFileParserConfig();
		countDataReader.tabFileParserConfig.setFileName(filename);
		countDataReader.tabFileParserConfig.setDelimiterTags(new String[] {"\t"}); // \t
//		this.tabFileParserConfig.setDelimiterTags(new String[] {"D"});
		countDataReader.count = count;
		countDataReader.startTag = startTag;
        new TabularFileParser().parse(countDataReader.tabFileParserConfig, countDataReader);
    }
	
	public void startRow(String[] row) throws IllegalArgumentException {
		
		if(row[0].contains("Wochentage")){
			//ignore
			StringBuffer tempBuffer = new StringBuffer();
			for (String string : row) {
				tempBuffer.append(string);
				tempBuffer.append(", ");
			}
//			log.info("Ignoring: " + tempBuffer);
		} else if(row.length == 11){
			if(row[0].equalsIgnoreCase(this.startTag)){
				if(row[1].contains("Tages") || row[1].contains("spitze")){
					StringBuffer tempBuffer = new StringBuffer();
					for (String string : row) {
						tempBuffer.append(string);
						tempBuffer.append(", ");
					}
//					log.info("Reading data for: " + tempBuffer);
				} else {
					this.count.createVolume(Integer.parseInt(row[1]) + 1, Double.parseDouble(row[2]));
				}
			}
		} else {
			StringBuffer tempBuffer = new StringBuffer();
			for (String string : row) {
				tempBuffer.append(string);
				tempBuffer.append(", ");
			}
			log.warn("Could not interpretate row: " + tempBuffer);
		}
		
	}

}
