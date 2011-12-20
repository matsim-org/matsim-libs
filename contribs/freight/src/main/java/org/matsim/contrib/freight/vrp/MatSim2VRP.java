package org.matsim.contrib.freight.vrp;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.vrp.basics.Shipment;

/**
 * It is a map for shipments and records matsim2vrp transformations, i.e. memorizes matsim carrierShipment and its corresponding vrpShipment.
 * @author stefan
 *
 */

public class MatSim2VRP {
	
	private Map<CarrierShipment,Shipment> carrierShipment2vrpShipment = new HashMap<CarrierShipment, Shipment>();
	
	private Map<Shipment,CarrierShipment> vrpShipment2carrierShipment = new HashMap<Shipment, CarrierShipment>();
		
	public void addShipmentEntry(CarrierShipment carrierShipment, Shipment vrpShipment){
		carrierShipment2vrpShipment.put(carrierShipment, vrpShipment);
		vrpShipment2carrierShipment.put(vrpShipment, carrierShipment);
	}
	
	public CarrierShipment getCarrierShipment(Shipment shipment){
		return vrpShipment2carrierShipment.get(shipment);
	}
	
	public Shipment getVrpShipment(CarrierShipment carrierShipment){
		return carrierShipment2vrpShipment.get(carrierShipment);
	}
	
	public Collection<Shipment> getVrpShipments(){
		return carrierShipment2vrpShipment.values();
	}

}
