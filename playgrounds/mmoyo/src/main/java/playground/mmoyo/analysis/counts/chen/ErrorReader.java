package playground.mmoyo.analysis.counts.chen;

import java.io.IOException;
import org.apache.log4j.Logger;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

/**Reads a tabular ErrorGraphData text file. **/
public class ErrorReader implements TabularFileHandler {
	private static final Logger log = Logger.getLogger(ErrorReader.class);
	private static final String[] HEADER = {"hour", "mean relative error", "mean absolute bias"};
	private final TabularFileParserConfig tabFileParserConfig;
	private boolean isFirstLine = true;
	private ErrorData errorData; 
	private int rowNum;
	
	public ErrorReader() {
		this.tabFileParserConfig = new TabularFileParserConfig();
		this.tabFileParserConfig.setDelimiterTags(new String[] {"\t"});
	}

	public void startRow(final String[] row) throws IllegalArgumentException {
		if (this.isFirstLine) {
			boolean equalsHeader = true;
			int i = 0;
			for (String s : row) {
				if (!s.equalsIgnoreCase(HEADER[i])) {
					equalsHeader = false;
					break;
				}
				i++;
			}
			if (!equalsHeader) {
				log.warn("#######################################################################");
				log.warn("Unfortunately the structure does not match!");
				log.warn("The header should be: ");
				for (String g : HEADER) {
					System.out.print(g + " ");
				}
				System.out.println();
				log.warn("#######################################################################");
				}
				this.isFirstLine = false;
			}else {
				errorData.getMeanRelError()[rowNum]= Double.parseDouble(row[1]);
				errorData.getMeanAbsBias()[rowNum] = Double.parseDouble(row[2]);
				rowNum++;
			}
		}

		public ErrorData readFile(final String filename, String Titel) throws IOException {
			this.tabFileParserConfig.setFileName(filename);
			this.errorData= new ErrorData (Titel);
			new TabularFileParser().parse(this.tabFileParserConfig, this);
			return errorData;
		}

	}