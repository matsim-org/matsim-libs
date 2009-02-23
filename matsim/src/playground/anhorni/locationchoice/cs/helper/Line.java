package playground.anhorni.locationchoice.cs.helper;

import java.util.ArrayList;
import java.util.TreeMap;

import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.geometry.CoordImpl;


public class Line {
	
	private String tripId;
	private String wmittel;
	
	private String ZIELPNR;
	private int tripNr;
	private String HHNR;
		
	//WP
	private double personWeight;
	//F23B
	private int age;
	//F23C: 1: man 2: woman
	private int gender;
	//F101: income per household
	private int incomeHH;
	//F22A
	private int numberOfPersonsHH;
	//F41
	private int civilStatus;
	//F43
	private int education;

	private Coord homeCoordinates;	
	private int start_is_home;
	
	private ReaderTripHandler tripHandler;
	private PersonAttributes personAttributes;
	

	
	public boolean catchLine(String [] entries) {
		
		this.tripId = entries[0].trim();
		// TODO:
		// Passed last line. Why not captured in null test?
		if (this.tripId.length() == 0) return false;
		
		this.wmittel = entries[70].trim();	
		this.HHNR = entries[3].trim();
		this.ZIELPNR = entries[6].trim();
		if (this.ZIELPNR.length() == 1) this.ZIELPNR = "0" + ZIELPNR; 
		
		this.tripNr = Integer.parseInt(entries[8].trim());
		this.personWeight = Double.parseDouble(entries[7].trim());
		this.age = Integer.parseInt(entries[108].trim());
		this.gender = Integer.parseInt(entries[109].trim());
		this.incomeHH = Integer.parseInt(entries[122].trim());
		this.numberOfPersonsHH = Integer.parseInt(entries[76].trim());
		this.civilStatus = Integer.parseInt(entries[78].trim());
		this.education = Integer.parseInt(entries[79].trim());
		this.start_is_home = Integer.parseInt(entries[79].trim());
		
		this.homeCoordinates = new CoordImpl(
				Double.parseDouble(entries[21].trim()), 
				Double.parseDouble(entries[22].trim())
				);
		
		this.personAttributes = new PersonAttributes(this.personWeight, this.age, this.gender, this.incomeHH, this.numberOfPersonsHH,
				this.civilStatus, this.education, this.start_is_home);
		
		return true;		
	}
	
	public void constructTrip(String [] entries, NetworkLayer network, TreeMap<Id, ArrayList<ZHFacility>> zhFacilitiesByLink, 
			MZTrip mzTrip) {
		this.tripHandler = new ReaderTripHandler();
		this.tripHandler.constructTrip(entries, network, zhFacilitiesByLink, mzTrip, this.tripNr);
	}
	
	public Trip getTrip() {
		return this.tripHandler.getTrip();
	}
	
	public ZHFacility getChosenZHFacility() {
		return this.tripHandler.getChosenZHFacility();
	}
	
	public double getTravelTimeBudget() {
		return this.tripHandler.getTravelTimeBudget();
	}

	public String getNextTrip() {
		return this.HHNR + this.ZIELPNR + Integer.toString(this.tripNr+1);
	}
	
	public String getZIELPNR() {
		return ZIELPNR;
	}
	public String getTripId() {
		return tripId;
	}

	public void setTripId(String tripId) {
		this.tripId = tripId;
	}

	public String getWmittel() {
		return wmittel;
	}

	public void setWmittel(String wmittel) {
		this.wmittel = wmittel;
	}

	public void setZIELPNR(String zielpnr) {
		ZIELPNR = zielpnr;
	}
	public int getTripNr() {
		return tripNr;
	}
	public void setTripNr(int tripNr) {
		this.tripNr = tripNr;
	}
	public double getPersonWeight() {
		return personWeight;
	}
	public void setPersonWeight(double personWeight) {
		this.personWeight = personWeight;
	}
	public int getAge() {
		return age;
	}
	public void setAge(int age) {
		this.age = age;
	}
	public int getGender() {
		return gender;
	}
	public void setGender(int gender) {
		this.gender = gender;
	}
	public int getIncomeHH() {
		return incomeHH;
	}
	public void setIncomeHH(int incomeHH) {
		this.incomeHH = incomeHH;
	}
	public int getNumberOfPersonsHH() {
		return numberOfPersonsHH;
	}
	public void setNumberOfPersonsHH(int numberOfPersonsHH) {
		this.numberOfPersonsHH = numberOfPersonsHH;
	}
	public int getCivilStatus() {
		return civilStatus;
	}
	public void setCivilStatus(int civilStatus) {
		this.civilStatus = civilStatus;
	}
	public int getEducation() {
		return education;
	}
	public void setEducation(int education) {
		this.education = education;
	}
	public Coord getHomeCoordinates() {
		return homeCoordinates;
	}
	public void setHomeCoordinates(Coord homeCoordinates) {
		this.homeCoordinates = homeCoordinates;
	}
	public int getStart_is_home() {
		return start_is_home;
	}
	public void setStart_is_home(int start_is_home) {
		this.start_is_home = start_is_home;
	}

	public PersonAttributes getPersonAttributes() {
		return personAttributes;
	}

	public void setPersonAttributes(PersonAttributes personAttributes) {
		this.personAttributes = personAttributes;
	}	
}
