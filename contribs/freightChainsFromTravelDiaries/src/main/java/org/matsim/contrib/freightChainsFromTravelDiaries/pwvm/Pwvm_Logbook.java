/**
 * 
 */
package org.matsim.contrib.freightChainsFromTravelDiaries.pwvm;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Vector;

/**
 * @author schn_se
 * Stores a logbook from KiD, consists of one or more trips
 */
/**
 * @author schn_se
 *
 */
public class Pwvm_Logbook {

	private int logbookIdInDB;	// this logbook's id in the database repository, or "-1" if not exists
	private int homeBusinessId;
	private String homeBusinessGeometryInVirtualWorld; // POINT geometry where the business is located in the synthetic world
	private String homeBusinessGeometryInLogbook;  // POINT geometry from the original logbook (as reported in KiD)
	private int vehicleType; // Fahrzeugart
	private int sourceType;
	private String sourceGeometry; // The logbook's starting point, not relevant if home business
	private Vector<Pwvm_LogbookTrip> trips = new Vector<Pwvm_LogbookTrip>();
	private String economicSector;
	private int companySize; // number of employees

	public Pwvm_Logbook() {

	}

	public Pwvm_Logbook(Pwvm_Logbook l) {
		
		this.logbookIdInDB = l.logbookIdInDB;
		this.homeBusinessId = l.homeBusinessId;
		this.homeBusinessGeometryInVirtualWorld = l.homeBusinessGeometryInVirtualWorld;
		this.homeBusinessGeometryInLogbook = l.homeBusinessGeometryInLogbook;
		this.vehicleType = l.vehicleType;
		this.sourceType = l.sourceType;
		this.sourceGeometry = l.sourceGeometry;
		this.trips = l.trips;
		this.economicSector = l.economicSector;
		this.companySize = l.companySize;
	}
	

	public Pwvm_Logbook(int sourceType, int vehicleType, String homeBusinessGeometryInLogbook, int homeBusinessId, String homeBusinessGeometryInVirtualWorld) {
		this.logbookIdInDB = -1;
		this.sourceType = sourceType;
		this.homeBusinessGeometryInLogbook = homeBusinessGeometryInLogbook;
		this.homeBusinessId = homeBusinessId;
		this.homeBusinessGeometryInVirtualWorld = homeBusinessGeometryInVirtualWorld;
		this.vehicleType = vehicleType;
	}

	public Pwvm_Logbook(int source_type, String sourceGeometry, int vehicleType) {
		this.logbookIdInDB = -1;
		this.sourceType = source_type;
		this.sourceGeometry = sourceGeometry;
		this.vehicleType = vehicleType;
	}

	public void addTrip(int tripId, int source_type, int dest_type, int purpose, double z_source, double z_home, double distance, double distance_empirical, String destGeometry, String start_time, String stop_time) {
		Pwvm_LogbookTrip t = new Pwvm_LogbookTrip(tripId, source_type, dest_type, purpose, z_source, z_home, distance, distance_empirical, destGeometry, start_time, stop_time);
		trips.add(t);
		if (dest_type == 4) // home business
			this.homeBusinessGeometryInLogbook = destGeometry;
	}

	public void addTrip(Pwvm_LogbookTrip t) {
		trips.add(t);
		if (t.getTypeOfDestination() == 4) // home business
			this.homeBusinessGeometryInLogbook = t.getDestGeometry();
	}
	
	public int getVehicleType() {
		return vehicleType;
	}

	public void setVehicleType(int vehicleType) {
		this.vehicleType = vehicleType;
	}

	public int getHomeBusinessId() {
		return homeBusinessId;
	}

	public void setHomeBusinessId(int home_business_id) {
		this.homeBusinessId = home_business_id;
	}

	public int getTypeOfLastStop() {
		Pwvm_LogbookTrip t = trips.lastElement();		
		return t.getTypeOfDestination();
	}

	public String getHomeBusinessGeometryInLogbook() {
		return homeBusinessGeometryInLogbook;
	}

	public void setHomeBusinessGeometryInLogbook(
			String homeBusinessGeometryInLogbook) {
		this.homeBusinessGeometryInLogbook = homeBusinessGeometryInLogbook;
	}

	public String getSourceGeometry() {
		return sourceGeometry;
	}

	public void setSourceGeometry(String sourceGeometry) {
		this.sourceGeometry = sourceGeometry;
	}


