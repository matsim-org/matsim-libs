package playground.vsp.andreas.utils.pt.transitSchedule2shape;

import org.apache.log4j.Logger;
import org.matsim.contrib.minibus.PConstants;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

public class ReadCoopLoggerFileAndReturnCoopsInBusiness implements TabularFileHandler {
	
	private static final Logger log = Logger.getLogger(ReadCoopLoggerFileAndReturnCoopsInBusiness.class);

	private TabularFileParserConfig tabFileParserConfig;
	private Set<String> coopIdInBusiness = new TreeSet<String>();
	private Sink sink = new SetAdder();
	private int linesRejected = 0;
	
	private int lastIteration = -1;

	static interface Sink {
		void process(String lineId);
	}

	class SetAdder implements Sink {
		@Override
		public void process(String lineId) {
			coopIdInBusiness.add(lineId);
		}
	}

	public ReadCoopLoggerFileAndReturnCoopsInBusiness(String filename) {
		tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(filename);
		tabFileParserConfig.setDelimiterTags(new String[] {"\t"}); // \t
	}

	public static Set<String> readCoopLoggerFileAndReturnCoopsInBusiness(String filename){
		ReadCoopLoggerFileAndReturnCoopsInBusiness reader = new ReadCoopLoggerFileAndReturnCoopsInBusiness(filename);
		log.info("Start parsing " + filename);
		reader.parse();
		log.info("Finished parsing " + filename);
		log.info("Rejected " + reader.linesRejected + " lines");
		log.info("Imported " + reader.coopIdInBusiness.size() + " lines");
		return reader.coopIdInBusiness;		
	}	

	public void parse() {
        new TabularFileParser().parse(tabFileParserConfig, this);
    }
	
	@Override
	public void startRow(String[] row) {
		if(!row[0].trim().startsWith("#")){
			try {
				int currentIteration = Integer.parseInt(row[0].trim());
				
				if (lastIteration < currentIteration) {
					// new iteration found
					lastIteration = currentIteration;
					this.coopIdInBusiness = new TreeSet<String>();
				}
				
				if (row[2].trim().equalsIgnoreCase(PConstants.OperatorState.INBUSINESS.toString())) {
					String coopId = row[1].trim();
					sink.process(coopId);
				}
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

	public void setSink(Sink sink) {
		this.sink = sink;
	}
}