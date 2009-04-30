package playground.anhorni.locationchoice.preprocess.analyzeMZ;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.utils.geometry.CoordImpl;

public class MZTrip {
	
	private Id id = null;
	private CoordImpl coordEnd = null;
	private CoordImpl coordStart = null;
	
	// F58
	private double startTime = 0.0;
	// F514
	private double endTime = 0.0;
	private String wmittel;
	private String ausmittel;	
	private String purposeCode;	
	private String shopOrLeisure;
	private String wzweck2;
	
	public MZTrip(Id id, Coord coordStart, Coord coordEnd, double startTime, double endTime) {
		super();
		this.id = id;
		this.coordStart = new CoordImpl(coordStart.getX(), coordStart.getY());
		this.coordEnd = new CoordImpl(coordEnd.getX(), coordEnd.getY());
		this.startTime = startTime;
		this.endTime = endTime;
	}

	public Id getId() {
		return id;
	}
	public void setId(Id id) {
		this.id = id;
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
	public String getShopOrLeisure() {
		return shopOrLeisure;
	}
	public void setShopOrLeisure(String shopOrLeisure) {
		this.shopOrLeisure = shopOrLeisure;
	}
	public String getWzweck2() {
		return wzweck2;
	}
	public void setWzweck2(String wzweck2) {
		this.wzweck2 = wzweck2;
	}
}
