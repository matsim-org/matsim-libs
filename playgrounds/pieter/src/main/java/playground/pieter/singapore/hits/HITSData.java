package playground.pieter.singapore.hits;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class HITSData implements Serializable{
	private static final long serialVersionUID = 1L;
	private ArrayList<HITSHousehold> households;
	private ArrayList<HITSPerson> persons;
	private ArrayList<HITSTrip> trips;	
	private ArrayList<HITSStage> stages;
	private ArrayList<String> householdIds;
	private ArrayList<String> personIds;
	private final boolean limited;
	private final DateFormat dfm = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


	public HITSData(Connection sqlConn, boolean limitedRows) {
		this.limited = limitedRows;
		setHouseholds(sqlConn, this.dfm);
		setHouseholdIds();
		setPersonIds();
	}

	void setHouseholds(Connection conn, DateFormat dfm) {
		
		try {
			Statement hs = conn.createStatement ();
			hs.executeQuery ("select distinct " +
					"h1_hhid,"+
					"h1_pcode,"+
					"h2_dwell,"+
					"h3_ethnic,"+
					"h3a_other,"+
					"h4_totpax,"+
					"h4c_eligible,"+
					"h4d_nereason,"+
					"h5_vehavail,"+
					"xqty_carncom,"+
					"xqty_carnind,"+
					"xqty_carnrent,"+
					"xqty_caropcom,"+
					"xqty_caropind,"+
					"xqty_caroprent,"+
					"xqty_buscoach,"+
					"xqty_hgv,"+
					"xqty_lgvgoods,"+
					"xqty_lgvpsgr,"+
					"xqty_minibus,"+
					"xqty_motocom,"+
					"xqty_motoind,"+
					"xqty_taxi,"+
					"h8_bikeqty "+
					"from d_hits.hitsshort" +
					(this.limited ? " limit 300" : "") +
					";");
			ResultSet hrs = hs.getResultSet ();
			int count = 0;
			ArrayList<HITSHousehold> households = new ArrayList<>();
			while (hrs.next())
			{
				if(count % 1000 == 0)  System.out.println(count);
				households.add(new HITSHousehold(hrs, conn, dfm));
				count++ ;
			}
			hrs.close ();
			hs.close ();
			System.out.println (count + " households were processed");
			households.trimToSize();
			this.households = households;
		} catch (SQLException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		getPersonsFromHouseholds();
		getTripsFromHouseholds();
		getStagesFromHouseholds();
		
	}

	private void setHouseholdIds() {
		this.householdIds = new ArrayList<>();
		for(HITSHousehold hh:this.households){
			this.householdIds.add(hh.h1_hhid);
		}
		this.householdIds.trimToSize();
	}
	
	private void setPersonIds() {
		this.personIds = new ArrayList<>();
		for(HITSPerson hh:this.persons){
			this.personIds.add(hh.pax_idx);
		}
		this.personIds.trimToSize();
	}

	public ArrayList<HITSHousehold> getHouseholds() {
		return households;
	}
	
	public ArrayList<HITSPerson> getPersons() {
		return persons;
	}
	public ArrayList<HITSTrip> getTrips() {
		return trips;
	}

	public ArrayList<HITSStage> getStages() {
		return stages;
	}
	
	private void getPersonsFromHouseholds() {
		this.persons = new ArrayList<>();
		for(HITSHousehold household:this.households){
			this.persons.addAll(household.getPersons());
		}
		this.persons.trimToSize();
	}
	
	private void getTripsFromHouseholds() {
		this.trips = new ArrayList<>();
		for(HITSHousehold household:this.households){
			this.trips.addAll(household.getTrips());
		}
		this.trips.trimToSize();
	}
	
	private void getStagesFromHouseholds() {
		this.stages = new ArrayList<>();
		for(HITSHousehold household:this.households){
			this.stages.addAll(household.getStages());
		}
		this.stages.trimToSize();
	}
	
	public HITSHousehold getHouseholdById(String id){
		return this.households.get(this.householdIds.indexOf(id));
	}
	
	public HITSPerson getPersonById(String id){
		return this.persons.get(this.personIds.indexOf(id));
	}
}
