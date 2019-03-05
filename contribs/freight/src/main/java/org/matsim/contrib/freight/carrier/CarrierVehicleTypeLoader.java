package org.matsim.contrib.freight.carrier;

import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

/**
 * Loader that loads/assigns vehicleTypes to their vehicles and carriers respectively.
 * 
 * @author sschroeder
 *
 */
public class CarrierVehicleTypeLoader {
	
	private static Logger logger = Logger.getLogger(CarrierVehicleTypeLoader.class);
	
	private Carriers carriers;

	/**
	 * Constructs the loader with the carriers the types should be assigned to.
	 * 
	 * @param carriers
	 */
	public CarrierVehicleTypeLoader(Carriers carriers) {
		super();
		this.carriers = carriers;
	}
	
	/**
	 * Assigns types to carriers and their vehicles.
	 * 
	 * @param types
	 */
	public void loadVehicleTypes(CarrierVehicleTypes types){
		for(Carrier c : carriers.getCarriers().values()){
			for(CarrierVehicle v : c.getCarrierCapabilities().getCarrierVehicles()){
				Id<VehicleType> typeId = v.getVehicleTypeId();
				if(typeId != null){
					if(types.getVehicleTypes().containsKey(typeId)){
						CarrierVehicleType vehicleType = types.getVehicleTypes().get(typeId);
						v.setVehicleType(vehicleType);
						Collection<CarrierVehicleType> vTypes = c.getCarrierCapabilities().getVehicleTypes();
						if(!vTypes.contains(vehicleType)){
							vTypes.add(vehicleType);
						}
					}
					else{
						throw new IllegalStateException("cannot assign all vehicleTypes, since vehicleType to typeId \"" + typeId + "\" is missing.");
					}
				}
				else{
					logger.warn("vehicleTypeId is missing, thus no vehicleType can be assigned.");
				}
			}
		}
	}

	/**
	 * Assigns types to carriers and their vehicles.
	 *
	 * @param vehicles
	 */
	public void loadVehicleTypes(Vehicles vehicles){
		for(Carrier c : carriers.getCarriers().values()){
			for(CarrierVehicle v : c.getCarrierCapabilities().getCarrierVehicles()){
				Id<VehicleType> typeId = v.getVehicleTypeId();
				if(typeId != null){
					if(vehicles.getVehicleTypes().containsKey(typeId)){
						VehicleType vehicleType = vehicles.getVehicleTypes().get(typeId);
						v.setVehicleType((CarrierVehicleType) vehicleType);
						Collection<CarrierVehicleType> vTypes = c.getCarrierCapabilities().getVehicleTypes();
						if(!vTypes.contains(vehicleType)){
							vTypes.add((CarrierVehicleType) vehicleType);
						}
					}
					else{
						throw new IllegalStateException("cannot assign all vehicleTypes, since vehicleType to typeId \"" + typeId + "\" is missing.");
					}
				}
				else{
					logger.warn("vehicleTypeId is missing, thus no vehicleType can be assigned.");
				}
			}
		}
	}

}
