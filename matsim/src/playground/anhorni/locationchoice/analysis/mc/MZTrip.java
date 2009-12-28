package playground.anhorni.locationchoice.analysis.mc;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordImpl;

public class MZTrip {
	
	private Id personId = null;
	private CoordImpl home = null;
	private CoordImpl coordEnd = null;
	private CoordImpl coordStart = null;
	
	// F58
	private double startTime = 0.0;
	// F514
	private double endTime = 0.0;
	private String wmittel;
	private String ausmittel;	
	private String purposeCode;	
	private String purpose;
	private String wzweck2;
	
	public MZTrip(Id personId, Coord coordStart, Coord coordEnd, double startTime, double endTime) {
		super();
		this.personId = personId;
		this.coordStart = new CoordImpl(coordStart.getX(), coordStart.getY());
		this.coordEnd = new CoordImpl(coordEnd.getX(), coordEnd.getY());
		this.startTime = startTime;
		this.endTime = endTime;
	}

	public Id getPersonId() {
		return personId;
	}
	public void setPersonId(Id personId) {
		this.personId = personId;
	}
	public CoordImpl getCoordEnd() {
		return coordEnd;
	}
	public void setCoordEnd(Coord coordEnd) {
		this.coordEnd = new CoordImpl(coordEnd.getX(), coordEnd.getY());
	}
	public CoordImpl getCoordStart() {
		return coordStart;
	}
	public void setCoordStart(Coord coordStart) {
		this.coordStart = new CoordImpl(coordStart.getX(), coordStart.getY());
	}
	public double getStartTime() {
		return startTime;
	}
	public void setStartTime(double startTime) {
		this.startTime = startTime;
	}
	public double getEndTime() {
		return endTime;
	}
	public void setEndTime(double endTime) {
		this.endTime = endTime;
	}
	public String getWmittel() {
		return wmittel;
	}
	public void setWmittel(String wmittel) {
		this.wmittel = wmittel;
	}
	public String getAusmittel() {
		return ausmittel;
	}
	public void setAusmittel(String ausmittel) {
		this.ausmittel = ausmittel;
	}
	public String getPurposeCode() {
		return purposeCode;
	}
	public void setPurposeCode(String purposeCode) {
		this.purposeCode = purposeCode;
	}
	public String getPurpose() {
		return purpose;
	}
	public void setPurpose(String purpose) {
		this.purpose = purpose;
	}
	public String getWzweck2() {
		return wzweck2;
	}
	public void setWzweck2(String wzweck2) {
		this.wzweck2 = wzweck2;
	}

	public CoordImpl getHome() {
		return home;
	}
	public double getDuration() {
		return (this.endTime - this.startTime);
	}

	public void setHome(CoordImpl home) {
		this.home = home;
	}
	public String getMatsimMode() {
		
		int wmittelInt = Integer.parseInt(this.wmittel);
		
		 //2: Bahn 3: Postauto 5: Tram 6: Bus
		if (wmittelInt == 2 || wmittelInt == 3 || wmittelInt == 5 || wmittelInt == 6) {
			return "pt";
		}
		// MIV
		//9: Auto  11: Taxi 12: Motorrad, Kleinmotorrad 13: Mofa
		else if (wmittelInt == 9 || wmittelInt == 11 || wmittelInt == 12 || wmittelInt == 13) {
			return "car";
		}
		//14: Velo
		else if (wmittelInt == 14) {
			return "bike";
		}
		//15: zu Fuss
		else if (wmittelInt == 15) {
			return "walk";
		}
		else return "undefined";
	}
}
