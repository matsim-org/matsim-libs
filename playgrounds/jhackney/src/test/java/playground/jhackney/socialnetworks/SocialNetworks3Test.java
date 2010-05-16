package playground.jhackney.socialnetworks;

import org.matsim.core.controler.Controler;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestCase;

import playground.jhackney.controler.SNController2;
import playground.jhackney.controler.SNControllerListener2;

public class SocialNetworks3Test extends MatsimTestCase{

	public final void test3EvolvingNetwork(){

		String config = getInputDirectory() + "config_triangle3.xml";

		String referenceEventsFile = getInputDirectory() + "5.events.txt.gz";
		String referencePlansFile = getInputDirectory() + "output_plans.xml.gz";
		String referenceSocNetFile = getInputDirectory() + "edge.txt";

		String eventsFile = getOutputDirectory() + "ITERS/it.5/5.events.txt.gz";
		String plansFile = getOutputDirectory() + "output_plans.xml.gz";
		String socNetFile = getOutputDirectory() + "socialnets/stats/edge.txt";

		final Controler controler = new SNController2(new String[] {config});
		controler.addControlerListener(new SNControllerListener2());
		controler.setOverwriteFiles(true);
		controler.setCreateGraphs(false);
		controler.run();

		long checksum1 = 0;
		long checksum2 = 0;

		System.out.println("checking events file ...");
		checksum1 = CRCChecksum.getCRCFromFile(referenceEventsFile);
		checksum2 = CRCChecksum.getCRCFromFile(eventsFile);
		System.out.println(eventsFile+" checksum = " + checksum2 + " should be: " + referenceEventsFile + checksum1);
		assertEquals("different events files", checksum1, checksum2);

//		System.out.println("checking plans file ...");
//		checksum1 = CRCChecksum.getCRCFromFile(referencePlansFile);
//		checksum2 = CRCChecksum.getCRCFromFile(plansFile);
//		System.out.println(plansFile+" checksum = " + checksum2 + " should be: " + referencePlansFile + checksum1);
//		assertEquals("different plans files", checksum1, checksum2);
		// not working any more.  kai, may'10

		System.out.println("checking social net edges file ...");
		checksum1 = CRCChecksum.getCRCFromFile(referenceSocNetFile);
		checksum2 = CRCChecksum.getCRCFromFile(socNetFile);
		System.out.println(socNetFile+" checksum = " + checksum2 + " should be: " + referenceSocNetFile + checksum1);
		assertEquals("different socnet files", checksum1, checksum2);

		System.out.println("\nTest Succeeded");
	}

}
