package playground.mmoyo.pttest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.basic.v01.IdImpl;
import org.matsim.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class PTLinesReader extends MatsimXmlParser {
	private final static String PTLINES = "ptLines";
	private final static String LINE = "ptLine";
	private final static String ROUTE = "route";

	private IdImpl idPTLine;
	private String lineRoute = "";
	public List<PTLine> ptLineList = new ArrayList<PTLine>();

	public PTLinesReader() {
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
		if (PTLINES.equals(name)) {
			startLines();
		} else if (LINE.equals(name)) {
			startLine(atts);
		} else if (ROUTE.equals(name)) {
			startRoute(atts);
		}
	}

	@Override
	public void endTag(final String name, final String content,	final Stack<String> context) {
		if (PTLINES.equals(name)) {
			endLines();
		} else if (LINE.equals(name)) {
			endLine();
		} else if (ROUTE.equals(name)) {
			endRoute();
		}
	}

	private void startLines() {
	}

	private void startLine(final Attributes atts) {
		idPTLine = new IdImpl(atts.getValue("id"));
	}

	private void startRoute(final Attributes atts) {
	}

	private void endRoute() {
	}

	private void endLine() {
		PTLine ptLine = new PTLine(idPTLine, "Bus", false, lineRoute);
		this.ptLineList.add(ptLine);
		idPTLine = null;
		lineRoute = "";
		ptLine = null;
	}

	private void endLines() {
		idPTLine = null;
		lineRoute = null;
	}

	@Override
	public void characters(char ch[], int start, int length) {
		for (int i = start; i < start + length; i++) {
			lineRoute = lineRoute + ch[i];
		}
	}
}// class

/*
 * Another version to validate chars 
 * public void characters (char ch[], int  start, int length) { 
 * 		for (int i = start; i < start + length; i++) 
 * 	{ switch (ch[i]) 
 * { case '\\': System.out.print("\\\\"); break; 
 * case '"':  System.out.print("\\\""); break; 
 * case '\n': System.out.print("\\n"); break;
 * case '\r': System.out.print("\\r"); break; 
 * case '\t':
 * System.out.print("\\t"); break; default: System.out.print(ch[i]); break;
 * }
 * switch } }
 */