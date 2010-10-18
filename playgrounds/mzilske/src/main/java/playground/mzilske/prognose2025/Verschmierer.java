package playground.mzilske.prognose2025;

import java.io.IOException;
import java.util.Collection;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.gis.ShapeFileReader;



public class Verschmierer {

	private static final String LANDKREISE = "../../prognose_2025/osm_zellen/landkreise.shp";
	
	private PopulationGenerator populationBuilder = new PopulationGenerator();

	private String filename;
	
	public Verschmierer(){
		filename = LANDKREISE;
	}
	
	public void prepare() {
		readShape();
	}
	
	private void readShape() {
		FeatureSource landkreisSource;
		try {
			landkreisSource = ShapeFileReader.readDataFile(filename);
			landkreisSource.getFeatures();
			Collection<Feature> landkreise = landkreisSource.getFeatures();
			for (Feature landkreis : landkreise) {
				Integer gemeindeschluessel = Integer.parseInt((String) landkreis.getAttribute("gemeindesc"));
				populationBuilder.addZone(gemeindeschluessel, landkreis.getDefaultGeometry());
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public Coord shootIntoSameZoneOrLeaveInPlace(Coord coord) {
		return populationBuilder.shootIntoSameZoneOrLeaveInPlace(coord);
	}
	
	public void setFilename(String filename){
		this.filename = filename;
	}

}
