package org.matsim.contrib.ev.strategic.replanning.innovator;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.ev.fleet.ElectricFleetSpecification;
import org.matsim.contrib.ev.fleet.ElectricVehicleSpecification;
import org.matsim.contrib.ev.strategic.StrategicChargingUtils;
import org.matsim.contrib.ev.strategic.plan.ChargingPlan;
import org.matsim.contrib.ev.strategic.plan.ChargingPlans;
import org.matsim.contrib.ev.strategic.replanning.innovator.ChargingInnovationParameters.ConstraintErrorMode;
import org.matsim.contrib.ev.strategic.replanning.innovator.ChargingInnovationParameters.ConstraintFallbackBehavior;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

public class ConstrainedChargingPlanInnovator implements ChargingPlanInnovator {
    private final static Logger logger = LogManager.getLogger(ConstrainedChargingPlanInnovator.class);

    private final ChargingPlanInnovator innovator;
    private final ElectricFleetSpecification electricFleetSpecification;
    private final ChargingPlanCalculatorFactory calculatorFactory;

    private final int iterations;
    private final String chargingMode;

    private final ConstraintErrorMode errorMode;
    private final ConstraintFallbackBehavior fallbackBehavior;

    public ConstrainedChargingPlanInnovator(ChargingPlanInnovator innovator,
            ElectricFleetSpecification electricFleetSpecification, ChargingPlanCalculatorFactory calculatorFactory,
            int iterations, String chargingMode, ConstraintErrorMode errorMode,
            ConstraintFallbackBehavior fallbackBehavior) {
        this.innovator = innovator;
        this.electricFleetSpecification = electricFleetSpecification;
        this.calculatorFactory = calculatorFactory;
        this.iterations = iterations;
        this.chargingMode = chargingMode;
        this.errorMode = errorMode;
        this.fallbackBehavior = fallbackBehavior;
    }

    @Override
    public ChargingPlan createChargingPlan(Person person, Plan plan, ChargingPlans chargingPlans) {
        // initialize predictive energy calculator
        ChargingPlanCalculator calculator = calculatorFactory.createCalculator(plan);

        // vehicle information
        Id<Vehicle> vehicleId = VehicleUtils.getVehicleId(person, chargingMode);
        ElectricVehicleSpecification specification = electricFleetSpecification.getVehicleSpecifications()
                .get(vehicleId);

        // obtain relevant energy quantities
        double initialEnergy = specification.getInitialCharge();

        Double minimumSoc = StrategicChargingUtils.getMinimumSoc(person);
        double minimumEnergy = 0.0;

        if (minimumSoc != null) {
            minimumEnergy = specification.getBatteryCapacity() * minimumSoc;
        }

        Double minimumEndSoc = StrategicChargingUtils.getMinimumEndSoc(person);
        double minimumEndEnergy = 0.0;

        if (minimumEndSoc != null) {
            minimumEndEnergy = specification.getBatteryCapacity() * minimumEndSoc;
        }

        // start generating plans
        ChargingPlan chargingPlan = null;
        for (int k = 0; k < iterations; k++) {
            chargingPlan = innovator.createChargingPlan(person, plan, chargingPlans);

            List<Double> chargeableEnergy = calculator.calculateChargeableEnergy(chargingPlan);

            if (calculator.isFeasible(initialEnergy, chargeableEnergy, minimumEnergy, minimumEndEnergy)) {
                // found a feasible one (zero soc, minimum soc, minimum end soc)
                return chargingPlan;
            }
        }

        switch (errorMode) {
            case printWarning:
                logger.warn("No feasible charging plan found for agent " + person.getId());
                break;
            case throwException:
                throw new IllegalStateException("No feasible charging plan found for agent " + person.getId());
            case none:
            default:
                break;
        }

        switch (fallbackBehavior) {
            case returnRandom:
                return chargingPlan; // last drawn (random) plan
            case returnNone:
                return new ChargingPlan(); // empty plan
            default:
                throw new IllegalStateException();
        }
    }
}
