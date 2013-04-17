package playground.vsp.analysis.modules.transitScheduleAnalyser;


public class TransitScheduleAnalyserElement {

	String lineId;
	String routeId;
	String firstDeparture;
	String lastDeparture;
	String tourLengthTime;
	String tourLengthDistance;
	String averageHeadway;
	String numberOfVehicles;

	
	public TransitScheduleAnalyserElement (String lineId, String routeId, String firstDeparture, String lastDeparture, String tourLengthTime, String tourLengthDistance, String averageHeadway, String numberOfVehicles){
		this.lineId = lineId;
		this.routeId = routeId;
		this.firstDeparture = firstDeparture;
		this.lastDeparture = lastDeparture;
		this.tourLengthTime = tourLengthTime;
		this.tourLengthDistance = tourLengthDistance;
		this.averageHeadway = averageHeadway;
		this.numberOfVehicles = numberOfVehicles;
		
		
	}


	@Override
	public String toString() {
		return "TransitScheduleAnalyserElement [lineId=" + lineId
				+ ", routeId=" + routeId + ", firstDeparture=" + firstDeparture
				+ ", lastDeparture=" + lastDeparture + ", tourLengthTime="
				+ tourLengthTime + ", tourLengthDistance=" + tourLengthDistance
				+ ", averageHeadway=" + averageHeadway + ", numberOfVehicles="
				+ numberOfVehicles + "]";
	}
	
	
	
	
}
