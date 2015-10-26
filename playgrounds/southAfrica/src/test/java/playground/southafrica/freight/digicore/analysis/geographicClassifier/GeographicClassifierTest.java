package playground.southafrica.freight.digicore.analysis.geographicClassifier;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.io.DigicoreVehicleWriter;


public class GeographicClassifierTest{

	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testConstructor() throws IOException{
		GeographicClassifier gc = new GeographicClassifier(utils.getOutputDirectory(), utils.getPackageInputDirectory() + "shapefile/testShapefile.shp", 1);
		
		Assert.assertEquals("Basic maps not created.", 3, gc.getLists().size());
		
		Assert.assertTrue("Should have an intra list", gc.getLists().containsKey("intra"));
		Assert.assertTrue("Intra list should be empty.", gc.getLists().get("intra").isEmpty());
		Assert.assertTrue("Should have an intra list", gc.getLists().containsKey("inter"));
		Assert.assertTrue("Inter list should be empty.", gc.getLists().get("inter").isEmpty());
		Assert.assertTrue("Should have an intra list", gc.getLists().containsKey("extra"));
		Assert.assertTrue("Extra list should be empty.", gc.getLists().get("extra").isEmpty());
	}
	
	@Test
	public void testSplitInterIntraExtra(){
		setUpIntraVehicle();
		setUpInterVehicle();
		setUpExtraVehicle();
		
		GeographicClassifier gc = new GeographicClassifier(utils.getOutputDirectory(), utils.getPackageInputDirectory() + "shapefile/testShapefile.shp", 1);
		gc.splitIntraInterExtra(0.6, 1);
		
		/* Check that there are one of each type. */
		Assert.assertEquals("There should only be one intra vehicle.", 1, gc.getLists().get("intra").size());
		Assert.assertEquals("There should only be one inter vehicle.", 1, gc.getLists().get("inter").size());
		Assert.assertEquals("There should only be one extra vehicle.", 1, gc.getLists().get("extra").size());
		
		/* Check that the right vehicles are in the right lists. */
		Assert.assertTrue("Wrong intra vehicle.", gc.getLists().get("intra").contains(Id.create("1", DigicoreVehicle.class)));
		Assert.assertTrue("Wrong inter vehicle.", gc.getLists().get("inter").contains(Id.create("2", DigicoreVehicle.class)));
		Assert.assertTrue("Wrong extra vehicle.", gc.getLists().get("extra").contains(Id.create("3", DigicoreVehicle.class)));
	}
	
