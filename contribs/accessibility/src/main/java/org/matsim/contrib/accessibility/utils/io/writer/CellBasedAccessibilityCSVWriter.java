package org.matsim.contrib.accessibility.utils.io.writer;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

public class CellBasedAccessibilityCSVWriter {
	private static final Logger log = Logger.getLogger(CellBasedAccessibilityCSVWriter.class);

	private static final String FILE_NAME= "accessibilities.csv";
	
	private static final String SEPARATOR = "\t"; // comma is the correct choice for excel.  But gnuplot cannot deal with comma!

	private BufferedWriter writer ;

	/**
	 * writes the header of accessibility data csv file
	 */
	public CellBasedAccessibilityCSVWriter(String matsimOutputDirectory){
		log.info("Initializing  ...");
		try {
		writer = IOUtils.getBufferedWriter( matsimOutputDirectory + "/" + FILE_NAME );
		} catch ( Exception ee ) {
			ee.printStackTrace();
			throw new RuntimeException("writer could not be instantiated") ;
		}

		if ( writer==null ) {
			throw new RuntimeException( "writer is null") ;
		}
		
//		// create header
//		try {
//			writer.write( Labels.ZONE_ID + SEPARATOR +
//					Labels.X_COORDINATE + SEPARATOR +
//					Labels.Y_COORDINATE + SEPARATOR + 
//					Labels.NEARESTNODE_ID + SEPARATOR +
//					Labels.NEARESTNODE_X_COORD + SEPARATOR +
//					Labels.NEARESTNODE_Y_COORD + SEPARATOR + 
//					Labels.ACCESSIBILITY_BY_FREESPEED + SEPARATOR +
//					Labels.ACCESSIBILITY_BY_CAR + SEPARATOR +
//					Labels.ACCESSIBILITY_BY_BIKE + SEPARATOR +
//					Labels.ACCESSIBILITY_BY_WALK + SEPARATOR +
//					Labels.ACCESSIBILITY_BY_PT);
//			writer.newLine();
//		} catch (IOException e) {
//			e.printStackTrace();
//			throw new RuntimeException("io did not work") ;
//		}

		log.info("... done!");
	}

//	/**
//	 * writing the accessibility measures into csv file.
//	 * <p/>
//	 * Design thoughs:<ul>
//	 * <li> yyyy I am not sure why it is meaningful to use zones or nodes for the coordinates.  Accessibility refers directly
//	 * to coordinates, and maybe directly to zones (if averaged). --> remove eventually.  kai, jul'13
//	 * <ul>
//	 * 
//	 */
//	public void writeRecord(ActivityFacility startZone, Node node, Map<Modes4Accessibility,Double> accessibilities,
//			Map<String,Double> additionalColumns ) { 
//
//		try{
//			assert(writer != null);
//			writer.write( startZone.getId().toString() + SEPARATOR +
//					startZone.getCoord().getX() + SEPARATOR +
//					startZone.getCoord().getY() + SEPARATOR +
//					node.getId() + SEPARATOR + 
//					node.getCoord().getX() + SEPARATOR +  
//					node.getCoord().getY() ) ;
//			for ( Modes4Accessibility mode : Modes4Accessibility.values() ) {
//				writer.write( SEPARATOR + accessibilities.get( mode ) );
//			}
//			for ( Double dbl : additionalColumns.values() ) {
//				writer.write( SEPARATOR + dbl );
//			}
//			writer.newLine();
//		}
//		catch(Exception e){
//			e.printStackTrace();
//			System.exit(-1) ;
//		}
//	}

	public void writeField( double val ) {
		try {
			writer.write( val + SEPARATOR ) ;
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("could not write");
		}
	}

	public void writeNewLine() {
		try {
			writer.newLine() ;
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("i/o failure") ;
		}
	}

	/**
	 * finalize and close csv file
	 */
	public void close(){
		try {
			log.info("Closing ...");
			assert(writer != null);
			writer.flush();
			writer.close();
			log.info("... done!");
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
}
