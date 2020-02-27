package org.matsim.contrib.noise;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

public class NoiseTollCalculator {

    private final NoiseContext noiseContext;

    public NoiseTollCalculator(NoiseContext noiseContext) {
        this.noiseContext = noiseContext;
    }

    public double calculateExpectedTollDisutility(Id<Link> linkId, double time, Id<Person> personId) {

		/* The following is an estimate of the tolls that an agent would have to pay if choosing that link in the next
		iteration i based on the tolls in iteration i-1 */

        double linkExpectedToll = 0.;
        double timeIntervalEndTime = ((int) (time / this.noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation()) + 1) * this.noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation();

        if (this.noiseContext.getTimeInterval2linkId2noiseLinks().get(timeIntervalEndTime) == null ||
                this.noiseContext.getTimeInterval2linkId2noiseLinks().get(timeIntervalEndTime).get(linkId) == null) {
            // expected toll on that link should be zero

        } else {

            boolean isHGV = false;
            for (String hgvPrefix : this.noiseContext.getNoiseParams().getHgvIdPrefixesArray()) {
                if (personId.toString().startsWith(hgvPrefix)) {
                    isHGV = true;
                    break;
                }
            }

            if (isHGV) {

                if (this.noiseContext.getNoiseParams().getNoiseAllocationApproach() == NoiseAllocationApproach.AverageCost) {
                    linkExpectedToll = this.noiseContext.getTimeInterval2linkId2noiseLinks().get(timeIntervalEndTime).get(linkId).getAverageDamageCostPerHgv();

                } else if (this.noiseContext.getNoiseParams().getNoiseAllocationApproach() == NoiseAllocationApproach.MarginalCost) {
                    linkExpectedToll = this.noiseContext.getTimeInterval2linkId2noiseLinks().get(timeIntervalEndTime).get(linkId).getMarginalDamageCostPerHgv();

                } else {
                    throw new RuntimeException("Unknown noise allocation approach. Aborting...");
                }

            } else {

                if (this.noiseContext.getNoiseParams().getNoiseAllocationApproach() == NoiseAllocationApproach.AverageCost) {
                    linkExpectedToll = this.noiseContext.getTimeInterval2linkId2noiseLinks().get(timeIntervalEndTime).get(linkId).getAverageDamageCostPerCar();

                } else if (this.noiseContext.getNoiseParams().getNoiseAllocationApproach() == NoiseAllocationApproach.MarginalCost) {
                    linkExpectedToll = this.noiseContext.getTimeInterval2linkId2noiseLinks().get(timeIntervalEndTime).get(linkId).getMarginalDamageCostPerCar();

                } else {
                    throw new RuntimeException("Unknown noise allocation approach. Aborting...");
                }
            }

        }

        double linkExpectedTollDisutility = this.noiseContext.getNoiseParams().getNoiseTollFactor() * linkExpectedToll;
        return linkExpectedTollDisutility;
    }
}
