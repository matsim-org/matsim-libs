package facilities;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.MatsimXmlWriter;




public class FacilityWriter extends MatsimXmlWriter{
	
	private static Logger logger = Logger.getLogger(FacilityWriter.class);
	
	private Collection<Facility> facilities;
	
	public FacilityWriter(Collection<Facility> facilities) {
		super();
		this.facilities = facilities;
	}

	public void write(String filename) {
		logger.info("write facilities");
		openFile(filename);
		writeXmlHead();
		startFacilities(this.writer);
		for(Facility facility : facilities){
			startFacility(facility,this.writer);
			endFacility(this.writer);
		}
		endFacilities(this.writer);
		close();
		logger.info("done");
	}

	private void endFacility(BufferedWriter writer) {
		
	}

	private void startFacility(Facility facility, BufferedWriter writer) {
		try {
			writer.write("\t\t<facility ");
			writer.write(" id=\"" + facility.getId().toString() + "\"");
			String locationValue = null;
			if(facility.getLocationId() != null){
				locationValue = facility.getLocationId().toString();
			}
			writer.write(" locationId=\"" + locationValue + "\"");
			writer.write(" type=\"" + facility.getType() + "\"");
			for(String otherAttributesName : facility.getAttributes().keySet()){
				writer.write(" " + otherAttributesName.toLowerCase() + "=\"" + facility.getAttributes().get(otherAttributesName) + "\"");
			}
			writer.write("/>\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
	}

	private void endFacilities(BufferedWriter writer){
		try {
			writer.write("\t</facilities>\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void startFacilities(BufferedWriter writer) {
		try {
			writer.write("\t<facilities>\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	

}