	/**
	 * Sets the geometry to where the business is located
	 * in the synthetic world.
	 * @param geometry a POINT geometry
	 */
	public void setHomeBusinessGeometryInVirtualWorld(String geometry) {
		homeBusinessGeometryInVirtualWorld = geometry;
	}

	/**
	 * Returns the home business's geometry
	 * in the synthetic world.
	 * 
	 * Note that this is not the one from KiD.
	 * Use getHomeBusinessGeometryInKID instead.
	 * 
	 * @return location in space as POINT
	 */
	public String getHomeBusinessGeometryInVirtualWorld() {
		return homeBusinessGeometryInVirtualWorld;
	}

	/**
	 * Returns an integer specifying the type of the logbook's
	 * first location (source) in line with the definition as
	 * used in KiD.
	 * 
	 * -9: "not set"
	 * -1: "not set"
	 * 1: "Umschlagpunkt (Bahnhof, Hafen, GVZ, etc.)"
	 * 2: "Spedition"
	 * 3: "Baustelle"
	 * 4: "Eigener Betrieb"
	 * 5: "Fremder Betrieb"
	 * 6: "Kundenhaushalt"
	 * 7: "sonstiges dienstlich/geschaeftliches Ziel"
	 * 8: "privates Ziel"
	 * 9: "Filiale (Fremdbetrieb mit gleichem WZ)"
	 * 
	 * @return the source's type
	 */
	public int getTypeOfSource() {
		return sourceType;
	}

	/**
	 * Like getTypeOfSource, but returns a human readable string 
	 * @return the source's type as a string
	 */
	public String getTypeOfSourceHumanReadable() {

		String s = "";
		switch (sourceType) {
		case -9: s = "not set"; break;
		case -1: s = "not set"; break;
		case 1: s = "Umschlagpunkt (Bahnhof, Hafen, GVZ, etc.)"; break;
		case 2: s = "Spedition"; break;
		case 3: s = "Baustelle"; break;
		case 4: s = "Eigener Betrieb"; break;
		case 5: s = "Fremder Betrieb"; break;
		case 6: s = "Kundenhaushalt"; break;
		case 7: s = "sonstiges dienstlich/geschaeftliches Ziel"; break;
		case 8: s = "privates Ziel"; break;
		case 9: s = "Filiale (Fremdbetrieb mit gleichem WZ)"; break;
		default: System.out.println("ERROR in getTypeOfSourceHumanReadable."); System.exit(-1); 
		}

		return s;
	}

	public void setSourceType(int source_type) {
		this.sourceType = source_type;
	}

	/**
	 * @return returns how many times the logbook stops at this logbook's home business.
	 */
	public int getNumberOfVisitsAtHomeBusiness() {

		int n = 0;

		// Does it start at the home business?
		if (this.getTypeOfSource() == 4)
			n++;

		// Go through all stops and check if they are of type home business (4)
		Pwvm_LogbookTrip t;
		ListIterator<Pwvm_LogbookTrip> itr = trips.listIterator();
		while(itr.hasNext()) {
			t = itr.next();
			if (t.getTypeOfDestination() == 4)
				n++;
		}

		return n;
	}

	/**
	 * returns how many customers are served in the logbook (based
	 * on the attribute purpose from KiD. A customer is regarded as
	 * visited if a trip's purpose is 1, 2, 3, or 4.
	 * 
	 * -1: "not set"
	 *  1: "Holen, Bringen, Transportieren von Guetern, Waren, Material, Maschinen, Geraeten, etc."
	 *  2: "Fahrt zur Erbringung beruflicher Leistungen (Montage, Reparatur, Beratung, Besuch, Betreuung, etc.)"
	 *  3: "Holen, Bringen, Befoerdern von Personen (dienstlich/geschaeftlich)"
	 *  4: "sonstige dienstlich/geschaeftliche Erledigung"
	 *  5: "Rueckfahrt zum Betrieb/Stellplatz"
	 *  6: "Fahrt zum Arbeitsplatz"
	 *  7: "Fahrt zur Ausbildung"
	 *  8: "Privater Einkauf"
	 *  9: "Freizeit, Erholung"
	 * 10: "Holen, Bringen, Befoerdern von Personen (privat)"
	 * 11: "sonstige private Erledigung"
	 * 12: "Fahrt nach Hause"
	 * 21: "dienstlich/geschaeftliche Erledigung"
	 * 22: "private Erledigung"
	 * 
	 * @return the number of customers served
	 */
//	public int getNumberOfCustomersVisited() {
//
//		int n = 0;
//
//		// Go through all trips and check if their purpose is 1, 2, 3 or 4
//		ListIterator<Pwvm_LogbookTrip> itr = trips.listIterator();
//		while(itr.hasNext()) {
//			Pwvm_LogbookTrip t = itr.next();
//			switch (t.getPurpose()) {
//			case 1: n++; break;
//			case 2: n++; break;
//			case 3: n++; break;
//			case 4: n++; break;
//			}
//		}
//
//		return n;
//	}

