package playground.andreas.osmBB.rohdata;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

public class ReadRohData implements TabularFileHandler{
	
private static final Logger log = Logger.getLogger(ReadRohData.class);
	
	private TabularFileParserConfig tabFileParserConfig;
	private TreeMap<String, TreeMap<String, ArrayList<RohDataBox>>> countId2DataBoxListMap = new TreeMap<String, TreeMap<String, ArrayList<RohDataBox>>>();
	private TreeMap<String, ArrayList<RohDataBox>> tempList;

	public static TreeMap<String, TreeMap<String, ArrayList<RohDataBox>>> readCountDataForWeek(String folderName) {
		
		ReadRohData countDataReader = new ReadRohData();
		File[] files = new File(folderName).listFiles();
		
		for (int fileInList = 0; fileInList < files.length; fileInList++){
			
			String filename = files[fileInList].toString();					
			countDataReader.tabFileParserConfig = new TabularFileParserConfig();
			countDataReader.tabFileParserConfig.setFileName(filename);
			countDataReader.tabFileParserConfig.setDelimiterTags(new String[] {"\t"}); // \t
//			this.tabFileParserConfig.setDelimiterTags(new String[] {"D"});
			
			countDataReader.tempList = new TreeMap<String, ArrayList<RohDataBox>>();
			try {
				new TabularFileParser().parse(countDataReader.tabFileParserConfig, countDataReader);
				
				String csName = filename.split("_")[1].split("\\.")[0];
				
				if(countDataReader.countId2DataBoxListMap.get(csName) == null){
					countDataReader.countId2DataBoxListMap.put(csName, countDataReader.tempList);
				}				
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				if(e instanceof FileNotFoundException){
					log.info("File not found: " + filename);
				} else {
					e.printStackTrace();
				}
			}
		}
		
		return countDataReader.countId2DataBoxListMap;
	}
	
	public void startRow(String[] row) throws IllegalArgumentException {
		
		if(row[0].contains("Zeit")){
			//ignore
			StringBuffer tempBuffer = new StringBuffer();
			for (String string : row) {
				tempBuffer.append(string);
				tempBuffer.append(", ");
			}
			log.info("Ignoring: " + tempBuffer);
		} else if(row.length == 7){
			// complete row			
			String[] dateTime = row[0].split(" ");
			String date = dateTime[0].replace("\"", "");
			String time;
			if(dateTime.length > 1){
				time = dateTime[1].replace("\"", "");
			} else {
				time = "00:00:00";
			}
			
			RohDataBox rohDataBox = new RohDataBox(date, time, true);
			
			rohDataBox.setDtvKfz(Integer.parseInt(row[1]));
			rohDataBox.setDtvLkw(Integer.parseInt(row[2]));
			rohDataBox.setDtvPkw(Integer.parseInt(row[3]));
			rohDataBox.setvKfz(Integer.parseInt(row[4]));
			rohDataBox.setvLkw(Integer.parseInt(row[5]));
			rohDataBox.setvPkw(Integer.parseInt(row[6]));
			
			if(this.tempList.get(date) == null){
				this.tempList.put(date, new ArrayList<RohDataBox>());
			}
			
			this.tempList.get(date).add(rohDataBox);
				
		} else {
			// empty row			
			String[] dateTime = row[0].split(" ");
			String date = dateTime[0].replace("\"", "");
			String time;
			if(dateTime.length > 1){
				time = dateTime[1].replace("\"", "");
			} else {
				time = "00:00:00";
			}
			RohDataBox rohDataBox = new RohDataBox(date, time, false);
			
			rohDataBox.setDtvKfz(0);
			rohDataBox.setDtvLkw(0);
			rohDataBox.setDtvPkw(0);
			rohDataBox.setvKfz(0);
			rohDataBox.setvLkw(0);
			rohDataBox.setvPkw(0);	
			
			if(this.tempList.get(date) == null){
				this.tempList.put(date, new ArrayList<RohDataBox>());
			}
			
			this.tempList.get(date).add(rohDataBox);
		}
		
	}

}
