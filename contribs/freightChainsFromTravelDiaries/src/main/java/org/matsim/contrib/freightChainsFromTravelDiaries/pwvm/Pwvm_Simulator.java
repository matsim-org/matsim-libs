package org.matsim.contrib.freightChainsFromTravelDiaries.pwvm;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;

public class Pwvm_Simulator {

	private boolean verbosemode = false;
	private double randomSampleRatio = 1.0;
	private static final int MAX_DIAMETER = 100; // in meters when GK 3

	private static final int BUSINESS = 1;
	private static final int HOUSEHOLD = 2;
	private static final int ADDRESS = 3;

	private static final int Z_SOURCE = 1;
	private static final int Z_HOME   = 2;
	private static final String SRID_IDENTIFIER = "31467";

	Connection con = Pwvm_DatabaseConnection.dbconnect();

	public void setVerbosemode(boolean verbosemode) {
		this.verbosemode = verbosemode;
	}

	public void setRandomSampleRatio(double randomSampleRatio) {
		this.randomSampleRatio = randomSampleRatio;
	}

	private void logToDatabaseProtocol(int type, String message, int logbookId) throws SQLException {
		Statement st = con.createStatement();
		st.executeUpdate("INSERT INTO pwvm_logmessages (type, message, \"logbookId\") VALUES ("+type+", '"+message+"', "+logbookId+")");		
		st.close();
	}
	
	private void logToDatabaseProtocol(int type, String message) throws SQLException {
		Statement st = con.createStatement();
		st.executeUpdate("INSERT INTO pwvm_logmessages (type, message) VALUES ("+type+", '"+message+"')");		
		st.close();
	}

	private int translateFromKidToPwvm(int typeOfDestination) {
		int r = -1;
		switch (typeOfDestination) {
		case 1: // terminal, station, port, airport
			r = BUSINESS;
			break;
		case 2: // shipping agency
			r = BUSINESS;
			break;
		case 3: // construction site
			r = ADDRESS;
			break;
		case 4: // own company
			r = -1;
			break;
		case 5: // external / other company
			r = BUSINESS;
			break;
		case 6: // private household
			r = HOUSEHOLD;
			break;
		case 7: // other business-related destination
			r = ADDRESS;
			break;
		case 8: // private destination
			r = ADDRESS;
			break;
		default:
			System.out.println("Exit at translateFromKidToPwvm().");
		System.out.println("typeOfDestination: "+typeOfDestination);
		System.exit(-1);
		}
		return r;
	}

	private boolean exceedsHouseholdDefinition(String fromGeometry,
			double radius, int width) throws SQLException {
	
		// Check if households are defined for target area
		int halfWidth = width / 2;
		String borderdefstr = "(SELECT the_geom FROM pwvm_borderdefinition WHERE id=10)"; // households
		String stmt = "SELECT ST_Covers(" +
		borderdefstr + ", " +
		"ST_Difference(" +
		"ST_Buffer('"+fromGeometry+"', "+(radius+halfWidth)+"), " +
		"ST_Buffer('"+fromGeometry+"', "+(radius-halfWidth)+")) " +
		")";
		System.out.println(stmt);
		Statement st = con.createStatement();
		ResultSet rs = st.executeQuery(stmt);
		rs.next();
		boolean b = rs.getBoolean(1);
		if (b == true)
			return false;
		else {
			stmt = "SELECT ST_Intersects(" +
			borderdefstr + ", " +
			"ST_Difference(" +
			"ST_Buffer('"+fromGeometry+"', "+(radius+halfWidth)+"), " +
			"ST_Buffer('"+fromGeometry+"', "+(radius-halfWidth)+")) " +
			")";
			rs = st.executeQuery(stmt);
			rs.next();
			b = rs.getBoolean(1);
			if (b == false) {
				// target circle must be completely outside of model definition area
				st.close();
				return true;
			} else {
				stmt = "SELECT 1 FROM pwvm_household h " +
				"LEFT JOIN gis_housecoordinate hc ON (h.house_coordinate = hc.s2) " +
				"WHERE ST_Covers(ST_Intersects(" +
				borderdefstr + ", " +
				"ST_Difference(" +
				"ST_Buffer('"+fromGeometry+"', "+(radius+halfWidth)+"), " +
				"ST_Buffer('"+fromGeometry+"', "+(radius-halfWidth)+")), hc.the_geom) " +
				") " +
				"LIMIT 1";
				System.out.println(stmt);
				rs = st.executeQuery(stmt);
				if (rs.next()) {
					st.close();
					return false;
				} else {
					st.close();
					return true;
				}
			}
		}
	
	}

	/**
	 * Selects a logbook from database randomly under consideration
	 * of economic sector and headcount class
	 * 
	 * @param wz economic sector
	 * @param headcountClass number of employees classified
	 * @return the logbook id that was selected
	 * @throws SQLException
	 */
	private int chooseRandomLogbook(String wz, int headcountClass) throws SQLException {

		String stmt = "SELECT id FROM pwvm_logbook " +
		"WHERE business_wz = '"+wz+"' " +
		"AND business_headcountclass = " + headcountClass + 
		" ORDER BY random() LIMIT 1";

		if (verbosemode)
			System.out.println(stmt);
		Statement st = con.createStatement();
		ResultSet l = st.executeQuery(stmt);
		int i = -1;
		if (l.next())
			i = l.getInt(1);
		st.close();
		return i;
	}

