package org.matsim.contrib.noise;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;

import static org.matsim.contrib.noise.RLS90VehicleType.*;

class NoiseTollCalculator implements TravelDisutility {

    private final NoiseContext noiseContext;

    NoiseTollCalculator(NoiseContext noiseContext) {
        this.noiseContext = noiseContext;
    }

    @Override
    public double getLinkMinimumTravelDisutility( Link link ) {
        return 0. ; // toll is never negative
    }

    @Override
    public double getLinkTravelDisutility( Link link, double time, Person person, Vehicle vehicle ) {

		/* The following is an estimate of the tolls that an agent would have to pay if choosing that link in the next
		iteration i based on the tolls in iteration i-1 */

        double linkExpectedToll = 0.;
        double timeIntervalEndTime = ((int) (time / this.noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation()) + 1) * this.noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation();

        if (this.noiseContext.getTimeInterval2linkId2noiseLinks().get(timeIntervalEndTime) == null ||
                this.noiseContext.getTimeInterval2linkId2noiseLinks().get(timeIntervalEndTime).get( link.getId() ) == null) {
            // expected toll on that link should be zero

        } else {

            boolean isHGV = false;
            for (String hgvPrefix : this.noiseContext.getNoiseParams().getHgvIdPrefixesArray()) {
                if ( person.toString().startsWith(hgvPrefix )) {
                    isHGV = true;
                    break;
                }
            }

            if (isHGV) {

                if (this.noiseContext.getNoiseParams().getNoiseAllocationApproach() == NoiseConfigGroup.NoiseAllocationApproach.AverageCost) {
                    linkExpectedToll = this.noiseContext.getTimeInterval2linkId2noiseLinks().get(timeIntervalEndTime).get( link.getId() ).getAverageDamageCostPerVehicle(hgv);

                } else if (this.noiseContext.getNoiseParams().getNoiseAllocationApproach() == NoiseConfigGroup.NoiseAllocationApproach.MarginalCost) {
                    linkExpectedToll =
                                    this.noiseContext.getTimeInterval2linkId2noiseLinks().get(timeIntervalEndTime).get( link.getId() ).getMarginalDamageCostPerVehicle(hgv);

                } else {
                    throw new RuntimeException("Unknown noise allocation approach. Aborting...");
                }

            } else {

                if (this.noiseContext.getNoiseParams().getNoiseAllocationApproach() == NoiseConfigGroup.NoiseAllocationApproach.AverageCost) {
                    linkExpectedToll = this.noiseContext.getTimeInterval2linkId2noiseLinks().get(timeIntervalEndTime).get( link.getId() ).getAverageDamageCostPerVehicle(car);

                } else if (this.noiseContext.getNoiseParams().getNoiseAllocationApproach() == NoiseConfigGroup.NoiseAllocationApproach.MarginalCost) {
                    linkExpectedToll =
                                    this.noiseContext.getTimeInterval2linkId2noiseLinks().get(timeIntervalEndTime).get( link.getId() ).getMarginalDamageCostPerVehicle(car);

                } else {
                    throw new RuntimeException("Unknown noise allocation approach. Aborting...");
                }
            }

        }

        double linkExpectedTollDisutility = this.noiseContext.getNoiseParams().getNoiseTollFactor() * linkExpectedToll;
        return linkExpectedTollDisutility;
    }
}
