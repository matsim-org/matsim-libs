package playground.rost.graph.nodepopulation;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import playground.rost.graph.nodepopulation.xmlnodepopulation.NodePopulationList;
import playground.rost.graph.nodepopulation.xmlnodepopulation.PopulationForSingleNode;

public class PopulationNodeMap {

	public Map<String, Integer> populationForNode = new HashMap<String, Integer>(); 
	
	public static PopulationNodeMap readXMLFile(String filename) throws JAXBException, IOException
	{
		PopulationNodeMap populationNodeMap = new PopulationNodeMap();
		
		JAXBContext context = JAXBContext.newInstance("playground.rost.graph.nodepopulation.xmlnodepopulation");

		Unmarshaller unmarshaller = context.createUnmarshaller();
		
		NodePopulationList nodePopulationList = (NodePopulationList)unmarshaller.unmarshal(new FileReader(filename));
		
		for(PopulationForSingleNode popForNode : nodePopulationList.getPopulationForNode())
		{
			populationNodeMap.populationForNode.put(popForNode.getId(), popForNode.getPopulation());
		}
		
		return populationNodeMap;
	}
	
	public void writeXMLFile(String filename) throws JAXBException, IOException
	{
		NodePopulationList result = new NodePopulationList();
		for(String id : this.populationForNode.keySet())
		{
			PopulationForSingleNode popForNode = new PopulationForSingleNode();
			popForNode.setId(id);
			popForNode.setPopulation(this.populationForNode.get(id));
			result.getPopulationForNode().add(popForNode);
		}
		
		JAXBContext context = JAXBContext.newInstance("playground.rost.graph.nodepopulation.xmlnodepopulation");
		Marshaller marshall = context.createMarshaller();
		marshall.marshal(result, new FileWriter(filename));
	}

	
}
