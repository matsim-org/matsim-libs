package playground.mmoyo.utils;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.pt.transitSchedule.api.TransitRoute;

import playground.mmoyo.analysis.counts.reader.TabularCountReader;

/**Parses a tabular text file with two columns representing the relation of old a new routes ids*/
public class TabularRouteFile_reader implements TabularFileHandler {
	private static final Logger log = Logger.getLogger(TabularCountReader.class);
	private static final String[] HEADER = {"oldRoute", "newRoute", "similarity"};
	private final TabularFileParserConfig tabFileParserConfig;
	private int rowNum=0;
	Map <Id<TransitRoute>, String> routeMap = new TreeMap <>();
	
	public TabularRouteFile_reader(){
		this.tabFileParserConfig = new TabularFileParserConfig();
		this.tabFileParserConfig.setDelimiterTags(new String[] {"\t"});
	}
	
	public void readFile(final String tabCountFile) throws IOException {
		this.tabFileParserConfig.setFileName(tabCountFile);
		new TabularFileParser().parse(this.tabFileParserConfig, this);
	}
	
	@Override
	public void startRow(String[] row) {
		if (rowNum>0) {
			Id<TransitRoute> oldRouteId = Id.create(row[0], TransitRoute.class); //
			String newRouteId = row[1]; 
			routeMap.put(oldRouteId, newRouteId);
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
	
	public Map <Id<TransitRoute>, String> getRouteMap (){
		return this.routeMap;
	}
	
	public static void main(String[] args) {
		TabularRouteFile_reader tabularRouteFile_reader= new TabularRouteFile_reader();
		try {
			tabularRouteFile_reader.readFile("../../");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		Map <Id<TransitRoute>, String> old_new_route_Map = tabularRouteFile_reader.getRouteMap(); 
		
		Map <String, Integer> new_route_Times_Map = new TreeMap <String, Integer>();;
		for ( String  newRoute : old_new_route_Map.values() ){
			if (!new_route_Times_Map.containsKey(newRoute)){
				new_route_Times_Map.put(newRoute, 1);
			}else{
				int value= new_route_Times_Map.get(newRoute);
				value++;
				new_route_Times_Map.put(newRoute, value);
			}
		}
		
		for(Map.Entry <String, Integer> entry: new_route_Times_Map.entrySet() ){
			String key = entry.getKey();
			Integer value = entry.getValue();
			if(value>1){
				System.out.println(key + "\t" + value);
			}
		}
	
		
	}
}
