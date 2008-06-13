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

	private void endRoute() {
	}

	private void endLine() {
		this.ptLineList.add(new PTLine(idPTLine, "Bus", false, lineRoute));
		idPTLine = null;
		lineRoute = "";
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
