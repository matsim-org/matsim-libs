package org.matsim.socialnetworks;

import org.matsim.controler.Controler;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.CRCChecksum;

import playground.jhackney.controler.SNController2;
import playground.jhackney.controler.SNControllerListener2;

public class SocialNetworksTest extends MatsimTestCase{
	
	public final void testEvolvingNetwork(){
		String config = getInputDirectory() + "/config_triangle1.xml";
		
		String referenceEventsFile = getInputDirectory() + "/5.events.txt.gz";
		String referencePlansFile = getInputDirectory() + "/output_plans.xml.gz";
		String referenceSocNetFile = getInputDirectory() + "/edge.txt";
		
		String eventsFile = getOutputDirectory() + "/ITERS/it.5/5.events.txt.gz";
		String plansFile = getOutputDirectory() + "/output_plans.xml.gz";
		String socNetFile = getOutputDirectory() + "/socialnets/stats/edge.txt";

		final Controler controler = new SNController2(new String[] {config});
		controler.addControlerListener(new SNControllerListener2());
		controler.setOverwriteFiles(true);
		controler.run();

		long checksum1 = 0;
		long checksum2 = 0;

		System.out.println("checking events file ...");
		checksum1 = CRCChecksum.getCRCFromGZFile(referenceEventsFile);
		checksum2 = CRCChecksum.getCRCFromGZFile(eventsFile);
		System.out.println("checksum = " + checksum2 + " should be: " + checksum1);
		assertEquals("different events files", checksum1, checksum2);

		System.out.println("checking plans file ...");
		checksum1 = CRCChecksum.getCRCFromGZFile(referencePlansFile);
		checksum2 = CRCChecksum.getCRCFromGZFile(plansFile);
		System.out.println("checksum = " + checksum2 + " should be: " + checksum1);
		assertEquals("different plans files", checksum1, checksum2);
		
		System.out.println("checking social net edges file ...");
		checksum1 = CRCChecksum.getCRCFromFile(referenceSocNetFile);
		checksum2 = CRCChecksum.getCRCFromFile(socNetFile);
		System.out.println("checksum = " + checksum2 + " should be: " + checksum1);
		assertEquals("different socnet files", checksum1, checksum2);
	}

}
