/**
 * 
 */
package playground.dgrether.koehlerstrehlersignal.solutionconverter;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import playground.dgrether.koehlerstrehlersignal.data.DgCrossing;
import playground.dgrether.koehlerstrehlersignal.data.DgCrossingNode;
import playground.dgrether.koehlerstrehlersignal.data.DgGreen;
import playground.dgrether.koehlerstrehlersignal.data.DgKSNetwork;
import playground.dgrether.koehlerstrehlersignal.data.DgProgram;
import playground.dgrether.koehlerstrehlersignal.data.DgStreet;

/**
 * @author tthunig
 */
public class KS2015NetworkXMLParser extends MatsimXmlParser {

	private static final Logger log = Logger.getLogger(KS2015NetworkXMLParser.class);
	
	// common tags
	private static final String ID = "id";
	private static final String NODE = "node";
	private static final String FROM = "from";
	private static final String TO = "to";
//	private static final String NETWORK = "network";
//	private static final String EXPANDED = "expanded";
//	private static final String NAME = "name";
//	private static final String DESCRIPTION = "description";
	// tags for the crossings element
	private static final String TYPE = "type";
//	private static final String CROSSINGS = "crossings";
	private static final String CROSSING = "crossing";
//	private static final String NODES = "nodes";
//	private static final String LIGHTS = "lights";
	private static final String LIGHT = "light";
//	private static final String PROGRAMS = "programs";
	private static final String PROGRAM = "program";
	private static final String CYCLE = "cycle";
	private static final String GREEN = "green";
	private static final String OFFSET = "offset";
	private static final String LENGTH = "length";
	private static final String XCOORD = "x";
	private static final String YCOORD = "y";
	// tags for the streets element
//	private static final String STREETS = "streets";
	private static final String STREET = "street";
	private static final String COST = "cost";
	private static final String CAPACITY = "capacity";
	
	private DgKSNetwork ksNet = new DgKSNetwork();
	
	private DgCrossing currentCrossing;
	private DgProgram currentProgram;
	private Map<Id<DgCrossingNode>, DgCrossingNode> allCrossingNodes = new HashMap<>();
	
	public void readFile(final String filename) {
		this.setValidating(false);
		parse(filename);
		log.info("Read ks network from file " + filename);
	}
	
	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (name.equals(CROSSING)){
			this.currentCrossing = new DgCrossing(Id.create(atts.getValue(ID),DgCrossing.class));
			this.currentCrossing.setType(atts.getValue(TYPE));
		}
		// sub tag "node" of crossing tag (not of commodity tag)
		if (name.equals(NODE) && !(atts.getValue(XCOORD) == null)){
			DgCrossingNode crossingNode = new DgCrossingNode(Id.create(atts.getValue(ID),DgCrossingNode.class));
			crossingNode.setCoordinate(new CoordImpl(atts.getValue(XCOORD), atts.getValue(YCOORD)));
			this.currentCrossing.addNode(crossingNode);
			this.allCrossingNodes.put(crossingNode.getId(),crossingNode);
		}
		if (name.equals(LIGHT)){
			DgCrossingNode fromNode = this.currentCrossing.getNodes().get(Id.create(atts.getValue(FROM), DgCrossingNode.class));
			DgCrossingNode toNode = this.currentCrossing.getNodes().get(Id.create(atts.getValue(TO), DgCrossingNode.class));
			DgStreet light = new DgStreet(Id.create(atts.getValue(ID), DgStreet.class), fromNode, toNode);
			this.currentCrossing.addLight(light);
		}
		if (name.equals(PROGRAM)){
			this.currentProgram = new DgProgram(Id.create(atts.getValue(ID), DgProgram.class));
			this.currentProgram.setCycle(Integer.parseInt(atts.getValue(CYCLE)));
			this.currentCrossing.addProgram(currentProgram);
		}
		if (name.equals(GREEN)){
			DgGreen green = new DgGreen(Id.create(atts.getValue(LIGHT), DgGreen.class));
			green.setOffset(Integer.parseInt(atts.getValue(OFFSET)));
			green.setLength(Integer.parseInt(atts.getValue(LENGTH)));
			this.currentProgram.addGreen(green);
		}
		if (name.equals(STREET)){
			DgCrossingNode fromNode = this.allCrossingNodes.get(Id.create(atts.getValue(FROM), DgCrossingNode.class));
			DgCrossingNode toNode = this.allCrossingNodes.get(Id.create(atts.getValue(TO), DgCrossingNode.class));
			DgStreet street = new DgStreet(Id.create(atts.getValue(ID), DgStreet.class), fromNode, toNode);
			street.setCost(Long.parseLong(atts.getValue(COST)));
			street.setCapacity(Double.parseDouble(atts.getValue(CAPACITY)));
			this.ksNet.addStreet(street);
		}
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		// nothing to do
	}
	
	public DgKSNetwork getKsNet(){
		return this.ksNet;
	}

}
