package playground.jjoubert.CommercialTraffic;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.jfree.util.Log;
import org.matsim.testcases.MatsimTestCase;

import playground.jjoubert.Utilities.FileSampler.MyFileSampler;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class SelectVehiclesTest extends MatsimTestCase{

	public void testCountFiles() {
		File testFolder = new File(getOutputDirectory());
		assertTrue(emptyFolder(testFolder) );
		boolean checkFolder = testFolder.mkdirs();
		if(!checkFolder){
			Log.warn("Could not create " + testFolder.toString() + ", or it already exists!");
		}
		assertEquals("Folder should be empty", 0, SelectVehicles.countVehicleFiles(testFolder) );
		
		createTestFiles(testFolder, 4);
		assertEquals("Folder should contain 4 files.", 4, SelectVehicles.countVehicleFiles(testFolder) );
		
		File folderFiles[] = testFolder.listFiles();
		boolean checkDelete = folderFiles[0].delete();
		if(!checkDelete){
			Log.warn("Could not delete " + folderFiles[0].toString());
		}
		assertEquals("Folder should contain 3 files.", 3, SelectVehicles.countVehicleFiles(testFolder) );
		
		assertTrue(emptyFolder(testFolder) );
	}	
	
	public void testMoveFile() {
		File fromFolder = new File(getOutputDirectory() + "fromFolder/");
		assertTrue("Folder should be empty", emptyFolder(fromFolder) );
		boolean checkFolder = fromFolder.mkdirs();
		if(!checkFolder){
			Log.warn("Could not create " + fromFolder.toString() + ", or it already exists!");
		}
		
		createTestFiles(fromFolder, 3);
		File files[] = fromFolder.listFiles();
		File fileOne = files[0];
		File fileTwo = files[1];
		File toFolder = new File(getOutputDirectory() + "toFolder/");
		assertTrue("Copy folder should have been created", toFolder.mkdirs() );
		
		MyFileSampler sampler = new MyFileSampler(fromFolder.getAbsolutePath(), toFolder.getAbsolutePath());
		
		assertTrue("First file should have been copied.", sampler.copyFile(toFolder, fileOne) );
		assertEquals("There should be one file in copy folder.", 1, toFolder.listFiles().length);
		
		assertTrue("Second file should have been copied.", sampler.copyFile(toFolder, fileTwo) );
		assertEquals("There should be two files in copy folder.", 2, toFolder.listFiles().length);
	}
	
	public void testCheckStudyArea() {
		
		MultiPolygon polygon = createTestPolygon();
		ArrayList<Point> points = createTestPoints();
		
		assertTrue("Point 1 is actually inside", SelectVehicles.testPolygon(polygon, points.get(0) ) );
		assertFalse("Point 2 is actually outside", SelectVehicles.testPolygon(polygon, points.get(1) ) );
		assertFalse("Point 3 is actually outside", SelectVehicles.testPolygon(polygon, points.get(2) ) );
		assertFalse("Point 4 is actually outside", SelectVehicles.testPolygon(polygon, points.get(3) ) );
		assertTrue("Point 5 is actually inside", SelectVehicles.testPolygon(polygon, points.get(4) ) );
		assertFalse("Point 6 is actually outside", SelectVehicles.testPolygon(polygon, points.get(5) ) );
	}
		
	private ArrayList<Point> createTestPoints() {
		
		GeometryFactory gf = new GeometryFactory();
		
		ArrayList<Coordinate> coords = new ArrayList<Coordinate>();
		Coordinate c1 = new Coordinate(1, 1.5);
		Coordinate c2 = new Coordinate(1, 4);
		Coordinate c3 = new Coordinate(3, 1);
		Coordinate c4 = new Coordinate(3, 10);
		Coordinate c5 = new Coordinate(6, 2);
		Coordinate c6 = new Coordinate(8, 4);
		coords.add(c1);
		coords.add(c2);
		coords.add(c3);
		coords.add(c4);
		coords.add(c5);
		coords.add(c6);
		ArrayList<Point> points = new ArrayList<Point>();
		for(int i=0; i<6; i++){
			Point p = gf.createPoint(coords.get(i) );
			points.add(p);			
		}		
		return points;
	}

	private MultiPolygon createTestPolygon () {
		
		// Something like a figure of eight
		Coordinate c1 = new Coordinate(0, 0);
		Coordinate c2 = new Coordinate(0, 3);
		Coordinate c3 = new Coordinate(3, 3);
		Coordinate c4 = new Coordinate(3, 5);		
		Coordinate c5 = new Coordinate(0, 5);		
		Coordinate c6 = new Coordinate(0, 8);		
		Coordinate c7 = new Coordinate(2, 8);		
		Coordinate c8 = new Coordinate(2, 6);		
		Coordinate c9 = new Coordinate(5, 6);		
		Coordinate c10 = new Coordinate(5, 8);		
		Coordinate c11 = new Coordinate(7, 8);		
		Coordinate c12 = new Coordinate(7, 5);		
		Coordinate c13 = new Coordinate(5, 5);		
		Coordinate c14 = new Coordinate(5, 3);		
		Coordinate c15 = new Coordinate(7, 3);		
		Coordinate c16 = new Coordinate(7, 0);		
		Coordinate c17 = new Coordinate(5, 0);		
		Coordinate c18 = new Coordinate(5, 2);		
		Coordinate c19 = new Coordinate(2, 2);		
		Coordinate c20 = new Coordinate(2, 0);		
		Coordinate c[] = {c1,c2,c3,c4,c5,c6,c7,c8,c9,c10,c11,c12,c13,c14,c15,c16,c17,c18,c19,c20,c1};
		
		GeometryFactory gf = new GeometryFactory();
		
		LinearRing lr = gf.createLinearRing(c);
		Polygon p = gf.createPolygon(lr, null);
		Polygon p1[] = new Polygon[1];
		p1[0] = p;
		MultiPolygon mp = gf.createMultiPolygon(p1);
		
		return mp;		
	}
			
	private void createTestFiles(File folder, int numberoffiles) {
		for (int fn = 0; fn < numberoffiles; fn++) {
			String theFileName = folder.getAbsolutePath() + "/File" + fn + ".txt";
			File theFile = new File(theFileName);
			try {
				FileWriter fw = new FileWriter(theFile);
				try{
					fw.toString();
				} finally{
					fw.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}		
		}
	}

	private boolean emptyFolder(File folder) {
		Boolean emptyResult = false;
		if (folder.exists()){
			File filesInFolder[] = folder.listFiles();
			if (filesInFolder != null){
				for (File aFile : filesInFolder) {
					if(aFile.isDirectory() ){
						emptyFolder(aFile);
					}else{
						boolean checkDelete = aFile.delete();
						if(!checkDelete){
							Log.warn("Could not delete " + aFile.toString());
						}
					}
				}
			}
			emptyResult = folder.delete();			
		} else{
			emptyResult = true;
		}
		return emptyResult;
	}
}
