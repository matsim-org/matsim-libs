package playground.smetzler.parseElevationData;

import java.awt.image.Raster;
import java.io.File;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.DirectPosition2D;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.geotools.referencing.CRS;

public class ParseEleDataFromGeoTiff {

	private static GridCoverage2D grid;
	private static  Raster gridData;
		
	public double parseGeoTiff(double xCoord, double yCoord, boolean firsttime) throws Exception {
		if (firsttime) {
		initTif();}		
		return getValue(xCoord, yCoord);
	}

	private void initTif() throws Exception {
		
		//Where to download elevation Raw Data?
		// SRTM1:  http://earthexplorer.usgs.gov/ (login in required)
		// SRTM3:  http://srtm.csi.cgiar.org/SELECTION/inputCoord.asp
		// EU-DEM: http://data.eox.at/eudem
		// oslo DEM10: http://data.kartverket.no/download/content/digital-terrengmodell-10-m-utm-32
		
//		//oslo EUDEM
//		File tiffFile = new File(
//				"../../../../desktop/Oslo/OsloEle/EUDEM_Oslo.tif");
		
		//oslo DEM10
//		File tiffFile = new File(
//				"../../../../desktop/Oslo/OsloEle/EntireGreatOsloUTM.tif");
		
		//berlin SRTM3
//		File tiffFile = new File(
//				"../../../shared-svn/studies/countries/de/berlin-bike/networkRawData/elevation_berlin/srtm3/srtm_39_02.tif");
		
		
		 //berlin EU-DEM testing
		File tiffFile = new File(
				"../../../../desktop/Oslo/countsAuslesen/OSM_counts/BerlinEleEUDEM.tif");
		
		
//	    //berlin EU-DEM
//		File tiffFile = new File(
//				"../../../shared-svn/studies/countries/de/berlin-bike/networkRawData/elevation_berlin/BerlinEUDEM.tif");
//		
//		//stuttgart EUDEM
//		File tiffFile = new File(
//				"../../../shared-svn/studies/countries/de/berlin-bike/networkRawData/elevation_stuttgart/stuttgartEUDEM.tif");		
//		
//		//stuttgart SRTM3
//		File tiffFile = new File(
//				"../../../shared-svn/studies/countries/de/berlin-bike/networkRawData/elevation_stuttgart/srtm_38_03.tif");
//		
//		//brasilia SRTM3
//		File tiffFile = new File(
//				"../../../shared-svn/studies/countries/de/berlin-bike/networkRawData/elevation_brasilia/srtm_27_16.tif");
		
		
		GeoTiffReader reader = new GeoTiffReader(tiffFile);

		grid = reader.read(null);
		gridData = grid.getRenderedImage().getData();

	}

	private double getValue(double x, double y) throws Exception {

		GridGeometry2D gg = grid.getGridGeometry();

//		oslo
//		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:32632", true); 
//		CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:32632", true); 
		
		//convert the the projection used in the MATSim Berlin scenario (DHDN / 3-degree Gauss-Kruger zone 4) to one used in the elevation data (Geotiff, WGS84) 
		//new
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:31468", true); //desired MATSIMnet    // DHDN / 3-degree Gauss-Kruger zone 4
		CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:4326", true); //projection of input  //WGS84
//		CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:4258", true); //projection of input  //ETRS89

		MathTransform mathTransform = CRS.findMathTransform(sourceCRS, targetCRS, true);

		DirectPosition transformedCoords = mathTransform.transform(new DirectPosition2D(x, y), null);


		DirectPosition2D posWorld = new DirectPosition2D(transformedCoords); 
		GridCoordinates2D posGrid = gg.worldToGrid(posWorld);

		// envelope is the size in the target projection
		double[] pixel = new double[1];
		double[] data = gridData.getPixel(posGrid.x, posGrid.y, pixel);
		return data[0];
	}
	
}
