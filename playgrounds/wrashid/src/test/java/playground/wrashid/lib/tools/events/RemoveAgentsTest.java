package playground.wrashid.lib.tools.events;

import java.io.File;
import java.util.HashSet;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestCase;

public class RemoveAgentsTest extends MatsimTestCase {

	public void testBasic(){
		String inputEventsFile="test/input/playground/wrashid/PSF2/pluggable/0.events.txt.gz";
		
		String outputEventsFile=getOutputDirectory() + "output-events.txt.gz";
		
		HashSet<Id> agentIds = new HashSet<Id>();
		
		agentIds.add(new IdImpl("1"));
		
		RemoveAgents.removeAgents(agentIds, inputEventsFile, outputEventsFile);
		
		File inputFile = new File(inputEventsFile);
		File outputFile = new File(outputEventsFile);
		
		assertTrue("filtered output cannot be larger than input",inputFile.length()>outputFile.length());
		assertTrue("outputFile is empty",outputFile.length()>0);
	}
	
}
