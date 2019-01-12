package electric.edrt.energyconsumption;

import org.matsim.api.core.v01.network.Link;
import org.matsim.vsp.ev.EvUnitConversions;
import org.matsim.vsp.ev.discharging.DriveEnergyConsumption;


/**
 * 
 * @author Joschka Bischoff
 * This class contains confidential values and should not be used outside the VW projects. 
 * There's intentionally no GPL header.
 *
 */
public class VwDrtDriveEnergyConsumption implements DriveEnergyConsumption {

	//Verbrauch pro km: 20kWh auf 100km (nur Fahren!)
	static final double consumption_per_m = 20.0 * EvUnitConversions.J_m_PER_kWh_100km;

	//Verbrauch Bordnetz konstant 1,5KW -> 1,5kWh/h -> 0,025kWh/min
	private static double auxConsumption_per_s = 1500;

	//Verbrauch Systeme automatische Fahren konstant 1,5KW -> 1,5kWh/h --> 1500Ws/s
	private static double AVauxConsumption_per_s = 1500;


	@Override
	public double calcEnergyConsumption(Link link, double travelTime) {

		return link.getLength() * consumption_per_m + travelTime * (auxConsumption_per_s + AVauxConsumption_per_s);
		
		
	}

}
