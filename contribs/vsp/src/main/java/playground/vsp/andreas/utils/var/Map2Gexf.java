package playground.vsp.andreas.utils.var;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.log4j.Logger;
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

/**
 * Transforms a given Map to a gexf network
 * 
 * @author aneumann
 *
 */
public class Map2Gexf extends MatsimJaxbXmlWriter{
	
	private static final Logger log = Logger.getLogger(Map2Gexf.class);
	
	private final static String xsdPath = "http://www.gexf.net/1.2draft/gexf.xsd";

	private ObjectFactory gexfFactory;
	private XMLGexfContent gexfContainer;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		String mapFile = "F:/test/test.txt";
		Map<String, List<String>> map = ReadMap.readMap(mapFile);
		
		Map2Gexf net2Gexf = new Map2Gexf();
		net2Gexf.addMapAsLayer(map, 0);
		
		net2Gexf.write("F:/test/test.gexf");
	}

	public void write(String filename) {
		log.info("writing output to " + filename);
		JAXBContext jc;
		try {
			jc = JAXBContext.newInstance(playground.vsp.gexf.ObjectFactory.class);
			Marshaller m = jc.createMarshaller();
			super.setMarshallerProperties(Map2Gexf.xsdPath, m);
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

	public Map2Gexf(){
		this.gexfFactory = new ObjectFactory();
		this.gexfContainer = this.gexfFactory.createXMLGexfContent();

		XMLGraphContent graph = this.gexfFactory.createXMLGraphContent();
		graph.setDefaultedgetype(XMLDefaultedgetypeType.DIRECTED);
		graph.setIdtype(XMLIdtypeType.STRING);
		graph.setMode(XMLModeType.DYNAMIC);
		graph.setTimeformat(XMLTimeformatType.INTEGER);
		this.gexfContainer.setGraph(graph);	
	}

	private void addMapAsLayer(Map<String, List<String>> map, int zCoord) {
		List<Object> attr = this.gexfContainer.getGraph().getAttributesOrNodesOrEdges();
		
		// nodes
		XMLNodesContent nodes = this.gexfFactory.createXMLNodesContent();
		attr.add(nodes);
		List<XMLNodeContent> nodeList = nodes.getNode();
		
		for (String node : map.keySet()) {
			XMLNodeContent n = this.gexfFactory.createXMLNodeContent();
			n.setId(node);
			n.setLabel(node);
			nodeList.add(n);
		}
		
		// edges
		XMLEdgesContent edges = this.gexfFactory.createXMLEdgesContent();
		attr.add(edges);
		List<XMLEdgeContent> edgeList = edges.getEdge();
		
		for (Entry<String, List<String>> source2TargetMap : map.entrySet()) {
			for (String target : source2TargetMap.getValue()) {
				XMLEdgeContent e = this.gexfFactory.createXMLEdgeContent();
				e.setId(source2TargetMap.getKey() + target);
				e.setLabel(source2TargetMap.getKey() + target);
				e.setSource(source2TargetMap.getKey());
				e.setTarget(target);
				
				edgeList.add(e);
			}
		}
		
	}

}
