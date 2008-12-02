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
import org.matsim.network.NetworkReader;
import org.matsim.network.NetworkReaderMatsimV1;
import org.matsim.network.NetworkWriter;
import org.matsim.network.Node;
import org.matsim.population.Person;
import org.matsim.population.PersonImpl;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.PopulationWriter;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.CoordImpl;

public class CMCFPopulationConverter {
	

	@SuppressWarnings("unchecked")
	public static Population readCMCFDemands(String filename, NetworkLayer network) throws JDOMException, IOException{
		Population result = new Population();
		SAXBuilder builder = new SAXBuilder();
		Document cmcfdemands = builder.build(filename);
		Element demandgraph = cmcfdemands.getRootElement();
		// read and set the nodes
		Element nodes = demandgraph.getChild("demands");
		 List<Element> commoditylist = nodes.getChildren();
		 for (Element commodity : commoditylist){
			 //read the values of the node xml Element as Strings
			 String id = commodity.getAttributeValue("id");
			 String from = commodity.getChildText("from");
			 String to = commodity.getChildText("to");
			 String demand = commodity.getChildText("demand");
			 //build  new Plans in the Population
			 int dem = (int) Math.round(Double.parseDouble(demand));
			 Node tonode = network.getNode(to);
			 Node fromnode = network.getNode(from);
			 for (int i = 1 ; i<= dem ;i++) {
				 
				 Id matsimid  = new IdImpl(id+"."+i);
				 Person p = new PersonImpl(matsimid);
				 Plan plan = new Plan(p);
			//TODO hier weitermachen

			 
			 }
		 }
		 
		return result;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) { 
		if(args.length<2 || args.length > 3){
			System.out.println("usage: 1. argument network file 2. argument inputfile 3. argument outfile (optional)");
			return;
		}
		String netfile = args[0].trim();
		String inputfile = args[1].trim();
		String outfile = inputfile.substring(0, inputfile.length()-4)+"_msimDEM.xml";
		if(args.length == 3){
			outfile = args[2];
		}
		try {
			NetworkLayer network = new NetworkLayer();
			NetworkReaderMatsimV1 netreader = new NetworkReaderMatsimV1(network);
			netreader.readFile(netfile);
			Population population = readCMCFDemands(inputfile,network); 
			PopulationWriter writer = new PopulationWriter( population, outfile, "V5");
			writer.write();
			System.out.println(inputfile+"conveted "+"output written in :"+outfile);
		} catch (JDOMException e) {
			e.printStackTrace();
			
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		

	}
}
