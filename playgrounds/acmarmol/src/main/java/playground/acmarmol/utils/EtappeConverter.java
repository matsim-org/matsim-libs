package playground.acmarmol.utils;


import org.matsim.api.core.v01.Coord;
import org.matsim.utils.objectattributes.AttributeConverter;

import playground.acmarmol.microcensus2010.Etappe;

public class EtappeConverter implements AttributeConverter<Etappe> {

	@Override
	public Etappe convert(String value) {
		
		String[] entries = value.split("\t", -1);
		int departureTime = Integer.parseInt(entries[1]);
		int arrivalTime = Integer.parseInt(entries[2]);
		Coord startCoord = new CoordConverter().convert(entries[3]);
		Coord endCoord = new CoordConverter().convert(entries[4]);
		return new Etappe(departureTime, arrivalTime, startCoord, endCoord, entries[0]);

	}

	@Override
	public String convertToString(Object o) {
		
		// mode \t  departureTime \t arrivalTime \t starCoord \t endCoord 
		
		Etappe etappe = ((Etappe) o); 
		StringBuilder str = new StringBuilder();
		str.append(etappe.getMode() + "\t");
		str.append(etappe.getDepartureTime() + "\t");
		str.append(etappe.getArrivalTime() + "\t");
		str.append(new CoordConverter().convertToString(etappe.getStartCoord()) + "\t");
		str.append(new CoordConverter().convertToString(etappe.getEndCoord()) + "\t");
		
		return str.toString();
	}






}