	/**
	 * loads a logbook from database and returns it as a logbook object
	 * 
	 * @param logbookId the logbook id of the logbook to retrieve
	 * @return the logbook object (that contains all trips)
	 * @throws SQLException
	 */
	private Pwvm_Logbook loadLogbookFromDatabase(int logbookId) throws SQLException {

		String stmt = "SELECT source_type, the_geom, vehicletype " +
		"FROM pwvm_logbook " +
		"WHERE id = "+logbookId;

		if (verbosemode)
			System.out.println(stmt);
		
		Statement st = con.createStatement();
		ResultSet rs = st.executeQuery(stmt);

		rs.next();
		Pwvm_Logbook l = new Pwvm_Logbook(rs.getInt(1), rs.getString(2), rs.getInt(3));
		
		int source_type = rs.getInt(1);
		
		System.out.println("source_type: "+source_type);

		stmt = "SELECT dest_type, purpose, z_source, z_home, distance_empirical, the_geom, (the_geom IS NULL) as the_geom_is_null, start_time, stop_time " +
		"FROM pwvm_logbook_trip " +
		"WHERE \"logbookId\" = "+logbookId +
		" ORDER BY \"tripId\" ASC";

		// TODO (the_geom IS NULL) ist ueberfluessig -> entfernen

		if (verbosemode)
			System.out.println(stmt);
		rs = st.executeQuery(stmt);
		Statement st2 = con.createStatement();

		//		System.out.println("\ndest_type | purpose | z_source | z_home | distance | distance_empirical | ISNULL(the_geom) | the_geom");

		int i = 0;
		while (rs.next()) {

			i++;
			int dest_type = rs.getInt(1);
			int purpose = rs.getInt(2);
			double z_source = rs.getDouble(3); // [m]
			double z_home = rs.getDouble(4); // [m]
			double distanceEmpirical = rs.getDouble(5) * 1000; // Umrechnung [km] in [m]
			double distance = distanceEmpirical;
			String geometry = rs.getString(6);
			String start_time = rs.getString(8);
			String stop_time = rs.getString(9);

			// calculate airline trip distance if a geometry is provided
			if (rs.getBoolean("the_geom_is_null") == false) {
				// Calculate air-line distance of current trip
				// (if the_geom is null, then the empirical one is used instead
				String stmt2;
				if (i == 1)  // first trip starts from source
					stmt2 = "SELECT ST_Distance((SELECT the_geom FROM pwvm_logbook WHERE id = "+logbookId+"), (SELECT the_geom FROM pwvm_logbook_trip WHERE \"logbookId\" = "+logbookId+" AND \"tripId\" = 1))";
				else // all other start from previous trip's destination
					stmt2 = "SELECT ST_Distance((SELECT the_geom FROM pwvm_logbook_trip WHERE \"logbookId\" = "+logbookId+" AND \"tripId\" = "+(i-1)+"), (SELECT the_geom FROM pwvm_logbook_trip WHERE \"logbookId\" = "+logbookId+" AND \"tripId\" = "+i+"))";

				if (verbosemode)
					System.out.println(stmt2);
				
				ResultSet rs2 = st2.executeQuery(stmt2);
				rs2.next();
				distance = rs2.getDouble(1);  // [m]
				if (distance == 0)	// wenn dabei Distanz = 0 entstand, wieder auf den empirischen Wert zuruecksetzen.
					if (distanceEmpirical >= 0) // ausgenommen wenn distanceEmpirical ist -9 (nicht erhoben) oder -1 (keine Angabe)
						distance = distanceEmpirical;
			}

			l.addTrip(i, source_type, dest_type, purpose, z_source, z_home, distance, distanceEmpirical, geometry, start_time, stop_time);

			//			System.out.println(dest_type+ " | " +purpose+ " | " +z_source+ " | " +z_home+ " | " +distance+ " | " +distance_empirical + " | "+rs.getBoolean("the_geom_is_null") + " | " + geometry);
			
			source_type = dest_type;

		}	

		st.close();
		st2.close();
		return l;
	}

	/**
		 * Returns a coordinate with the given distance from fromGeometry.
		 * @param fromGeometry
		 * @param distance
		 * @return
		 * @throws SQLException
		 */
		private ResultSet getRandomCoordinate(String fromGeometry, double distance) throws SQLException {
	
			if (verbosemode)
				System.out.println("Generating random coordinate...");
			
			String stmt = "SELECT (ST_Dump(ST_GeomFromText(overlay(ST_AsText(" +	
			"ST_ExteriorRing(ST_Buffer('"+fromGeometry+"', "+distance+")) " +
			") placing 'MULTIPOINT' from 1 for 10), '"+SRID_IDENTIFIER+"'))).geom AS the_geom";
			if (verbosemode)
				System.out.println(stmt);
			Statement st = con.createStatement();
			ResultSet rs = st.executeQuery(stmt);
			return rs;
	//		rs.next();
	//		String s = rs.getString(1);
	//		st.close();
	//		return s;
		}

