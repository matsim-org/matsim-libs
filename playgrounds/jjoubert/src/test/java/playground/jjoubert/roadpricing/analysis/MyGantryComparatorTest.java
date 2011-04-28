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
		assertEquals("Wrong number of vehicles for link 2 in base file.", 13.6, mgc.getBaseMap().get(new IdImpl(2)).doubleValue(), 1.e-8);
		assertEquals("Wrong number of vehicles for link 6 in base file.", 10.2, mgc.getBaseMap().get(new IdImpl(6)), 1.e-8);
		assertEquals("Wrong number of vehicles for link 10 in base file.", 11.0, mgc.getBaseMap().get(new IdImpl(10)), 1.e-8);

		/* Check that links 2, 6 & 10 have the right AVG totals in comparison file. */
		assertEquals("Wrong number of vehicles for link 2 in compare file.", 11.0, mgc.getCompareMap().get(new IdImpl(2)), 1.e-8);
		assertEquals("Wrong number of vehicles for link 6 in compare file.", 11.0, mgc.getCompareMap().get(new IdImpl(6)), 1.e-8);
		assertEquals("Wrong number of vehicles for link 10 in compare file.", 15.4, mgc.getCompareMap().get(new IdImpl(10)), 1.e-8);

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
		assertEquals("Wrong values for link 2.", "2,13.6,11.0,-0.1912", map.get(new IdImpl(2)));
		assertTrue("Could not find link 3.", map.containsKey(new IdImpl(3)));
		assertEquals("Wrong values for link 3.", "3,9.6,12.6,0.3125", map.get(new IdImpl(3)));
		assertTrue("Could not find link 4.", map.containsKey(new IdImpl(4)));
		assertEquals("Wrong values for link 4.", "4,8.8,12.0,0.3636", map.get(new IdImpl(4)));
		assertTrue("Could not find link 5.", map.containsKey(new IdImpl(5)));
		assertEquals("Wrong values for link 5.", "5,12.6,11.4,-0.0952", map.get(new IdImpl(5)));
		assertTrue("Could not find link 6.", map.containsKey(new IdImpl(6)));
		assertEquals("Wrong values for link 6.", "6,10.2,11.0,0.0784", map.get(new IdImpl(6)));
		assertTrue("Could not find link 7.", map.containsKey(new IdImpl(7)));
		assertEquals("Wrong values for link 7.", "7,10.2,11.2,0.0980", map.get(new IdImpl(7)));
		assertTrue("Could not find link 8.", map.containsKey(new IdImpl(8)));
		assertEquals("Wrong values for link 8.", "8,10.4,14.0,0.3462", map.get(new IdImpl(8)));
		assertTrue("Could not find link 9.", map.containsKey(new IdImpl(9)));
		assertEquals("Wrong values for link 9.", "9,13.6,11.4,-0.1618", map.get(new IdImpl(9)));
		assertTrue("Could not find link 10.", map.containsKey(new IdImpl(10)));
		assertEquals("Wrong values for link 10.", "10,11.0,15.4,0.4000", map.get(new IdImpl(10)));
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
						assertEquals("Wrong base count for link " + id, 14, r.getNumberValue("baseCount").intValue());
						assertEquals("Wrong compare count for link " + id, 11, r.getNumberValue("compCount").intValue());
						assertEquals("Wrong change value for link " + id, -0.1912, r.getNumberValue("change").doubleValue());
						break;
					case 3:
						assertEquals("Wrong base count for link " + id, 10, r.getNumberValue("baseCount").intValue());
						assertEquals("Wrong compare count for link " + id, 13, r.getNumberValue("compCount").intValue());
						assertEquals("Wrong change value for link " + id, 0.3125, r.getNumberValue("change").doubleValue());
						break;
					case 4:
						assertEquals("Wrong base count for link " + id, 9, r.getNumberValue("baseCount").intValue());
						assertEquals("Wrong compare count for link " + id, 12, r.getNumberValue("compCount").intValue());
						assertEquals("Wrong change value for link " + id, 0.3636, r.getNumberValue("change").doubleValue());
						break;
					case 5:
						assertEquals("Wrong base count for link " + id, 13, r.getNumberValue("baseCount").intValue());
						assertEquals("Wrong compare count for link " + id, 11, r.getNumberValue("compCount").intValue());
						assertEquals("Wrong change value for link " + id, -0.0952, r.getNumberValue("change").doubleValue());
						break;
					case 6:
						assertEquals("Wrong base count for link " + id, 10, r.getNumberValue("baseCount").intValue());
						assertEquals("Wrong compare count for link " + id, 11, r.getNumberValue("compCount").intValue());
						assertEquals("Wrong change value for link " + id, 0.0784, r.getNumberValue("change").doubleValue());
						break;
					case 7:
						assertEquals("Wrong base count for link " + id, 10, r.getNumberValue("baseCount").intValue());
						assertEquals("Wrong compare count for link " + id, 11, r.getNumberValue("compCount").intValue());
						assertEquals("Wrong change value for link " + id, 0.0980, r.getNumberValue("change").doubleValue());
						break;
					case 8:
						assertEquals("Wrong base count for link " + id, 10, r.getNumberValue("baseCount").intValue());
						assertEquals("Wrong compare count for link " + id, 14, r.getNumberValue("compCount").intValue());
						assertEquals("Wrong change value for link " + id, 0.3462, r.getNumberValue("change").doubleValue());
						break;
					case 9:
						assertEquals("Wrong base count for link " + id, 14, r.getNumberValue("baseCount").intValue());
						assertEquals("Wrong compare count for link " + id, 11, r.getNumberValue("compCount").intValue());
						assertEquals("Wrong change value for link " + id, -0.1618, r.getNumberValue("change").doubleValue());
						break;
					case 10:
						assertEquals("Wrong base count for link " + id, 11, r.getNumberValue("baseCount").intValue());
						assertEquals("Wrong compare count for link " + id, 15, r.getNumberValue("compCount").intValue());
						assertEquals("Wrong change value for link " + id, 0.4000, r.getNumberValue("change").doubleValue());
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
		
		config.controler().setWriteEventsInterval(0);
		config.controler().setWritePlansInterval(0);
		
		Controler c = new Controler(config);
		c.setCreateGraphs(false);
		c.setDumpDataAtEnd(false);
		c.run();
		
		config.plans().setInputFile(getClassInputDirectory() + "plans110.xml.gz");
		config.controler().setOutputDirectory(getOutputDirectory() + "Output2/");
		
		c = new Controler(config);
		c.setCreateGraphs(false);
		c.setDumpDataAtEnd(false);
		c.run();		
	}

}
