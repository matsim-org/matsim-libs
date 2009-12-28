package playground.wrashid.tryouts.examples;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationWriter;

public class CreateAndWritePerson {

	/*
	public MyMobSim(Scenario sc,Events ev){
		Network net = sc.getNetwork();
		Population pop=sc.getPopulation();	
	}
	*/
	
	public static void main(String[] args) {
		Scenario sc=new ScenarioImpl();
		
		
		createAndWritePerson();
	}
	
	/*
	 * 
	 */
	public static void createAndWritePerson(){
		Scenario sc=new ScenarioImpl();
		Id nodeId1= sc.createId("nodeId1");
		Id nodeId2= sc.createId("nodeId2");
		Id linkId= sc.createId("linkId");
		Id personId = sc.createId("personId");
		Coord coord= sc.createCoord(1.0, 1.0);
		
		Node node1= sc.getNetwork().getFactory().createNode(nodeId1,coord);
		Node node2= sc.getNetwork().getFactory().createNode(nodeId2,coord);
		
		sc.getNetwork().addNode(node1);
		sc.getNetwork().addNode(node2);

		Link link= sc.getNetwork().getFactory().createLink(linkId,nodeId1,nodeId2);
		sc.getNetwork().addLink(link);
		
		Person person=sc.getPopulation().getFactory().createPerson(personId);
		
		sc.getPopulation().addPerson(person);
		
		PopulationWriter writer = new PopulationWriter(sc.getPopulation()) ;
		writer.write("abcccc.xml");
	}
	
	public static void missingAPIFunctions(){
		Scenario sc=new ScenarioImpl();
		
		// reader for population is missing
		// reader, writer for network are missing
		// facilities are missing
		// replanning: how to add strategy module? parts still in core.
		// events is missing
		
		// class module is not in api package...
		//Module module = sc.getConfig().getModule("");
	}
	
}
