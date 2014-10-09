package playground.vbmh.einzel_klassen_tests;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import playground.vbmh.vmEV.EVControl;

public class EVTests {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		EVControl evControl = new EVControl();
		evControl.startUp("input/SF_PLUS/generalinput/evs.xml", null);
		Id<Person> personId = Id.create("35287_1", Person.class);
		Id<Person> personIdb = Id.create("39780_1", Person.class);
		System.out.println(evControl.hasEV(personIdb));
		System.out.println(evControl.stateOfChargePercentage(personId));
		System.out.println(evControl.clalcChargedAmountOfEnergy(personId, 8.04, -3600));
		System.out.println(evControl.calcNewStateOfChargePercentage(personId, 32, 0.5*3600));
		System.out.println(evControl.stateOfChargePercentage(personId));
	}

}
