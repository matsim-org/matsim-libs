package pl.poznan.put.vrp.dynamic.data.network;

public interface Arc
{
    Vertex getFromVertex();


    Vertex getToVertex();


    /**
     * @param departureTime departure time
     * @return arc time (depending on the departure time)
     */
    int getTimeOnDeparture(int departureTime);


    /**
     * @param arrivalTime arrival time
     * @return arc time (depending on the arrival time)
     */
    int getTimeOnArrival(int arrivalTime);


    /**
     * @param departureTime departure time
     * @return arc cost (depending on the departure time)
     */
    double getCostOnDeparture(int departureTime);
}
