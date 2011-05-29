package playground.wrashid.sschieffer.SetUp;

import org.matsim.core.basic.v01.IdImpl;

import playground.wrashid.PSF2.vehicle.vehicleFleet.ElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.PlugInHybridElectricVehicle;
import playground.wrashid.sschieffer.DSC.Battery;
import playground.wrashid.sschieffer.DSC.GasType;
import playground.wrashid.sschieffer.DSC.VehicleType;
import playground.wrashid.sschieffer.DSC.VehicleTypeCollector;

/**
 * this class sets up the Vehicle types for the simulation
 * it defines gas types, battery types and vehicle types
 * <li>"normal gas"
 * <li>EV battery
 * <li>PHEV battery
 * @author Stella
 *
 */
public class SetUpVehicleCollector {

	
	private VehicleTypeCollector myVehicleTypes;
	
	public SetUpVehicleCollector(){
		
	}
	
	/**
	 * defines and sets up the gas types for the simulation
	 * @return
	 */
	public VehicleTypeCollector setUp(){
		
		/*
		 * GAS TYPES
		 * - Specify gas types and their characteristics
		 * 
		 * - Gas Price [currency]
		 * - Joules per liter in gas [J]
		 * - emissions of Co2 per liter gas [kg]
		 
		 */
		
		/**
		 * NORMAL GAS
		 */
		
		// GAS PRICE CH //http://www.tanktipp.ch/
		// GAS PRICE USA //	http://gasbuddy.com/ //3.75 (U.S. dollars / US gallon) = 0.849974428 Swiss francs / l
		double gasPricePerLiter= 1.70; 
		
		
		//JoulesPerLiter = theoretisch moegliche Energie * Wirkungsgrad; 
		//http://de.wikipedia.org/wiki/Wirkungsgrad
		//http://de.wikipedia.org/wiki/Motorenbenzin
		double gasJoulesPerLiter = 43.0*Math.pow(10,6);// Benzin 42,7–44,2 MJ/kg  * 0.3
		double massPerLiter=0.75;// kg/l
		double emissionPerLiter = 2.36; // 2,36 kg/L
		
		GasType normalGas=new GasType("normal gas", 
				gasJoulesPerLiter, 
				gasPricePerLiter, 
				emissionPerLiter);
		
		
		/*
		 * Define battery types (e.g. EV mode, PHEV model)
		 * 
		 * Battery characteristics:
		 * - full capacity [J]
		 * e.g. common size is 24kWh = 24kWh*3600s/h*1000W/kW = 24*3600*1000Ws= 24*3600*1000J
		 * - minimum level of state of charge, avoid going below this SOC= batteryMin
		 * (0.1=10%)
		 * - maximum level of state of charge, avoid going above = batteryMax
		 * (0.9=90%)
		 * 
		 * Create desired Battery Types
		 */
		double batterySizeEV= 24*3600*1000; 
		double batterySizePHEV= 24*3600*1000; 
		double batteryMinEV= 0.1; 		
		double batteryMinPHEV= 0.1; 
		double batteryMaxEV= 0.9; 
		double batteryMaxPHEV= 0.9; 		
		
		Battery EVBattery = new Battery(batterySizeEV, batteryMinEV, batteryMaxEV);
		Battery PHEVBattery = new Battery(batterySizePHEV, batteryMinPHEV, batteryMaxPHEV);
		
		/*
		 * Specify vehicle types </br>
		 * 
		 * each vehicle has 
		 * <ul>
		 * <li> a name [String]
		 * <li> a battery type
		 * <li> a gas type ( EVs do not need a gas type--> leave it null )
		 * <li> a reference/dummy vehicle object to check the classes of vehicles find the corresponding Vehicle type for it
		 * <li> energy burn rate of the engine [W] 
		 * </ul>
		 */
		VehicleType EVTypeStandard= new VehicleType("standard EV", 
				EVBattery, 
				null, 
				ElectricVehicle.class,
				80000,// Nissan leaf 80kW Engine
				1.0);
		
		VehicleType PHEVTypeStandard= new VehicleType("standard PHEV", 
				PHEVBattery, 
				normalGas, 
				PlugInHybridElectricVehicle.class,
				80000,
				0.3);
		
		/*
		 * The vehicle types are saved within the VehicleTypeCollector
		 * which is then passed into the Decentralized Smart Charger
		 */
		myVehicleTypes= new VehicleTypeCollector();
		myVehicleTypes.addVehicleType(EVTypeStandard);
		myVehicleTypes.addVehicleType(PHEVTypeStandard);		
	
		
		return myVehicleTypes;
	}
	
}
