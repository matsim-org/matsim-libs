package playground.mmoyo.input;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.network.BasicNode;
import org.matsim.core.basic.v01.BasicNodeImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

 /**
 * Parses the xml file with simple node description to create PTNodes
 */
public class PTNodeReader extends MatsimXmlParser{
	private final static String NODE = "node";
	private final static String LINE = "ptLine";
	String strPTLine;
	List<BasicNode> nodeList;
	Map <String, List<BasicNode>> lineMap = new TreeMap <String, List<BasicNode>>();
	
	public PTNodeReader(){
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
		if (NODE.equals(name)){
			startNode(atts);
		}else if (LINE.equals(name)){
			startLine(atts);
		}
	}

	@Override
	public void endTag(final String name, final String content,	final Stack<String> context) {
		if (NODE.equals(name)) {
			endNode();
		}else if (LINE.equals(name)){
			endLine();
		}
	}

	private void startNode(final Attributes atts) {
		Id idNode = new IdImpl(atts.getValue("id"));
		double x= Double.parseDouble(atts.getValue("x"));
		double y= Double.parseDouble(atts.getValue("y"));
		Coord coord = new CoordImpl(x,y);
		BasicNode node = new BasicNodeImpl(idNode, coord);
		nodeList.add(node);
	}
	
	private void endNode() {
		
	}
	
	private void startLine(final Attributes atts){
		nodeList = new ArrayList<BasicNode>();
		strPTLine = atts.getValue("id");
	}
	
	private void endLine(){
		System.out.print("endLine: ");
		lineMap.put(strPTLine, nodeList);
		
		System.out.println();
		for (BasicNode basicNode: nodeList){
			System.out.print(basicNode.getId().toString() + " ");
		}
	}

	
	
}