package playgroundMeng.publicTransitServiceAnalysis.basicDataBank;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playgroundMeng.publicTransitServiceAnalysis.kpiCalculator.CarTravelTimeCalculator.CarTravelInfo;
import playgroundMeng.publicTransitServiceAnalysis.kpiCalculator.PtTravelTimeCaculator.PtTraveInfo;

public class Trip {
	
	private double[] OriginCoordinate = new double[2];
	private double[] DestinationCoordinate = new double[2];
	
	private ActivityImp activityEndImp;
	private ActivityImp activityStartImp;
	
	private double travelTime;
	private double travelDistance;
	private Id<Person> personId;
	private List<String> modes = new LinkedList<String>();
	private String mode;
	private CarTravelInfo carTravelInfo;
	private PtTraveInfo ptTraveInfo;
	private double ratio;
	private double ratioWithOutWaitingTime;
	private boolean foundOriginZone = false;
	private boolean foundDestinationZone = false;

	public void setFoundDestinationZone(boolean foundDestinationZone) {
		this.foundDestinationZone = foundDestinationZone;
	}

	public void setFoundOriginZone(boolean foundOriginZone) {
		this.foundOriginZone = foundOriginZone;
	}

	public boolean isFoundDestinationZone() {
		return foundDestinationZone;
	}

	public boolean isFoundOriginZone() {
		return foundOriginZone;
	}

	public Trip() {
		// TODO Auto-generated constructor stub
	}
	
	public double[] getOriginCoordinate() {
		CoordinateTransformation transformation = TransformationFactory
				.getCoordinateTransformation("EPSG:25832", "EPSG:4326");
		double x = activityEndImp.getCoord().getX();
		double y = activityEndImp.getCoord().getY();
		
		Coord companyCoord = new Coord(x, y);
		companyCoord = transformation.transform(companyCoord);
		this.OriginCoordinate[0] = companyCoord.getY();
		this.OriginCoordinate[1] = companyCoord.getX();		
		
		return OriginCoordinate;
	}
	
	public double[] getDestinationCoordinate() {
		CoordinateTransformation transformation = TransformationFactory
				.getCoordinateTransformation("EPSG:25832", "EPSG:4326");
		double x = activityStartImp.getCoord().getX();
		double y = activityStartImp.getCoord().getY();
		
		Coord companyCoord = new Coord(x, y);
		companyCoord = transformation.transform(companyCoord);
		this.DestinationCoordinate[0] = companyCoord.getY();
		this.DestinationCoordinate[1] = companyCoord.getX();		
		
		return DestinationCoordinate;
	}
	
	public void setRatioWithOutWaitingTime(double ratioWithOutWaitingTime) {
		this.ratioWithOutWaitingTime = ratioWithOutWaitingTime;
	}
	
	public double getRatioWithOutWaitingTime() {
		return ratioWithOutWaitingTime;
	}

	public void setTravelDistance(double travelDistance) {
		this.travelDistance = travelDistance;
	}

	public double getTravelDistance() {
		double x1 = activityEndImp.getCoord().getX();
		double y1 = activityEndImp.getCoord().getY();
		double x2 = activityStartImp.getCoord().getX();
		double y2 = activityStartImp.getCoord().getY();
		this.travelDistance = Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
		return travelDistance;
	}

	public void setRatio(double ratio) {
		this.ratio = ratio;
	}

	public double getRatio() {
		return this.ratio;
	}

	public void setCarTravelInfo(CarTravelInfo carTravelInfo) {
		this.carTravelInfo = carTravelInfo;
	}

	public CarTravelInfo getCarTravelInfo() {
		return carTravelInfo;
	}

	public void setPtTraveInfo(PtTraveInfo ptTraveInfo) {
		this.ptTraveInfo = ptTraveInfo;
	}

	public PtTraveInfo getPtTraveInfo() {
		return ptTraveInfo;
	}

	public ActivityImp getActivityEndImp() {
		return activityEndImp;
	}

	public void setActivityEndImp(ActivityImp activityEndImp) {
		this.activityEndImp = activityEndImp;
	}

	public ActivityImp getActivityStartImp() {
		return activityStartImp;
	}

	public void setActivityStartImp(ActivityImp activityStartImp) {
		this.activityStartImp = activityStartImp;
		this.setTravelTime(this.getTravelTime());
	}

	public double getTravelTime() {
		return this.activityStartImp.getTime() - this.activityEndImp.getTime();
	}

	public void setTravelTime(double travelTime) {
		this.travelTime = travelTime;
	}

	public Id<Person> getPersonId() {
		return personId;
	}

	public void setPersonId(Id<Person> personId) {
		this.personId = personId;
	}

	@Override
	public String toString() {
		return "Trip [beginnActivityImp=" + activityEndImp + ", endActivityImp=" + activityStartImp + ", legMode="
				+ modes + ", travelTime=" + travelTime + ", personId=" + personId + "]";
	}

	public void setModes(List<String> modes) {
		this.modes = modes;
	}

	public List<String> getModes() {
		return modes;
	}
	public String getMode() throws Exception {
		if(this.getModes().size() == 1) {
			return this.getModes().get(0);
		} else if (this.getModes().contains("pt")) {
			return "pt";
		} else {
			throw new Exception("unknown trip mode");
		}
	}

}
