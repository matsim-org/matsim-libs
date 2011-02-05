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

public class SfMATSimAirport {
	
	private static final double runwayLength = 3000.0;
	private static final double taxiwayLength = 500.0;
	
	public Coord coord;
	public Id id;
	
	public SfMATSimAirport(Id id, Coord coord) {
		this.id=id;
		this.coord=coord;
	}
	
	@SuppressWarnings("deprecation")
	public void createRunways(Network network) {
		
//		Node mit IATA als Zentrales Element, Link mit IATA Code an Node,
//		Taxiway von IATA zu Runway_outbound und Runway_inbound
//		Runway_inbound, Runway_outbound
//		coord.getX und coord.getY gibt double, dann addieren 0.001 = 111m
		
//		create Ids for nodes and links
		
		Id idApron = this.id;													//Id for apron link and central airport node
		Id idApronEnd = new IdImpl(this.id+"apron");							//Id for end of apron node
		Id idTaxiIn = new IdImpl(this.id.toString()+"taxiInbound");				//Id for taxiway link and end of taxiway node
		Id idTaxiOut = new IdImpl(this.id.toString()+"taxiOutbound");			//Id for taxiway link and end of taxiway node
		Id idRunwayIn = new IdImpl(this.id.toString()+"runwayInbound");			//Id for runway link and end of runway node
		Id idRunwayOut = new IdImpl(this.id.toString()+"runwayOutbound");		//Id for runway link and end of runway node	
		
//		create Coords for nodes
		
		Coord coordApronEnd = new CoordImpl(this.coord.getX()+taxiwayLength, this.coord.getY());			//shifting end of apron
		Coord coordTaxiIn = new CoordImpl(this.coord.getX()-taxiwayLength, this.coord.getY()-taxiwayLength);		//shifting taxiway
		Coord coordTaxiOut = new CoordImpl(this.coord.getX()-taxiwayLength, this.coord.getY()+taxiwayLength);		//shifting taxiway
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
		Link linkApron = network.getFactory().createLink(idApron, idApron, idApronEnd);
		Link linkTaxiIn = network.getFactory().createLink(idTaxiIn, idTaxiIn, idApron);
		Link linkTaxiOut = network.getFactory().createLink(idTaxiOut, idApronEnd, idTaxiOut);
		Link linkRunwayIn = network.getFactory().createLink(idRunwayIn, idRunwayIn, idTaxiIn);
		Link linkRunwayOut = network.getFactory().createLink(idRunwayOut, idTaxiOut, idRunwayOut);
		
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
		
		linkApron.setCapacity(50.0);
		linkTaxiIn.setCapacity(5.0);
		linkTaxiOut.setCapacity(5.0);
		linkRunwayIn.setCapacity(1.0);
		linkRunwayOut.setCapacity(1.0);
		
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
