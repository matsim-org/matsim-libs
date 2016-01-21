package playground.sergioo.hits2012;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class Trip implements Serializable {

	//Enumerations
	public static enum Column implements Serializable {
	
		START_POSTAL_CODE(73,"BV"),
		END_POSTAL_CODE(74,"BW"),
		START_TIME(75,"BX"),
		END_TIME(76,"BY"),
		PLACE_TYPE(77, "BZ"),
		PURPOSE(78, "CA"),
		MODE(80, "CC"),
		FACTOR(107, "DD");
	
		//Attributes
		public int column;
		public String columnName;
	
		//Constructors
		private Column(int column, String columnName) {
			this.column = column;
			this.columnName = columnName;
		}
	
	}
	public static enum PlaceType implements Serializable {
		TEMPLE("temple"),
		GOVERMENT("goverment"),
		PETROL("petrol"),
		WORK_RELATED("workRelated"),
		EAT("eat"),
		WATER("water"),
		CIVIC("civic"),
		FUN("fun"),
		SHOP("shop"),
		FINANTIAL("finantial"),
		SCHOOL("school"),
		MEDICAL("medical"),
		PARK("park"),
		HOME_OTHER("homeOther"),
		EDU("edu"),
		REC("rec"),
		ISLAND("island"),
		HOME("home"),
		HOTEL("hotel"),
		CHECK("check"),
		INDUSTRIAL("industrial"),
		TRANSPORT("transport"),
		PORT("port"),
		AIRPORT("airport"),
		MILITARY("military"),
		OFFICE("office");
		//Attributes
		public String text;
	
		//Contructors
		private PlaceType(String text) {
			this.text = text;
		}
		public static PlaceType getPlaceType(String text) {
			for(PlaceType placeType:PlaceType.values())
				if(placeType.text.equals(text))
					return placeType;
			return null;
		}
	}
	public static enum Purpose {
		MEDICAL("medical"),
		WORK_FLEX("workFlex"),
		SHOP("shop"),
		P_U_D_O("pUdO"),
		SOCIAL("social"),
		HOME("home"),
		NS("ns"),
		WORK("work"),
		EAT("eat"),
		EDU("edu"),
		ACCOMP("accomp"),
		REC("rec"),
		ERRANDS("errands"),
		DRIVE("drive"),
		RELIGION("religion");
		//Attributes
		public String text;
	
		//Contructors
		private Purpose(String text) {
			this.text = text;
		}
	}

	//Constants
	public static Set<String> PLACE_TYPES = new HashSet<String>();
	public static Set<String> PURPOSES = new HashSet<String>();
	public static Set<String> MODES = new HashSet<String>();

	//Attributes
	private final String id;
	private final String startPostalCode;
	private final String endPostalCode;
	private final Date startTime;
	private final Date endTime;
	private final String placeType;
	private final String purpose;
	private final String mode;
	private final double factor;
	
	private final SortedMap<String, Stage> stages = new TreeMap<String, Stage>();

	//Constructors
	public Trip(String id, String startPostalCode, String endPostalCode,
			Date startTime, Date endTime, String placeType, String purpose,
			String mode, double factor) {
		super();
		this.id = id;
		this.startPostalCode = startPostalCode;
		this.endPostalCode = endPostalCode;
		this.startTime = startTime;
		this.endTime = endTime;
		this.placeType = placeType;
		this.purpose = purpose;
		this.mode = mode;
		this.factor = factor;
	}

	//Methods
	public String getId() {
		return id;
	}
	public void addStage(Stage stage) {
		stages.put(stage.getId(), stage);
	}
	public Stage getStage(String id) {
		return stages.get(id);
	}
	public SortedMap<String, Stage> getStages() {
		return stages;
	}
	public String getStartPostalCode() {
		return startPostalCode;
	}
	public String getEndPostalCode() {
		return endPostalCode;
	}
	public Date getStartTime() {
		return startTime;
	}
	public Date getEndTime() {
		return endTime;
	}
	public String getPlaceType() {
		return placeType;
	}
	public String getPurpose() {
		return purpose;
	}
	public String getMode() {
		return mode;
	}
	public double getFactor() {
		return factor;
	}

}
