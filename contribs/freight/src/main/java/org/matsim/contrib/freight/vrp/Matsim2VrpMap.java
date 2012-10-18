package org.matsim.contrib.freight.vrp;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.CarrierShipment.TimeWindow;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.vrp.basics.Shipment;
import org.matsim.contrib.freight.vrp.basics.Vehicle;
import org.matsim.contrib.freight.vrp.utils.VrpUtils;
import org.matsim.contrib.freight.vrp.utils.matsim2vrp.VRPVehicleAdapter;

/**
 * Translates matsim-carrier data to what the vehicle router needs, and
 * memorizes translation that it can be translated back.
 * 
 * @author schroeder
 * 
 */
class Matsim2VrpMap {

	private static class MatSimVrpVehicleMap {

		private Map<CarrierVehicle, Vehicle> matsim2vrpMap;

		private Map<Vehicle, CarrierVehicle> vrp2matsimMap;

		MatSimVrpVehicleMap(Collection<CarrierVehicle> carrierVehicles) {
			super();
			makeVehicles(carrierVehicles);
		}

		private void makeVehicles(Collection<CarrierVehicle> carrierVehicles) {
			matsim2vrpMap = new HashMap<CarrierVehicle, Vehicle>();
			vrp2matsimMap = new HashMap<Vehicle, CarrierVehicle>();
			for (CarrierVehicle cv : carrierVehicles) {
				VRPVehicleAdapter v = new VRPVehicleAdapter(cv);
				matsim2vrpMap.put(cv, v);
				vrp2matsimMap.put(v, cv);
			}
		}

		public Collection<Vehicle> getVehicles() {
			return matsim2vrpMap.values();
		}

		public Collection<CarrierVehicle> getCarrierVehicles() {
			return vrp2matsimMap.values();
		}

		public Vehicle getVehicle(CarrierVehicle carrierVehicle) {
			return matsim2vrpMap.get(carrierVehicle);
		}

		public CarrierVehicle getCarrierVehicle(Vehicle vehicle) {
			return vrp2matsimMap.get(vehicle);
		}
	}

	private class MatSim2VRPShipmentMap {

		private Integer shipmentIdCounter = 1;

		private Map<CarrierShipment, Shipment> carrierShipment2vrpShipment = new HashMap<CarrierShipment, Shipment>();

		private Map<Shipment, CarrierShipment> vrpShipment2carrierShipment = new HashMap<Shipment, CarrierShipment>();

		public MatSim2VRPShipmentMap(
				Collection<CarrierShipment> carrierShipments) {
			makeShipments(carrierShipments);
		}

		private void makeShipments(Collection<CarrierShipment> carrierShipments) {
			for (CarrierShipment cs : carrierShipments) {
				Shipment s = createVrpShipment(cs);
				carrierShipment2vrpShipment.put(cs, s);
				vrpShipment2carrierShipment.put(s, cs);
			}

		}

		private org.matsim.contrib.freight.vrp.basics.TimeWindow makeTW(
				TimeWindow timeWindow) {
			return VrpUtils.createTimeWindow(timeWindow.getStart(),
					timeWindow.getEnd());
		}

		private String makeId() {
			String id = shipmentIdCounter.toString();
			shipmentIdCounter++;
			return id;
		}

		private Shipment createVrpShipment(CarrierShipment carrierShipment) {
			String id = makeId();
			Shipment s = VrpUtils.createShipment(id, carrierShipment.getFrom()
					.toString(), carrierShipment.getTo().toString(),
					carrierShipment.getSize(), makeTW(carrierShipment
							.getPickupTimeWindow()), makeTW(carrierShipment
							.getDeliveryTimeWindow()));
			s.setPickupServiceTime(carrierShipment.getPickupServiceTime());
			s.setDeliveryServiceTime(carrierShipment.getDeliveryServiceTime());
			return s;
		}

		CarrierShipment getCarrierShipment(Shipment shipment) {
			return vrpShipment2carrierShipment.get(shipment);
		}

		Shipment getVrpShipment(CarrierShipment carrierShipment) {
			return carrierShipment2vrpShipment.get(carrierShipment);
		}

		Collection<Shipment> getVrpShipments() {
			return carrierShipment2vrpShipment.values();
		}
	}

	private MatSimVrpVehicleMap vehicleMap;

	private MatSim2VRPShipmentMap shipmentMap;

	Matsim2VrpMap(Collection<CarrierShipment> shipments,Collection<CarrierVehicle> vehicles) {
		vehicleMap = new MatSimVrpVehicleMap(vehicles);
		shipmentMap = new MatSim2VRPShipmentMap(shipments);
	}

	public Vehicle getVehicle(CarrierVehicle carrierVehicle) {
		return vehicleMap.getVehicle(carrierVehicle);
	}

	public CarrierVehicle getCarrierVehicle(Vehicle vehicle) {
		return vehicleMap.getCarrierVehicle(vehicle);
	}

	public Shipment getShipment(CarrierShipment carrierShipment) {
		return shipmentMap.getVrpShipment(carrierShipment);
	}

	public CarrierShipment getCarrierShipment(Shipment shipment) {
		return shipmentMap.getCarrierShipment(shipment);
	}

	public Collection<Vehicle> getVehicles() {
		return vehicleMap.getVehicles();
	}

	public Collection<Shipment> getShipments() {
		return shipmentMap.getVrpShipments();
	}

}
