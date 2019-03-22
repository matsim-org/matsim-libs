package vwExamples.utils.DemandFromCSV;

public class Trip {
	double request_time;
	double origin_lon;
	double origin_lat;
	double destination_lon;
	double destination_lat;
	double adult_passengers;
	double earliest_departure_time;

	Trip(double request_time, double origin_lon, double origin_lat, double destination_lon, double destination_lat,
			double adult_passengers, double earliest_departure_time) {
		this.request_time = request_time;
		this.origin_lon = origin_lon;
		this.origin_lat = origin_lat;
		this.destination_lon = destination_lon;
		this.destination_lat = destination_lat;
		this.adult_passengers = adult_passengers;
		this.earliest_departure_time = earliest_departure_time;
	}

}
