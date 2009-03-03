package playground.mmoyo.input;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.basic.v01.IdImpl;
import org.matsim.interfaces.basic.v01.BasicNode;

import org.matsim.basic.v01.BasicNodeImpl;
import org.matsim.utils.io.MatsimXmlParser;
import org.matsim.utils.geometry.CoordImpl;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.basic.v01.Id;


 /**
 * Parses the xml file with simple node description to create PTNodes
 */
public class PTNodeReader extends MatsimXmlParser{
	private final static String NODE = "node";
	private char type;
	List<BasicNode> nodeList = new ArrayList<BasicNode>();
	
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
		}
	}

	@Override
	public void endTag(final String name, final String content,	final Stack<String> context) {
		if (NODE.equals(name)) {
			endNode(content);
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
	
	private void endNode(String idNode) {

	}
	
	public List<BasicNode> getNodes(){
		return this.nodeList;
	}
	
}