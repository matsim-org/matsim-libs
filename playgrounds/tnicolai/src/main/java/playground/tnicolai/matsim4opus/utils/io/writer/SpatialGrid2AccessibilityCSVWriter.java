package playground.tnicolai.matsim4opus.utils.io.writer;

import java.io.BufferedWriter;

import org.apache.log4j.Logger;
import org.matsim.contrib.matsim4opus.gis.SpatialGrid;
import org.matsim.core.utils.io.IOUtils;

import playground.tnicolai.matsim4opus.utils.helperObjects.SquareLayer;

public class SpatialGrid2AccessibilityCSVWriter {
	
	private static final Logger log = Logger.getLogger(SpatialGrid2AccessibilityCSVWriter.class);
	
	public static void write(SpatialGrid<SquareLayer> grid, String file){
		
		BufferedWriter writer = IOUtils.getBufferedWriter(file);
		log.info("... done!");
		
		assert(grid!=null);
		
		
		
		
	}

}
