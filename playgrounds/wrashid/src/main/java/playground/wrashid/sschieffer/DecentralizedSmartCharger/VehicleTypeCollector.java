package playground.wrashid.sschieffer.DecentralizedSmartCharger;
import java.util.HashMap;

import org.matsim.core.basic.v01.IdImpl;

import playground.wrashid.PSF2.vehicle.vehicleFleet.PlugInHybridElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.Vehicle;
import playground.wrashid.lib.obj.LinkedListValueHashMap;


public class VehicleTypeCollector {

	HashMap<Class, VehicleType> vehicleTypeList=new HashMap<Class, VehicleType>();
	
	public VehicleTypeCollector(){
		
	}
	
	public void addVehicleType(VehicleType v){
		vehicleTypeList.put(v.getVehicleClassType(), v);
	}
	
	
	public GasType getGasType(Vehicle v){
		return vehicleTypeList.get(v.getClass()).getGasType();
	}
	
	public Battery getBattery(Vehicle v){
		return vehicleTypeList.get(v.getClass()).getBattery();
	}

	
	public double getWattOfEngine(Vehicle v){
		return vehicleTypeList.get(v.getClass()).getWattOfEngine();
	}
	
	public double getEfficiencyOfEngine(Vehicle v){
		return vehicleTypeList.get(v.getClass()).getEfficiency();
	}
	

	public boolean containsVehicleTypeForThisVehicle(Vehicle v){
		for (Class c: vehicleTypeList.keySet()){
			if (c.equals(v.getClass())){
				return true;
			}			
		}
		return false;
	}
	
	
	public String printHTMLSummary(){
		String html="";
		for (Class c: vehicleTypeList.keySet()){
				html=html.concat(vehicleTypeList.get(c).printVehicleTypeHTML());
		}
		return html;
	}
}
