package playground.southAfrica.freight.digicore.analysis.geographicClassifier;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.TimeZone;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestCase;

import playground.southafrica.freight.digicore.analysis.geographicClassifier.GeographicClassifier;
import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.io.DigicoreVehicleWriter;


public class GeographicClassifierTest extends MatsimTestCase {
	
	public void testConstructor() throws IOException{
		GeographicClassifier gc = new GeographicClassifier(getOutputDirectory(), getPackageInputDirectory() + "shapefile/testShapefile.shp", 1);
		
		assertEquals("Basic maps not created.", 3, gc.getLists().size());
		
		assertTrue("Should have an intra list", gc.getLists().containsKey("intra"));
		assertTrue("Intra list should be empty.", gc.getLists().get("intra").isEmpty());
		assertTrue("Should have an intra list", gc.getLists().containsKey("inter"));
		assertTrue("Inter list should be empty.", gc.getLists().get("inter").isEmpty());
		assertTrue("Should have an intra list", gc.getLists().containsKey("extra"));
		assertTrue("Extra list should be empty.", gc.getLists().get("extra").isEmpty());
	}
	
	
	public void testSplitInterIntraExtra(){
		setUpIntraVehicle();
		setUpInterVehicle();
		setUpExtraVehicle();
		
		GeographicClassifier gc = new GeographicClassifier(getOutputDirectory(), getPackageInputDirectory() + "shapefile/testShapefile.shp", 1);
		gc.splitIntraInterExtra(0.6, 1);
		
		/* Check that there are one of each type. */
		assertEquals("There should only be one intra vehicle.", 1, gc.getLists().get("intra").size());
		assertEquals("There should only be one inter vehicle.", 1, gc.getLists().get("inter").size());
		assertEquals("There should only be one extra vehicle.", 1, gc.getLists().get("extra").size());
		
		/* Check that the right vehicles are in the right lists. */
		assertTrue("Wrong intra vehicle.", gc.getLists().get("intra").contains(new IdImpl(1)));
		assertTrue("Wrong inter vehicle.", gc.getLists().get("inter").contains(new IdImpl(2)));
		assertTrue("Wrong extra vehicle.", gc.getLists().get("extra").contains(new IdImpl(3)));
	}
	
	
	public void testWriteLists(){
		setUpIntraVehicle();
		setUpInterVehicle();
		setUpExtraVehicle();
		
		GeographicClassifier gc = new GeographicClassifier(getOutputDirectory(), getPackageInputDirectory() + "shapefile/testShapefile.shp", 1);
		gc.splitIntraInterExtra(0.6, 1);
		gc.writeLists(getOutputDirectory(), "test");
		
		/* Check that the output files were written. */
		assertTrue("Intra file doesn't exist.", new File(getOutputDirectory() + "intra_test.txt").exists());
		assertTrue("Inter file doesn't exist.", new File(getOutputDirectory() + "inter_test.txt").exists());
		assertTrue("Extra file doesn't exist.", new File(getOutputDirectory() + "extra_test.txt").exists());
		
		/* Check each file contains the right vehicle Id. */
		String[] sa = {"intra", "inter", "extra"};
		for(int i = 0; i < sa.length; i++){
			BufferedReader br = IOUtils.getBufferedReader(getOutputDirectory() + sa[i] + "_test.txt");
			try{
				String line = br.readLine();
				assertTrue("Wrong Id in " + sa[i], line.equalsIgnoreCase(String.valueOf(i+1)));
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
		DigicoreVehicle vehicle = new DigicoreVehicle(new IdImpl(1));
		
		DigicoreChain chain = new DigicoreChain();
		
		DigicoreActivity da1 = new DigicoreActivity("major", TimeZone.getTimeZone("GMT+2"), new Locale("en", "za"));
		da1.setCoord(new CoordImpl(7,7));
		DigicoreActivity da2 = new DigicoreActivity("minor", TimeZone.getTimeZone("GMT+2"), new Locale("en", "za"));
		da2.setCoord(new CoordImpl(8,7));
		DigicoreActivity da3 = new DigicoreActivity("minor", TimeZone.getTimeZone("GMT+2"), new Locale("en", "za"));
		da3.setCoord(new CoordImpl(12,7));
		DigicoreActivity da4 = new DigicoreActivity("minor", TimeZone.getTimeZone("GMT+2"), new Locale("en", "za"));
		da4.setCoord(new CoordImpl(7,7));
		DigicoreActivity da5 = new DigicoreActivity("minor", TimeZone.getTimeZone("GMT+2"), new Locale("en", "za"));
		da5.setCoord(new CoordImpl(7,7));
		DigicoreActivity da6 = new DigicoreActivity("minor", TimeZone.getTimeZone("GMT+2"), new Locale("en", "za"));
		da6.setCoord(new CoordImpl(7,7));
		DigicoreActivity da7 = new DigicoreActivity("major", TimeZone.getTimeZone("GMT+2"), new Locale("en", "za"));
		da7.setCoord(new CoordImpl(7,7));

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
		dvw.write(getOutputDirectory() + "/" + vehicle.getId().toString() + ".xml.gz", vehicle);
	}
	
	
	/**
	 * Inter-area vehicle with at least one, but less than 60% of its activities 
	 * inside the area.
	 * @throws IOException
	 */
	public void setUpInterVehicle(){
		DigicoreVehicle vehicle = new DigicoreVehicle(new IdImpl(2));
		
		DigicoreChain chain = new DigicoreChain();
		
		DigicoreActivity da1 = new DigicoreActivity("major", TimeZone.getTimeZone("GMT+2"), new Locale("en", "za"));
		da1.setCoord(new CoordImpl(7,7));
		DigicoreActivity da2 = new DigicoreActivity("minor", TimeZone.getTimeZone("GMT+2"), new Locale("en", "za"));
		da2.setCoord(new CoordImpl(12,7));
		DigicoreActivity da3 = new DigicoreActivity("minor", TimeZone.getTimeZone("GMT+2"), new Locale("en", "za"));
		da3.setCoord(new CoordImpl(12,7));
		DigicoreActivity da4 = new DigicoreActivity("minor", TimeZone.getTimeZone("GMT+2"), new Locale("en", "za"));
		da4.setCoord(new CoordImpl(7,7));
		DigicoreActivity da5 = new DigicoreActivity("minor", TimeZone.getTimeZone("GMT+2"), new Locale("en", "za"));
		da5.setCoord(new CoordImpl(12,7));
		DigicoreActivity da6 = new DigicoreActivity("minor", TimeZone.getTimeZone("GMT+2"), new Locale("en", "za"));
		da6.setCoord(new CoordImpl(12,7));
		DigicoreActivity da7 = new DigicoreActivity("major", TimeZone.getTimeZone("GMT+2"), new Locale("en", "za"));
		da7.setCoord(new CoordImpl(7,7));

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
		dvw.write(getOutputDirectory() + "/" + vehicle.getId().toString() + ".xml.gz", vehicle);
	}
	
	
	public void setUpExtraVehicle(){
		DigicoreVehicle vehicle = new DigicoreVehicle(new IdImpl(3));
		
		DigicoreChain chain = new DigicoreChain();
		
		DigicoreActivity da1 = new DigicoreActivity("major", TimeZone.getTimeZone("GMT+2"), new Locale("en", "za"));
		da1.setCoord(new CoordImpl(12,7));
		DigicoreActivity da2 = new DigicoreActivity("minor", TimeZone.getTimeZone("GMT+2"), new Locale("en", "za"));
		da2.setCoord(new CoordImpl(12,7));
		DigicoreActivity da3 = new DigicoreActivity("minor", TimeZone.getTimeZone("GMT+2"), new Locale("en", "za"));
		da3.setCoord(new CoordImpl(12,7));
		DigicoreActivity da4 = new DigicoreActivity("minor", TimeZone.getTimeZone("GMT+2"), new Locale("en", "za"));
		da4.setCoord(new CoordImpl(12,7));
		DigicoreActivity da5 = new DigicoreActivity("minor", TimeZone.getTimeZone("GMT+2"), new Locale("en", "za"));
		da5.setCoord(new CoordImpl(12,7));
		DigicoreActivity da6 = new DigicoreActivity("minor", TimeZone.getTimeZone("GMT+2"), new Locale("en", "za"));
		da6.setCoord(new CoordImpl(12,7));
		DigicoreActivity da7 = new DigicoreActivity("major", TimeZone.getTimeZone("GMT+2"), new Locale("en", "za"));
		da7.setCoord(new CoordImpl(12,7));

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
		dvw.write(getOutputDirectory() + "/" + vehicle.getId().toString() + ".xml.gz", vehicle);
	}
}
