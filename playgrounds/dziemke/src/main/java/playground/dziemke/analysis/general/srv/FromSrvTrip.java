package playground.dziemke.analysis.general.srv;

import org.matsim.api.core.v01.Id;
import playground.dziemke.analysis.general.Trip;
import playground.dziemke.cemdapMatsimCadyts.Zone;

/**
 * @author gthunig on 18.04.2017.
 */
public class FromSrvTrip extends Trip {

    private Id<Zone> departureZoneId;
    private Id<Zone> arrivalZoneId;
    private double speed_m_s;

    //getters and setters
    Id<Zone> getDepartureZoneId() {
        return departureZoneId;
    }

    void setDepartureZoneId(Id<Zone> departureZoneId) {
        this.departureZoneId = departureZoneId;
    }

    Id<Zone> getArrivalZoneId() {
        return arrivalZoneId;
    }

    void setArrivalZoneId(Id<Zone> arrivalZoneId) {
        this.arrivalZoneId = arrivalZoneId;
    }

    double getSpeed_m_s() {
        return speed_m_s;
    }

    void setSpeed_m_s(double speed_m_s) {
        this.speed_m_s = speed_m_s;
    }
}
