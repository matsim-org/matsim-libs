package kid.filter;

import java.util.Set;

import kid.KiDSchema;
import kid.Vehicle;

import org.apache.log4j.Logger;


public class BusinessSectorFilter implements VehicleFilter{

	private static Logger logger = Logger.getLogger(BusinessSectorFilter.class);
	
	private String sector;
	
	public BusinessSectorFilter(String sector) {
		super();
		this.sector = sector;
	}
	
	public BusinessSectorFilter() {
		
	}

	public boolean judge(Vehicle vehicle) {
		if(vehicle.getAttributes().get(KiDSchema.COMPANY_WIRTSCHAFTSZWEIG).equals(sector)){
			if(vehicle.getAttributes().get(KiDSchema.COMPANY_KREISTYP).equals("1")){
//				if(vehicle.getAttributes().get(KiDSchema.COMPANY_FUHRPARK_PKW).equals("450")){
//					print(vehicle);
//				}
				return true;
			}
		}
		return false;
	}

	private void print(Vehicle vehicle) {
		Set<String> keys = vehicle.getAttributes().keySet();
		for(String key : keys){
			System.out.println(key + "\t" + vehicle.getAttributes().get(key));
		}
		
	}

}
