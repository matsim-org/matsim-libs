package playground.andreas.utils.ana.point2kml;

import java.io.IOException;
import java.util.LinkedList;

import org.apache.log4j.Logger;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

/**
 * Read a file with xy-coord in the first two, headline in the third and description in the following columns
 *
 * @author aneumann
 *
 */
public class ReadXYPoints implements TabularFileHandler{

	private static final Logger log = Logger.getLogger(ReadXYPoints.class);
	
	private TabularFileParserConfig tabFileParserConfig;
	private LinkedList<XYPointData> results = new LinkedList<XYPointData>();
	
	public static LinkedList<XYPointData> readXYPointData(String filename) throws IOException {
		
		ReadXYPoints reader = new ReadXYPoints();
		
		reader.tabFileParserConfig = new TabularFileParserConfig();
		reader.tabFileParserConfig.setFileName(filename);
		reader.tabFileParserConfig.setDelimiterTags(new String[] {";"}); // \t
//		this.tabFileParserConfig.setDelimiterTags(new String[] {"D"});
		new TabularFileParser().parse(reader.tabFileParserConfig, reader);

		return reader.results;		
	}	

	@Override
	public void startRow(String[] row) {
		if(row[0].contains("X")){
			StringBuffer tempBuffer = new StringBuffer();
			for (String string : row) {
				tempBuffer.append(string);
				tempBuffer.append(", ");
			}
			log.info("Ignoring: " + tempBuffer);
		} else {
			XYPointData xyPointData = new XYPointData();
			xyPointData.setCoord(new CoordImpl(Double.parseDouble(row[0]), Double.parseDouble(row[1])));
			xyPointData.setHeadline(row[2]);
			StringBuffer strB = new StringBuffer();
			for (int i = 3; i < row.length; i++) {
				strB.append(row[i]);
				strB.append(", ");
			}
			xyPointData.setDescription(strB.toString());
			this.results.add(xyPointData);
		}		
	}	
}