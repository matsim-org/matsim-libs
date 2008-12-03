package playground.dressler.ea_flow;
import java.io.IOException;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkWriter;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.CoordImpl;

public class CMCFNetworkConverter {
	
	
	
	@SuppressWarnings("unchecked")
	public static NetworkLayer readCMCFNetwork(String filename) throws JDOMException, IOException{
		NetworkLayer result = new NetworkLayer();
		SAXBuilder builder = new SAXBuilder();
		Document cmcfGraph = builder.build(filename);
		Element basegraph = cmcfGraph.getRootElement();
		// read and set the nodes
		Element nodes = basegraph.getChild("nodes");
		 List<Element> nodelist = nodes.getChildren();
		 for (Element node : nodelist){
			 //read the values of the node xml Element as Strings
			 String id = node.getAttributeValue("id");
			 String x = node.getAttributeValue("x");
			 String y = node.getAttributeValue("y");
			 //build a new node in the NetworkLayer
			 Coord coord = new CoordImpl(x,y);
			 Id matsimid  = new IdImpl(id);
			 result.createNode(matsimid, coord);
		 }
		 //read the edges
		 Element edges = basegraph.getChild("edges");
		 List<Element> edgelist = edges.getChildren();
		 for (Element edge : edgelist){
			//read the values of the edge xml Element as Strings
			 String id = edge.getAttributeValue("id");
			 String from = edge.getChildText("from");
			 String to	= edge.getChildText("to");
			 String length = edge.getChildText("length");
			 String capacity = edge.getChildText("capacity");
			 //build a new edge in 
			 Id matsimid  = new IdImpl(id);
			 //TODO free speed is set to 1.3 find something better
			 result.createLink(matsimid, result.getNode(from), result.getNode(to),
					 Double.parseDouble(length),
					  1.3 ,
					 Double.parseDouble(capacity),
					 1.);
		 }
			
		return result;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) { 
		if(args.length == 0 && args.length > 2){
			System.out.println("usage: 1. argument inputfile 2. argument outfile (optional)");
			return;
		}
		String inputfile = args[0].trim();
		String outfile = inputfile.substring(0, inputfile.length()-4)+"_msimNW.xml";
		if(args.length == 2){
			outfile = args[1];
		}
		try {
			NetworkLayer network = readCMCFNetwork(inputfile);
			NetworkWriter writer = new NetworkWriter( network, outfile);
			writer.write();
			System.out.println(inputfile+"  conveted successfully \n"+"output written in: "+outfile);
		} catch (JDOMException e) {
			e.printStackTrace();
			
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		

	}

}
