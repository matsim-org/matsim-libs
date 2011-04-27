package playground.wrashid.sschieffer.DecentralizedSmartCharger;
import org.matsim.core.basic.v01.IdImpl;

import playground.wrashid.PSF2.vehicle.vehicleFleet.PlugInHybridElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.Vehicle;
import playground.wrashid.lib.obj.LinkedListValueHashMap;


public class VehicleTypeCollector {

	LinkedListValueHashMap<Class, VehicleType> vehicleTypeList=new LinkedListValueHashMap<Class, VehicleType>();
	
	public VehicleTypeCollector(){
		
	}
	
	public void addVehicleType(VehicleType v){
		vehicleTypeList.put(v.getVehicleClassType(), v);
	}
	
	
	public GasType getGasType(Vehicle v){
		return vehicleTypeList.getValue(v.getClass()).getGasType();
	}
	
	public Battery getBattery(Vehicle v){
		return vehicleTypeList.getValue(v.getClass()).getBattery();
	}

	
	public double getWattOfEngine(Vehicle v){
		return vehicleTypeList.getValue(v.getClass()).getWattOfEngine();
	}

	public boolean containsVehicleTypeForThisVehicle(Vehicle v){
		for (Class c: vehicleTypeList.getKeySet()){
			if (c.equals(v.getClass())){
				return true;
			}			
		}
		return false;
	}
}
