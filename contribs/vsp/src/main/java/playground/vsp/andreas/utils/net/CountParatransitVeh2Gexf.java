package playground.vsp.andreas.utils.net;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimJaxbXmlWriter;

import playground.vsp.gexf.ObjectFactory;
import playground.vsp.gexf.XMLAttributeContent;
import playground.vsp.gexf.XMLAttributesContent;
import playground.vsp.gexf.XMLAttrtypeType;
import playground.vsp.gexf.XMLAttvalue;
import playground.vsp.gexf.XMLAttvaluesContent;
import playground.vsp.gexf.XMLClassType;
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

public class CountParatransitVeh2Gexf extends MatsimJaxbXmlWriter {
	
	private static final Logger log = Logger.getLogger(CountParatransitVeh2Gexf.class);
	
	private final static String xsdPath = "http://www.gexf.net/1.2draft/gexf.xsd";

	private ObjectFactory gexfFactory;

	private XMLGexfContent gexfContainer;

	private HashMap<Id,XMLEdgeContent> edgeMap;

	private HashMap<Id,XMLAttvaluesContent> attValueContentMap;


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		CountParatransitVeh2Gexf net2Gexf = new CountParatransitVeh2Gexf();
		
		Scenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		final Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile("F:/p/network_real.xml");
//		new MatsimNetworkReader(scenario).readFile("D:/berlin_bvg3/bvg_3_bln_inputdata/rev554B-bvg00-0.1sample/network/network.final.xml.gz");
		
		net2Gexf.addNetworkAsLayer(network, 0);
		net2Gexf.createAttValues();
		
		for (int i = 0; i <= 100; i++) {
			net2Gexf.handleEvents("abbacddct", "F:/p/", i, "p_");
		}
		
		net2Gexf.write("F:/p/gexf_out_abbacddct.gexf");

	}

	private void createAttValues() {
		this.attValueContentMap = new HashMap<Id, XMLAttvaluesContent>();
		
		for (Entry<Id, XMLEdgeContent> entry : this.edgeMap.entrySet()) {
			XMLAttvaluesContent attValueContent = new XMLAttvaluesContent();
			entry.getValue().getAttvaluesOrSpellsOrColor().add(attValueContent);
			this.attValueContentMap.put(entry.getKey(), attValueContent);
		}		
	}

	private void handleEvents(String runName, String path, int iteration, String paratransitCode) {
		try {
			CountParatransitVehicleHandler handler = new CountParatransitVehicleHandler(paratransitCode);
			
			EventsManager eventsManager = EventsUtils.createEventsManager();
			eventsManager.addHandler(handler);
			EventsReaderXMLv1 eventsReader = new EventsReaderXMLv1(eventsManager);
			eventsReader.readFile(path + runName + "/ITERS/it." + iteration + "/" + runName +"." + iteration + ".events.xml.gz");
			
			addValuesToGexf(iteration, handler);
			
		} catch (Exception e) {
			// TODO: handle exception
		}		
		
		
	}

	private void addValuesToGexf(int iteration, CountParatransitVehicleHandler handler) {
		
		
		for (Entry<Id, XMLAttvaluesContent> entry : this.attValueContentMap.entrySet()) {
						
			XMLAttvalue attValue = new XMLAttvalue();
			attValue.setFor("weight");
			attValue.setValue(Integer.toString(Math.max(1, handler.getCountForLinkId(entry.getKey()))));
			attValue.setStart(Double.toString(iteration));
//			attValue.setEndopen(Integer.toString(iteration + 1));
			
			entry.getValue().getAttvalue().add(attValue);
		}
		
	}

	public void write(String filename) {
		log.info("writing output to " + filename);
		JAXBContext jc;
		try {
			jc = JAXBContext.newInstance(playground.vsp.gexf.ObjectFactory.class);
			Marshaller m = jc.createMarshaller();
			super.setMarshallerProperties(CountParatransitVeh2Gexf.xsdPath, m);
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

	public CountParatransitVeh2Gexf(){
		this.gexfFactory = new ObjectFactory();
		this.gexfContainer = this.gexfFactory.createXMLGexfContent();

		XMLGraphContent graph = this.gexfFactory.createXMLGraphContent();
		graph.setDefaultedgetype(XMLDefaultedgetypeType.DIRECTED);
		graph.setIdtype(XMLIdtypeType.STRING);
		graph.setMode(XMLModeType.DYNAMIC);
		graph.setTimeformat(XMLTimeformatType.DOUBLE);
		this.gexfContainer.setGraph(graph);
		
		XMLAttributesContent attsContent = new XMLAttributesContent();
		attsContent.setClazz(XMLClassType.EDGE);
		attsContent.setMode(XMLModeType.DYNAMIC);
		
		XMLAttributeContent attContent = new XMLAttributeContent();
		attContent.setId("weight");
		attContent.setTitle("Number of paratransit vehicles per day");
		attContent.setType(XMLAttrtypeType.FLOAT);
		
		attsContent.getAttribute().add(attContent);		
		this.gexfContainer.getGraph().getAttributesOrNodesOrEdges().add(attsContent);
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
//			n.setLabel("network edge");
			
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
		
		this.edgeMap = new HashMap<Id, XMLEdgeContent>();
		
		for (Link link : network.getLinks().values()) {
			
			if(link.getFromNode().getId().toString().equalsIgnoreCase(link.getToNode().getId().toString())){
				log.info("Omitting link " + link.getId().toString() + " Gephi cannot display edges with the same to and fromNode, yet, Sep'11");
			} else {
				XMLEdgeContent e = this.gexfFactory.createXMLEdgeContent();
				e.setId(link.getId().toString());
				e.setLabel("network link");
				e.setSource(link.getFromNode().getId().toString());
				e.setTarget(link.getToNode().getId().toString());
				e.setWeight(new Float(1.0));

				edgeList.add(e);

				edgeMap.put(link.getId(), e);
			}
		}
		
	}

}
