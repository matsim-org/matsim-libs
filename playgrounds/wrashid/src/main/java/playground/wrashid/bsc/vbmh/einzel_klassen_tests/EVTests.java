package playground.wrashid.bsc.vbmh.einzel_klassen_tests;

import org.matsim.core.basic.v01.IdImpl;

import playground.wrashid.bsc.vbmh.vmEV.EVControl;

public class EVTests {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		EVControl evControl = new EVControl();
		evControl.startUp("input/SF_PLUS/generalinput/evs.xml", null);
		IdImpl personId = new IdImpl("35287_1");
		IdImpl personIdb = new IdImpl("39780_1");
		System.out.println(evControl.hasEV(personIdb));
		System.out.println(evControl.stateOfChargePercentage(personId));
		System.out.println(evControl.clalcChargedAmountOfEnergy(personId, 8.04, -3600));
		System.out.println(evControl.calcNewStateOfChargePercentage(personId, 8.04, 2500));
		System.out.println(evControl.stateOfChargePercentage(personId));
	}

}
