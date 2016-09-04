package besttimeresponse;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 * @param L
 *            the location type (generic such that both link-to-link and
 *            zone-to-zone are supported)
 * @param M
 *            the mode type
 *
 */
public interface TripTravelTimes<L, M> {

	public double getTravelTime_s(L origin, L destination, double dptTime_s, M mode);

}
