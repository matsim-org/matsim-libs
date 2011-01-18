package playground.jjoubert.roadpricing.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import nl.knaw.dans.common.dbflib.CorruptedTableException;
import nl.knaw.dans.common.dbflib.IfNonExistent;
import nl.knaw.dans.common.dbflib.Record;
import nl.knaw.dans.common.dbflib.Table;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestCase;

public class MyGantryComparatorTest extends MatsimTestCase{
	private final static Logger log = Logger.getLogger(MyGantryComparatorTest.class);
	
	@SuppressWarnings("unused")
	public void testMyGantryComparatorConstructor(){
		createLinkstatsFiles();
		try {
			MyGantryComparator mgc = new MyGantryComparator(
					getOutputDirectory() + "dummy.txt.gz", 
					getOutputDirectory() + "Output2/ITERS/it.50/50.linkstats.txt.gz", 
					getClassInputDirectory() + "gantryLinks.txt");
			fail("Base file does not exist");
		} catch (FileNotFoundException e) {
			log.info("Caught `base file not found' exception correctly.");
		}

		try {
			MyGantryComparator mgc = new MyGantryComparator(
					getOutputDirectory() + "Output1/ITERS/it.50/50.linkstats.txt.gz", 
					getOutputDirectory() + "dummy.txt.gz", 
					getClassInputDirectory() + "gantryLinks.txt");
			fail("Comparison file does not exist");
		} catch (FileNotFoundException e) {
			log.info("Caught `compare file not found' exception correctly.");
		}

		try {
			MyGantryComparator mgc = new MyGantryComparator(
					getOutputDirectory() + "Output1/ITERS/it.50/50.linkstats.txt.gz", 
					getOutputDirectory() + "Output2/ITERS/it.50/50.linkstats.txt.gz", 
					getOutputDirectory() + "dummy.txt.gz");
			fail("Link Id file does not exist");
		} catch (FileNotFoundException e) {
			log.info("Caught `link Id file not found' exception correctly.");
		}
	}
	
	
	public void testCompare(){
		createLinkstatsFiles();
		MyGantryComparator mgc = null;
		try {
			mgc = new MyGantryComparator(
					getOutputDirectory() + "Output1/ITERS/it.50/50.linkstats.txt.gz", 
					getOutputDirectory() + "Output2/ITERS/it.50/50.linkstats.txt.gz", 
					getClassInputDirectory() + "gantryLinks.txt");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		mgc.compareTotalCount();

		/* Check that the right number of links are compared. */
		assertEquals("Wrong number of link Ids to compare.", 9, mgc.getLinkList().size());
		assertEquals("Wrong number of links in base map.", 9, mgc.getBaseMap().size());
		assertEquals("Wrong number of links in compared map.", 9, mgc.getCompareMap().size());

		/* Check that links 2, 6 & 10 have the right AVG totals in base file. */
		assertEquals("Wrong number of vehicles for link 2 in base file.", Integer.valueOf(11), Integer.valueOf(mgc.getBaseMap().get(new IdImpl(2))));
		assertEquals("Wrong number of vehicles for link 6 in base file.", Integer.valueOf(9), Integer.valueOf(mgc.getBaseMap().get(new IdImpl(6))));
		assertEquals("Wrong number of vehicles for link 10 in base file.", Integer.valueOf(23), Integer.valueOf(mgc.getBaseMap().get(new IdImpl(10))));

		/* Check that links 2, 6 & 10 have the right AVG totals in comparison file. */
		assertEquals("Wrong number of vehicles for link 2 in compare file.", Integer.valueOf(11), Integer.valueOf(mgc.getCompareMap().get(new IdImpl(2))));
		assertEquals("Wrong number of vehicles for link 6 in compare file.", Integer.valueOf(9), Integer.valueOf(mgc.getCompareMap().get(new IdImpl(6))));
		assertEquals("Wrong number of vehicles for link 10 in compare file.", Integer.valueOf(26), Integer.valueOf(mgc.getCompareMap().get(new IdImpl(10))));

	}
	
	
	public void testWriteComparisontoFile(){
		createLinkstatsFiles();
		MyGantryComparator mgc = null;
		try {
			mgc = new MyGantryComparator(
					getOutputDirectory() + "Output1/ITERS/it.50/50.linkstats.txt.gz", 
					getOutputDirectory() + "Output2/ITERS/it.50/50.linkstats.txt.gz", 
					getClassInputDirectory() + "gantryLinks.txt");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		mgc.compareTotalCount();
		mgc.writeComparisonToFile(getOutputDirectory() + "comparison.txt");
		
		List<String> list = new ArrayList<String>();
		Map<Id, String> map = new HashMap<Id, String>();
		try {
			BufferedReader br = IOUtils.getBufferedReader(getOutputDirectory() + "comparison.txt");
			try{
				String line = null;
				while((line = br.readLine()) != null){
					list.add(line);
					map.put(new IdImpl(line.split(",")[0]), line);
				}
			} finally{
				br.close();
			}
		} catch (FileNotFoundException e) {
			fail("Incorrect `FileNotFound' exception thrown.");
		} catch (IOException e) {
			fail("Incorrect `IOException' thrown.");
		}
		
		assertTrue("Could not find link 2.", map.containsKey(new IdImpl(2)));
		assertEquals("Wrong values for link 2.", true, map.get(new IdImpl(2)).equalsIgnoreCase("2,11,11,0.0000"));
		assertTrue("Could not find link 3.", map.containsKey(new IdImpl(3)));
		assertEquals("Wrong values for link 3.", true, map.get(new IdImpl(3)).equalsIgnoreCase("3,9,10,0.1111"));
		assertTrue("Could not find link 4.", map.containsKey(new IdImpl(4)));
		assertEquals("Wrong values for link 4.", true, map.get(new IdImpl(4)).equalsIgnoreCase("4,9,11,0.2222"));
		assertTrue("Could not find link 5.", map.containsKey(new IdImpl(5)));
		assertEquals("Wrong values for link 5.", true, map.get(new IdImpl(5)).equalsIgnoreCase("5,9,9,0.0000"));
		assertTrue("Could not find link 6.", map.containsKey(new IdImpl(6)));
		assertEquals("Wrong values for link 6.", true, map.get(new IdImpl(6)).equalsIgnoreCase("6,9,9,0.0000"));
		assertTrue("Could not find link 7.", map.containsKey(new IdImpl(7)));
		assertEquals("Wrong values for link 7.", true, map.get(new IdImpl(7)).equalsIgnoreCase("7,7,10,0.4286"));
		assertTrue("Could not find link 8.", map.containsKey(new IdImpl(8)));
		assertEquals("Wrong values for link 8.", true, map.get(new IdImpl(8)).equalsIgnoreCase("8,9,10,0.1111"));
		assertTrue("Could not find link 9.", map.containsKey(new IdImpl(9)));
		assertEquals("Wrong values for link 9.", true, map.get(new IdImpl(9)).equalsIgnoreCase("9,9,10,0.1111"));
		assertTrue("Could not find link 10.", map.containsKey(new IdImpl(10)));
		assertEquals("Wrong values for link 10.", true, map.get(new IdImpl(10)).equalsIgnoreCase("10,23,26,0.1304"));
	}
	
	
	public void testWriteComparisonToDbf(){
		createLinkstatsFiles();
		MyGantryComparator mgc = null;
		try {
			mgc = new MyGantryComparator(
					getOutputDirectory() + "Output1/ITERS/it.50/50.linkstats.txt.gz", 
					getOutputDirectory() + "Output2/ITERS/it.50/50.linkstats.txt.gz", 
					getClassInputDirectory() + "gantryLinks.txt");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		mgc.compareTotalCount();
		
		/* Write comparison to file, and check that comparison id calculated correctly. */
		mgc.writeComparisonToDbf(getOutputDirectory() + "comparison.dbf");
		
		Table t = new Table(new File(getOutputDirectory() + "comparison.dbf"));
		try {
			t.open(IfNonExistent.ERROR);
			try{
				Iterator<Record> it = t.recordIterator();
				while(it.hasNext()){
					Record r = it.next();
					int id = r.getNumberValue("linkId").intValue();
					switch (id) {
					case 2:
						assertEquals("Wrong base count for link " + id, 11, r.getNumberValue("baseCount").intValue());
						assertEquals("Wrong compare count for link " + id, 11, r.getNumberValue("compCount").intValue());
						assertEquals("Wrong change value for link " + id, 0.0, r.getNumberValue("change").doubleValue());
						break;
					case 3:
						assertEquals("Wrong base count for link " + id, 9, r.getNumberValue("baseCount").intValue());
						assertEquals("Wrong compare count for link " + id, 10, r.getNumberValue("compCount").intValue());
						assertEquals("Wrong change value for link " + id, 0.1111, r.getNumberValue("change").doubleValue());
						break;
					case 4:
						assertEquals("Wrong base count for link " + id, 9, r.getNumberValue("baseCount").intValue());
						assertEquals("Wrong compare count for link " + id, 11, r.getNumberValue("compCount").intValue());
						assertEquals("Wrong change value for link " + id, 0.2222, r.getNumberValue("change").doubleValue());
						break;
					case 5:
						assertEquals("Wrong base count for link " + id, 9, r.getNumberValue("baseCount").intValue());
						assertEquals("Wrong compare count for link " + id, 9, r.getNumberValue("compCount").intValue());
						assertEquals("Wrong change value for link " + id, 0.0, r.getNumberValue("change").doubleValue());
						break;
					case 6:
						assertEquals("Wrong base count for link " + id, 9, r.getNumberValue("baseCount").intValue());
						assertEquals("Wrong compare count for link " + id, 9, r.getNumberValue("compCount").intValue());
						assertEquals("Wrong change value for link " + id, 0.0, r.getNumberValue("change").doubleValue());
						break;
					case 7:
						assertEquals("Wrong base count for link " + id, 7, r.getNumberValue("baseCount").intValue());
						assertEquals("Wrong compare count for link " + id, 10, r.getNumberValue("compCount").intValue());
						assertEquals("Wrong change value for link " + id, 0.4286, r.getNumberValue("change").doubleValue());
						break;
					case 8:
						assertEquals("Wrong base count for link " + id, 9, r.getNumberValue("baseCount").intValue());
						assertEquals("Wrong compare count for link " + id, 10, r.getNumberValue("compCount").intValue());
						assertEquals("Wrong change value for link " + id, 0.1111, r.getNumberValue("change").doubleValue());
						break;
					case 9:
						assertEquals("Wrong base count for link " + id, 9, r.getNumberValue("baseCount").intValue());
						assertEquals("Wrong compare count for link " + id, 10, r.getNumberValue("compCount").intValue());
						assertEquals("Wrong change value for link " + id, 0.1111, r.getNumberValue("change").doubleValue());
						break;
					case 10:
						assertEquals("Wrong base count for link " + id, 23, r.getNumberValue("baseCount").intValue());
						assertEquals("Wrong compare count for link " + id, 26, r.getNumberValue("compCount").intValue());
						assertEquals("Wrong change value for link " + id, 0.1304, r.getNumberValue("change").doubleValue());
						break;						
					default:
						break;
					}
				}

			}finally{
				t.close();
			}
		} catch (CorruptedTableException e) {
			fail("Incorrect `corrupted table' exception thrown.");
		} catch (IOException e) {
			fail("Incorrect `IOException' thrown.");
		}

	}


	private void createLinkstatsFiles(){
		Config config = loadConfig(getClassInputDirectory() + "config.xml");
		config.network().setInputFile(getClassInputDirectory() + "networkSmall.xml.gz");
		config.plans().setInputFile(getClassInputDirectory() + "plans100.xml.gz");
		config.controler().setOutputDirectory(getOutputDirectory() + "Output1/");
		
		Controler c = new Controler(config);
		c.setCreateGraphs(false);
		c.setOverwriteFiles(true);
		c.run();
		
		config.plans().setInputFile(getClassInputDirectory() + "plans110.xml.gz");
		config.controler().setOutputDirectory(getOutputDirectory() + "Output2/");
		
		c = new Controler(config);
		c.setCreateGraphs(false);
		c.setOverwriteFiles(true);
		c.run();		
	}

}