	//	/**
	//	 * Returns a coordinate with a given distance to fromGeometry
	//	 * and a given distance to homeBusinessGeometry.
	//	 * 
	//	 * If fromGeometry and homeBusinessGeometry are equal, a random
	//	 * coordinate in the given distance is returned
	//	 * 
	//	 * @param fromGeometry
	//	 * @param distance
	//	 * @param homeBusinessGeometry
	//	 * @param zFromHome
	//	 * @return
	//	 * @throws SQLException
	//	 */
	//	private String getCoordinate(String fromGeometry,
	//			double distance, String homeBusinessGeometry, double zFromHome) throws SQLException {
	//
	//		if (verbosemode)
	//			System.out.println("GETTING COORDINATE WITH zFromHome AND DISTANCE!!!!!!!!!");
	//		if (fromGeometry.equals(homeBusinessGeometry))
	//			return getRandomCoordinate(fromGeometry, distance);
	//		else {
	//			String stmt = "SELECT ST_GeometryN(ST_Intersection(" +
	//			"ST_ExteriorRing(ST_Buffer('"+fromGeometry+"', "+distance+")), " +
	//			"ST_ExteriorRing(ST_Buffer('"+homeBusinessGeometry+"', "+zFromHome+")) " +
	//			"), 1)";
	//			if (verbosemode)
	//				System.out.println(stmt);
	//			Statement st = con.createStatement();
	//			ResultSet rs = st.executeQuery(stmt);
	//			rs.next();
	//			String s = rs.getString(1);
	//			st.close();
	//			if (s != null) 
	//				return s;
	//			else
	//				// if both circles do not intersect return any coordinate
	//				return getRandomCoordinate(fromGeometry, distance);
	//		}
	//	}
		
		/**
		 * Returns a coordinate with a given distance to fromGeometry
		 * and a given distance to homeBusinessGeometry.
		 * 
		 * If fromGeometry and homeBusinessGeometry are equal, a random
		 * coordinate in the given distance is returned
		 * 
		 * @param fromGeometry
		 * @param distance
		 * @param homeBusinessGeometry
		 * @param zFromHome
		 * @return
		 * @throws SQLException
		 */
		private ResultSet getCoordinatesFromDistanceAndZ(String fromGeometry,
				double distance, String homeBusinessGeometry, double zFromHome) throws SQLException {
	
			if (verbosemode)
				System.out.println("Generating coordinates based on zFromHome and distance...");
			if (fromGeometry.equals(homeBusinessGeometry))
				// fromGeometry and homeBusinessGeometry are equal, therefore value z can't be used
				return getRandomCoordinate(fromGeometry, distance);
			else {
				String stmt = "SELECT (ST_Dump(ST_Intersection(" +
				"ST_ExteriorRing(ST_Buffer('"+fromGeometry+"', "+distance+")), " +
				"ST_ExteriorRing(ST_Buffer('"+homeBusinessGeometry+"', "+zFromHome+")) " +
				"))).geom AS the_geom";
				if (verbosemode)
					System.out.println(stmt);
				Statement st = con.createStatement();
				ResultSet rs = st.executeQuery(stmt);
				
				if (rs.isBeforeFirst() == false) {
					// no record found
					return getRandomCoordinate(fromGeometry, distance);
				} else {
					System.out.println("  circles intersect [OK]");
					return rs;
				}
	//			rs.next();
	//			String s = rs.getString(1);
	//			st.close();
	//			if (s != null) 
	//				return s;
	//			else
	//				// if both circles do not intersect return any coordinate
	//				return getRandomCoordinate(fromGeometry, distance);
			}
		}

	/**
	 * Adds a trip to the trip matrix in the database.
	 * 
	 * @param logbookId the id of the logbook we are currently generating
	 * 		  (not the template logbook's id)
	 * @param tripId the trip's position within the logbook
	 * @param fromGeometry must be POINT
	 * @param toGeometry must be POINT
	 * @param purpose
	 * @param homeBusinessId
	 * @throws SQLException
	 */
	private void storeTripToDatabase(int logbookId, int tripId, String fromGeometry, String toGeometry, int purpose, int destinationType, int vehicleType, int homeBusinessId, String start_time, String stop_time, int sourceType, boolean isReversed) throws SQLException {
	
		if (start_time != null)
			start_time = "'"+start_time+"'";
		if (stop_time != null)
			stop_time = "'"+stop_time+"'";
		
		String fromGeom = fromGeometry;
		String toGeom = toGeometry;
		int dest_type = destinationType;
		int source_type = sourceType;
		
		if (isReversed) {
			fromGeom = toGeometry;
			toGeom = fromGeometry;
			source_type = destinationType;
			dest_type = sourceType;
		}
		
		
		Statement st = con.createStatement();
		String stmt = "INSERT INTO pwvm_matrix (source_geom, dest_geom, purpose, sourcetype, destinationtype, vehicletype, business_id, logbook_id, trip_id, start_time, stop_time) " +
		"VALUES ('" +
		fromGeom + "', '" +
		toGeom + "', " +
		purpose + ", " +
		source_type + ", " +
		dest_type + ", " +
		vehicleType + ", " +
		homeBusinessId + ", " +
		logbookId + ", " +
		tripId + ", " +
		start_time + ", " +
		stop_time + ")";
		st.executeUpdate(stmt);
		if (verbosemode)
			System.out.println(stmt);
		st.close();
	}

	private void storeSearchRadiusToDatabase(String fromGeometry,
			double radius, int width, int typeOfDestination, int homeBusinessId) throws SQLException {
		int halfWidth = width / 2;
		Statement st = con.createStatement();
		String stmt = "INSERT INTO searchradius (dest_type, homebusiness, the_geom) SELECT "+typeOfDestination+", "+homeBusinessId+", ST_Difference(" +
		"ST_Buffer('"+fromGeometry+"', "+(radius+halfWidth)+"), " +
		"ST_Buffer('"+fromGeometry+"', "+(radius-halfWidth)+"))";
		st.executeUpdate(stmt);
	}

