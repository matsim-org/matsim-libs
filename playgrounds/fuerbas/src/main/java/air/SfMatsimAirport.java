package air;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;

public class SfMatsimAirport {
	
	private static final double runwayLength = 450.0;
	private static final double taxiwayLength = 200.0;
	
	public Coord coord;
	public Id id;
	
	public SfMatsimAirport(Id id, Coord coord) {
		this.id=id;
		this.coord=coord;
	}
	
	public void createRunways(Network network) {
		
	
//		create Ids for nodes and links
		
		Id idApron = this.id;													//Id for apron link and central airport node
		Id idApronEnd = new IdImpl(this.id+"apron");							//Id for end of apron node
		Id idTaxiIn = new IdImpl(this.id.toString()+"taxiInbound");				//Id for taxiway link and end of taxiway node
		Id idTaxiOut = new IdImpl(this.id.toString()+"taxiOutbound");			//Id for taxiway link and end of taxiway node
		Id idRunwayIn = new IdImpl(this.id.toString()+"runwayInbound");			//Id for runway link and end of runway node
		Id idRunwayOut = new IdImpl(this.id.toString()+"runwayOutbound");		//Id for runway link and end of runway node	
		
//		create Coords for nodes
		
		Coord coordApronEnd = new CoordImpl(this.coord.getX(), this.coord.getY()+taxiwayLength);			//shifting end of apron
		Coord coordTaxiIn = new CoordImpl(this.coord.getX()-taxiwayLength, this.coord.getY()-taxiwayLength);		//shifting taxiway
		Coord coordTaxiOut = new CoordImpl(coordApronEnd.getX()-taxiwayLength, coordApronEnd.getY()+taxiwayLength);		//shifting taxiway
		Coord coordRunwayInEnd = new CoordImpl(coordTaxiIn.getX()-runwayLength, coordTaxiIn.getY());		//shifting runway
		Coord coordRunwayOutEnd = new CoordImpl(coordTaxiOut.getX()-runwayLength, coordTaxiOut.getY());		//shifting runway
		
//		create nodes
		
		Node nodeAirport = network.getFactory().createNode(idApron, this.coord);			//central node of any airport	
		Node nodeApron = network.getFactory().createNode(idApronEnd, coordApronEnd);		//end of apron node, apron is used for parking and as transit stop
		Node nodeTaxiIn = network.getFactory().createNode(idTaxiIn, coordTaxiIn);			//taxiway inbound start = runway inbound end
		Node nodeTaxiOut = network.getFactory().createNode(idTaxiOut, coordTaxiOut);		//taxiway outbound end = runway outbound start
		Node nodeRunwayIn = network.getFactory().createNode(idRunwayIn, coordRunwayInEnd);	//start of inbound runway
		Node nodeRunwayOut = network.getFactory().createNode(idRunwayOut, coordRunwayOutEnd);	//end of outbound runway
		
//		add nodes to network
		
		network.addNode(nodeAirport); 			
		network.addNode(nodeApron);
		network.addNode(nodeTaxiIn);
		network.addNode(nodeTaxiOut);
		network.addNode(nodeRunwayIn);
		network.addNode(nodeRunwayOut);
		
//		create links
		Link linkApron = network.getFactory().createLink(idApron, nodeAirport, nodeApron);
		Link linkTaxiIn = network.getFactory().createLink(idTaxiIn, nodeTaxiIn, nodeAirport);
		Link linkTaxiOut = network.getFactory().createLink(idTaxiOut, nodeApron, nodeTaxiOut);
		Link linkRunwayIn = network.getFactory().createLink(idRunwayIn, nodeRunwayIn, nodeTaxiIn);
		Link linkRunwayOut = network.getFactory().createLink(idRunwayOut, nodeTaxiOut, nodeRunwayOut);
		
//		set capacity, freespeed and modes
		
		Set<String> allowedModes = new HashSet<String>();
		allowedModes.add("pt");
		allowedModes.add("car");
		linkApron.setAllowedModes(allowedModes);
		linkTaxiIn.setAllowedModes(allowedModes);
		linkTaxiOut.setAllowedModes(allowedModes);
		linkRunwayIn.setAllowedModes(allowedModes);
		linkRunwayOut.setAllowedModes(allowedModes);
		
		linkApron.setFreespeed(30.0/3.6);
		linkTaxiIn.setFreespeed(30.0/3.6);
		linkTaxiOut.setFreespeed(30.0/3.6);
		linkRunwayIn.setFreespeed(220.0/3.6);
		linkRunwayOut.setFreespeed(250.0/3.6);
		
		linkApron.setCapacity(1.);
		linkTaxiIn.setCapacity(1./60.);
		linkTaxiIn.setNumberOfLanes(2.25);
		linkTaxiOut.setCapacity(1.);
		linkTaxiOut.setNumberOfLanes(2.25);
		linkRunwayIn.setCapacity(1.);
		linkRunwayOut.setCapacity(1./60.);
		
		linkApron.setLength(taxiwayLength);
		linkTaxiIn.setLength(taxiwayLength);
		linkTaxiOut.setLength(taxiwayLength);
		linkRunwayIn.setLength(runwayLength);
		linkRunwayOut.setLength(runwayLength);
		
		
//		add links to network
				
		network.addLink(linkApron);	
		network.addLink(linkTaxiIn);
		network.addLink(linkTaxiOut);		
		network.addLink(linkRunwayIn);	
		network.addLink(linkRunwayOut);
		

	}

}
