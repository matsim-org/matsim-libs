package electric.edrt.energyconsumption;

import org.matsim.api.core.v01.network.Link;
import org.matsim.vsp.ev.EvUnits;
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
	static final double consumption_per_m = 20.0 * EvUnits.J_m_PER_kWh_100km;



	@Override
	public double calcEnergyConsumption(Link link, double travelTime) {

		return link.getLength() * consumption_per_m;
		
		
	}

}
