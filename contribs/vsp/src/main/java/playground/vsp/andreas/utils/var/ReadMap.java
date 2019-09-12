package playground.vsp.andreas.utils.var;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

/**
 * Reads a map from file. Format is:<br>
 * source1,target1,target2,target3<br>
 * source2,target1,target2<br>
 * source3,target1,target2,target3,target4
 * 
 * @author aneumann
 *
 */
public class ReadMap implements TabularFileHandler {
	
	private static final Logger log = Logger.getLogger(ReadMap.class);

	private TabularFileParserConfig tabFileParserConfig;
	private Map<String, List<String>> String2StingsMap = new HashMap<String, List<String>>();
	private LineSink sink = new MapAdder();
	private int linesRejected = 0;

	static interface LineSink {
		void process(String source, List<String> targets);
	}

	class MapAdder implements LineSink {
		@Override
		public void process(String source, List<String> targets) {
			String2StingsMap.put(source, targets);
		}
	}

	public ReadMap(String filename) {
		tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(filename);
		tabFileParserConfig.setDelimiterTags(new String[] {","}); // \t
	}

	public static Map<String, List<String>> readMap(String filename){
		ReadMap reader = new ReadMap(filename);
		log.info("Start parsing " + filename);
		reader.parse();
		log.info("Finished parsing " + filename);
		log.info("Rejected " + reader.linesRejected + " lines");
		log.info("Imported " + reader.String2StingsMap.size() + " lines");
		return reader.String2StingsMap;		
	}	

	public void parse() {
        new TabularFileParser().parse(tabFileParserConfig, this);
    }
	
	@Override
	public void startRow(String[] row) {
		if(!row[0].trim().startsWith("#")){
			try {
				String source = row[0].trim();
				List<String> targets = new LinkedList<String>();
				
				if (row.length > 0) {
					for (int i = 1; i < row.length; i++) {
						targets.add(row[i].trim());
					}
				}

				sink.process(source, targets);
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