package playground.andreas.fggeoinfo;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;


public class ReadBBITimeStructure implements TabularFileHandler{

	private static final Logger log = Logger.getLogger(ReadBBITimeStructure.class);
	
	private TabularFileParserConfig tabFileParserConfig;
	private double[] timeStructure = new double[24];
	
	public static double[] readBBITimeStructure(String filename) throws IOException {
		
		ReadBBITimeStructure reader = new ReadBBITimeStructure();
		
		reader.tabFileParserConfig = new TabularFileParserConfig();
		reader.tabFileParserConfig.setFileName(filename);
		reader.tabFileParserConfig.setDelimiterTags(new String[] {";"}); // \t
//		this.tabFileParserConfig.setDelimiterTags(new String[] {"D"});
		new TabularFileParser().parse(reader.tabFileParserConfig, reader);

		return reader.timeStructure;		
	}	

	@Override
	public void startRow(String[] row) {
		if(row[0].contains("#")){
			StringBuffer tempBuffer = new StringBuffer();
			for (String string : row) {
				tempBuffer.append(string);
				tempBuffer.append(", ");
			}
			log.info("Ignoring: " + tempBuffer);
		} else {
			this.timeStructure[Integer.parseInt(row[0])] = Double.parseDouble(row[1]);			
		}
		
	}
	
}