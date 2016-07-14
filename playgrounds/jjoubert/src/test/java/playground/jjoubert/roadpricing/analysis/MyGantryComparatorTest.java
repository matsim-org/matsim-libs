package playground.jjoubert.roadpricing.analysis;

import nl.knaw.dans.common.dbflib.CorruptedTableException;
import nl.knaw.dans.common.dbflib.IfNonExistent;
import nl.knaw.dans.common.dbflib.Record;
import nl.knaw.dans.common.dbflib.Table;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class MyGantryComparatorTest{
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	private final static Logger log = Logger.getLogger(MyGantryComparatorTest.class);
	
	@SuppressWarnings("unused")
	@Test
	public void testMyGantryComparatorConstructor(){
		createLinkstatsFiles();
		try {
			MyGantryComparator mgc = new MyGantryComparator(
					utils.getOutputDirectory() + "dummy.txt.gz", 
					utils.getOutputDirectory() + "Output2/ITERS/it.50/50.linkstats.txt.gz", 
					utils.getClassInputDirectory() + "gantryLinks.txt");
			Assert.fail("Base file does not exist");
		} catch (FileNotFoundException e) {
			log.info("Caught `base file not found' exception correctly.");
		}

		try {
			MyGantryComparator mgc = new MyGantryComparator(
					utils.getOutputDirectory() + "Output1/ITERS/it.50/50.linkstats.txt.gz", 
					utils.getOutputDirectory() + "dummy.txt.gz", 
					utils.getClassInputDirectory() + "gantryLinks.txt");
			Assert.fail("Comparison file does not exist");
		} catch (FileNotFoundException e) {
			log.info("Caught `compare file not found' exception correctly.");
		}

		try {
			MyGantryComparator mgc = new MyGantryComparator(
					utils.getOutputDirectory() + "Output1/ITERS/it.50/50.linkstats.txt.gz", 
					utils.getOutputDirectory() + "Output2/ITERS/it.50/50.linkstats.txt.gz", 
					utils.getOutputDirectory() + "dummy.txt.gz");
			Assert.fail("Link Id file does not exist");
		} catch (FileNotFoundException e) {
			log.info("Caught `link Id file not found' exception correctly.");
		}
	}
	
	@Test
	public void testCompare(){
		createLinkstatsFiles();
		MyGantryComparator mgc = null;
		try {
			mgc = new MyGantryComparator(
					utils.getOutputDirectory() + "Output1/ITERS/it.50/50.linkstats.txt.gz", 
					utils.getOutputDirectory() + "Output2/ITERS/it.50/50.linkstats.txt.gz", 
					utils.getClassInputDirectory() + "gantryLinks.txt");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		mgc.compareTotalCount();

		/* Check that the right number of links are compared. */
		Assert.assertEquals("Wrong number of link Ids to compare.", 9, mgc.getLinkList().size());
		Assert.assertEquals("Wrong number of links in base map.", 9, mgc.getBaseMap().size());
		Assert.assertEquals("Wrong number of links in compared map.", 9, mgc.getCompareMap().size());

		/* Check that links 2, 6 & 10 have the right AVG totals in base file. */
		Assert.assertEquals("Wrong number of vehicles for link 2 in base file.", 13.0, mgc.getBaseMap().get(Id.create(2, Link.class)).doubleValue(), 1.e-8);
		Assert.assertEquals("Wrong number of vehicles for link 6 in base file.", 11.0, mgc.getBaseMap().get(Id.create(6, Link.class)), 1.e-8);
		Assert.assertEquals("Wrong number of vehicles for link 10 in base file.", 12.0, mgc.getBaseMap().get(Id.create(10, Link.class)), 1.e-8);

		/* Check that links 2, 6 & 10 have the right AVG totals in comparison file. */
		Assert.assertEquals("Wrong number of vehicles for link 2 in compare file.", 12.0, mgc.getCompareMap().get(Id.create(2, Link.class)), 1.e-8);
		Assert.assertEquals("Wrong number of vehicles for link 6 in compare file.", 11.0, mgc.getCompareMap().get(Id.create(6, Link.class)), 1.e-8);
		Assert.assertEquals("Wrong number of vehicles for link 10 in compare file.", 15.0, mgc.getCompareMap().get(Id.create(10, Link.class)), 1.e-8);

	}
	
	@Test
	public void testWriteComparisontoFile(){
		createLinkstatsFiles();
		MyGantryComparator mgc = null;
		try {
			mgc = new MyGantryComparator(
					utils.getOutputDirectory() + "Output1/ITERS/it.50/50.linkstats.txt.gz", 
					utils.getOutputDirectory() + "Output2/ITERS/it.50/50.linkstats.txt.gz", 
					utils.getClassInputDirectory() + "gantryLinks.txt");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		mgc.compareTotalCount();
		mgc.writeComparisonToFile(utils.getOutputDirectory() + "comparison.txt");
		
		List<String> list = new ArrayList<String>();
		Map<Id<Link>, String> map = new HashMap<Id<Link>, String>();
		try {
			BufferedReader br = IOUtils.getBufferedReader(utils.getOutputDirectory() + "comparison.txt");
			try{
				String line = null;
				while((line = br.readLine()) != null){
					list.add(line);
					map.put(Id.create(line.split(",")[0], Link.class), line);
				}
			} finally{
				br.close();
			}
		} catch (FileNotFoundException e) {
			Assert.fail("Incorrect `FileNotFound' exception thrown.");
		} catch (IOException e) {
			Assert.fail("Incorrect `IOException' thrown.");
		}
		
		Assert.assertTrue("Could not find link 2.", map.containsKey(Id.create(2, Link.class)));
		Assert.assertEquals("Wrong values for link 2.", "2,13.0,12.0,-0.0769", map.get(Id.create(2, Link.class)));
		Assert.assertTrue("Could not find link 3.", map.containsKey(Id.create(3, Link.class)));
		Assert.assertEquals("Wrong values for link 3.", "3,9.0,19.0,1.1111", map.get(Id.create(3, Link.class)));
		Assert.assertTrue("Could not find link 4.", map.containsKey(Id.create(4, Link.class)));
		Assert.assertEquals("Wrong values for link 4.", "4,7.0,11.0,0.5714", map.get(Id.create(4, Link.class)));
		Assert.assertTrue("Could not find link 5.", map.containsKey(Id.create(5, Link.class)));
		Assert.assertEquals("Wrong values for link 5.", "5,15.0,11.0,-0.2667", map.get(Id.create(5, Link.class)));
		Assert.assertTrue("Could not find link 6.", map.containsKey(Id.create(6, Link.class)));
		Assert.assertEquals("Wrong values for link 6.", "6,11.0,11.0,0.0000", map.get(Id.create(6, Link.class)));
		Assert.assertTrue("Could not find link 7.", map.containsKey(Id.create(7, Link.class)));
		Assert.assertEquals("Wrong values for link 7.", "7,9.0,12.0,0.3333", map.get(Id.create(7, Link.class)));
		Assert.assertTrue("Could not find link 8.", map.containsKey(Id.create(8, Link.class)));
		Assert.assertEquals("Wrong values for link 8.", "8,7.0,13.0,0.8571", map.get(Id.create(8, Link.class)));
		Assert.assertTrue("Could not find link 9.", map.containsKey(Id.create(9, Link.class)));
		Assert.assertEquals("Wrong values for link 9.", "9,17.0,6.0,-0.6471", map.get(Id.create(9, Link.class)));
		Assert.assertTrue("Could not find link 10.", map.containsKey(Id.create(10, Link.class)));
		Assert.assertEquals("Wrong values for link 10.", "10,12.0,15.0,0.2500", map.get(Id.create(10, Link.class)));
	}
	
	@Test
	public void testWriteComparisonToDbf(){
		createLinkstatsFiles();
		MyGantryComparator mgc = null;
		try {
			mgc = new MyGantryComparator(
					utils.getOutputDirectory() + "Output1/ITERS/it.50/50.linkstats.txt.gz", 
					utils.getOutputDirectory() + "Output2/ITERS/it.50/50.linkstats.txt.gz", 
					utils.getClassInputDirectory() + "gantryLinks.txt");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		mgc.compareTotalCount();
		
		/* Write comparison to file, and check that comparison id calculated correctly. */
		mgc.writeComparisonToDbf(utils.getOutputDirectory() + "comparison.dbf");
		
		Table t = new Table(new File(utils.getOutputDirectory() + "comparison.dbf"));
		try {
			t.open(IfNonExistent.ERROR);
			try{
				Iterator<Record> it = t.recordIterator();
				while(it.hasNext()){
					Record r = it.next();
					int id = r.getNumberValue("linkId").intValue();
					switch (id) {
					case 2:
						Assert.assertEquals("Wrong base count for link " + id, 13, r.getNumberValue("baseCount").intValue());
						Assert.assertEquals("Wrong compare count for link " + id, 12, r.getNumberValue("compCount").intValue());
						Assert.assertEquals("Wrong change value for link " + id, -0.0769, r.getNumberValue("change").doubleValue(), MatsimTestUtils.EPSILON);
						break;
					case 3:
						Assert.assertEquals("Wrong base count for link " + id, 9, r.getNumberValue("baseCount").intValue());
						Assert.assertEquals("Wrong compare count for link " + id, 19, r.getNumberValue("compCount").intValue());
						Assert.assertEquals("Wrong change value for link " + id, 1.1111, r.getNumberValue("change").doubleValue(), MatsimTestUtils.EPSILON);
						break;
					case 4:
						Assert.assertEquals("Wrong base count for link " + id, 7, r.getNumberValue("baseCount").intValue());
						Assert.assertEquals("Wrong compare count for link " + id, 11, r.getNumberValue("compCount").intValue());
						Assert.assertEquals("Wrong change value for link " + id, 0.5714, r.getNumberValue("change").doubleValue(), MatsimTestUtils.EPSILON);
						break;
					case 5:
						Assert.assertEquals("Wrong base count for link " + id, 15, r.getNumberValue("baseCount").intValue());
						Assert.assertEquals("Wrong compare count for link " + id, 11, r.getNumberValue("compCount").intValue());
						Assert.assertEquals("Wrong change value for link " + id, -0.2667, r.getNumberValue("change").doubleValue(), MatsimTestUtils.EPSILON);
						break;
					case 6:
						Assert.assertEquals("Wrong base count for link " + id, 11, r.getNumberValue("baseCount").intValue());
						Assert.assertEquals("Wrong compare count for link " + id, 11, r.getNumberValue("compCount").intValue());
						Assert.assertEquals("Wrong change value for link " + id, 0.0000, r.getNumberValue("change").doubleValue(), MatsimTestUtils.EPSILON);
						break;
					case 7:
						Assert.assertEquals("Wrong base count for link " + id, 9, r.getNumberValue("baseCount").intValue());
						Assert.assertEquals("Wrong compare count for link " + id, 12, r.getNumberValue("compCount").intValue());
						Assert.assertEquals("Wrong change value for link " + id, 0.3333, r.getNumberValue("change").doubleValue(), MatsimTestUtils.EPSILON);
						break;
					case 8:
						Assert.assertEquals("Wrong base count for link " + id, 7, r.getNumberValue("baseCount").intValue());
						Assert.assertEquals("Wrong compare count for link " + id, 13, r.getNumberValue("compCount").intValue());
						Assert.assertEquals("Wrong change value for link " + id, 0.8571, r.getNumberValue("change").doubleValue(), MatsimTestUtils.EPSILON);
						break;
					case 9:
						Assert.assertEquals("Wrong base count for link " + id, 17, r.getNumberValue("baseCount").intValue());
						Assert.assertEquals("Wrong compare count for link " + id, 6, r.getNumberValue("compCount").intValue());
						Assert.assertEquals("Wrong change value for link " + id, -0.6471, r.getNumberValue("change").doubleValue(), MatsimTestUtils.EPSILON);
						break;
					case 10:
						Assert.assertEquals("Wrong base count for link " + id, 12, r.getNumberValue("baseCount").intValue());
						Assert.assertEquals("Wrong compare count for link " + id, 15, r.getNumberValue("compCount").intValue());
						Assert.assertEquals("Wrong change value for link " + id, 0.2500, r.getNumberValue("change").doubleValue(), MatsimTestUtils.EPSILON);
						break;						
					default:
						break;
					}
				}

			}finally{
				t.close();
			}
		} catch (CorruptedTableException e) {
			Assert.fail("Incorrect `corrupted table' exception thrown.");
		} catch (IOException e) {
			Assert.fail("Incorrect `IOException' thrown.");
		}

	}


	private void createLinkstatsFiles(){
		Config config = ConfigUtils.loadConfig(utils.getClassInputDirectory() + "config.xml");
		config.network().setInputFile(utils.getClassInputDirectory() + "networkSmall.xml.gz");
		config.plans().setInputFile(utils.getClassInputDirectory() + "50.plans100.xml.gz");
		config.controler().setOutputDirectory(utils.getOutputDirectory() + "Output1/");
		
		config.controler().setWriteEventsInterval(0);
		config.controler().setWritePlansInterval(0);
		
		Controler c = new Controler(config);
        c.getConfig().controler().setCreateGraphs(false);
		c.getConfig().controler().setDumpDataAtEnd(false);
		c.run();
		
		config.plans().setInputFile(utils.getClassInputDirectory() + "50.plans110.xml.gz");
		config.controler().setOutputDirectory(utils.getOutputDirectory() + "Output2/");
		
		c = new Controler(config);
        c.getConfig().controler().setCreateGraphs(false);
		c.getConfig().controler().setDumpDataAtEnd(false);
		c.run();
	}

}
