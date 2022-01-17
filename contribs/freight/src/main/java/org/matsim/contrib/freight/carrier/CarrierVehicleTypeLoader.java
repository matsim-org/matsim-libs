package org.matsim.contrib.freight.carrier;

import org.apache.log4j.Logger;

/**
 * @deprecated Functionality is removed. VehicleTypes must be set (and available) when creating the vehicle. kai/kai jan'22
 *
 * Loader that loads/assigns vehicleTypes to their vehicles and carriers respectively.
 * 
 * @author sschroeder
 *
 */
@Deprecated
public class CarrierVehicleTypeLoader {
	
	private static final  Logger logger = Logger.getLogger(CarrierVehicleTypeLoader.class);

	private final Carriers carriers;

	/**
	 * Constructs the loader with the carriers the types should be assigned to.
	 * 
	 * @param carriers
	 *
	 *  * @deprecated Functionality is removed. VehicleTypes must be set (and available) when creating the vehicle. kai/kai jan'22
	 */
	@Deprecated
	public CarrierVehicleTypeLoader(Carriers carriers) {
		super();
		this.carriers = carriers;
	}
	
	/**
	 * Assigns types to carriers and their vehicles.
	 * 
	 * @param types
	 *
	 * @deprecated Functionality is removed. VehicleTypes must be set (and available) when creating the vehicle. kai/kai jan'22
	 */
	@Deprecated
	public void loadVehicleTypes(CarrierVehicleTypes types){
		logger.error("Functionality is removed. VehicleTypes must be set (and available) when creating the vehicle.");
	}

}
