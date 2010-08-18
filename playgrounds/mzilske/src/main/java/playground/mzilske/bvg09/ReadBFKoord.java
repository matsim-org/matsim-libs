package playground.mzilske.bvg09;

import java.io.IOException;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

/**
 * Reads a HAFAS transit stop coordinates from Hafas BFKoord file 
 * 
 * @author aneumann
 *
 */
public class ReadBFKoord implements TabularFileHandler{

	private static final Logger log = Logger.getLogger(ReadBFKoord.class);
	
	private TabularFileParserConfig tabFileParserConfig;
	
	private HashMap<Id, Coord> stopCoords = new HashMap<Id, Coord>();
	private HashMap<Id, String> stopNames = new HashMap<Id, String>();
	
	public void readBFKoord(String filename) throws IOException {
		
		this.tabFileParserConfig = new TabularFileParserConfig();
		this.tabFileParserConfig.setFileName(filename);
		this.tabFileParserConfig.setDelimiterTags(new String[] {" "}); // \t
//		this.tabFileParserConfig.setDelimiterTags(new String[] {"D"});
		new TabularFileParser().parse(this.tabFileParserConfig, this);
		log.info("Finished parsing.");
				
	}
	
	public HashMap<Id, Coord> getStopCoords(){
		return this.stopCoords;
	}
	
	public HashMap<Id, String> getStopNames(){
		return this.stopNames;
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
			this.stopCoords.put(new IdImpl(row[0]), new CoordImpl(row[1], row[2]));
			StringBuffer sB = new StringBuffer();
			for (int i = 3; i < row.length; i++) {
				sB.append(row[i]);
				sB.append(" ");
			}
			this.stopNames.put(new IdImpl(row[0]), sB.toString().trim());			
		}
		
	}
	
}