package org.matsim.pt.routes;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.pt.routes.DefaultTransitPassengerRoute.RouteDescription;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

class RouteDescriptionTest {

	private static final String JSON = """
			{"transitRouteId":"routeLineId","boardingTime":"00:00:02","transitLineId":"transitLineId","accessFacilityId":"accessFacility","egressFacilityId":"egressFacilityId","chainedRoute":{"transitRouteId":null,"boardingTime":"00:00:04","transitLineId":null,"accessFacilityId":null,"egressFacilityId":null}}""";

	@Test
	void deserializeTest() throws JsonProcessingException {

		RouteDescription rd = RouteDescription.fromJson(JSON);
		RouteDescription expectedRd = createRouteDescription();

		Assertions.assertEquals(expectedRd.boardingTime, rd.boardingTime);
		Assertions.assertEquals(expectedRd.accessFacilityId, rd.accessFacilityId);
		Assertions.assertEquals(expectedRd.egressFacilityId, rd.egressFacilityId);
		Assertions.assertEquals(expectedRd.transitLineId, rd.transitLineId);
		Assertions.assertEquals(expectedRd.transitRouteId, rd.transitRouteId);

		Assertions.assertEquals(expectedRd.chainedRoute.boardingTime, rd.chainedRoute.boardingTime);
		Assertions.assertEquals(expectedRd.chainedRoute.accessFacilityId, rd.chainedRoute.accessFacilityId);
		Assertions.assertEquals(expectedRd.chainedRoute.egressFacilityId, rd.chainedRoute.egressFacilityId);
		Assertions.assertEquals(expectedRd.chainedRoute.transitLineId, rd.chainedRoute.transitLineId);
		Assertions.assertEquals(expectedRd.chainedRoute.transitRouteId, rd.chainedRoute.transitRouteId);
	}

	@Test
	void serializeTest() throws JsonProcessingException {
		RouteDescription rd = createRouteDescription();
		String json = RouteDescription.toJson(rd);
		Assertions.assertEquals(JSON, json);
	}

	private static RouteDescription createRouteDescription() {
		RouteDescription rd = new RouteDescription();
		rd.setAccessFacilityId("accessFacility");
		rd.setBoardingTime("2.0");
		rd.setEgressFacilityId("egressFacilityId");
		rd.setRouteLineId("routeLineId");
		rd.setTransitLineId("transitLineId");
		rd.setChainedRoute(new RouteDescription());
		rd.chainedRoute.setBoardingTime("4.0");
		return rd;
	}
}
