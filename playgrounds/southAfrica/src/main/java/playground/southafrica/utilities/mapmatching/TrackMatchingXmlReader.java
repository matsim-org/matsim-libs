package playground.southafrica.utilities.mapmatching;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import playground.southafrica.utilities.Header;

public class TrackMatchingXmlReader extends MatsimXmlParser {
	final private static Logger LOG = Logger.getLogger(TrackMatchingXmlReader.class);

	private final static String STATUS = "status";
	private final static String AUTHORIZED = "authorized";
	private final static String RESULT = "result";
	private final static String DIARY = "diary";
	private final static String ENTRIES = "entries";
	private final static String ENTRY = "entry";
	private final static String ROUTE = "route";
	private final static String LINK = "link";
	
	/* Attributes */
	private final static String ATTR_SOURCE = "src";
	private final static String ATTR_ID = "id";
	private final static String ATTR_DESTINATION = "dst";
	
	private List<List<Tuple<Id<Node>, Id<Node>>>> routeList = new ArrayList<>();
	private List<Tuple<Id<Node>, Id<Node>>> nodeList = null;
	private int routeCounter = 0;


	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if(STATUS.equals(name)){
			LOG.warn("Did not find a valid route for vehicle.");
		} else if(ROUTE.equals(name)){
			nodeList = new ArrayList<>();
		} else if(LINK.equals(name)){
			Id<Node> o = Id.createNodeId( atts.getValue(ATTR_SOURCE) );
			Id<Node> d = Id.createNodeId( atts.getValue(ATTR_DESTINATION) );
			Tuple<Id<Node>, Id<Node>> tuple = new Tuple<Id<Node>, Id<Node>>(o, d);
			nodeList.add(tuple);
		}
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if(ROUTE.equals(name)){
			routeList.add(nodeList);
			LOG.info(" --> Route " + ++routeCounter + " has " + nodeList.size() + " link record(s).");
		} else if(STATUS.equals(name) || RESULT.equals(name)){
			LOG.info(" Vehicle has " + routeList.size() + " usable route(s).");
		}
	}

	public static void main(String[] args) {
		Header.printHeader(TrackMatchingXmlReader.class.toString(), args);
		String filename = args[0];
		
		TrackMatchingXmlReader tmx = new TrackMatchingXmlReader();
		tmx.setValidating(false);
		tmx.parse(filename);
		tmx.getLargestRoute();
		
		Header.printFooter();
	}
	
	/**
	 * Gets the route with the largest number of link records. If there are 
	 * multiple routes with the same maximum length, only the first is 
	 * returned.
	 * @return
	 */
	public List<Tuple<Id<Node>, Id<Node>>> getLargestRoute(){
		List<Tuple<Id<Node>, Id<Node>>> largestList = new ArrayList<>();
		for(int i = 0; i < routeList.size(); i++){
			List<Tuple<Id<Node>, Id<Node>>> list = routeList.get(i);
			if(list.size() > largestList.size()){
				largestList = list;
			}
		}
		LOG.info("Largest route found, with " + largestList.size() + " link record(s).");
		return largestList;
	}
	
	
	public List<List<Tuple<Id<Node>, Id<Node>>>> getAllRoutes(){
		return this.routeList;
	}
	
	public List<Tuple<Id<Node>, Id<Node>>> consolidateRoutes(){
		List<Tuple<Id<Node>, Id<Node>>> list = new ArrayList<>();
		for(List<Tuple<Id<Node>, Id<Node>>> l : routeList){
			if(l.size() > 1){
				list.addAll(l);
			}
		}
		
		LOG.info("Total of " + list.size() + " records in consolidated list.");
		return list;
	}

}