	private boolean isWithinModelArea(String fromGeometry, double radius, int width) throws SQLException {
		int halfWidth = width / 2;
		String borderdefstr = "(SELECT the_geom FROM pwvm_borderdefinition WHERE id=10)"; // households		
		String stmt = "SELECT ST_Intersects(" +
		borderdefstr + ", " +
		"ST_Difference(" +
		"ST_Buffer('"+fromGeometry+"', "+(radius+halfWidth)+"), " +
		"ST_Buffer('"+fromGeometry+"', "+(radius-halfWidth)+")) " +
		")";
		if (verbosemode)
			System.out.println(stmt);
		Statement st = con.createStatement();
		ResultSet rs = st.executeQuery(stmt);
		rs.next();
		return rs.getBoolean(1);
	}

	/**
	 * Selects geometries of all locations with distance "radius" arround
	 * a given point "fromGeometry". What locations are returned depends
	 * on typeOfDestination 
	 * @param fromGeometry must be POINT
	 * @param homeBusinessId
	 * @param typeOfDestination
	 * @param radius How far locations must be away from "fromGeometry"
	 * @param width The value "radius" is expanded by "width".
	 * @return a ResultSet object containing one column named the_geom
	 * @throws SQLException
	 */
	private ResultSet findLocation(String fromGeometry, int homeBusinessId,
			int typeOfDestination, double radius, int width, double zFromHome, String homeBusinessGeometry, double maxAllowableDistanceFromHomeBusiness, String homeBusinessEconomicSector) throws SQLException {

		/* TODO Geschwindigkeit optimieren, indem bei kurzen Distanzen erst der groessere Radius
		 * eingeschraenkt wird, und dann der kleinere weiter eingrenzt.
		 * Bei grossem Radius hingegen erst den kleinen erfragen, und danach den grossen berechnen.
		 * Dies fuehrt dazu, dass bei grossen Distanzen (z.B. weit ausserhalb Berlins) direkt
		 * im ersten Schritt alle Berliner Adressen herausgefiltert werden und nicht erst im zweiten.
		 * 
		 * Alternativ ein Polygon erstellen, dass den Kreis darstellt und dann alle Adressen zurueckgeben,
		 * die darin liegen. Koennte schneller sein.
		 */

		// DEBUG: storeSearchRadiusToDatabase(fromGeometry, radius, width, typeOfDestination, homeBusinessId);


		// wenn ein Haushalt gesucht wird erst pruefen, ob Haushalte in dieser Entfernung in der synthetischen Bevoelkerung definiert sind.
		//		if (typeOfDestination == 6) { // private households
		//			if (isWithinSyntheticPopulation(fromGeometry, radius, width) == false) {
		//				
		//				typeOfDestination = -1; // ansonsten nach beliebigen Adresse suchen
		//				if (isWithinSyntheticEconomicStructure(fromGeometry, radius, width) == false)
		//					return null; // Route nicht weiter verfolgen da ausserhalb des Modellbereichs
		//			}
		//		} else
		//			if (isWithinSyntheticEconomicStructure(fromGeometry, radius, width) == false)
		//				return null; // Route nicht weiter verfolgen da ausserhalb des Modellbereichs

		String stmt = "";
		int halfWidth = width / 2;

		switch (typeOfDestination) {
		case 1: // terminal, station, port, airport			
			stmt = "SELECT hc.the_geom FROM gis_housecoordinate hc JOIN gis_dlm_fot fot ON (ST_Covers(fot.the_geom, hc.the_geom)) " +
			"WHERE ST_DWithin('"+fromGeometry+"', hc.the_geom, "+(radius+halfWidth)+") " +
			"AND ST_Distance('"+fromGeometry+"', hc.the_geom) >= "+(radius-halfWidth)+" " +
			"AND fot.objart = '2114' "; // special land use
			if (maxAllowableDistanceFromHomeBusiness > 0)
				stmt += "AND ST_DWithin('"+homeBusinessGeometry+"', hc.the_geom, "+maxAllowableDistanceFromHomeBusiness+") ";
			stmt += "GROUP BY hc.the_geom ";
			break;
		case 2: // shipping agency
			stmt = "SELECT hc.the_geom FROM pwvm_business b LEFT JOIN gis_housecoordinate hc ON (b.house_coordinate = hc.s2) " +
			"WHERE ST_DWithin('"+fromGeometry+"', hc.the_geom, "+(radius+halfWidth)+") " +
			"AND ST_Distance('"+fromGeometry+"', hc.the_geom) >= "+(radius-halfWidth)+" " +
			"AND b.id != "+homeBusinessId+" " +
			"AND b.wz_1steller = 'I' ";
			if (maxAllowableDistanceFromHomeBusiness > 0)
				stmt += "AND ST_DWithin('"+homeBusinessGeometry+"', hc.the_geom, "+maxAllowableDistanceFromHomeBusiness+") ";
			stmt += "GROUP BY hc.the_geom ";
			break;
		case 5: // external company
			stmt = "SELECT hc.the_geom FROM pwvm_business b LEFT JOIN gis_housecoordinate hc ON (b.house_coordinate = hc.s2) " +
			"WHERE ST_DWithin('"+fromGeometry+"', hc.the_geom, "+(radius+halfWidth)+") " +
			"AND ST_Distance('"+fromGeometry+"', hc.the_geom) >= "+(radius-halfWidth)+" " +
			"AND b.id != "+homeBusinessId + " ";
			if (maxAllowableDistanceFromHomeBusiness > 0)
				stmt += "AND ST_DWithin('"+homeBusinessGeometry+"', hc.the_geom, "+maxAllowableDistanceFromHomeBusiness+") ";
			stmt += "GROUP BY hc.the_geom ";
			break;
		case 6: // private household
			stmt = "SELECT hc.the_geom FROM pwvm_household h LEFT JOIN gis_housecoordinate hc ON (h.house_coordinate = hc.s2) " +
			"WHERE ST_DWithin('"+fromGeometry+"', hc.the_geom, "+(radius+halfWidth)+") " +
			"AND ST_Distance('"+fromGeometry+"', hc.the_geom) >= "+(radius-halfWidth)+" ";
			if (maxAllowableDistanceFromHomeBusiness > 0)
				stmt += "AND ST_DWithin('"+homeBusinessGeometry+"', hc.the_geom, "+maxAllowableDistanceFromHomeBusiness+") ";
			stmt += "GROUP BY hc.the_geom ";	
			break;
		case 9: // "Filiale"
			stmt = "SELECT hc.the_geom FROM pwvm_business b LEFT JOIN gis_housecoordinate hc ON (b.house_coordinate = hc.s2) " +
			"WHERE ST_DWithin('"+fromGeometry+"', hc.the_geom, "+(radius+halfWidth)+") " +
			"AND ST_Distance('"+fromGeometry+"', hc.the_geom) >= "+(radius-halfWidth)+" " +
			"AND b.id != "+homeBusinessId + " " +
			"AND b.wz_1steller = '"+homeBusinessEconomicSector+"' ";
			if (maxAllowableDistanceFromHomeBusiness > 0)
				stmt += "AND ST_DWithin('"+homeBusinessGeometry+"', hc.the_geom, "+maxAllowableDistanceFromHomeBusiness+") ";
			stmt += "GROUP BY hc.the_geom ";
			break;
		default: // -> look for any address in range
			stmt = "SELECT hc.the_geom FROM gis_housecoordinate hc " +
			"WHERE ST_DWithin('"+fromGeometry+"', hc.the_geom, "+(radius+halfWidth)+") " +
			"AND ST_Distance('"+fromGeometry+"', hc.the_geom) >= "+(radius-halfWidth)+" ";
			if (maxAllowableDistanceFromHomeBusiness > 0)
				stmt += "AND ST_DWithin('"+homeBusinessGeometry+"', hc.the_geom, "+maxAllowableDistanceFromHomeBusiness+") ";
			stmt += "GROUP BY hc.the_geom ";
			break;
		}

		if (zFromHome != -1 && homeBusinessGeometry != null)
			stmt += "ORDER BY ABS("+zFromHome+"-ST_Distance('"+homeBusinessGeometry+"', hc.the_geom)) ASC";
		else
			stmt += "ORDER BY random()";


		if (verbosemode)
			System.out.println(stmt);
		Statement st = con.createStatement();
		ResultSet rs = st.executeQuery(stmt);

		return rs;
	}

