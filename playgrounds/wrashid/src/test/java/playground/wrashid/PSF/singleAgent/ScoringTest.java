package playground.wrashid.PSF.singleAgent;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.PSF.ParametersPSF;
import playground.wrashid.PSF.ParametersPSFMutator;
import playground.wrashid.PSF.PSS.PSSControler;
import playground.wrashid.PSF.data.HubPriceInfo;
import playground.wrashid.PSF.energy.AfterSimulationListener;
import playground.wrashid.PSF.energy.charging.ChargeLog;
import playground.wrashid.PSF.energy.charging.ChargingTimes;

public class ScoringTest extends MatsimTestCase {
	
	// this test has unveiled a bug in the optimized charger, therefore it is added.
	public void testScoring1() {
		ParametersPSFMutator paramMutator=new ParametersPSFMutator() {
			@Override
			public void mutateParameters() {
				ParametersPSF.setHubPriceInfo(new HubPriceInfo(45000,65000,0.18,0.09));
			}
		};
		
		PSSControler pssControler=new PSSControler("test/input/playground/wrashid/PSF/singleAgent/config9.xml", paramMutator);

		pssControler.getConfig().plansCalcRoute().setInsertingAccessEgressWalk(false);
		// too many things don't work with access/egress walk true. kai, jun'16

		pssControler.runMATSimIterations();
		
		ChargingTimes chargingTimesOfAgentOne = AfterSimulationListener.getChargingTimes().get(Id.create("1", Person.class));
		ChargeLog chargeLogOfAgentOne;
		
		// TODO: has problem with update of jdeqsim/ empty car routes -> why, unknown at the moment
		//chargeLogOfAgentOne = chargingTimesOfAgentOne.getChargingTimes().get(25);
		//assertEquals(ParametersPSF.getDefaultMaxBatteryCapacity(), chargeLogOfAgentOne.getEndSOC(), 1.0); 
	}    
}
