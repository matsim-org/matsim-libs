package electric.edrt.energyconsumption;

import org.jfree.util.Log;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;


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

		//double speed_kmh = link.getLength() / travelTime * 3.6;		
		double speedControl_kmh = link.getLength() / travelTime * 3.6;
		if (speedControl_kmh > 130) {
			// Raserkante Ausgeben mit Geschwindigkeit
			Log.warn("Kante:"+ link.getId()+" Geschwindigkeit zu hoch!:" + speedControl_kmh + "kmh");			
		}
	
		
		//Test von Joschka
		double speed_kmh = Math.min(130,link.getLength() / travelTime * 3.6);
		
//		double energyConsumtion = (0.00215147989604308*Math.pow(speed_kmh,2.0)+ 0.0315951009941873 *speed_kmh + 14.1494158004944)*link.getLength()*EvUnits.J_m_PER_kWh_100km;
		double energyConsumtion = (0.0015 * Math.pow(speed_kmh, 2.0) + 13)*link.getLength()*EvUnits.J_m_PER_kWh_100km;
		double defaultConsumption = link.getLength() * consumption_per_m;
		return energyConsumtion;
		
		
	}

}
