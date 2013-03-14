package vehicles;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.Carriers;

public class VehicleTypeLoader {
	
	private static Logger logger = Logger.getLogger(VehicleTypeLoader.class);
	
	private Carriers carriers;

	public VehicleTypeLoader(Carriers carriers) {
		super();
		this.carriers = carriers;
	}
	
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
