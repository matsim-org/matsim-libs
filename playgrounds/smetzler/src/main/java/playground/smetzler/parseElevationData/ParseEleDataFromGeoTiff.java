package playground.smetzler.parseElevationData;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.DirectPosition2D;

import java.awt.image.Raster;
import java.io.File;

//to-do
// pruefe ausmaße der inputdatei und und gib warning wenn anfragelocation ausserhalb ist

public class ParseEleDataFromGeoTiff {

	    private static GridCoverage2D grid;
	    private static Raster gridData;

	 
	    public static void main(String[] args) throws Exception {
	        initTif();
	        System.out.println("teufelsberg " + getValue(13.2407,52.4971));
	        System.out.println("tempelhofer feld " + getValue(13.3989,52.4755));
	        System.out.println("mueggelsee " + getValue(13.6354,52.4334));
	        System.out.println("mueggelberg " + getValue(13.64048,52.41594));
	        
	        System.out.println("alexanderplatz " + getValue(13.40993,52.52191));	        
	        System.out.println("der kreuzberg " + getValue(13.379491,52.487610));
	        
	    }
	 
	    private static void initTif() throws Exception {
	    	// download data from http://earthexplorer.usgs.gov/ (login in required)
	        File tiffFile = new File("../../../../13.Sem - Uni WS 15-16/Masterarbeit/netzwerk/elevation_berlin/n52_e013_1arc_v3.tif");
	        GeoTiffReader reader = new GeoTiffReader(tiffFile);
	 
	        grid =reader.read(null);
	        gridData = grid.getRenderedImage().getData();
	    }
	 
	    private static double getValue(double x, double y) throws Exception {
	 
	        GridGeometry2D gg = grid.getGridGeometry();
	 
	        DirectPosition2D posWorld = new DirectPosition2D(x,y);
	        GridCoordinates2D posGrid = gg.worldToGrid(posWorld);
	 
	        // envelope is the size in the target projection
	        double[] pixel= new double[1];
	        double[] data = gridData.getPixel(posGrid.x, posGrid.y, pixel);
	        return data[0];
	    }
	     
	}
	
	
	
	



