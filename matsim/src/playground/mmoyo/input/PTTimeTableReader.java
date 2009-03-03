package playground.mmoyo.input;

import java.io.IOException;
import java.util.Stack;

import org.matsim.basic.v01.IdImpl;
import org.matsim.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException;
import playground.mmoyo.PTRouter.PTTimeTable;
/** 
 * Parses the xml file with the information of departures
 */
public class PTTimeTableReader extends MatsimXmlParser {
	private final static String SCHEDULE = "schedule";
	private final static String NODE = "node";
	private final static String PTLINE = "ptLine";
	private final static String DEPARTURE = "departure";
	
	private StringBuffer bufferDeparture = null;
	private String strIdNode="";
	private String strIdPTLine="";
	
	//private Map <IdImpl, Map<IdImpl,int[]>> nodeTimeTableMap = new TreeMap <IdImpl, Map<IdImpl,int[]>>();
	IdImpl idNode = null;
	private PTTimeTable ptTimeTable = new PTTimeTable();
	
	public PTTimeTableReader() {
		super();
	}
	
	public void readFile(final String filename) {
		try {
			parse(filename);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		if (DEPARTURE.equals(name)) {
			startNodes();
		} else if (NODE.equals(name)) {
			startNode(atts);
		} else if (PTLINE.equals(name)) {
			startPTLine(atts);
		}	
	}

	@Override
	public void endTag(final String name, final String content,	final Stack<String> context) {
		if (DEPARTURE.equals(name)) {
			endNodes();
		} else if (NODE.equals(name)) {
			endNode();
		} else if (PTLINE.equals(name)) {
			endPTLine();
		} else if (SCHEDULE.equals(name)) {
			endSchedule();
		}
	}

	/////////Start methods//////////////////////////////
	private void startNodes() {
	}

	private void startNode(final Attributes atts) {
		strIdNode=atts.getValue("id");
	}

	private void startPTLine(final Attributes atts) {
		strIdPTLine= atts.getValue("id");
	}

	//////////// End methods/////////////////////////////
	private void endNodes(){
	}
	
	private void endNode() {
	}

	private void endPTLine(){
	    //addTimeTable(strIdNode, strIdPTLine, bufferDeparture.toString());
	    this.ptTimeTable.addDepartures(strIdNode, strIdPTLine, bufferDeparture.toString());
	    bufferDeparture=null;
	}

	private void endSchedule () {
	}
	
	@Override
	public void characters(char ch[], int start, int length) {
		bufferDeparture = new StringBuffer(length);
		for (int i = start; i < start + length; i++) {
			bufferDeparture.append(ch[i]);
		}
	}
	
	public PTTimeTable getTimeTable(){
		return this.ptTimeTable;
	}
}// class


/////////////OLD CODE///////////////////////////////////////
/*private void addTimeTable(String idnode, String idPTLine, String departure){  
*	idNode = new IdImpl(idnode);	
*	
*	if (!nodeTimeTableMap.containsKey(idNode)){
*		Map<IdImpl,int[]> map2 = new TreeMap <IdImpl,int[]>();
*		map2.put(new IdImpl(idPTLine), departuresToArray(departure));
*		nodeTimeTableMap.put(idNode,  map2);
*	}else{
*		nodeTimeTableMap.get(idNode).put(new IdImpl(idPTLine), departuresToArray(departure));
*	}	
*	idNode= null;
*	}
*	private int[] departuresToArray(String dep){
*	String[] strDep = dep.split(" ");
*	int [] intDep= new int[strDep.length];
*	for (int x= 0; x < strDep.length; x++){
*		intDep[x] = toSeconds(strDep[x]);
*	}
*	return intDep;
*}
*	
*	private int toSeconds(String strDeparture){
*	String[] strTime = strDeparture.split(":");  //if we had seconds:  + (departure + ":00").split(":");   //
*	return ((Integer.parseInt(strTime[0]) * 3600) + (Integer.parseInt(strTime[1]))*60) ;  	////if we had seconds:   + Integer.parseInt(strTime[2] 
*}

/*
public Map <IdImpl, Map<IdImpl,int[]>> getTimeTable(){
	return this.nodeTimeTableMap;
}
*/
