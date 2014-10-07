package playground.mmoyo.utils.counts;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.pt.transitSchedule.api.TransitLine;

import playground.mmoyo.analysis.counts.reader.TabularCountReader;

/**
 * Reads counts file in tabular format (see variable HEADER)
 */
public class TabularCount_reader implements TabularFileHandler {
	private static final Logger log = Logger.getLogger(TabularCountReader.class);
	private static final String[] HEADER = {"Linienname", "Richtung", "Fahrzeitprofil-Index", "Haltestellennummer", "Haltestellenname", "Belastung_soll", "Belastung_ist"};
	private final TabularFileParserConfig tabFileParserConfig;
	private int rowNum=0;
	public Map <Integer, TabularCountRecord> countRecordMap = new TreeMap <Integer, TabularCountRecord>();
	
	public TabularCount_reader(){
		this.tabFileParserConfig = new TabularFileParserConfig();
		this.tabFileParserConfig.setDelimiterTags(new String[] {"\t"});
	}
	
	public void readFile(final String tabCountFile) throws IOException {
		//rowNum= 0;
		this.tabFileParserConfig.setFileName(tabCountFile);
		new TabularFileParser().parse(this.tabFileParserConfig, this);
	}
	
	@Override
	public void startRow(String[] row) {
		if (rowNum>0) {
			Id<TransitLine> id = Id.create(row[0], TransitLine.class); //line
			char direction = row[1].charAt(0); //direction
			String stop = row[3]; //stop
			double count = Double.parseDouble(row[4]);  //the file does not contain "haltestellnummer", so this is column 4
			TabularCountRecord countRecord = new TabularCountRecord(id, direction, stop, count);
			countRecordMap.put(rowNum, countRecord);
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
		}
		rowNum++;
	}
	
	
	protected Map<Integer, TabularCountRecord> getCountRecordMap() {
		return countRecordMap;
	}

	public static void main(String[] args) throws IOException {
		String filePath= "../../bvg.run189.10pct.100.ptLineCounts.txt";
		TabularCount_reader tabCount_reader= new TabularCount_reader();
		tabCount_reader.readFile(filePath);
		
		final String sp = " ";
		for (TabularCountRecord countRecord : tabCount_reader.countRecordMap.values()){
			System.out.println(countRecord.getLineId() + sp + countRecord.getDirection() + sp + countRecord.getStop() + sp + countRecord.getCount());
		}
	
	}
	
}
