package org.matsim.contrib.freightChainsFromTravelDiaries.pwvm;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;


public class Pwvm_LogbookRepositoryGenerator {
	
	Connection con1;
	Connection con2;
	Vector<String[]> v = new Vector();
	double sum_km;
	double sum_z;
	private boolean verbosemode;
	int problemfaelle = 0;
	
	public Pwvm_LogbookRepositoryGenerator() {
		con2 = Pwvm_DatabaseConnection.dbconnect();
		try {
			con2.setAutoCommit(false);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}
		verbosemode = false;
	}
	
	public void setVerbosemode(boolean verbosemode) {
		this.verbosemode = verbosemode;
	}
	
	/**
	 * Creates a new row in table pwvm_logbook
	 * @param f03 type of source (Art der Quelle)
	 * @param fahrzeugid originally K00
	 * @param f02c zipcode of source
	 * @param f02a KGS of source
	 * @param h01 WZ laut Befragtem (oder P bei Privatfahrzeug)
	 * @param h05 Anzahl Mitarbeiter oder Haushaltsmitglieder (bei h01=P)
	 * @return true if no error occured, else false
	 * @throws SQLException 
	 */
	private void createNewLogbookEntry(String logbookid, String f03, String fahrzeugid, String f02c, String f02a, String f02d, String f02e, String h01, String h05) throws SQLException {
			
		Statement st = con2.createStatement();

		String stmt = "INSERT INTO pwvm_logbook " +
				"(id, " +
				"source_type, " +
				"\"kid_vehicleId\", " +
				"source_plz, " +
				"source_kgs, " +
				"the_geom, " +
				"business_wz, " +
				"business_headcountclass) " +
				"VALUES (" +
			logbookid+", "+
			Pwvm_DatabaseConnection.quote(f03)+", " +
			fahrzeugid+", " +
			Pwvm_DatabaseConnection.quote(f02c)+", " +
			Pwvm_DatabaseConnection.quote(f02a)+", " +
			"CASE WHEN ('"+f02d+"'='-1' OR '"+f02e+"'='-1' OR '"+f02d+"'='-9' OR '"+f02e+"'='-9') " +
					"THEN " +
						"CASE WHEN '"+f02c+"' IN (SELECT plz FROM plz) THEN (SELECT ST_Centroid(the_geom) FROM plz WHERE plz='"+f02c+"') ELSE null END" +
					" ELSE " +
					"ST_Transform(ST_PointFromText('POINT("+f02d+" "+f02e+")',4326),31467) " + // WKS84 to GK3 transformation
					"END, " + 
			Pwvm_DatabaseConnection.quote(h01)+", " +
			Pwvm_DatabaseConnection.quote(h05)+")";
		
		System.out.println("---");
		System.out.println(stmt);
		st.executeUpdate(stmt);
	
	}
	
	/**
	 * Creates a new row in table pwvm_logbook_trip
	 * @param startlocation_plz zipcode where the tour starts
	 * @param fahrtid the Fahrtid from KiD (f00)
	 * @param f09 type of destination (Art des Ziels)
	 * @param f08a kgs of destination
	 * @param f08c zipcode of destination
	 * @param f08d X coordinate of destination
	 * @param f08e Y coordinate of destination
	 * @param f07a purpose (Fahrtzweck)
	 * @param f14 distance (Fahrtweite)
	 * @param persowive_relevant was any of the previous trips for this vehicle relevant for persowive?
	 * @return true if no error occured, else false
	 * @throws SQLException 
	 */
	private void createNewLogbookTripEntry(String logbookid, String fahrtid, String f09, String f08a, String f08c, String f08d, String f08e, String f07a, String f14, boolean persowive_relevant) throws SQLException {
		
		// Fahrten mit fahrtid >= 12 aufloesen (mit Durchschnittswerten)
		// Aber nur dann, wenn mindestens eine der ersten 11 Fahrten dem PersoWiVe zugeordnet waren
		
		if (Integer.valueOf(fahrtid) >= 12 && persowive_relevant) {
			problemfaelle++;
			
//			f08d = v.get(v.size())[1];
//			f14 = Double.valueOf(sum_km/11).toString();
			
		}	
		
		Statement st = con2.createStatement();
		
		String stmt = "INSERT INTO pwvm_logbook_trip (" +
				"\"logbookId\", " +
				"\"tripId\", " +
				"dest_type, " +
				"dest_plz, " +
				"purpose, " +
				"distance, " +
				"dest_kgs, " +
				"the_geom " +
//				"z " +				
				") VALUES (" +
				logbookid+", " +
				fahrtid+", " +
				Pwvm_DatabaseConnection.quote(f09)+", " +
				Pwvm_DatabaseConnection.quote(f08c)+", " +
				Pwvm_DatabaseConnection.quote(f07a)+", " +
				Pwvm_DatabaseConnection.quote(f14)+", " +
				Pwvm_DatabaseConnection.quote(f08a)+", " +
				"CASE WHEN ('"+f08d+"'='-1' OR '"+f08e+"'='-1' OR '"+f08d+"'='-9' OR '"+f08e+"'='-9') THEN " +
						"CASE WHEN '"+f08c+"' IN (SELECT plz FROM plz) THEN (SELECT ST_Centroid(the_geom) FROM plz WHERE plz='"+f08c+"') ELSE null END " +
					"ELSE ST_Transform(ST_PointFromText('POINT("+f08d+" "+f08e+")',4326),31467) END " +
				")";  // WKS84 to GK3 transformation
			
		if (this.verbosemode)
			System.out.println(stmt);
		st.executeUpdate(stmt);
		
		stmt = "UPDATE pwvm_logbook_trip " +
				"SET z = CASE WHEN (SELECT the_geom FROM pwvm_logbook_trip WHERE \"logbookId\"='"+logbookid+"' AND \"tripId\"='"+fahrtid+"') IS NOT NULL " +
				"AND (SELECT the_geom FROM pwvm_logbook WHERE id='"+logbookid+"') IS NOT NULL " +
					"THEN ST_distance((SELECT the_geom FROM pwvm_logbook WHERE id='"+logbookid+"'), (SELECT the_geom FROM pwvm_logbook_trip WHERE \"logbookId\"='"+logbookid+"' AND \"tripId\"='"+fahrtid+"')) " +
					"ELSE null END " +
				"WHERE \"logbookId\"='"+logbookid+"' AND \"tripId\"='"+fahrtid+"'";
		
		if (this.verbosemode)
			System.out.println(stmt);
		
		st.executeUpdate(stmt);
		
		if (Integer.valueOf(fahrtid) >= 12 && persowive_relevant) {
			stmt = "SELECT z FROM pwvm_logbook_trip WHERE \"logbookId\"='"+logbookid+"' AND \"tripId\"='"+fahrtid+"'";

			if (this.verbosemode)
				System.out.println(stmt);

			st.executeQuery(stmt);
			ResultSet rs = st.getResultSet();
			rs.next();
			// purpose, dest_type, distance, z
			String[] s = {f07a,f09,f14,rs.getString(1)};
			v.add(s);
			sum_km += Double.valueOf(f14);
			if (rs.getString(1) != null)	// might be null
				sum_z += Double.valueOf(rs.getString(1));
		}
	}

