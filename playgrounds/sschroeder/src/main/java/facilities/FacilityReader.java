package facilities;

import java.util.Collection;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import com.vividsolutions.jts.geom.Coordinate;



public class FacilityReader extends MatsimXmlParser{
	
	private static Logger logger = Logger.getLogger(FacilityReader.class);
	
	private static final String SHOP = "shop";
	
	private static final String WAREHOUSE = "warehouse";
	
	private static final String TRANSHIPMENT = "transhipment centre";
	
	private static final String FIRM = "firm";

	private static String FACILITIES = "facilities";
	
	private static String FACILITY = "facility";
	
	private static String TYPE = "type";
	
	private static String ID = "id";
	
	private static String X_COORDINATE = "x";
	
	private static String Y_COORDINATE = "y";
	
	private static String LOCATIONID = "locationId";
	
	private Collection<Facility> facilities;

	public FacilityReader(Collection<Facility> facilities) {
		super();
		this.facilities = facilities;
	}
	
	public void read(String filename) {
		logger.info("read facilities");
		this.setValidating(false);
		parse(filename);
		logger.info("done");
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if(name.equals(FACILITY)){
			Facility f = null;
			String type = atts.getValue(TYPE);
			String idString = atts.getValue(ID);
			Id id = makeId(idString);
			String locationId = atts.getValue(LOCATIONID);
			String x = atts.getValue(X_COORDINATE);
			String y = atts.getValue(Y_COORDINATE);
			if(type.equals(SHOP)){
				f = new Shop(id);
			}
			if(type.equals(WAREHOUSE)){
				f = new Warehouse(id);
			}
			if(type.equals(FIRM)){
				f = new Firm(id);
			}
			if(type.equals(TRANSHIPMENT)){
				f = new TranshipmentCentre(id);
			}
			if(f == null){
				throw new IllegalStateException("cannot find type => cannot create facility");
			}
			if(locationId != null){
				f.setLocationId(makeId(locationId));
			}
			if(x != null && y != null){
				double xVal = Double.parseDouble(x);
				double yVal = Double.parseDouble(y);
				Coordinate coord = new Coordinate(xVal,yVal);
				f.setCoordinate(coord);
			}
			readOtherAttributes(f,atts);
			facilities.add(f);
		}
		
	}

	private void readOtherAttributes(Facility f, Attributes atts) {
		for(int i=3;i<atts.getLength();i++){
			String attName = atts.getQName(i);
			String attValue = atts.getValue(i);
			f.getAttributes().put(attName, attValue);
		}
		
	}

	private Id makeId(String id) {
		return new IdImpl(id);
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		
		
	}

}
