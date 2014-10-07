package playground.mmoyo.utils.counts;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;

/**
 * Reads stop counts in text files with this tabular structure 
 * StopNo StopName X Y E_real A_real U_real	E_sim A_sim	U_sim
 */

public class TabularCountReader2 implements TabularFileHandler {
	private static final Logger log = Logger.getLogger(TabularCountReader2.class);
	private static final String[] HEADER = {"StopNo", "StopName", "X", "Y", "E_2007", "A_2007", "U_2007", "E_sim", "A_sim",	"U_sim"};
	private final TabularFileParserConfig tabFileParserConfig;
	private boolean isFirstLine = true;
	private Counts counts = new Counts();

	final String EMPTY = "--";
	final String ZERO = "0.0";
	final String POINT = ".";

	public TabularCountReader2(final String countName, final String description,int year) {
		this.tabFileParserConfig = new TabularFileParserConfig();
		this.tabFileParserConfig.setDelimiterTags(new String[] {"\t"});
		counts.setName(countName);
		counts.setDescription(description);
		counts.setYear(year);
	}

	@Override
	public void startRow(final String[] row) throws IllegalArgumentException {
		if (!this.isFirstLine) {
			//For each station of tabular file
			String strStop =row[0];
			Id<Link> stopId =  Id.create(strStop, Link.class);
			String  str_cs_id= row[1];
			counts.createAndAddCount(stopId, str_cs_id);
			Count count = counts.getCount(stopId);
			Coord coord = new CoordImpl(row[2], row[3]);
			count.setCoord(coord); 
			count.createVolume(1, Double.parseDouble(row[5]));  //set the 24 hours volume to volume 1;
			//count.createVolume(1, Double.parseDouble(row[4]));  //boarding;
			//count.createVolume(1, Double.parseDouble(row[6]));  //alighting;
		}else{
			boolean equalsHeader = true;
			int i = 0;
			for (String s : row) {
				if (!s.equalsIgnoreCase(HEADER[i])){
					equalsHeader = false;
					break;
				}
				i++;
			}
			if (!equalsHeader) {
				log.warn("the structure does not match. The header should be:  ");
				for (String g : HEADER) {
					System.out.print(g + " ");
				}
				System.out.println();
			}
			this.isFirstLine = false;
		}
	}

	public void writeCounts (final String outputFile){
		new CountsWriter(this.counts).write(outputFile);
	}

	public void readFile(final String filename) throws IOException {
		this.tabFileParserConfig.setFileName(filename);
		new TabularFileParser().parse(this.tabFileParserConfig, this);
	}

	public static void main(String[] args) throws IOException {
		String tabularFile = "../../";
		TabularCountReader2 countReader = new TabularCountReader2("occupancy counts", "ptStopCounts.txt", 2012) ;
		countReader.readFile(tabularFile);
		countReader.writeCounts("../../");   //<-output file name
		System.out.println("done.");
	}
}