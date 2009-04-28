package playground.mmoyo.input;

import java.io.IOException;
import java.util.Stack;
import org.matsim.core.utils.io.MatsimXmlParser;
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
}

