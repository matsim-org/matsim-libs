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
	
	// so machen, dass init nur einmal aufgerufen werden muss! momentane lösung mit "firsttime" nicht so elegant
	// methoden in unterschiedliche klassen?!
	
	public double parseGeoTiff(double xCoord, double yCoord, boolean firsttime) throws Exception {
		if (firsttime) {
		initTif();}		
		return getValue(xCoord, yCoord);
	}

	private void initTif() throws Exception {
		// download data from http://earthexplorer.usgs.gov/ (login in required)
		File tiffFile = new File("../../../../13.Sem - Uni WS 15-16/Masterarbeit/netzwerk/elevation_berlin/n52_e013_1arc_v3.tif");
		GeoTiffReader reader = new GeoTiffReader(tiffFile);

		grid = reader.read(null);
		gridData = grid.getRenderedImage().getData();

	}

	private double getValue(double x, double y) throws Exception {

		GridGeometry2D gg = grid.getGridGeometry();

		//da die GeoTiff in WGS84 ist, jedoch das MAtsim Netz in DHDN, müssen die übergebenen Koordinaten von DHDN in WGS84 transformiert werden

		//new
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:31468", true); // DHDN / 3-degree Gauss-Kruger zone 4
		CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:4326", true); //WGS84

		MathTransform mathTransform = CRS.findMathTransform(sourceCRS, targetCRS, true);

		DirectPosition transformedCoords = mathTransform.transform(new DirectPosition2D(x, y), null);

//		System.out.println("new DirectPosition2D(x, y)" + new DirectPosition2D(x, y));
//		System.out.println("transformedCoords" + transformedCoords);


		DirectPosition2D posWorld = new DirectPosition2D(transformedCoords); // new DirectPosition2D(x, y); //
		GridCoordinates2D posGrid = gg.worldToGrid(posWorld);

		// envelope is the size in the target projection
		double[] pixel = new double[1];
		double[] data = gridData.getPixel(posGrid.x, posGrid.y, pixel);
		return data[0];
	}
	
}
