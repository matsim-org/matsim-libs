package playground.jbischoff.taxi.sim;

import org.matsim.core.mobsim.qsim.interfaces.Netsim;

import pl.poznan.put.vrp.dynamic.optimizer.taxi.TaxiOptimizer;
import playground.jbischoff.energy.charging.DepotArrivalDepartureCharger;
import playground.michalm.vrp.data.MatsimVrpData;
import playground.michalm.vrp.taxi.TaxiSimEngine;

public class ElectricTaxiSimEngine extends TaxiSimEngine {
private  DepotArrivalDepartureCharger dac;
	public ElectricTaxiSimEngine(Netsim netsim, MatsimVrpData data,
			TaxiOptimizer optimizer, DepotArrivalDepartureCharger dac) {
		super(netsim, data, optimizer);
		this.dac=dac;
	}

	  @Override
	    public void doSimStep(double time)
	    {
		  if (time%60==0){
		  this.dac.chargeAllVehiclesInDepots(time, 60);
		  this.dac.refreshLog(time);
		  }
	    }

	
}
