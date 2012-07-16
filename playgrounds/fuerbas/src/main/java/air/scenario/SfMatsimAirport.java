/* *********************************************************************** *
 * project: org.matsim.*
 * SfAirScheduleBuilder
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package air.scenario;

import java.util.HashSet;
import java.util.Set;

import org.jfree.util.Log;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;

public class SfMatsimAirport {
	
	public static final double runwayLength = 1500.0;
	public static final double taxiwayLength = 500.0;
	public static final double taxiwayFreespeed = 20.0/3.6;
	public static final double runwayFreespeed = 220.0/2.6;
	public static final double TAXI_TOL_TIME = 3*(taxiwayLength/taxiwayFreespeed)+	//time for taxi out and taxi in
			2*(runwayLength/runwayFreespeed);	//time for take-off and landing (TOL)
	
	public Coord coord;
	public Id id;
	
	public SfMatsimAirport(Id id, Coord coord) {
		this.id=id;
		this.coord=coord;
	}
	
	public void createTwoRunways(Network network) {
			
//		create Ids for nodes and links
		
		Id idApron = this.id;													//Id for apron link and central airport node
		Id idApronEnd = new IdImpl(this.id+"apron");							//Id for end of apron node
		Id idTaxiIn = new IdImpl(this.id+"taxiInbound");				//Id for taxiway link and end of taxiway node
		Id idTaxiOut = new IdImpl(this.id+"taxiOutbound");			//Id for taxiway link and end of taxiway node
		Id idRunwayIn = new IdImpl(this.id+"runwayInbound");			//Id for runway link and end of runway node
		Id idRunwayOut = new IdImpl(this.id+"runwayOutbound");		//Id for runway link and end of runway node	
		
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
		
//		c_storage != 1; c_storage = (length*n_lanes)/7.5
		
//		WARNING HINZUFÜGEN, CAP*5 NEHMEN
		
		linkApron.setCapacity(1.*network.getCapacityPeriod());
		linkTaxiIn.setCapacity((1./60.)*network.getCapacityPeriod());
		linkTaxiIn.setNumberOfLanes(0.015);	
		linkTaxiOut.setCapacity((1./60.)*network.getCapacityPeriod());
		linkRunwayIn.setCapacity((1.)*network.getCapacityPeriod());
		linkRunwayOut.setCapacity((1.)*network.getCapacityPeriod());
		linkRunwayOut.setNumberOfLanes(0.005);
		
		linkApron.setLength(taxiwayLength);
		linkTaxiIn.setLength(taxiwayLength);
		linkTaxiOut.setLength(taxiwayLength);
		linkRunwayIn.setLength(runwayLength);
		linkRunwayOut.setLength(runwayLength);
			
		linkApron.setFreespeed(taxiwayFreespeed);
		linkTaxiIn.setFreespeed(taxiwayFreespeed);
		linkTaxiOut.setFreespeed(taxiwayFreespeed);
		linkRunwayIn.setFreespeed(runwayFreespeed);
		linkRunwayOut.setFreespeed(runwayFreespeed);	
		
//		add links to network
				
		network.addLink(linkApron);	
		network.addLink(linkTaxiIn);
		network.addLink(linkTaxiOut);		
		network.addLink(linkRunwayIn);	
		network.addLink(linkRunwayOut);
		
//		add STAR to network

		Id idStar = new IdImpl(this.id.toString()+"star");						//Id for STAR route

		if (DgCreateFlightScenario.stars.containsKey(idApron.toString())) {
			Coord coordStar = new CoordImpl(coordRunwayInEnd.getX()-DgCreateFlightScenario.stars.get(idApron.toString()).getLength(), coordRunwayInEnd.getY());
			Node nodeStar = network.getFactory().createNode(idStar, coordStar);						//start of STAR 
			network.addNode(nodeStar);
			Link linkStarIn = network.getFactory().createLink(idStar, nodeStar, nodeRunwayIn);
			linkStarIn.setAllowedModes(allowedModes);
			linkStarIn.setLength(DgCreateFlightScenario.stars.get(idApron.toString()).getLength());
			linkStarIn.setCapacity(network.getCapacityPeriod() * DgCreateFlightScenario.stars.get(idApron.toString()).getCapacity());
			System.out.println("HOLE STAR MUC.................................................");
			linkStarIn.setFreespeed(DgCreateFlightScenario.stars.get(idApron.toString()).getFreespeed());
			network.addLink(linkStarIn);
		}
		else {
			Coord coordStar = new CoordImpl(coordRunwayInEnd.getX()-DgCreateFlightScenario.DEFAULTSTAR.getLength(), coordRunwayInEnd.getY());
			Node nodeStar = network.getFactory().createNode(idStar, coordStar);						//start of STAR 
			network.addNode(nodeStar);
			Link linkStarIn = network.getFactory().createLink(idStar, nodeStar, nodeRunwayIn);
			linkStarIn.setAllowedModes(allowedModes);
			linkStarIn.setLength(DgCreateFlightScenario.DEFAULTSTAR.getLength());
			linkStarIn.setCapacity(network.getCapacityPeriod() * DgCreateFlightScenario.DEFAULTSTAR.getCapacity());
			linkStarIn.setFreespeed(DgCreateFlightScenario.DEFAULTSTAR.getFreespeed());
			network.addLink(linkStarIn);
		}

	}
	
	public void createOneRunway(Network network) {
		
//		create Ids for nodes and links
		
		Id idApron = this.id;													//Id for apron link and central airport node
		Id idApronEnd = new IdImpl(this.id+"apron");							//Id for end of apron node
		Id idTaxiIn = new IdImpl(this.id+"taxiInbound");				//Id for taxiway link and end of taxiway node
		Id idTaxiOut = new IdImpl(this.id+"taxiOutbound");			//Id for taxiway link and end of taxiway node
		Id idRunwayIn = new IdImpl(this.id+"runway");			//Id for runway link and end of runway node
		
//		create Coords for nodes
		
		Coord coordApronEnd = new CoordImpl(this.coord.getX()-runwayLength, this.coord.getY());			//shifting end of apron
		Coord coordTaxiOut = new CoordImpl(coordApronEnd.getX(), this.coord.getY()-taxiwayLength);		//shifting taxiway
		Coord coordTaxiIn = new CoordImpl(this.coord.getX(), this.coord.getY());		//shifting taxiway
		Coord coordRunwayEnd = new CoordImpl(coordTaxiIn.getX(), coordTaxiIn.getY()-taxiwayLength);		//shifting runway
		
//		create nodes
		
		Node nodeAirport = network.getFactory().createNode(idApron, this.coord);			//central node of any airport	
		Node nodeApron = network.getFactory().createNode(idApronEnd, coordApronEnd);		//end of apron node, apron is used for parking and as transit stop
		Node nodeTaxiOut = network.getFactory().createNode(idTaxiIn, coordTaxiOut);			//taxiway inbound start = runway inbound end
		Node nodeRunway = network.getFactory().createNode(idRunwayIn, coordRunwayEnd);	//start of inbound runway
		
//		add nodes to network
		
		network.addNode(nodeAirport); 			
		network.addNode(nodeApron);
		network.addNode(nodeTaxiOut);
		network.addNode(nodeRunway);
		
//		create links
		Link linkApron = network.getFactory().createLink(idApron, nodeAirport, nodeApron);
		Link linkTaxiIn = network.getFactory().createLink(idTaxiIn, nodeRunway, nodeAirport);
		Link linkTaxiOut = network.getFactory().createLink(idTaxiOut, nodeApron, nodeTaxiOut);
		Link linkRunway = network.getFactory().createLink(idRunwayIn, nodeTaxiOut, nodeRunway);
		
//		set capacity, freespeed and modes
		
		Set<String> allowedModes = new HashSet<String>();
		allowedModes.add("pt");
		allowedModes.add("car");
		linkApron.setAllowedModes(allowedModes);
		linkTaxiIn.setAllowedModes(allowedModes);
		linkTaxiOut.setAllowedModes(allowedModes);
		linkRunway.setAllowedModes(allowedModes);
		
//		c_storage != 1; c_storage = (length*n_lanes)/7.5
		
//		WARNING HINZUFÜGEN, CAP*5 NEHMEN
		
		linkApron.setCapacity(1.*network.getCapacityPeriod());
		linkTaxiIn.setCapacity((1./60.)*network.getCapacityPeriod());
		linkTaxiIn.setNumberOfLanes(0.015);	
		linkTaxiOut.setCapacity((1./60.)*network.getCapacityPeriod());
		linkRunway.setCapacity((1.)*network.getCapacityPeriod());
//		linkRunwayOut.setCapacity((1.)*network.getCapacityPeriod());
//		linkRunwayOut.setNumberOfLanes(0.005);
		
		linkApron.setLength(taxiwayLength);
		linkTaxiIn.setLength(taxiwayLength);
		linkTaxiOut.setLength(taxiwayLength);
		linkRunway.setLength(runwayLength);
			
		linkApron.setFreespeed(taxiwayFreespeed);
		linkTaxiIn.setFreespeed(taxiwayFreespeed);
		linkTaxiOut.setFreespeed(taxiwayFreespeed);
		linkRunway.setFreespeed(runwayFreespeed);
		
//		add links to network
				
		network.addLink(linkApron);	
		network.addLink(linkTaxiIn);
		network.addLink(linkTaxiOut);		
		network.addLink(linkRunway);	
		
//		add STAR to network

		Id idStar = new IdImpl(this.id.toString()+"star");						//Id for STAR route

		if (DgCreateFlightScenario.stars.containsKey(idApron.toString())) {
			Coord coordStar = new CoordImpl(coordRunwayEnd.getX()-DgCreateFlightScenario.stars.get(idApron.toString()).getLength(), coordRunwayEnd.getY());
			Node nodeStar = network.getFactory().createNode(idStar, coordStar);						//start of STAR 
			network.addNode(nodeStar);
			Link linkStarIn = network.getFactory().createLink(idStar, nodeStar, nodeRunway);
			linkStarIn.setAllowedModes(allowedModes);
			linkStarIn.setLength(DgCreateFlightScenario.stars.get(idApron.toString()).getLength());
			linkStarIn.setCapacity(network.getCapacityPeriod() * DgCreateFlightScenario.stars.get(idApron.toString()).getCapacity());
			System.out.println("HOLE STAR MUC.................................................");
			linkStarIn.setFreespeed(DgCreateFlightScenario.stars.get(idApron.toString()).getFreespeed());
			network.addLink(linkStarIn);
		}
		else {
			Coord coordStar = new CoordImpl(coordRunwayEnd.getX()-DgCreateFlightScenario.DEFAULTSTAR.getLength(), coordRunwayEnd.getY());
			Node nodeStar = network.getFactory().createNode(idStar, coordStar);						//start of STAR 
			network.addNode(nodeStar);
			Link linkStarIn = network.getFactory().createLink(idStar, nodeStar, nodeRunway);
			linkStarIn.setAllowedModes(allowedModes);
			linkStarIn.setLength(DgCreateFlightScenario.DEFAULTSTAR.getLength());
			linkStarIn.setCapacity(network.getCapacityPeriod() * DgCreateFlightScenario.DEFAULTSTAR.getCapacity());
			linkStarIn.setFreespeed(DgCreateFlightScenario.DEFAULTSTAR.getFreespeed());
			network.addLink(linkStarIn);
		}
		
	}
	

}