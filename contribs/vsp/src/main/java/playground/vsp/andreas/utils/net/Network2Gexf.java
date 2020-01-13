package playground.vsp.andreas.utils.net;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimJaxbXmlWriter;
import playground.vsp.gexf.ObjectFactory;
import playground.vsp.gexf.XMLDefaultedgetypeType;
import playground.vsp.gexf.XMLEdgeContent;
import playground.vsp.gexf.XMLEdgesContent;
import playground.vsp.gexf.XMLGexfContent;
import playground.vsp.gexf.XMLGraphContent;
import playground.vsp.gexf.XMLIdtypeType;
import playground.vsp.gexf.XMLModeType;
import playground.vsp.gexf.XMLNodeContent;
import playground.vsp.gexf.XMLNodesContent;
import playground.vsp.gexf.XMLTimeformatType;
import playground.vsp.gexf.viz.PositionContent;

public class Network2Gexf extends MatsimJaxbXmlWriter{
	
	private static final Logger log = Logger.getLogger(Network2Gexf.class);
	
	private final static String xsdPath = "http://www.gexf.net/1.2draft/gexf.xsd";

	private ObjectFactory gexfFactory;

	private XMLGexfContent gexfContainer;


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Network2Gexf net2Gexf = new Network2Gexf();
		
		Scenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		final Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile("F:/p_run/network_real.xml");
//		new MatsimNetworkReader(scenario).readFile("D:/berlin_bvg3/bvg_3_bln_inputdata/rev554B-bvg00-0.1sample/network/network.final.xml.gz");
		
		net2Gexf.addNetworkAsLayer(network, 0);
		
		net2Gexf.write("F:/p_run/gexf_out.gexf");

	}

	public void write(String filename) {
		log.info("writing output to " + filename);
		JAXBContext jc;
		try {
			jc = JAXBContext.newInstance(playground.vsp.gexf.ObjectFactory.class);
			Marshaller m = jc.createMarshaller();
			super.setMarshallerProperties(Network2Gexf.xsdPath, m);
			BufferedWriter bufout = IOUtils.getBufferedWriter(filename);
			m.marshal(this.gexfContainer, bufout);
			bufout.close();
			log.info(filename + " written successfully.");
		} catch (JAXBException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	public Network2Gexf(){
		this.gexfFactory = new ObjectFactory();
		this.gexfContainer = this.gexfFactory.createXMLGexfContent();

		XMLGraphContent graph = this.gexfFactory.createXMLGraphContent();
		graph.setDefaultedgetype(XMLDefaultedgetypeType.DIRECTED);
		graph.setIdtype(XMLIdtypeType.STRING);
		graph.setMode(XMLModeType.DYNAMIC);
		graph.setTimeformat(XMLTimeformatType.INTEGER);
		this.gexfContainer.setGraph(graph);	
	}

	private void addNetworkAsLayer(Network network, int zCoord) {
		List<Object> attr = this.gexfContainer.getGraph().getAttributesOrNodesOrEdges();
		
		// nodes
		XMLNodesContent nodes = this.gexfFactory.createXMLNodesContent();
		attr.add(nodes);
		List<XMLNodeContent> nodeList = nodes.getNode();
		
		for (Node node : network.getNodes().values()) {
			XMLNodeContent n = this.gexfFactory.createXMLNodeContent();
			n.setId(node.getId().toString());
			n.setLabel("network edge");
			
			playground.vsp.gexf.viz.ObjectFactory vizFac = new playground.vsp.gexf.viz.ObjectFactory();
			PositionContent pos = vizFac.createPositionContent();
			pos.setX((float) node.getCoord().getX());
			pos.setY((float) node.getCoord().getY());
			pos.setZ((float) zCoord);
			
//			XMLAttributeContent attContent = this.gexfFactory.createXMLAttributeContent();
//			XMLAttvalue xCoord = this.gexfFactory.createXMLAttvalue();
//			xCoord.setFor("xCoord");
//			xCoord.setValue(Double.toString(node.getCoord().getX()));
//			attContent.setId("test");
//			attContent.setTitle("titel");
//			attContent.setType(XMLAttrtypeType.STRING);
//			n.getAttvaluesOrSpellsOrNodes().add(attContent);
			
			n.getAttvaluesOrSpellsOrNodes().add(pos);
			
			nodeList.add(n);
		}
		
		// edges
		XMLEdgesContent edges = this.gexfFactory.createXMLEdgesContent();
		attr.add(edges);
		List<XMLEdgeContent> edgeList = edges.getEdge();
		
		for (Link link : network.getLinks().values()) {
			XMLEdgeContent e = this.gexfFactory.createXMLEdgeContent();
			e.setId(link.getId().toString());
			e.setLabel("network link");
			e.setSource(link.getFromNode().getId().toString());
			e.setTarget(link.getToNode().getId().toString());
			
			edgeList.add(e);
		}
		
	}

}
