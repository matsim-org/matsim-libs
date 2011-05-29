package playground.wrashid.sschieffer.DSC;
import playground.wrashid.PSF2.vehicle.vehicleFleet.Vehicle;

/**
 * define a vehicle such as EV, PHEV and its characteristics
 * <li> name
 * <li> battery type
 * <li> gas type
 * <li> Class
 * <li> efficiency of engine, i.e. 30% for combustion engine
 * <li> Watt of engine
 * @author Stella
 *
 */
public class VehicleType {
	
	final private String name;
	final Battery myBattery;
	final private GasType myGasType;
	final  private Class referenceClass;
	final private double efficiency;
	final private double engineWatt;
	
	public VehicleType(String name,
			Battery myBattery,
			GasType myGasType, 
			Class referenceClass,
			double engineWatt,
			double efficiency){
		
		this.myBattery=myBattery;
		this.myGasType=myGasType;
		this.referenceClass=referenceClass;
		this.name=name;
		this.engineWatt=engineWatt;
		this.efficiency=efficiency;
	}
	
	
	
	
	public Battery getBattery(){
		return myBattery;
	}
	
	public GasType getGasType(){
		return myGasType;
	}
	
	public double getWattOfEngine(){
		return engineWatt;
	}
	
	public double getEfficiency(){
		return efficiency;
	}
	
	public String getName(){
		return name;
	}
	
	public Class getVehicleClassType(){
		return referenceClass;
	}
	
	
	public void printVehicleType(){
		System.out.println(
		myGasType.printGasType() + "\n"+
		myBattery.printBattery()+ "\n"+
		"Watt of Engine"+ engineWatt
		);
	}
	
	
	public String printVehicleTypeHTML(){
		String html=		
		myGasType.printGasType() + "</br>"+
		myBattery.printBattery()+"</br>"+
		"Watt of Engine"+ engineWatt + "</br>"+
		"Efficiency of Engine"+ efficiency + "</br>";
		return html;
	}
	
}