	/**
	 * Number of trips of the logbook
	 * @return number of trips
	 */
	public int getNumberOfTrips() {
		return trips.size();	
	}

	/**
	 * the total air-line distance of all of the logbook's
	 * trips. The unit (meters, yards, ...) is chosen based
	 * on the underlaying coordinate reference system.
	 * If GK3, the distance is returned in meters. 
	 * @return total air-line distance
	 */
	public double getTotalAirLineDistance() {

		double d = 0;

		// Go through all trips and add up distances
		ListIterator<Pwvm_LogbookTrip> itr = trips.listIterator();
		while(itr.hasNext()) {
			Pwvm_LogbookTrip t = itr.next();
			d += t.getAirLineDistance();

		}

		return d;
	}

	/**
	 * the total air-line distance of all of the logbook's
	 * trips after the trip indexOfTrip. The unit (meters,
	 * yards, ...) is chosen based on the underlaying
	 * coordinate reference system.
	 * If GK3, the distance is returned in meters. 
	 * @param indexOfTrip The trip after which distances
	 * are summed up. The first trip is 0.
	 * @return total air-line distance after a given trip
	 */
	public double getAirLineDistanceAfterWaypoint(int indexOfTrip) {

		double d = 0;
		int i = 0;

		// Go through all trips and add up distances
		ListIterator<Pwvm_LogbookTrip> itr = trips.listIterator();
		while(itr.hasNext()) {
			Pwvm_LogbookTrip t = itr.next();

			if (i >= indexOfTrip) {
				d += t.getAirLineDistance();
			}
			i++;
		}

		return d;
	}	

	/**
	 * the total distance of all of the logbook's trips as
	 * stated by the participants of KiD (not air-line distance,
	 * but the one of the survey). The unit (meters, yards, ...)
	 * is chosen based on the underlaying coordinate reference
	 * system. If GK3, the distance is returned in meters. 
	 * @return total empirical distance from KiD
	 */
	public double getTotalEmpiricalDistance() {

		double d = 0;

		// Go through all trips and add up distances
		ListIterator<Pwvm_LogbookTrip> itr = trips.listIterator();
		while(itr.hasNext()) {
			Pwvm_LogbookTrip t = itr.next();
			d += t.getEmpiricalDistance();

		}

		return d;
	}

	/**
	 * Clears the logbook by removing all trips. When done, the
	 * logbook will contain zero trips.
	 * The home business and type of source setting will remain
	 * unchanged.
	 */
	public void clear() {
		trips.clear();
	}

