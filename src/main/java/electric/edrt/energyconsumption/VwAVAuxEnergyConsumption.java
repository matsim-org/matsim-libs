package electric.edrt.energyconsumption;

import org.matsim.vsp.ev.discharging.AuxEnergyConsumption;


/**
 * 
 * @author Joschka Bischoff
 * This class contains confidential values and should not be used outside the VW projects. 
 * There's intentionally no GPL header.
 *
 */
public class VwAVAuxEnergyConsumption implements AuxEnergyConsumption {

	//Verbrauch Bordnetz konstant 1,5KW -> 1,5kWh/h -> 0,025kWh/min
	private static double auxConsumption_per_s = 1500;

	//Verbrauch Systeme automatische Fahren konstant 1,5KW -> 1,5kWh/h --> 1500Ws/s 
	private static double AVauxConsumption_per_s = 1500;
	
	@Override
	public double calcEnergyConsumption(double period) {
		return period*(AVauxConsumption_per_s+auxConsumption_per_s);
	}

}
