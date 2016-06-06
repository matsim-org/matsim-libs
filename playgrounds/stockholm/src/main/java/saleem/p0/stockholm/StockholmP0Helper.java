package saleem.p0.stockholm;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkImpl;

import au.com.bytecode.opencsv.CSVReader;

public class StockholmP0Helper {
	NetworkImpl network;
	Map<Id<Link>,Link> alllinks;
	public StockholmP0Helper(NetworkImpl network){
		this.network=network;
		this.alllinks = network.getLinks();
	}
	public List<String> getPretimedNodes(String path){
		List<String> timednodes = new ArrayList<String>();
		try {
			CSVReader csvr = new CSVReader(new FileReader(path));
			String[] row;
			try {
				while((row = csvr.readNext()) != null)
				{
					if(row.length==1){//If CSV format is actually an excel file
						row = row[0].split(",");
					}
					if(row[8].equals("Pretimed")||row[8].contains("Pretimed")) {
						timednodes.add(row[0]);
					}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return timednodes;
	}
	public Map<String, List<Link>> getInLinksForJunctions(List<String> timednodes, NetworkImpl network){
		Map<String, List<Link>> incominglinks = new HashMap<String, List<Link>>();
		Iterator<String> tnodesiter = timednodes.iterator();
		while(tnodesiter.hasNext()) {
			incominglinks.put(tnodesiter.next(), new ArrayList<Link>());
		}
		Map<Id<Link>,Link> alllinks = network.getLinks();
		Iterator<Id<Link>> alllinksiter = alllinks.keySet().iterator();
		while(alllinksiter.hasNext()){
			Link link = alllinks.get(alllinksiter.next());
			if(timednodes.contains(link.getToNode().getId().toString())){
				incominglinks.get(link.getToNode().getId().toString()).add(link);
			}
		}
		return incominglinks;
	}
	public Map<String, List<Link>> getOutLinksForJunctions(List<String> timednodes, NetworkImpl network){
		Map<String, List<Link>> outgoinglinks = new HashMap<String, List<Link>>();
		Iterator<String> tnodesiter = timednodes.iterator();
		while(tnodesiter.hasNext()) {
			outgoinglinks.put(tnodesiter.next(), new ArrayList<Link>());
		}
		Iterator<Id<Link>> alllinksiter = alllinks.keySet().iterator();
		while(alllinksiter.hasNext()){
			Link link = alllinks.get(alllinksiter.next());
			if(timednodes.contains(link.getFromNode().getId().toString())){
				outgoinglinks.get(link.getFromNode().getId().toString()).add(link);
			}
		}
		return outgoinglinks;
	}
}
