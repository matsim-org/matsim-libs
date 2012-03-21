package playground.andreas.bvg4;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

public class ReadStopNameMap implements TabularFileHandler {
	
	private static final Logger log = Logger.getLogger(ReadStopNameMap.class);

	private TabularFileParserConfig tabFileParserConfig;
	private Map<String, String> old2NewStopNameMap = new HashMap<String, String>();
	private LineSink sink = new MapAdder();
	private int linesRejected = 0;

	static interface LineSink {
		void process(String oldStopName, String newStopName);
	}

	class MapAdder implements LineSink {
		@Override
		public void process(String oldStopName, String newStopName) {
			old2NewStopNameMap.put(oldStopName, newStopName);
		}
	}

	public ReadStopNameMap(String filename) {
		tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(filename);
		tabFileParserConfig.setDelimiterTags(new String[] {";"}); // \t
	}

	public static Map<String, String> readStopNameMap(String filename){
		ReadStopNameMap reader = new ReadStopNameMap(filename);
		log.info("Start parsing " + filename);
		reader.parse();
		log.info("Finished parsing " + filename);
		log.info("Rejected " + reader.linesRejected + " lines");
		log.info("Imported " + reader.old2NewStopNameMap.size() + " lines");
		return reader.old2NewStopNameMap;		
	}	

	public void parse() {
        new TabularFileParser().parse(tabFileParserConfig, this);
    }
	
	@Override
	public void startRow(String[] row) {
		if(!row[0].trim().startsWith("#")){
			try {
				String oldStopName = row[1].trim();
				String newStopName = row[0].trim();
				
				if(oldStopName.equalsIgnoreCase("")){
					oldStopName = null;
				}
				
				if(newStopName.equalsIgnoreCase("")){
					newStopName = null;
				}

				sink.process(oldStopName, newStopName);
			} catch (NumberFormatException e) {
				this.linesRejected++;
				log.info("Ignoring line : " + Arrays.asList(row));
			}
			
		} else {
			StringBuffer tempBuffer = new StringBuffer();
			for (String string : row) {
				tempBuffer.append(string);
				tempBuffer.append(", ");
			}
			this.linesRejected++;
			log.info("Ignoring: " + tempBuffer);
		}
	}

	public void setSink(LineSink sink) {
		this.sink = sink;
	}
}