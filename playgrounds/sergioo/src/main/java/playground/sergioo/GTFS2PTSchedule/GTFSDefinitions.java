package playground.sergioo.GTFS2PTSchedule;

public enum GTFSDefinitions {
	
	//Constants
	/**
	 * Values
	 */
	STOPS("Stop","stops.txt",new String[]{"stop_id","stop_lon","stop_lat","stop_name"}),
	CALENDAR("Calendar","calendar.txt",new String[]{"service_id","monday","start_date","end_date"}),
	CALENDAR_DATES("CalendarDate","calendar_dates.txt",new String[]{"service_id","date","exception_type"}),
	SHAPES("Shape","shapes.txt",new String[]{"shape_id","shape_pt_lon","shape_pt_lat","shape_pt_sequence"}),
	ROUTES("Route","routes.txt",new String[]{"route_id","route_short_name","route_type"}),
	TRIPS("Trip","trips.txt",new String[]{"route_id","trip_id","service_id","shape_id"}),
	STOP_TIMES("StopTime","stop_times.txt",new String[]{"trip_id","stop_sequence","arrival_time","departure_time","stop_id"}),
	FREQUENCIES("Frequency","frequencies.txt",new String[]{"trip_id","start_time","end_time","headway_secs"});
	public enum WayTypes {
		RAIL,
		ROAD,
		WATER,
		CABLE;
	}
	public enum RouteTypes {
		//Values
		TRAM("tram",WayTypes.RAIL),
		SUBWAY("subway",WayTypes.RAIL),
		RAIL("rail",WayTypes.RAIL),
		BUS("bus",WayTypes.ROAD),
		FERRY("ferry",WayTypes.WATER),
		CABLE_CAR("cable car",WayTypes.CABLE);
		//Attributes
		public String name;
		public WayTypes wayType;
		//Methods
		private RouteTypes(String name,WayTypes wayType) {
			this.name = name;
			this.wayType = wayType;
		}
	}
	
	//Attributes
	public String name;
	public String fileName;
	public String[] columns;
	
	//Methods
	/**
	 * 
	 */
	private GTFSDefinitions(String name, String fileName, String[] columns) {
		this.name = name;
		this.fileName = fileName;
		this.columns = columns;
	}
	/**
	 * @param line
	 * @return Column positions of all column names 
	 */
	public int[] getIndices(String line) {
		String[] columnNames = line.split(",");
		int[] indices = new int[columns.length];
		for(int i=0; i<columnNames.length; i++)
			columnNames[i] = columnNames[i].trim();
		for(int i=0; i<columns.length; i++)
			indices[i] = getAttributeIndex(columns[i], columnNames);
		return indices;
	}
	/**
	 * @param attribute
	 * @param attributes
	 * @return the position of an attribute in an array
	 */
	private int getAttributeIndex(final String attribute, final String[] attributes) {
		for (int i = 0; i < attributes.length; i++)
			if (attributes[i].equals(attribute))
				return i;
		return -1;
	}
	/**
	 * @return the name of the function that processes a line of the aspect
	 */
	public String getFunction() {
		return "process"+name;
	}
	
}
