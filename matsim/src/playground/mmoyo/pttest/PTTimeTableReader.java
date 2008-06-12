package playground.mmoyo.pttest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.basic.v01.IdImpl;
import org.matsim.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class PTTimeTableReader extends MatsimXmlParser {
	private final static String SCHEDULE = "schedule";
	private final static String NODE = "node";
	private final static String PTLINE = "ptLine";
	private final static String DEPARTURE = "departure";
	
	private String strDeparture= "";
	private String idnode="";
	private String idPTLine="";
	//we will have a single file PTLines reader with schedules in the future
	//private String lineRoute = "";
	//public List<PTLine> ptLineList = new ArrayList<PTLine>();
	//public List<PTLine> ptNodeSchedule = new ArrayList<PTLine>();
	
	//public List<String,String,String> timetable = new ArrayList<>();
	public List<PTTimeTable> timetable = new ArrayList<PTTimeTable>();
	
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
		} else if (SCHEDULE.equals(name)) {
			startSchedule(atts);
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
		idnode=atts.getValue("id");
	}

	private void startPTLine(final Attributes atts) {
		idPTLine= atts.getValue("id");
	}

	private void startSchedule (final Attributes atts) {
	}
	//////////////////////////////////////////////////////

	//////////// End methods/////////////////////////////
	private void endNodes(){
	}
	
	private void endNode() {
	}

	private void endPTLine(){
		timetable.add(new PTTimeTable(new IdImpl(idnode), new IdImpl(idPTLine), Departures(strDeparture)));
		strDeparture = "";
	}

	private void endSchedule () {
	}

	@Override
	public void characters(char ch[], int start, int length) {
		for (int i = start; i < start + length; i++) {
			strDeparture = strDeparture + ch[i];
		}
	}
	
	private int[] Departures(String dep){
		String[] strDep = dep.split(" ");
		int [] intDep= new int[strDep.length];
		for (int x= 0; x < strDep.length; x++){
			intDep[x] = ToSeconds(strDep[x]);
		}
		return intDep;
	}//Fill
	
	private int ToSeconds(String strDeparture){
		String[] strTime = strDeparture.split(":");  //if we had seconds:  + (departure + ":00").split(":");   //
		return ((Integer.parseInt(strTime[0]) * 3600) + (Integer.parseInt(strTime[1]))*60) ;  	////if we had seconds:   + Integer.parseInt(strTime[2] 
	}
	
	
}// class