package playground.acmarmol.matsim2030.microcensus2010.objectAttributesConverters;


import org.matsim.utils.objectattributes.AttributeConverter;

import playground.acmarmol.matsim2030.microcensus2010.Etappe;



public class EtappeConverter implements AttributeConverter<Etappe> {

	@Override
	public Etappe convert(String value) {
		
		String[] entries = value.split("\t", -1);
		return new Etappe(Integer.parseInt(entries[2].trim()),Integer.parseInt(entries[3].trim()), new CoordConverter().convert(entries[4].trim()), new CoordConverter().convert(entries[5].trim()), Integer.parseInt(entries[0].trim()), entries[6].trim(), entries[7].trim(), entries[8].trim());

	}

	@Override
	public String convertToString(Object o) {
		
		// modeInteger \t mode \t  departureTime \t arrivalTime \t starCoord \t endCoord \t startCountry \t endCountry \t carType
		
		Etappe etappe = ((Etappe) o); 
		StringBuilder str = new StringBuilder();
		str.append(etappe.getModeInteger() + "\t" + etappe.getMode() +"\t");
		str.append(etappe.getDepartureTime() + "\t");
		str.append(etappe.getArrivalTime() + "\t");
		str.append(new CoordConverter().convertToString(etappe.getStartCoord()) + "\t");
		str.append(new CoordConverter().convertToString(etappe.getEndCoord()) + "\t");
		str.append(etappe.getStartCountry() + "\t");
		str.append(etappe.getEndCountry() + "\t");
		str.append(etappe.getCarType());
		
		return str.toString();
	}






}
