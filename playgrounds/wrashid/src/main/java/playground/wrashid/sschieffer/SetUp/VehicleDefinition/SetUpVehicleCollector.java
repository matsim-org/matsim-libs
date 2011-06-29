package playground.wrashid.sschieffer.SetUp.VehicleDefinition;

import org.matsim.core.basic.v01.IdImpl;

import playground.wrashid.PSF2.vehicle.vehicleFleet.ElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.PlugInHybridElectricVehicle;

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
	private double gasPricePerLiter;
	
	private Battery EVBattery ;
	private Battery PHEVBattery ;
	private VehicleType EVTypeStandard, PHEVTypeStandard;
	private GasType normalGas;
	
	public SetUpVehicleCollector(){
		
	}
	
	
	
	/**
	 * defines and sets up the gas types for the simulation indicating only between high or low gas price
	 * using the default gas price values
	 * @return
	 */
	public VehicleTypeCollector setUp(double kWHEV, double kWHPHEV, Object gasHigh){
		
		/**
		 * NORMAL GAS
		 */
		if (gasHigh.getClass()==Double.class){
			setUpGasPrice((Double)gasHigh);
		}else{
			setUpGasPrice((Boolean)gasHigh);
		}
		
		setNormalGas();
		
		setBatteryTypes(kWHEV, kWHPHEV);		
		
		setVehicleTypes();
		/*
		 * The vehicle types are saved within the VehicleTypeCollector
		 * which is then passed into the Decentralized Smart Charger
		 */
		myVehicleTypes= new VehicleTypeCollector();
		myVehicleTypes.addVehicleType(EVTypeStandard);
		myVehicleTypes.addVehicleType(PHEVTypeStandard);		
	
		return myVehicleTypes;
	}

	
	/**
	 * defines and sets up the gas types for the simulation
	 * with passed value of gasPrice </br>
	 * IMPORTANT: The gas price is given in: = CHF/l * l/J * J/s </br>
	 * CHF per second charging = price per liter *1/(joules/liter) * engineWatt
	 * @return
	 */
	public VehicleTypeCollector setUp(double kWHEV, double kWHPHEV, double gasPrice){
		
		/**
		 * NORMAL GAS
		 */
		setUpGasPrice(gasPrice);
		
		setNormalGas();
		
		setBatteryTypes(kWHEV, kWHPHEV);		
		
		setVehicleTypes();
		/*
		 * The vehicle types are saved within the VehicleTypeCollector
		 * which is then passed into the Decentralized Smart Charger
		 */
		myVehicleTypes= new VehicleTypeCollector();
		myVehicleTypes.addVehicleType(EVTypeStandard);
		myVehicleTypes.addVehicleType(PHEVTypeStandard);		
	
		return myVehicleTypes;
	}


	private void setUpGasPrice(boolean gasHigh){
		if(gasHigh){
			// GAS PRICE CH //http://www.tanktipp.ch/
			gasPricePerLiter= 1.70; 
		}else{
			// GAS PRICE USA //	http://gasbuddy.com/ //3.75 (U.S. dollars / US gallon) = 0.849974428 Swiss francs / l
			gasPricePerLiter= 0.85;
			
		}
	}
	
	
	
	private void setUpGasPrice(double gasHigh){
		
			gasPricePerLiter= gasHigh; 
	}
	
	
	private void setBatteryTypes(double kWHEV, double kWHPHEV){
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
		double batterySizeEV= kWHEV*3600*1000; 
		double batterySizePHEV= kWHPHEV*3600*1000; 
		double batteryMinEV= 0.1; 		
		double batteryMinPHEV= 0.1; 
		double batteryMaxEV= 0.9; 
		double batteryMaxPHEV= 0.9; 		
		
		EVBattery = new Battery(batterySizeEV, batteryMinEV, batteryMaxEV);
		PHEVBattery = new Battery(batterySizePHEV, batteryMinPHEV, batteryMaxPHEV);
	}
	
	
	public void setNormalGas(){
		/*
		 * GAS TYPES
		 * - Specify gas types and their characteristics
		 * 
		 * - Gas Price [currency]
		 * - Joules per liter in gas [J]
		 * - emissions of Co2 per liter gas [kg]
		 
		 */
		//JoulesPerLiter = theoretisch moegliche Energie * Wirkungsgrad; 
		//http://de.wikipedia.org/wiki/Wirkungsgrad
		//http://de.wikipedia.org/wiki/Motorenbenzin
		double massPerLiter=0.75;// kg/l
		double gasJoulesPerLiter = 43.0*Math.pow(10,6)*massPerLiter;// Benzin 42,7–44,2 MJ/kg  * 0.75
		
		double emissionPerLiter = 2.36; // 2,36 kg/L
		
		normalGas=new GasType("normal gas", 
				gasJoulesPerLiter, 
				gasPricePerLiter, 
				emissionPerLiter);
	}
	
	
	public void setVehicleTypes(){
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
		EVTypeStandard= new VehicleType("standard EV", 
				EVBattery, 
				null, 
				ElectricVehicle.class,
				80000,// Nissan leaf 80kW Engine
				1.0);
		
		PHEVTypeStandard= new VehicleType("standard PHEV", 
				PHEVBattery, 
				normalGas, 
				PlugInHybridElectricVehicle.class,
				80000,
				0.3);
		
	}
	
}
