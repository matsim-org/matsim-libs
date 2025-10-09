package playground.vsp.drt.accessibilityOrientedDrt.optimizer;

import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.matsim.contrib.dvrp.passenger.DefaultPassengerRequestValidator.EQUAL_FROM_LINK_AND_TO_LINK_CAUSE;

public class HeterogeneousRequestValidator implements PassengerRequestValidator {
	private static final String USE_ALTERNATIVE_MODE = "please_use_alternative_mode";
	private final Map<String, String> personAttributeMap;
	private final Map<String, Boolean> alternativeModeIsAGoodOption;

	public HeterogeneousRequestValidator(Map<String, String> personAttributeMap, Map<String, Boolean> alternativeModeIsAGoodOption) {
		this.personAttributeMap = personAttributeMap;
		this.alternativeModeIsAGoodOption = alternativeModeIsAGoodOption;
	}

	@Override
	public Set<String> validateRequest(PassengerRequest request) {
		// same as in DefaultPassengerRequestValidator, the request is invalid if fromLink == toLink
		if (request.getFromLink() == request.getToLink()) {
			return Collections.singleton(EQUAL_FROM_LINK_AND_TO_LINK_CAUSE);
		}

		String personId = request.getPassengerIds().getFirst().toString();
		String personAttribute = personAttributeMap.get(personId);
		// if the request is made by a person with special need (e.g., attribute: "remark" = "special")
		// Then they will always be valid request

		if (!personAttribute.equals(PassengerAttribute.NORMAL)) {
			return Set.of();
		}

		// otherwise, we determine if a request is valid based on its alternative modes
		if (alternativeModeIsAGoodOption.get(personId)) {
			return Set.of(USE_ALTERNATIVE_MODE);
		}
		return Set.of();
	}
}
