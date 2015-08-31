package playground.dhosse.utils.osm;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.xml.v0_6.XmlReader;

public class OsmObjectsToFacilitiesParser {

	private final static Logger log = Logger.getLogger(OsmObjectsToFacilitiesParser.class);
	private QuadTree<Id<ActivityFacility>> linkQT;
	private ActivityFacilities facilities;
	private ObjectAttributes facilityAttributes;
	private final CoordinateTransformation ct;
	private Map<String, String> osmToMatsimTypeMap;
	private final Set<String> keysOfInterest;
	
	private final String file;
	
	public ActivityFacilities getFacilities() {
		return facilities;
	}

	public ObjectAttributes getFacilityAttributes() {
		return facilityAttributes;
	}

	public OsmObjectsToFacilitiesParser(String file, CoordinateTransformation ct, Map<String, String> osmToMatsimTypeMap, Set<String> keysOfInterest){
		
		log.info("Initializing osm parser...");
		
		this.file = file;
		this.ct = ct;
		this.osmToMatsimTypeMap = osmToMatsimTypeMap;
		this.keysOfInterest = keysOfInterest;
		
	}
	
	public QuadTree<Id<ActivityFacility>> getQuadTree(){
		return this.linkQT;
	}
	
	public void parse(){
		
		try {
			
			File f = new File(this.file);
			
			if(!f.exists()){
				
				throw new FileNotFoundException("Coud not find " + file);
				
			}
			
			OsmSink sink = new OsmSink(this.ct, this.osmToMatsimTypeMap, this.keysOfInterest);
			XmlReader reader = new XmlReader(f, false, CompressionMethod.None);
			reader.setSink(sink);
			reader.run();
			
			this.facilities = sink.getFacilities();
			this.facilityAttributes = sink.getFacilityAttributes();
			
		} catch (FileNotFoundException e) {
			
			e.printStackTrace();
			
		}
		
	}
	
	public void writeFacilityCoordinates(String out){
		log.info("Writing facility coordinates to " + out);
		BufferedWriter bw = IOUtils.getBufferedWriter(out);
		try{
			bw.write("FacilityId,Long,Lat,Type");
			bw.newLine();
			for(Id<ActivityFacility> id : this.facilities.getFacilities().keySet()){
				ActivityFacility facility = this.facilities.getFacilities().get(id);
				String ao = "";
				for(String s : facility.getActivityOptions().keySet()){
					ao += s + "_";
				}
				bw.write(id.toString());
				bw.write(",");
				bw.write(String.format("%.0f,%.0f", facility.getCoord().getX(), facility.getCoord().getY()) + "," + ao + "\n");
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not write to BufferedWriter " + out);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close BufferedWriter " + out);
			}
		}
		log.info("Done writing coordinates to file.");
	}
	
	public void writeFacilities(String out){
		new FacilitiesWriter(this.facilities).write(out);
	}
	
	public void writeFacilityAttributes(String out){
		new ObjectAttributesXmlWriter(this.facilityAttributes).writeFile(out);
	}
	
}