	/**
	 * @param the id of the logbook that we are generating at the moment
	 *        (global because this logbook l might be part of a global logbook
	 *        consisting of multiple trips)
	 * @param fromGeometry
	 * @param l
	 * @param tripId
	 * @return returns true if trip could be assigned, else false
	 * @throws SQLException
	 */
	private boolean assignTrip(int globalLogbookId, String fromGeometry, Pwvm_Logbook l, int tripId) throws SQLException {

		System.out.println("tripId = "+tripId+":");

		double maxAllowableDistanceFromHomeBusiness = -1;
		String toGeometry = "";
		int numberOfLocatonsFound = 0;
		ResultSet rs = null;

		Pwvm_LogbookTrip trip = l.getTrip(tripId);
		
//		// try half the diameter first, then expand to MAX_DIAMETER if necessary
//		int variableDiameter = MAX_DIAMETER / 4;
//		for (int i = 0; i < 2; i++) {

		int variableDiameter = MAX_DIAMETER;
//			variableDiameter = variableDiameter * 2;

			if (l.getTypeOfSource() == 4 && l.getTypeOfLastStop() == 4) {
				// is Roundtour
				// Thus limit query to objects within distance from home business,
				// but only if not last trip of tour
				if (l.getNumberOfTrips()-1 > tripId)
					// not last trip
					maxAllowableDistanceFromHomeBusiness = l.getAirLineDistanceAfterWaypoint(tripId);
				else {
					// is Roundtour, with last trip about to be processed
					if (verbosemode)
						System.out.println("Generating home trip...");
					
					storeTripToDatabase(globalLogbookId, trip.getTripId(), fromGeometry, l.getHomeBusinessGeometryInVirtualWorld(), trip.getPurpose(), trip.getTypeOfDestination(), l.getVehicleType(), l.getHomeBusinessId(), trip.getStartTime(), trip.getStopTime(), trip.getTypeOfSource(), trip.isReversed());
					return true; // trip was assigned successfully
				}
			}

			rs = findLocation(fromGeometry, l.getHomeBusinessId(), trip.getTypeOfDestination(), trip.getAirLineDistance(), variableDiameter, trip.getZFromHome(), l.getHomeBusinessGeometryInVirtualWorld(), maxAllowableDistanceFromHomeBusiness, l.getEconomicSector());		

			if (rs.isBeforeFirst() == false) {
			
				/* Es wurden keine Locations gefunden. Dies kann zwei Gruende haben:
				 * 1. Die Entfernung der Strecke faehrt aus der Modellregion heraus
				 *    -> in diesem Fall soll eine Koordinate synthetisch erzeugt werden
				 * 2. Die Entfernung der Strecke passt in das Modell, jedoch
				 *    wurden keine geeigneten Locations gefunden.
				 *    -> Dann kann die Tour nicht zugewiesen werden.
				 */

				if (!isWithinModelArea(fromGeometry, trip.getAirLineDistance(), variableDiameter)) {
					rs = getCoordinatesFromDistanceAndZ(fromGeometry, trip.getAirLineDistance(), l.getHomeBusinessGeometryInVirtualWorld(), trip.getZFromHome());
				}
			}

			while (rs.next()) {
				// suitable locations found!

				numberOfLocatonsFound++;
				toGeometry = rs.getString("the_geom");

				// wenn das Logbuch weitere Trips enthaelt, diese auch behandeln
				if (l.getNumberOfTrips()-1 > tripId)
					if(assignTrip(globalLogbookId, toGeometry, l, tripId+1))
						// Alle weiteren Trips konnten behandelt werden
						// Success!
						break;
					else
						// Der nachfolgende Trip konnte nicht zugewiesen werden
						toGeometry = "";
				else
					// Keine weiteren Trips uebrig zum berechnen
					// Success!
					break;
			}


			if (!toGeometry.equals("")) {
				storeTripToDatabase(globalLogbookId, trip.getTripId(), fromGeometry, toGeometry, trip.getPurpose(), trip.getTypeOfDestination(), l.getVehicleType(), l.getHomeBusinessId(), trip.getStartTime(), trip.getStopTime(), trip.getTypeOfSource(), trip.isReversed());
				return true; // trip was assigned successfully
			}
			
//		}

		// No suitable location could be found
		return false;
	}