	/**
	 * Reverses trip order. Last trip will become first,
	 * first becomes last, and so on.
	 * 
	 * When invoking this method, Z_source and Z_home are
	 * set to -1.
	 * 
	 * They must be recalculated using the database.
	 * @throws SQLException 
	 * 
	 */
	public void reverse() throws SQLException {
		trips.trimToSize();

		Vector<Pwvm_LogbookTrip> tmp = new Vector<Pwvm_LogbookTrip>();
		tmp.setSize(trips.size());

		int i = tmp.size()-1;
//		int newDestType = this.getTypeOfSource();
		String newDestGeometry = this.getSourceGeometry();

		Connection con = Pwvm_DatabaseConnection.dbconnect();
		Statement st = con.createStatement();
		String stmt = "";

		for (Iterator<Pwvm_LogbookTrip> iterator = trips.iterator(); iterator.hasNext();) {
			Pwvm_LogbookTrip t = new Pwvm_LogbookTrip(iterator.next());

			// Locationtypes bei von und nach vertauschen.
			int tmpType = t.getTypeOfDestination();
			t.setTypeOfDestination(t.getTypeOfSource());
			t.setTypeOfSource(tmpType);
			
			
			//t.setTypeOfDestination(newDestType);
			//newDestType = tmpDestType;
			//newDestType = t.getTypeOfDestination();
			
//			tmpType = t.getTypeOfSource();
//			t.setTypeOfSource(t.getTypeOfDestination());
//			t.setTypeOfDestination(tmpType);

			// auch geometry Angabe vertauschen
			String tmpDestGeometry = t.getDestGeometry();
			t.setDestGeometry(newDestGeometry);
			newDestGeometry = tmpDestGeometry;

			// Z auf ungueltig setzen
//			t.setZFromSource(-1);
			t.setZFromHome(-1);

			// Use GIS database to calculate new ZFromHome value
			if ((this.getHomeBusinessGeometryInLogbook() != null) && (t.getDestGeometry() != null)) {
				stmt = "SELECT ST_Distance('"+this.getHomeBusinessGeometryInLogbook()+"', '" +
				t.getDestGeometry()+"')";
				//System.out.println(stmt);
				ResultSet rs = st.executeQuery(stmt);
				rs.next();
				t.setZFromHome(rs.getDouble(1));
			}
			
			t.setReversed(true);

			tmp.setElementAt(t, i);

			i--;
		}
		con.close();
		//this.setSourceType(newDestType);
		this.setSourceType(tmp.firstElement().getTypeOfSource());
		this.setSourceGeometry(newDestGeometry);
		trips = tmp;

	}

	public Iterator<Pwvm_LogbookTrip> getTripsIterator() {
		return trips.iterator();
	}

	/**
	 * Returns an object of type Pwvm_LogbookTrip. id specifies
	 * which trip
	 * @param id the trip ID
	 * @return trip
	 */
	public Pwvm_LogbookTrip getTrip(int id) {
		return trips.elementAt(id);
	}

	public String toString() {

		String str = "Attributes:\n ";
		str += "Economic sector:"+this.getEconomicSector()+ "\n " +
		"Business size (number of employees):"+this.getCompanySize()+ "\n " +
		"Total airline distance: "+this.getTotalAirLineDistance() + "\n " +
		"Total empirical distance: "+this.getTotalEmpiricalDistance() + "\n " +
		"Total number of trips: "+this.getNumberOfTrips() + "\n " +
		"Total number of visits at home site: "+this.getNumberOfVisitsAtHomeBusiness() + "\n " +
		"Type of source: "+this.getTypeOfSource() + " (" +this.getTypeOfSourceHumanReadable()+ ")\n " +
		"Type of last stop: "+this.getTypeOfLastStop() + "\n " +
		"Home business ID: "+this.getHomeBusinessId() + "\n " +
		"Vehicle Type: "+this.getVehicleType() + "\n "+
		"Geometry to home business [in model]: "+this.getHomeBusinessGeometryInVirtualWorld() + "\n " +
		"Geometry to home business [in KiD]: "+this.getHomeBusinessGeometryInLogbook();

		str += "\n\nTrips: (starting from type: "+this.getTypeOfSource() +")";

		str += "\n nr, source_type, dest_type, purpose, airline_distance, distance_empirical, z_home, start_time, stop_time";

		int i = 0;
		for (Iterator<Pwvm_LogbookTrip> iterator = trips.iterator(); iterator.hasNext();) {
			Pwvm_LogbookTrip t = iterator.next();
			i++;
			str += "\n "+i+". \t| " +
			t.getTypeOfSource() + " \t| " +
			t.getTypeOfDestination() + " \t| " +
			t.getPurpose() + " \t| " +
			t.getAirLineDistance() + " \t| " +
			t.getEmpiricalDistance() + " \t| " +
			t.getZFromHome() + " \t| " +
//			t.getZFromSource() + " \t| " +
			t.getStartTime() + " \t| " +
			t.getStopTime();

		}		
		return str;
	}
	
	public void setCompanySize(int size) {
		this.companySize = size;
	}

	public int getCompanySize() {
		return companySize;
	}

	public void setEconomicSector(String business_wz) {
		this.economicSector = business_wz;
		
	}
	
	public String getEconomicSector() {
		return economicSector;
	}

	public int getLogbookIdInDB() {
		return logbookIdInDB;
	}

	public void setLogbookIdInDB(int logbookIdInDB) {
		this.logbookIdInDB = logbookIdInDB;
	}

}