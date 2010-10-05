package playground.wrashid.PSF2.chargingSchemes.dumbCharging;

import playground.wrashid.PSF.ParametersPSFMutator;
import playground.wrashid.PSF.PSS.PSSControler;

public class LocalHost implements ParametersPSFMutator {

	public static void main(String[] args) {		
		// for starting one the local computer
		PSSControler pssControler=new PSSControlerDumbCharging("a:/data/matsim/input/runRW1002/config.xml", null);
		pssControler.runMATSimIterations();
		
	} 

	public void mutateParameters() {
		// TODO Auto-generated method stub
		
	}
	
}