	/**
	 * Assigns a roundtour. By definition, a round tour always starts and stops
	 * at its home business.
	 * @param l
	 * @throws SQLException 
	 */
	
/*	
	private void assignRoundTour(Pwvm_Logbook l) throws SQLException {

//		ResultSet rs;
//		Pwvm_LogbookTrip trip = l.getTrip(0);

		switch (l.getNumberOfTrips()) {
		case 1:
			System.out.println("Roundtour with 1 trip. Not yet implemented.");
			logToDatabaseProtocol(3, "Roundtour with 1 trip. Not yet implemented.");
			break;
/*		case 2:
			
			System.out.println("Roundtour with 2 trips.");
			rs = findLocation(l.getHomeBusinessGeometryInVirtualWorld(), l.getHomeBusinessId(), trip.getTypeOfDestination(), trip.getAirLineDistance(), DIAMETER, -1, l.getHomeBusinessGeometryInVirtualWorld(), -1);
				
			if (rs.next()) {
				// Ein oder mehr Locations kommen in Frage. Daher die erste (ist auch die mit bestem z-Wert) waehlen
				storeTripToDatabase(l.getHomeBusinessGeometryInVirtualWorld(), rs.getString(1), trip.getPurpose(), l.getVehicleType(), l.getHomeBusinessId());
				// Da die Rundtour aus nur 2 Fahrten besteht, im Anschluss die Rueckfahrt in die DB eintragen
				storeTripToDatabase(rs.getString(1), l.getHomeBusinessGeometryInVirtualWorld(), l.getTrip(1).getPurpose(), l.getVehicleType(), l.getHomeBusinessId());
				System.out.println("Successfully assigned.");
			} else {
				/* Es wurden keine Locations gefunden. Dies kann zwei Gruende haben:
				 * 1. Die Entfernung der Strecke fuehrt aus der Modellregion heraus
				 *    -> in diesem Fall soll eine beliebige Koordinate generiert werden
				 * 2. Die Entfernung der Strecke passt in das Modell, jedoch
				 *    wurden keine geeigneten Locations gefunden.
				 *    -> Dann kann die Tour nicht zugewiesen werden.
				 *

				if (!isWithinModelArea(l.getHomeBusinessGeometryInVirtualWorld(), trip.getAirLineDistance(), DIAMETER)) {
					// Entfernung fuehrt aus der Modellregion heraus
					// -> zufaellige Koordinate waehlen
					String toGeometry = getCoordinate(l.getHomeBusinessGeometryInVirtualWorld(), trip.getAirLineDistance(), l.getHomeBusinessGeometryInVirtualWorld(), trip.getZFromHome());
					// dann in DB eintragen
					storeTripToDatabase(l.getHomeBusinessGeometryInVirtualWorld(), toGeometry, trip.getPurpose(), l.getVehicleType(), l.getHomeBusinessId());
					// Da die Rundtour aus nur 2 Fahrten besteht, im Anschluss die Rueckfahrt in die DB eintragen
					storeTripToDatabase(toGeometry, l.getHomeBusinessGeometryInVirtualWorld(), l.getTrip(1).getPurpose(), l.getVehicleType(), l.getHomeBusinessId());

					System.out.println("Roundtour with 2 trips leaves model border. Therefore generating a random coordinate.");
					logToDatabaseProtocol(20, "Roundtour with 2 trips leaves model border. Trip assignment through random coordinate generation.\r\n"+l.toString());
					System.out.println(l.toString());
					System.out.println("Successfully assigned.");
				} else {
					// Es wurden keine passenden Locations gefunden, obwohl
					// die Entfernung nicht aus der Modellregion herausfuehrt.
					System.out.println("Route could not get assigned.");
					logToDatabaseProtocol(5, "Roundtour with 2 trips could not get assigned.\r\n"+l.toString());
					System.out.println(l.toString());
				}		
			}
			break;

		default:
			System.out.println("Roundtour with "+l.getNumberOfTrips()+" trips.");

			if (assignTrip(l.getHomeBusinessGeometryInVirtualWorld(), l, 0))
				System.out.println("Successfully assigned.");
			else {
				System.out.println("Route could not get assigned.");
				System.out.println(l.toString());
				logToDatabaseProtocol(6, "Roundtour with "+l.getNumberOfTrips()+" trips could not get assigned.\r\n"+l.toString());
			}
			break;
		}
	}
*/