	public void generate() {

		int logbookId = 0;
		boolean persowive_relevant = false;	// wird true wenn mind. 1 Fahrt PersoWiVe ist
		Connection con1 = Pwvm_DatabaseConnection.dbconnect();

		try {

			// make sure autocommit is off
			con1.setAutoCommit(false);
			Statement st = con1.createStatement();

			// Turn use of the cursor on.
			st.setFetchSize(1000);

			String stmt = "SELECT " +
					"kid_fahrt.*, " +
					"kid_fahrzeug.h01, " +
					"kid_fahrzeug.h05, " +
					"(CASE WHEN kid_fahrzeug.h01='P' THEN null ELSE \"pwvm_Theadcountclass\".id END) AS headcountclass " +
					"FROM kid_fahrt " +
					"LEFT JOIN kid_fahrzeug ON (kid_fahrt.fahrzeugid = kid_fahrzeug.fahrzeugid) " +
					"LEFT JOIN \"pwvm_Theadcountclass\" " +
					"ON (kid_fahrzeug.h05 >= \"pwvm_Theadcountclass\".min " +
					"AND kid_fahrzeug.h05 <= \"pwvm_Theadcountclass\".max) " +
					"ORDER BY kid_fahrt.fahrzeugid, kid_fahrt.fahrtid";
			
			System.out.println(stmt);
			ResultSet rs = st.executeQuery(stmt);

			int kid_fahrtid = 2; // causes a logbook to be created when beginning

			// jede zeile nach der Reihe laden
			while (rs.next()) {

				if (rs.getInt("fahrtid") <= kid_fahrtid) {
					// Neues Fahrtenbuch anlegen

					logbookId++;
					kid_fahrtid = rs.getInt("fahrtid");
					sum_km = 0;
					sum_km = 0;
					if (!persowive_relevant)
						con2.rollback();
					else
						con2.commit();
					persowive_relevant = false;
					v.clear();	

					// Startlocation eintragen
					createNewLogbookEntry(Integer.toString(logbookId), rs.getString("f03"), rs.getString("fahrzeugid"), rs.getString("f02c"), rs.getString("f02a"), rs.getString("f02d"), rs.getString("f02e"), rs.getString("h01"), rs.getString("headcountclass") );
					
				}

				kid_fahrtid = rs.getInt("fahrtid");

				createNewLogbookTripEntry(Integer.toString(logbookId), rs.getString("fahrtid"), rs.getString("f09"), rs.getString("f08a"), rs.getString("f08c"), rs.getString("f08d"), rs.getString("f08e"), rs.getString("f07a"), rs.getString("f14"), persowive_relevant);			
							
				if ((rs.getInt("f07a") == 2) | (rs.getInt("f07a") == 3))
					persowive_relevant = true;

			}
			
			if (!persowive_relevant)
				con2.rollback();
			else
				con2.commit();

		} catch (Exception e){
			e.printStackTrace();
			System.exit(-1);
		}
		
		System.out.println("Personenwirtschaftsverkehr-relevante Faelle mit mehr als 11 Fahrten: "+problemfaelle);

	}

}