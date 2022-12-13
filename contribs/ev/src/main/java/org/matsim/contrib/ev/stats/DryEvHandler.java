package org.matsim.contrib.ev.stats;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.ev.fleet.ElectricFleet;

import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.core.events.MobsimScopeEventHandler;
import org.matsim.core.network.NetworkUtils;

import org.matsim.vehicles.Vehicle;


import java.util.*;

/**
 * This class implements listeners to determine where and when EVs run dry during the simulation.
 * The output file, `dryEvsDuringMobsim.csv`,  could be useful for planning the placement of charging infrastructure.
 * Similar structure to {@link org.matsim.contrib.ev.charging.VehicleChargingHandler}
 *
 * @author rgraebe
 */

public class DryEvHandler implements
		VehicleLeavesTrafficEventHandler, LinkLeaveEventHandler, MobsimScopeEventHandler {
	private static final Logger logger = LogManager.getLogger(DryEvHandler.class);
	public Map<String, List<String>> getDryEVs() {
		return dryEVs;
	}
	private final Map<String, List<String>> dryEVs = new HashMap<>();
	private final Network network;
	private Network chargerNetwork;
	private final ElectricFleet electricFleet;
	private final ChargingInfrastructure chargingInfrastructure;

	@Inject
	public DryEvHandler( Network network, Network chargerNetwork, ElectricFleet electricFleet, ChargingInfrastructure chargingInfrastructure ) {
		this.network = network;
		this.chargerNetwork = chargerNetwork;
		this.electricFleet = electricFleet;
		this.chargingInfrastructure = chargingInfrastructure;
	}

	@Override
	public void handleEvent( VehicleLeavesTrafficEvent event ) {
		if ( chargerNetwork.getLinks().isEmpty() ) { createChargerNetwork(); }
		Id<Link> linkId = event.getLinkId();
		Id<Vehicle> vehicleId = event.getVehicleId();
		collectDryEVs( linkId, vehicleId, chargerNetwork );
	}

	@Override
	public void handleEvent( LinkLeaveEvent event ) {
		if ( chargerNetwork.getLinks().isEmpty() ) { createChargerNetwork(); }
		Id<Link> linkId = event.getLinkId();
		Id<Vehicle> vehicleId = event.getVehicleId();
		collectDryEVs( linkId, vehicleId, chargerNetwork );
	}

	private void collectDryEVs( Id<Link> linkId, Id<Vehicle> vehicleId, Network chargerNetwork ) {

		if (electricFleet.getElectricVehicles().containsKey( vehicleId )) {
			double SoC = Objects.requireNonNull(
					electricFleet.getElectricVehicles().get( vehicleId )).getBattery().getSoc() ;
			if ( SoC <= 0. ) {
				logger.warn( "Found an EV with (less than) 0% charge" );
				Link thisLink = network.getLinks().get( linkId );
				Link nearestChargerLink = NetworkUtils.getNearestLink( chargerNetwork, thisLink.getCoord() );
				assert nearestChargerLink != null;
				// "VehicleId","SoC","xCoord","yCoord","LinkId","NearestChargerLinkId","DistanceToNearestCharger_m"
				dryEVs.put(vehicleId.toString(), List.of( new String[]{
						Double.toString( SoC ),
						Double.toString( thisLink.getCoord().getX() ),
						Double.toString( thisLink.getCoord().getY() ),
						thisLink.getId().toString(),
						nearestChargerLink.getId().toString(),
						Double.toString( NetworkUtils.getEuclideanDistance( nearestChargerLink.getCoord(), thisLink.getCoord() ))}));
			}
		}
	}

	private void createChargerNetwork() {
		chargerNetwork = NetworkUtils.createNetwork();
		for ( Charger charger : chargingInfrastructure.getChargers().values() ) {
			chargerNetwork.addNode( charger.getLink().getFromNode() );
			chargerNetwork.addNode( charger.getLink().getToNode() );
			chargerNetwork.addLink( charger.getLink() );
		}
	}
}
