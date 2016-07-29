package playground.dhosse.scenarios.generic.utils;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import playground.dhosse.scenarios.generic.Configuration;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

/**
 * 
 * Class for reading and storing geoinformation.
 * 
 * @author dhosse
 *
 */
public class Geoinformation {
	
	private static Map<String, AdministrativeUnit> adminUnits = new HashMap<>();
	
	//no instance!
	private Geoinformation(){};
	
	public static void readGeodataFromShapefile(String filename, Set<String> ids){
		
		Collection<SimpleFeature> features = new ShapeFileReader().readFileAndInitialize(filename);
		
		for(SimpleFeature feature : features){
			
			String kennzahl = Long.toString((Long)feature.getAttribute("GEM_KENNZ"));
			
			if(ids.contains(kennzahl)){
				
				AdministrativeUnit au = new AdministrativeUnit(kennzahl);
				au.setGeometry((Geometry)feature.getDefaultGeometry());
				adminUnits.put(kennzahl, au);
				
			}
			
		}
		
	}
	
	public static void readGeodataFromDatabase(Configuration configuration, Set<String> ids) throws Exception{
		
		WKTReader wktReader = new WKTReader();
		
		try {
			
			Class.forName("org.postgresql.Driver").newInstance();
			Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/geodata",
					configuration.getDatabaseUsername(), configuration.getPassword());
			
			if(connection != null){

				Statement statement = connection.createStatement();
				StringBuilder builder = new StringBuilder();

				int i = 0;
				
				for(String id : ids){

					if(i < ids.size() - 1){
						
						builder.append(" id like '" + id + "%' OR");
						
					} else {
						
						builder.append(" id like '" + id + "%'");
						
					}
					
					i++;
					
				}
				
				ResultSet set = statement.executeQuery("select id, st_astext(geometry)"
						+ " from XXX where" + builder.toString());
				
				while(set.next()){
					
					//TODO
					String key = set.getString("id");
					String g = set.getString("wkt");
					int districtType = set.getInt("");
					int municipalityType = set.getInt("");
					int regionType = set.getInt("");
					
					if(g != null){
						
						if(!g.isEmpty()){
							
							AdministrativeUnit au = new AdministrativeUnit(key);
							au.setGeometry(wktReader.read(g));
							au.setDistrictType(districtType);
							au.setMunicipalityType(municipalityType);
							au.setRegionType(regionType);
							adminUnits.put(key, au);
							
						}
						
					}
					
				}
				
				connection.close();
				
			} else{
				
				throw new Exception("Database connection could not be established! Aborting...");
				
			}
			
		} catch (IOException | InstantiationException | IllegalAccessException | ClassNotFoundException |
				SQLException | ParseException e) {
			
			e.printStackTrace();
			
		}
		
	}
	
	public static void readGeodataFromShapefileWithFilter(String filename, Set<String> filterIds){
		
		Collection<SimpleFeature> features = new ShapeFileReader().readFileAndInitialize(filename);
		
		for(SimpleFeature feature : features){
			
			String kennzahl = Long.toString((Long)feature.getAttribute("GEM_KENNZ"));
			
			for(String id : filterIds){
				
				if(kennzahl.startsWith(id)){
	
					AdministrativeUnit au = new AdministrativeUnit(kennzahl);
					au.setGeometry((Geometry)feature.getDefaultGeometry());
					adminUnits.put(kennzahl, au);
					break;
					
				}
				
			}
			
		}
		
	}
	
	public static Map<String, AdministrativeUnit> getAdminUnits(){
		
		return adminUnits;
		
	}
	
}
