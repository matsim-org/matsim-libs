package playground.vsp.drt.accessibilityOrientedDrt.optimizer;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;
import org.matsim.core.utils.collections.Tuple;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.matsim.contrib.dvrp.passenger.DefaultPassengerRequestValidator.EQUAL_FROM_LINK_AND_TO_LINK_CAUSE;

public class HeteogeneousRequestValidator implements PassengerRequestValidator {
    private static final String USE_ALTERNATIVE_MODE = "please_use_alternative_mode";
    private final Population population;
    private final Map<Integer, Double> thresholdMap;
    private final int timeBinSize;
    private final Map<String, Tuple<Double, Double>> alternativeModeTripData;

    public HeteogeneousRequestValidator(Population population, Map<Integer, Double> thresholdMap, int timeBinSize,
                                        Map<String, Tuple<Double, Double>> alternativeModeTripData) {
        this.population = population;
        this.thresholdMap = thresholdMap;
        this.timeBinSize = timeBinSize;
        this.alternativeModeTripData = alternativeModeTripData;
    }

    @Override
    public Set<String> validateRequest(PassengerRequest request) {
        // same as in DefaultPassengerRequestValidator, the request is invalid if fromLink == toLink
        if (request.getFromLink() == request.getToLink()) {
            return Collections.singleton(EQUAL_FROM_LINK_AND_TO_LINK_CAUSE);
        }

        // if the request is made by a person with special need (e.g., attribute: "remark" = "special")
        // Then they will always be valid request
        Person person = population.getPersons().get(request.getPassengerIds().get(0));
        if (!person.getAttributes().getAttribute(PassengerAttribute.ATTRIBUTE_NAME).toString().equals(PassengerAttribute.NORMAL)) {
            return Set.of();
        }

        // otherwise, we determine if a request is valid based on its alternative modes
        double travelTimeRatioOfAlternativeMode = alternativeModeTripData.get(person.getId().toString()).getSecond();
        int timeBin = (int) (Math.floor(request.getEarliestStartTime() / timeBinSize) * timeBinSize);
        double threshold = thresholdMap.get(timeBin);
        if (travelTimeRatioOfAlternativeMode <  threshold) {
            return Set.of(USE_ALTERNATIVE_MODE);
        }
        return Set.of();
    }
}
