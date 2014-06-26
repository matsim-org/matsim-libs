package playground.southafrica.population.census2011.attributeConverters;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.utils.objectattributes.AttributeConverter;

public class CoordConverter implements AttributeConverter<CoordImpl> {
	private final Logger log = Logger.getLogger(CoordConverter.class);

	@Override
	public CoordImpl convert(String value) {
		String s = value.replace("(", "");
		s = s.replace(")", "");
		String[] sa = s.split(";");
		return new CoordImpl(Double.parseDouble(sa[0]), Double.parseDouble(sa[1]));
	}

	@Override
	public String convertToString(Object o) {
		if(!(o instanceof CoordImpl)){
			log.error("Object is not of type Coord: " + o.getClass().toString());
			return null;
		}
		Coord c = (Coord)o;
		
		return String.format("(%.2f;%.2f)", c.getX(), c.getY());
	}

}