	private void assignLocations(Pwvm_Logbook l, int globalLogbookId) throws SQLException {

		if (l.getNumberOfVisitsAtHomeBusiness() == 0) {
			// Logbook does not stop at home business
			System.out.println("Logbook does not visit home business. Not implemented.");
			logToDatabaseProtocol(2, "Logbook does not visit home business. Not implemented.\r\n"+l.toString(), l.getLogbookIdInDB());
			// TODO evtl. l.setSourceGeometry(XXX) setzen und dann aufrufen: assignTrip(l.getSourceGeometry, l, 0)
		} else {
			// Logbook visits one or more times home business

			// Split logbook into parts (devide at home business)
			Pwvm_LogbookCollection lc = new Pwvm_LogbookCollection(l);
			
			boolean logbookCompletelyAssigned = true;

			// Now process each part separately
			for (Iterator<Pwvm_Logbook> iterator = lc.getLogbookIterator(); iterator.hasNext();) {
				Pwvm_Logbook part = iterator.next();

				if ((part.getTypeOfSource() == 4) && (part.getTypeOfLastStop() == 4)) {
					// Round tour; Thus start and stop is home business

					System.out.println("Roundtour with "+part.getNumberOfTrips()+" trips.");

//					if (l.getNumberOfTrips() == 1) {
//						System.out.println("Not yet implemented.");
//						logToDatabaseProtocol(3, "Roundtour with 1 trip. Not yet implemented.");
//					} else { // valid roundtour with more than one trip

					if (assignTrip(globalLogbookId, part.getHomeBusinessGeometryInVirtualWorld(), part, 0)) {
						System.out.println("Successfully assigned.");
					} else {
						System.out.println("Route could not get assigned.");
						System.out.println(l.toString() + "\r\n --> thereof "+part.toString());
						logToDatabaseProtocol(6, "Roundtour with "+part.getNumberOfTrips()+" trips could not get assigned.\r\n"+l.toString(), l.getLogbookIdInDB());
						logbookCompletelyAssigned = false;
					}
//					}

				} else {
					// No round tour; either start or stop is home business

					if (part.getTypeOfSource() != 4) {
						// source is not home business, thus must be at end instead
						// therefore reverse logbook
						if (verbosemode)
							System.out.println("Reversing logbook...");
						part.reverse();
					}

					if (assignTrip(globalLogbookId, part.getHomeBusinessGeometryInVirtualWorld(), part, 0))
						System.out.println("Successfully assigned.");
					else {
						System.out.println("Route could not get assigned.");
						System.out.println(part.toString());
						logToDatabaseProtocol(7, "Route could not get assigned (non-round).\n"+l.toString() + "\r\n --> thereof "+part.toString(), l.getLogbookIdInDB());
						logbookCompletelyAssigned = false;
					}

				}

			}
			
			if (logbookCompletelyAssigned)
				logToDatabaseProtocol(999, "OK Logbook assigned.", l.getLogbookIdInDB());
			else
				logToDatabaseProtocol(998, "ERR could not get assigned.", l.getLogbookIdInDB());
		}
	}

