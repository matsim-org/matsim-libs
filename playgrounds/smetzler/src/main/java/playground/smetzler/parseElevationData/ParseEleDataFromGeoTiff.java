package playground.smetzler.parseElevationData;

import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.DirectPosition2D;

public class ParseEleDataFromGeoTiff {

	    private final static String url = "jdbc:postgresql://localhost/bag";
	    private static GridCoverage2D grid;
	    private static Raster gridData;

	 
	    public static void main(String[] args) throws Exception {
	        initTif();
	        System.out.println(getValue(13.45, 52.33));
//	        loadData();
	    }
	    
	 
	    private static void initTif() throws Exception {
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
	        double[] pixel=new double[1];
	        double[] data = gridData.getPixel(posGrid.x, posGrid.y, pixel);
	        return data[0];
	    }
	 
//	    private static void loadData() throws Exception {
//	        Connection conn = DriverManager.getConnection(url);
//	        QueryRunner runner = new QueryRunner();
//	        final Map<Long, Double> map = new HashMap<Long, Double>();
//	        ResultSetHandler handler = new ResultSetHandler() {
//	 
//	            @Override
//	            public Object handle(ResultSet resultSet) throws SQLException {
//	                while (resultSet.next()) {
//	                    String point = resultSet.getString("point");
//	                    double x = Double.parseDouble(point.substring(
//	                            point.indexOf('(') + 1,
//	                            point.indexOf(' ')
//	                    ));
//	 
//	                    double y = Double.parseDouble(point.substring(
//	                            point.indexOf(' ') + 1,
//	                            point.indexOf(')')
//	                    ));
//	 
//	                    try {
//	                        double hoogte = getValue(x, y);
//	                        map.put(resultSet.getLong("gid"),hoogte);
//	                    } catch (Exception e) {
//	                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//	                    }
//	                }
//	                return null;  //To change body of implemented methods use File | Settings | File Templates.
//	            }
//	        };
//	 
//	        runner.query(conn, "SELECT gid, ST_AsText(ST_Centroid(geovlak)) as point \n" +
//	                "FROM bag8mrt2014.pand\n" +
//	                "WHERE geovlak && ST_MakeEnvelope(130153, 408769,132896, 410774, 28992) ORDER by gid ;", handler);
//	 
//	        int count = 0;
//	        for (Long key : map.keySet()) {
//	 
//	            System.out.println("Inserting for key = " + key + " value: " + map.get(key));
//	            int col = runner.update(conn, "UPDATE bag8mrt2014.pand SET hoogte= ? where gid = ?",
//	                    map.get(key), key);
//	 
//	            count++;
//	 
//	            if (count%100 == 0) {
//	                System.out.println("count = " + count);
//	            }
//	        }
//	    
	    
	}
	
	
	
	
	

	
//	public GridCoverage2D getGridCoverage() {
//	    GeoTiffFormat format = new GeoTiffFormat();
//	    GeoTiffReader reader = null;
//	    GridCoverage2D coverage = null;
//
//	    String inputTIFF = "../../../../13.Sem - Uni WS 15-16/Masterarbeit/netzwerk/skalitzer/skalitzer_OSM.xml";
//	    
//	    try {
//	        if (inputTIFF == null) {
//	            throw new RuntimeException("Path not set");
//	        }
//	        
//	        reader = format.getReader(inputTIFF);
//	        coverage = reader.read(null);
//	    } catch (IOException e) {
//	        throw new RuntimeException("Error getting coverage automatically. ", e);
//	    }
//
//	    return coverage;
//	}
//	
//
//}


