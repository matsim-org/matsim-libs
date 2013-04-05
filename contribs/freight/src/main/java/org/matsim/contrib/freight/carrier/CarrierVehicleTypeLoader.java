package org.matsim.contrib.freight.carrier;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

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
				Id typeId = v.getVehicleTypeId();
				if(typeId != null){
					if(types.getVehicleTypes().containsKey(typeId)){
						v.setVehicleType(types.getVehicleTypes().get(typeId));
					}
					else{
						logger.warn("vehicleType without vehicleTypeInformation. set default vehicleType");
					}
				}
				else{
					logger.warn("no vehicleTypeInformation. set default vehicleType");
				}
			}
		}
	}

}
