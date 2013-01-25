package playground.andreas.P2.ana.log2something;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

/**
 * Reads a pLogger file and writes it to tex. Parses the last iteration only.
 * 
 * @author aneumann
 *
 */
public class LogTex implements TabularFileHandler {
	
	private static final Logger log = Logger.getLogger(LogTex.class);

	private TabularFileParserConfig tabFileParserConfig;
	private ArrayList<String> processedLines = new ArrayList<String>();
	private LineSink sink = new LineAdder();
	private int linesRejected = 0;
	
	private int lastIteration = -1;
	private int coopCounterInThatIteration = 1;
	private String lastCoopName = "";
	private int lastPaxSum = 0;

	static interface LineSink {
		void process(String line);
	}

	class LineAdder implements LineSink {
		@Override
		public void process(String line) {
			processedLines.add(line);
		}
	}

	public LogTex(String filename) {
		tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(filename);
		tabFileParserConfig.setDelimiterTags(new String[] {"\t"}); // \t
	}

	public static ArrayList<String> readFile(String filename){
		LogTex reader = new LogTex(filename);
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
					
					if (Integer.parseInt(row[0]) != this.lastIteration) {
						this.lastIteration = Integer.parseInt(row[0]);
						this.coopCounterInThatIteration = 1;
						this.lastCoopName = "";
						this.lastPaxSum = 0;
						sink.process("\\addlinespace[1.0em]");
					}
					
					StringBuffer strB = new StringBuffer();
					
					strB.append(" & ");
					
					String operatorName = row[1].split("_")[1];
					if (!this.lastCoopName.equalsIgnoreCase(operatorName)) {
						strB.append("O" + this.coopCounterInThatIteration + " -- ");
						this.coopCounterInThatIteration++;
						strB.append(operatorName);
						this.lastCoopName = operatorName;
					}
					
					strB.append(" & ");
					
					String iteration = row[3].split("_")[0];
					strB.append(iteration);
					
					strB.append(" & ");
					
					String startTime = row[9].split(":")[0] + ":" + row[9].split(":")[1];
					strB.append(startTime);
					strB.append("--");
					String endTime = row[10].split(":")[0] + ":" + row[10].split(":")[1];
					strB.append(endTime);
					
					strB.append(" & ");
					
					String nodes = row[11];
					nodes = nodes.substring(1, nodes.length() - 1);
					String[] n = nodes.split(",");
					boolean firstIsDone = false;
					for (String node : n) {
						if (firstIsDone) {
							strB.append("--");
						}
						firstIsDone = true;
						strB.append(node.trim());
					}
					
					strB.append(" & ");

					String veh = row[5];
					strB.append(veh);
					
					strB.append(" & ");

					String trips = row[6];
					strB.append(trips);
					
					this.lastPaxSum += Integer.parseInt(trips);
					
					strB.append(" & ");

					double score = Double.parseDouble(row[7]);
					score = score / Double.parseDouble(veh);
					score = Math.round(score * 10.) / 10.;
					DecimalFormat df = (DecimalFormat)DecimalFormat.getInstance(Locale.US);
					df.applyPattern("#,###,##0.0");
					String s = df.format(score);
					
					if (s.contains("-")) {
						s = s.substring(1, s.length());
						strB.append("$-$");
					} else {
						strB.append("$+$");
					}
					
					strB.append(s);

					strB.append(" \\tabularnewline");
					
					strB.append(" % pax sum " + this.lastPaxSum);
					
					sink.process(strB.toString());
					
					
					
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

	public void setSink(LineSink sink) {
		this.sink = sink;
	}
	
	public static void main(String[] args) {
		String inputFile = args[0];
		ArrayList<String> result = LogTex.readFile(inputFile);

		BufferedWriter writer = IOUtils.getBufferedWriter("F:/output.txt");
		try {
			for (String string : result) {
				writer.write(string); writer.newLine();
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}