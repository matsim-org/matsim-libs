package org.matsim.utils.objectattributes.attributeconverters;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.utils.objectattributes.AttributeConverter;

public class CoordConverter implements AttributeConverter<Coord> {
	private final Logger log = Logger.getLogger(CoordConverter.class);

	@Override
	public Coord convert(String value) {
		String s = value.replace("(", "");
		s = s.replace(")", "");
		String[] sa = s.split(";");
		return new Coord(Double.parseDouble(sa[0]), Double.parseDouble(sa[1]));
	}

	@Override
	public String convertToString(Object o) {
		if(!(o instanceof Coord)){
			log.error("Object is not of type Coord: " + o.getClass().toString());
			return null;
		}
		Coord c = (Coord)o;
		
		return String.format("(%.2f;%.2f)", c.getX(), c.getY());
	}

}