	@Test
	public void testWriteLists(){
		setUpIntraVehicle();
		setUpInterVehicle();
		setUpExtraVehicle();
		
		GeographicClassifier gc = new GeographicClassifier(utils.getOutputDirectory(), utils.getPackageInputDirectory() + "shapefile/testShapefile.shp", 1);
		gc.splitIntraInterExtra(0.6, 1);
		gc.writeLists(utils.getOutputDirectory(), "test");
		
		/* Check that the output files were written. */
		Assert.assertTrue("Intra file doesn't exist.", new File(utils.getOutputDirectory() + "intra_test.txt").exists());
		Assert.assertTrue("Inter file doesn't exist.", new File(utils.getOutputDirectory() + "inter_test.txt").exists());
		Assert.assertTrue("Extra file doesn't exist.", new File(utils.getOutputDirectory() + "extra_test.txt").exists());
		
		/* Check each file contains the right vehicle Id. */
		String[] sa = {"intra", "inter", "extra"};
		for(int i = 0; i < sa.length; i++){
			BufferedReader br = IOUtils.getBufferedReader(utils.getOutputDirectory() + sa[i] + "_test.txt");
			try{
				String line = br.readLine();
				Assert.assertTrue("Wrong Id in " + sa[i], line.equalsIgnoreCase(String.valueOf(i+1)));
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot read from test file.");
			} finally{
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException("Cannot close BufferedReader.");
				}
			}
			
		}
	}
	
	
	/*
	 * The shapefile used in this test case has an "area" from pretty much more or less
	 *  almost exactly (5,5) to (10,10)
	 * 
	 * 
	 * 11 		    ___________
	 * 10 		   |           |
	 *  9 		   |           |
	 *  8 		   |           |
	 *  7 		   |           |
	 *  6 		   |           |
	 *  5		   |___________|
	 *  4
	 *  3
	 *  2
	 *  1
	 *  0
	 * 	 0 1 2 3 4 5 6 7 8 9 10 11
	 */

	/**
	 * Intra-area vehicle with more than 60% of its activities inside the area.
	 * @throws IOException
	 */
	public void setUpIntraVehicle(){
		DigicoreVehicle vehicle = new DigicoreVehicle(Id.create("1", Vehicle.class));
		
		DigicoreChain chain = new DigicoreChain();
		
		DigicoreActivity da1 = new DigicoreActivity("major", TimeZone.getTimeZone("GMT+2"), new Locale("en", "za"));
		da1.setCoord(new Coord((double) 7, (double) 7));
		DigicoreActivity da2 = new DigicoreActivity("minor", TimeZone.getTimeZone("GMT+2"), new Locale("en", "za"));
		da2.setCoord(new Coord((double) 8, (double) 7));
		DigicoreActivity da3 = new DigicoreActivity("minor", TimeZone.getTimeZone("GMT+2"), new Locale("en", "za"));
		da3.setCoord(new Coord((double) 12, (double) 7));
		DigicoreActivity da4 = new DigicoreActivity("minor", TimeZone.getTimeZone("GMT+2"), new Locale("en", "za"));
		da4.setCoord(new Coord((double) 7, (double) 7));
		DigicoreActivity da5 = new DigicoreActivity("minor", TimeZone.getTimeZone("GMT+2"), new Locale("en", "za"));
		da5.setCoord(new Coord((double) 7, (double) 7));
		DigicoreActivity da6 = new DigicoreActivity("minor", TimeZone.getTimeZone("GMT+2"), new Locale("en", "za"));
		da6.setCoord(new Coord((double) 7, (double) 7));
		DigicoreActivity da7 = new DigicoreActivity("major", TimeZone.getTimeZone("GMT+2"), new Locale("en", "za"));
		da7.setCoord(new Coord((double) 7, (double) 7));

		/* Add the activities to the chain. */
		chain.add(da1);
		chain.add(da2);
		chain.add(da3);
		chain.add(da4);
		chain.add(da5);
		chain.add(da6);
		chain.add(da7);
		
		/* Add the chain to the vehicle */
		vehicle.getChains().add(chain);
			
		/* Write vehicle to output folder */
		DigicoreVehicleWriter dvw = new DigicoreVehicleWriter();
		dvw.write(utils.getOutputDirectory() + "/" + vehicle.getId().toString() + ".xml.gz", vehicle);
	}
	
	
	/**
	 * Inter-area vehicle with at least one, but less than 60% of its activities 
	 * inside the area.
	 * @throws IOException
	 */
	public void setUpInterVehicle(){
		DigicoreVehicle vehicle = new DigicoreVehicle(Id.create("2", Vehicle.class));
		
		DigicoreChain chain = new DigicoreChain();
		
		DigicoreActivity da1 = new DigicoreActivity("major", TimeZone.getTimeZone("GMT+2"), new Locale("en", "za"));
		da1.setCoord(new Coord((double) 7, (double) 7));
		DigicoreActivity da2 = new DigicoreActivity("minor", TimeZone.getTimeZone("GMT+2"), new Locale("en", "za"));
		da2.setCoord(new Coord((double) 12, (double) 7));
		DigicoreActivity da3 = new DigicoreActivity("minor", TimeZone.getTimeZone("GMT+2"), new Locale("en", "za"));
		da3.setCoord(new Coord((double) 12, (double) 7));
		DigicoreActivity da4 = new DigicoreActivity("minor", TimeZone.getTimeZone("GMT+2"), new Locale("en", "za"));
		da4.setCoord(new Coord((double) 7, (double) 7));
		DigicoreActivity da5 = new DigicoreActivity("minor", TimeZone.getTimeZone("GMT+2"), new Locale("en", "za"));
		da5.setCoord(new Coord((double) 12, (double) 7));
		DigicoreActivity da6 = new DigicoreActivity("minor", TimeZone.getTimeZone("GMT+2"), new Locale("en", "za"));
		da6.setCoord(new Coord((double) 12, (double) 7));
		DigicoreActivity da7 = new DigicoreActivity("major", TimeZone.getTimeZone("GMT+2"), new Locale("en", "za"));
		da7.setCoord(new Coord((double) 7, (double) 7));

		/* Add the activities to the chain. */
		chain.add(da1);
		chain.add(da2);
		chain.add(da3);
		chain.add(da4);
		chain.add(da5);
		chain.add(da6);
		chain.add(da7);
		
		/* Add the chain to the vehicle. */
		vehicle.getChains().add(chain);
			
		/* Write vehicle to output folder */
		DigicoreVehicleWriter dvw = new DigicoreVehicleWriter();
		dvw.write(utils.getOutputDirectory() + "/" + vehicle.getId().toString() + ".xml.gz", vehicle);
	}
	
	
	public void setUpExtraVehicle(){
		DigicoreVehicle vehicle = new DigicoreVehicle(Id.create("3", Vehicle.class));
		
		DigicoreChain chain = new DigicoreChain();
		
		DigicoreActivity da1 = new DigicoreActivity("major", TimeZone.getTimeZone("GMT+2"), new Locale("en", "za"));
		da1.setCoord(new Coord((double) 12, (double) 7));
		DigicoreActivity da2 = new DigicoreActivity("minor", TimeZone.getTimeZone("GMT+2"), new Locale("en", "za"));
		da2.setCoord(new Coord((double) 12, (double) 7));
		DigicoreActivity da3 = new DigicoreActivity("minor", TimeZone.getTimeZone("GMT+2"), new Locale("en", "za"));
		da3.setCoord(new Coord((double) 12, (double) 7));
		DigicoreActivity da4 = new DigicoreActivity("minor", TimeZone.getTimeZone("GMT+2"), new Locale("en", "za"));
		da4.setCoord(new Coord((double) 12, (double) 7));
		DigicoreActivity da5 = new DigicoreActivity("minor", TimeZone.getTimeZone("GMT+2"), new Locale("en", "za"));
		da5.setCoord(new Coord((double) 12, (double) 7));
		DigicoreActivity da6 = new DigicoreActivity("minor", TimeZone.getTimeZone("GMT+2"), new Locale("en", "za"));
		da6.setCoord(new Coord((double) 12, (double) 7));
		DigicoreActivity da7 = new DigicoreActivity("major", TimeZone.getTimeZone("GMT+2"), new Locale("en", "za"));
		da7.setCoord(new Coord((double) 12, (double) 7));

		/* Add the activities to the chain. */
		chain.add(da1);
		chain.add(da2);
		chain.add(da3);
		chain.add(da4);
		chain.add(da5);
		chain.add(da6);
		chain.add(da7);
				
		/* Add the chain to the vehicle */
		vehicle.getChains().add(chain);
			
		/* Write vehicle to output folder */
		DigicoreVehicleWriter dvw = new DigicoreVehicleWriter();
		dvw.write(utils.getOutputDirectory() + "/" + vehicle.getId().toString() + ".xml.gz", vehicle);
	}
}
