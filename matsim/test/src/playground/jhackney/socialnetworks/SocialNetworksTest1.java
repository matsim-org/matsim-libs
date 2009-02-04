package playground.jhackney.socialnetworks;

import org.matsim.controler.Controler;
import org.matsim.gbl.Gbl;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.CRCChecksum;

import playground.jhackney.controler.SNControllerListener2;

//import playground.jhackney.controler.SNControllerListener2;

public class SocialNetworksTest1 extends MatsimTestCase{

	public final void test1EvolvingNetwork(){
		String config = getInputDirectory() + "config_triangle1.xml";
		String referenceEventsFile = getInputDirectory() + "5.events.txt.gz";
		String referencePlansFile = getInputDirectory() + "output_plans.xml.gz";
		String referenceSocNetFile = getInputDirectory() + "graph.txt";
		
		String eventsFile = getOutputDirectory() + "ITERS/it.5/5.events.txt.gz";
		String plansFile = getOutputDirectory() + "output_plans.xml.gz";
		String socNetFile = getOutputDirectory() + "socialnets/stats/graph.txt";

		final Controler controler = new Controler(new String[] {config});
		controler.addControlerListener(new SNControllerListener2()); // had to comment this line out because SNControllerListener2 is in the playground, but this class is in core
		controler.setOverwriteFiles(true);
		controler.run();

		long checksum1 = 0;
		long checksum2 = 0;

		System.out.println("checking events file ...");
		checksum1 = CRCChecksum.getCRCFromGZFile(referenceEventsFile);
		checksum2 = CRCChecksum.getCRCFromGZFile(eventsFile);
		System.out.println(eventsFile+" checksum = " + checksum2 + " should be: " + referenceEventsFile + checksum1);
		assertEquals("different events files", checksum1, checksum2);

		System.out.println("checking plans file ...");
		checksum1 = CRCChecksum.getCRCFromGZFile(referencePlansFile);
		checksum2 = CRCChecksum.getCRCFromGZFile(plansFile);
		System.out.println(plansFile+" checksum = " + checksum2 + " should be: " + referencePlansFile + checksum1);
		assertEquals("different plans files", checksum1, checksum2);
		
		System.out.println("checking social net edges file ...");
		checksum1 = CRCChecksum.getCRCFromFile(referenceSocNetFile);
		checksum2 = CRCChecksum.getCRCFromFile(socNetFile);
		System.out.println(socNetFile+" checksum = " + checksum2 + " should be: " + referenceSocNetFile + checksum1);
		assertEquals("different socnet files", checksum1, checksum2);
		
		System.out.println("\nTest Succeeded");
	}
	protected void tearDown() throws Exception {
        super.tearDown();
        Gbl.reset();
        // 
    }
}
