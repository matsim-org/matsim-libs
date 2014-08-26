package playground.andreas.P2.ana.log2something;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.core.utils.misc.Time;

/**
 * Reads a pLogger file omitting the header and all totals.
 * 
 * @author aneumann
 *
 */
public class LogReader implements TabularFileHandler {
	
	private static final Logger log = Logger.getLogger(LogReader.class);

	private TabularFileParserConfig tabFileParserConfig;
	private ArrayList<LogElement> processedLines = new ArrayList<LogElement>();
	private LogElementSinkImpl sink = new LogElementSinkImpl();
	private int linesRejected = 0;

	static interface LogElementSink {
		void process(LogElement logElement);
	}

	class LogElementSinkImpl implements LogElementSink {
		@Override
		public void process(LogElement logElement) {
			processedLines.add(logElement);
		}
	}

	public LogReader(String filename) {
		tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(filename);
		tabFileParserConfig.setDelimiterTags(new String[] {"\t"}); // \t
	}

	public static ArrayList<LogElement> readFile(String filename){
		LogReader reader = new LogReader(filename);
		log.info("Start parsing " + filename);
		reader.parse();
		log.info("Finished parsing " + filename);
		log.info("Rejected " + reader.linesRejected + " lines");
		log.info("Imported " + reader.processedLines.size() + " lines");
		return reader.processedLines;		
	}	

	public void parse() {
        new TabularFileParser().parse(tabFileParserConfig, this);
    }
	
	@Override
	public void startRow(String[] row) {
		if(!row[0].trim().startsWith("iter")){
			if(!row[3].trim().startsWith("===")){
				try {
					LogElement logElement = new LogElement();
					
					logElement.setIteration(Integer.parseInt(row[0]));
					logElement.setCoopId(row[1]);
					logElement.setStatus(row[2]);
					logElement.setPlanId(row[3]);
					logElement.setCreatorId(row[4]);
					logElement.setnVeh(Integer.parseInt(row[5]));
					logElement.setnPax(Integer.parseInt(row[6]));
					logElement.setScore(Double.parseDouble(row[7]));
					logElement.setBudget(Double.parseDouble(row[8]));
					logElement.setStartTime(Time.parseTime(row[9]));
					logElement.setEndTime(Time.parseTime(row[10]));
					
					String nodes = row[11];
					nodes = nodes.substring(1, nodes.length() - 1);
					String[] n = nodes.split(",");
					logElement.setStopsToBeServed(n);
					
					sink.process(logElement);
					
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
	}

	public static void main(String[] args) {
		String inputFile = args[0];
		ArrayList<LogElement> result = LogReader.readFile(inputFile);

		BufferedWriter writer = IOUtils.getBufferedWriter("F:/output.txt");
		try {
//			for (String string : result) {
//				writer.write(string); writer.newLine();
//			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}