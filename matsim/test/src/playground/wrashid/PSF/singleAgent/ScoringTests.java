package playground.wrashid.PSF.singleAgent;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.PSF.ParametersPSF;
import playground.wrashid.PSF.ParametersPSFMutator;
import playground.wrashid.PSF.PSS.PSSControler;
import playground.wrashid.PSF.data.HubPriceInfo;
import playground.wrashid.PSF.energy.AfterSimulationListener;
import playground.wrashid.PSF.energy.charging.ChargeLog;
import playground.wrashid.PSF.energy.charging.ChargingTimes;

public class ScoringTests extends MatsimTestCase {
	
	// this test has unveiled a bug in the optimized charger, therefore it is added.
	public void testScoring1() {
		ParametersPSFMutator paramMutator=new ParametersPSFMutator() {
			public void mutateParameters() {
				ParametersPSF.setHubPriceInfo(new HubPriceInfo(45000,65000,0.18,0.09));
			}
		};
		
		PSSControler pssControler=new PSSControler("test/input/playground/wrashid/PSF/singleAgent/config9.xml", paramMutator);
		pssControler.runMATSimIterations();
		
		ChargingTimes chargingTimesOfAgentOne = AfterSimulationListener.getChargingTimes().get(new IdImpl("1"));
		ChargeLog chargeLogOfAgentOne;
		
		// TODO: has problem with update of jdeqsim/ empty car routes -> why, unknown at the moment
		//chargeLogOfAgentOne = chargingTimesOfAgentOne.getChargingTimes().get(25);
		//assertEquals(ParametersPSF.getDefaultMaxBatteryCapacity(), chargeLogOfAgentOne.getEndSOC(), 1.0); 
	}    
}
