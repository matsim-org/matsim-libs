package air;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;

public class MATSimAirport {
	
	public Coord coord;
	public Id id;
	
	public MATSimAirport(Id id, Coord coord) {
		this.id=id;
		this.coord=coord;
	}
	
	@SuppressWarnings("deprecation")
	public void createRunways(Network network) {
		
//		Node mit IATA als Zentrales Element, Link mit IATA Code an Node,
//		Taxiway von IATA zu Runway_outbound und Runway_inbound
//		Runway_inbound, Runway_outbound
//		coord.getX und coord.getY gibt double, dann addieren 0.001 = 111m
		
		Id idApron = this.id;													//Id for apron link and central airport node
		Id idApronEnd = new IdImpl(this.id+"apron");							//Id for end of apron node
		Id idTaxiIn = new IdImpl(this.id.toString()+"taxiInbound");				//Id for taxiway link and end of taxiway node
		Id idTaxiOut = new IdImpl(this.id.toString()+"taxiOutbound");			//Id for taxiway link and end of taxiway node
		Id idRunwayIn = new IdImpl(this.id.toString()+"runwayInbound");			//Id for runway link and end of runway node
		Id idRunwayOut = new IdImpl(this.id.toString()+"runwayOutbound");		//Id for runway link and end of runway node	
		
		Coord coordApronEnd = new CoordImpl(this.coord.getX(), this.coord.getY()+0.001);			//shifting end of apron by 111 meters in Y direction
		Coord coordTaxiIn = new CoordImpl(this.coord.getX()-0.001, this.coord.getY()-0.001);		//shifting taxiway
		Coord coordTaxiOut = new CoordImpl(this.coord.getX()-0.001, this.coord.getY()+0.001);		//shifting taxiway
		Coord coordRunwayInEnd = new CoordImpl(coordTaxiIn.getX()-0.01, coordTaxiIn.getY());		//shifting runway
		Coord coordRunwayOutEnd = new CoordImpl(coordTaxiOut.getX()-0.01, coordTaxiIn.getY());		//shifting runway
		
		network.addNode(network.getFactory().createNode(idApron, this.coord)); 			//central node of any airport	
		network.addNode(network.getFactory().createNode(idApronEnd, coordApronEnd));
		
		network.addNode(network.getFactory().createNode(idTaxiIn, coordTaxiIn));
		network.addNode(network.getFactory().createNode(idTaxiOut, coordTaxiOut));
		
		network.addNode(network.getFactory().createNode(idRunwayIn, coordRunwayInEnd));
		network.addNode(network.getFactory().createNode(idRunwayOut, coordRunwayOutEnd));
		
		network.addLink(network.getFactory().createLink(idApron, idApron, idApronEnd));
		
		network.addLink(network.getFactory().createLink(idTaxiIn, idTaxiIn, idApron));
		network.addLink(network.getFactory().createLink(idTaxiOut, idApron, idTaxiOut));
		
		network.addLink(network.getFactory().createLink(idRunwayIn, idRunwayIn, idTaxiIn));
		network.addLink(network.getFactory().createLink(idRunwayOut, idTaxiOut, idRunwayOut));
	}

}
