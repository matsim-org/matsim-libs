package playground.smetzler.parseElevationData;

import java.awt.image.Raster;
import java.io.File;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;

public class initTiffToParse {

	public static GridCoverage2D grid;
	public static  Raster gridData;
	
	public static void initTif() throws Exception {
		// download data from http://earthexplorer.usgs.gov/ (login in required)
		File tiffFile = new File("../../../../13.Sem - Uni WS 15-16/Masterarbeit/netzwerk/elevation_berlin/n52_e013_1arc_v3.tif");
		GeoTiffReader reader = new GeoTiffReader(tiffFile);

		grid = reader.read(null);
		gridData = grid.getRenderedImage().getData();
	}
	
}
