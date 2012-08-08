package playground.acmarmol.microcensus2010.utils;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.utils.objectattributes.AttributeConverter;

	/**
	 * @author acmarmol
	 */
	public class CoordConverter implements AttributeConverter<Coord> {

		@Override
		public Coord convert(String value) {
		Double x = Double.parseDouble(value.substring(value.indexOf('=')+1, value.indexOf(',')-1));	
		Double y = Double.parseDouble(value.substring(value.indexOf(',')+1, value.indexOf(']')-1));	
			return new CoordImpl(x,y);
		}

		@Override
		public String convertToString(Object o) {
			Coord coord = (Coord) o;
			return coord.toString();
		}
	
}