	public void start() {
		try {
			
			Statement st = con.createStatement();
			st.executeUpdate("DELETE FROM pwvm_logmessages");
			st.executeUpdate("DELETE FROM pwvm_matrix");
			st.executeUpdate("DELETE FROM searchradius");
			
			Pwvm_RAMRepository lrep = new Pwvm_RAMRepository();
	
			String stmt = 
			"SELECT b.id, " +
			"b.wz_1steller, " +
			"\"pwvm_getHeadcountClass\"(b.headcount),  " +
			"hc.the_geom, " +
			"a.aufkommen " +
			"FROM pwvm_business b " +
			"LEFT JOIN gis_housecoordinate hc ON (b.house_coordinate = hc.s2) " +
			"LEFT JOIN pwvm_aufkommenstabelle a ON (\"pwvm_getHeadcountClass\"(b.headcount) = a.headcountclass AND b.wz_1steller = a.economic_sector) " +
			"WHERE wz_1steller IS NOT NULL " +
			"AND b.headcount IS NOT NULL " +
			"AND b.headcount != 0 " +
			"AND a.aufkommen IS NOT NULL " +
			"AND hc.the_geom IS NOT NULL " +
			"AND ST_Covers((SELECT the_geom FROM pwvm_borderdefinition WHERE id=10), hc.the_geom) " + // nur businesses aus dem Modell Bereich
			"";
			
	//		stmt += "AND b.id = 3432760";  // DEBUG: just single business 
	//		stmt += "AND b.id = 3464572";
	//		stmt += "AND b.id = 3508949";
			
			
			
			stmt += " ORDER BY RANDOM() ";
			
			ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM ( " +

					"SELECT b.id " +
					"FROM pwvm_business b " + 
					"LEFT JOIN gis_housecoordinate hc ON (b.house_coordinate = hc.s2) " +
					"LEFT JOIN pwvm_aufkommenstabelle a ON (\"pwvm_getHeadcountClass\"(b.headcount) = a.headcountclass AND b.wz_1steller = a.economic_sector)	" +		
					"WHERE wz_1steller IS NOT NULL " +
					"AND b.headcount IS NOT NULL " +
					"AND b.headcount != 0 " +
					"AND a.aufkommen IS NOT NULL " +
					"AND hc.the_geom IS NOT NULL " +
					"AND ST_Covers((SELECT the_geom FROM pwvm_borderdefinition WHERE id=10), hc.the_geom) " +
					") tmp");
			
			rs.next();
			int totalNumberOfBusinesses = rs.getInt(1);
			rs.close();
			
			if (randomSampleRatio < 1.0) {
				totalNumberOfBusinesses = (int) (totalNumberOfBusinesses*randomSampleRatio);
				stmt += " LIMIT "+totalNumberOfBusinesses;
			}
			
			// id: business id
			// wz_1steller: WZ-Abteilung (A, B, C, ...)
			// getHetcountClass(headcount): MA-Klasse
			// the_geom: Koordinate des Betriebs

			if (verbosemode)
				System.out.println(stmt);
			
			ResultSet business = st.executeQuery(stmt);
			
			int business_count = 0;
			int globalLogbookCount = 0;

			// go through all businesses
			while (business.next()) {
				
				business_count++;

				System.out.println("");
				int business_id = business.getInt(1);
				String business_wz = business.getString(2);
				int business_headcountClass = business.getInt(3);
				String business_geometry = business.getString(4);
				int numberOfMobileVehicles = (int) (business.getDouble(5) + 0.5); // Rounding doubles greater zero to int

				// EXPERIMENT
				//numberOfMobileVehicles = 1;
				
				
				
				System.out.println("Processing business "+business_count+" of "+totalNumberOfBusinesses+"...");
				System.out.println("\nDetails of the business about to be processed: ");
				System.out.println("Business Id: "+business_id);
				System.out.println("Headcount Class: "+business_headcountClass);
				System.out.println("Economic Sector: "+business_wz);
				System.out.println("Number of mobile vehicles: "+numberOfMobileVehicles);
				
				System.out.println("Number of mobile vehicles (as double): "+business.getString(5));
				
				logToDatabaseProtocol(4, "Processing business "+business_id+" ("+numberOfMobileVehicles+" vehicle(s))");


				// Assign a logbook to each vehicle
				for (int i = 1; i <= numberOfMobileVehicles; i++) {
					
					globalLogbookCount++;

					System.out.println("Vehicle no. "+i+"/"+numberOfMobileVehicles+":");

					// select a logbook based on economic sector and headcount class
					int logbookId = chooseRandomLogbook(business_wz, business_headcountClass);
					
					// EXPERIMENT
					//logbookId = 2362412;
//					logbookId = 1503107;	// Logbook das lange rekursiert mit business id 3464572
		//			logbookId = 2261130;	
//					dauert auch ewig: "Processing business 3456623 with logbook 2261130 for vehicle 1/1"
//					logbookId = 2174159;
					

					System.out.println("Logbook chosen: "+logbookId);
					
					logToDatabaseProtocol(8, "Processing business "+business_id+" with logbook "+logbookId+" for vehicle "+i+"/"+numberOfMobileVehicles, logbookId);


					if (logbookId != -1) {
						
						Pwvm_Logbook l = new Pwvm_Logbook();
						
						Pwvm_Logbook lfromrep = lrep.getLogbook(logbookId);
						
						if (lfromrep != null)
							l = new Pwvm_Logbook(lfromrep);
						else {
							// Retrieve template logbook from database and store in cache
							l = loadLogbookFromDatabase(logbookId);
							l.setLogbookIdInDB(logbookId);
							lrep.addLogbook(l);
						}
						
						
				//		Pwvm_Logbook l = lrep.getLogbook(logbookId);					
//						if (l == null) {
//							l = loadLogbookFromDatabase(logbookId);
//							l.setLogbookIdInDB(logbookId);
//							lrep.addLogbook(l);
//						}
						
						l.setHomeBusinessId(business_id);
						l.setEconomicSector(business_wz);
						l.setCompanySize(business_headcountClass);
						l.setHomeBusinessGeometryInVirtualWorld(business_geometry);

						if (verbosemode)
							System.out.println("Details of logbook chosen:\n"+l.toString());

						assignLocations(l, globalLogbookCount);
					} else {
						// no appropriate logbook found in repository
						logToDatabaseProtocol(1, "Repository contains no logbooks for economic sector "+business_wz+" and headcount class "+business_headcountClass+".");
					}

				}
			}

			System.out.println("DONE.");

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}