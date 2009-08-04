package playground.mmoyo.deprecVersion;

import playground.mmoyo.PTRouter.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.Arrays;

import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.api.basic.v01.TransportMode;
/** 
 * Second version of parser for PTLines file. 
 * Used for PTCase2 with no upper (father) node describing the station but a set of nodes with prefix-sufixes
 * to represent each PTline lane in the station
 */
public class PTLinesReader extends MatsimXmlParser {
	private final static String PTLINES = "ptLines";
	private final static String LINE = "ptLine";
	private final static String NODE = "node";
	private final static String DEPARTURES = "departures";
	private Id idPTLine;
	private char type;
	private String direction;
	private List<Id> route;
	private List<Double> minutes;
	private List<String> departureList;
	private TransportMode transportMode;
	private List<PTLine> ptLineList;
	
	public PTLinesReader(List<PTLine> ptLineList) {
		this.ptLineList = ptLineList;
		
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
		if (LINE.equals(name)) {
			startLine(atts);
		} else if (NODE.equals(name)){
			startNode(atts);
		} else if (DEPARTURES.equals(name)){
			startDepartures();
		} 
	}

	@Override
	public void endTag(final String name, final String content,	final Stack<String> context) {
		if (PTLINES.equals(name)) {
			endLines();
		} else if (LINE.equals(name)) {
			endLine();
		} else if (NODE.equals(name)) {
			endNode(content);
		} else if (DEPARTURES.equals(name)){
			endDepartures(content);
		}
	}

	private void startLine(final Attributes atts) {
		idPTLine = new IdImpl(atts.getValue("id"));  //+ " " + atts.getValue("direction")
		type = atts.getValue("id").charAt(0); //-> Read from atts.getValue("type")
		transportMode = getTransportMode(type);
		direction =atts.getValue("direction");
		minutes= new ArrayList<Double>();
		route = new ArrayList<Id>();
		departureList = new ArrayList <String>(); 
	}
	
	private void startNode(final Attributes atts) {
		minutes.add(Double.parseDouble(atts.getValue("minute")));
	}
	
	private void startDepartures(){
	}
	
	private void endDepartures(String departures) {
		departureList = Arrays.asList(departures.split("[ \t\n]+"));
	}
	
	private void endNode(String strIdNode) {
		route.add(new IdImpl(strIdNode));
	}
	
	private void endLine() {	
		this.ptLineList.add(new PTLine(idPTLine, transportMode, direction, route, minutes, departureList));
	}

	private void endLines(){
		idPTLine = null;
		type = '\u0000'; //null
		direction =null;
		minutes= null;
		route = null;
		departureList = null;
	}
	
	//-> write this value directly in ptTimeTable file
	private TransportMode getTransportMode(char type){
		TransportMode transportMode = TransportMode.pt;
		if (type =='T'){
			transportMode = TransportMode.tram;
		}else if(type =='S'){
			transportMode = TransportMode.train;
		}
		return transportMode;
	}

}// class
