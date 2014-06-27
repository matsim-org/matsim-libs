package playground.southafrica.projects.treasury2014;

/**
 * Descriptive names for each of the study areas covered in the National
 * Treasury project than span 2011-2014.
 * 
 * @author jwjoubert
 *
 */
public enum StudyAreas {
	/* Constants */
	GAUTENG("Gauteng"),
	BUFFALO_CITY("BuffaloCity"),
	CAPE_TOWN("CapeTown"),
	ETHEKWINI("eThekwini"),
	MANGAUNG("Mangaung"),
	MBOMBELA("Mbombela"),
	NMBM("NelsonMandelaBay"),
	POLOKWANE("Polokwane"),
	RUSTENBURG("Rustenburg")
	;
	
	private final String name;
	
	StudyAreas(String s){ name = s; }
	
	public String getName(){
		return name;
	}
		

}
