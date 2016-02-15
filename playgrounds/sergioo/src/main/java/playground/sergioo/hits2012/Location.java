package playground.sergioo.hits2012;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jdesktop.swingx.JXMapKit.DefaultProviders;
import org.matsim.api.core.v01.Coord;

import playground.sergioo.hits2012.Trip.PlaceType;

public class Location {

	public final static File SINGAPORE_COORDS_FILE = new File("./data/singaporeCoordsFile.dat");
	public static Map<String, Coord> SINGAPORE_COORDS_MAP = null;
	public static Map<PlaceType, DetailedType[]> DETAILED_TYPES = new HashMap<PlaceType, Location.DetailedType[]>();
	{
		DetailedType[] dTypes = new DetailedType[] {DetailedType.SHOP_HIGH, DetailedType.SHOP_LOW};
		DETAILED_TYPES.put(Trip.PlaceType.SHOP, dTypes);
		dTypes = new DetailedType[] {DetailedType.EAT_HIGH, DetailedType.EAT_LOW};
		DETAILED_TYPES.put(Trip.PlaceType.EAT, dTypes);
		dTypes = new DetailedType[] {DetailedType.CIVIC};
		DETAILED_TYPES.put(Trip.PlaceType.CIVIC, dTypes);
		dTypes = new DetailedType[] {DetailedType.HOME_OTHER};
		DETAILED_TYPES.put(Trip.PlaceType.HOME_OTHER, dTypes);
		dTypes = new DetailedType[] {DetailedType.PARK_HIGH, DetailedType.PARK_LOW};
		DETAILED_TYPES.put(Trip.PlaceType.PARK, dTypes);
		dTypes = new DetailedType[] {DetailedType.REC};
		DETAILED_TYPES.put(Trip.PlaceType.REC, dTypes);
		/*dTypes = new DetailedType[] {DetailedType.FUN};
		DETAILED_TYPES.put(Trip.PlaceType.FUN, dTypes);
		dTypes = new DetailedType[] {DetailedType.FINANTIAL};
		DETAILED_TYPES.put(Trip.PlaceType.FINANTIAL, dTypes);*/
	}
	public static PlaceType getTypeOfDetailedType(DetailedType detailedType) {
		for(Entry<PlaceType, DetailedType[]> detailedTypes:DETAILED_TYPES.entrySet())
			for(DetailedType detailedTypeI:detailedTypes.getValue())
				if(detailedTypeI.equals(detailedType))
					return detailedTypes.getKey();
		return null;
	}
	//Enumerations
	public static enum DetailedType implements Serializable {
		SHOP_HIGH, SHOP_LOW,
		EAT_HIGH, EAT_LOW,
		CIVIC,
		HOME_OTHER,
		PARK_HIGH, PARK_LOW,
		REC/*,
		FUN,
		FINANTIAL*/;
	}
	public static enum Column {
		POSTAL_CODE(5, "F"),
		AREA_CODE(6, "G"),
		AREA_NAME(7, "H");
		public int column;
		public String columnName;
		private Column(int column, String columnName) {
			this.column = column;
			this.columnName = columnName;
		}
	}

	//Constants
	public static Map<String, String> AREAS = new HashMap<String, String>();
	
	//Attributes
	private final String postalCode;
	private String areaCode;
	private final Map<String, Integer> types = new HashMap<String, Integer>();
	private final Map<String, Integer> purposes = new HashMap<String, Integer>();
	private final Coord coord;
	private final Set<DetailedType> detailedTypes = new HashSet<DetailedType>();
	
	//Constructors
	public Location(String postalCode) {
		super();
		this.postalCode = postalCode;
		this.coord = SINGAPORE_COORDS_MAP.get(postalCode);
	}
	public Location(String postalCode, String areaCode) {
		super();
		this.postalCode = postalCode;
		this.areaCode = areaCode;
		this.coord = SINGAPORE_COORDS_MAP.get(postalCode);
	}

	//Methods
	public String getPostalCode() {
		return postalCode;
	}
	public String getAreaCode() {
		return areaCode;
	}
	public Map<String, Integer> getTypes() {
		return types;
	}
	public void addType(String type) {
		Integer num = types.get(type);
		if(num==null)
			num = 0;
		types.put(type, num+1);
	}
	public Map<String, Integer> getPurposes() {
		return purposes;
	}
	public void addPurpose(String purpose) {
		Integer num = purposes.get(purpose);
		if(num==null)
			num = 0;
		purposes.put(purpose, num+1);
	}
	public Coord getCoord() {
		return coord;
	}
	public Set<DetailedType> getDetailedTypes() {
		return detailedTypes;
	}
	public void setDetailedTypes(Map<PlaceType, Map<Object, Integer>> distributions) {
		for(Entry<PlaceType, Map<Object, Integer>> map:distributions.entrySet())
			if(types.containsKey(map.getKey().text))
				setDetailedType(map);
	}
	private void setDetailedType(Entry<PlaceType, Map<Object, Integer>> map) {
		DetailedType[] dTypes = DETAILED_TYPES.get(map.getKey());
		int num = 0;
		for(Integer integer:map.getValue().values())
			num += integer;
		double part = num/(double)dTypes.length;
		num = 0;
		int pos=dTypes.length-1;
		for(Entry<Object, Integer> entry:map.getValue().entrySet()) {
			if(((Integer)entry.getKey()).equals(types.get(map.getKey().text))) {
				detailedTypes.add(DETAILED_TYPES.get(map.getKey())[pos]);
				return;
			}
			num+=entry.getValue();
			if(num>part) {
				num-=part;
				pos--;
			}
		}
	}
	public DetailedType getDetailedType(PlaceType placeType) {
		DetailedType[] detailedTypes = Location.DETAILED_TYPES.get(placeType);
		if(detailedTypes!=null)
			for(DetailedType detailedTypeT:detailedTypes)
				for(DetailedType detailedTypeL:this.detailedTypes)
					if(detailedTypeT.equals(detailedTypeL))
						return detailedTypeT;
		return null;
	}
	public PlaceType getType(DetailedType detailedType) {
		for(Entry<PlaceType, DetailedType[]> entry: DETAILED_TYPES.entrySet())
			for(DetailedType detailedTypeI:entry.getValue())
				if(detailedTypeI.equals(detailedType))
					if(types.containsKey(entry.getKey().text))
						return entry.getKey();
					else
						return null;
		return null;
	}
	
}
	
