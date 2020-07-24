package org.matsim.contrib.noise;

import com.google.inject.Inject;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.CoordUtils;

import java.util.EnumMap;
import java.util.Map;

import static org.matsim.contrib.noise.RLS19VehicleType.*;

public class RLS19NoiseImmission implements NoiseImmission {

    private final static Logger log = Logger.getLogger(RLS19NoiseImmission.class);

    private static final double AVERAGE_GROUND_HEIGHT = 2.5;
    private static final double MINIMUM_DISTANCE = 5.;

    private final NoiseConfigGroup noiseParams;
    private final NoiseContext noiseContext;

    private final Network network;
    private final ShieldingContext shielding;

    @Inject
    RLS19NoiseImmission(NoiseContext noiseContext, ShieldingContext shielding) {
        this.noiseParams = noiseContext.getNoiseParams();
        this.noiseContext = noiseContext;
        this.network = noiseContext.getScenario().getNetwork();
        this.shielding = shielding;
    }

    @Override
    public void calculateImmission(NoiseReceiverPoint rp, double currentTimeBinEndTime) {

        double resultingNoiseImmission = 0.;
        double sumTmp = 0.;

        Map<RLS19VehicleType, TObjectDoubleMap<Id<Link>>> linkId2IsolatedImmissionPlusOneVehicle = new EnumMap<>(RLS19VehicleType.class);
        TObjectDoubleMap<Id<Link>> linkId2IsolatedImmission = new TObjectDoubleHashMap<>(rp.getRelevantLinks().size());
        if (!rp.getRelevantLinks().isEmpty()) {
            for (Id<Link> linkId : rp.getRelevantLinks()) {
                double noiseImmission = 0;
                if (noiseParams.getTunnelLinkIDsSet().contains(linkId)) {
                    linkId2IsolatedImmissionPlusOneVehicle.computeIfAbsent(pkw, type -> new TObjectDoubleHashMap<>()).put(linkId, 0.);
                    linkId2IsolatedImmissionPlusOneVehicle.computeIfAbsent(lkw1, type -> new TObjectDoubleHashMap<>()).put(linkId, 0.);
                    linkId2IsolatedImmissionPlusOneVehicle.computeIfAbsent(lkw2, type -> new TObjectDoubleHashMap<>()).put(linkId, 0.);
                } else {
                    NoiseLink noiseLink = this.noiseContext.getNoiseLinks().get(linkId);
                    if (noiseLink != null) {
                        noiseImmission = calculateIsolatedLinkImmission(rp, noiseLink);
                        linkId2IsolatedImmission.put(linkId, noiseImmission);
                        for (RLS19VehicleType vehicleType: RLS19VehicleType.values()) {
                            double immissionPlusOne = calculateIsolatedLinkImmissionPlusOneVehicle(rp, noiseLink, vehicleType);
                            if (immissionPlusOne < 0.) {
                                immissionPlusOne = 0.;
                            }
                            if (immissionPlusOne < noiseImmission) {
                                throw new RuntimeException("noise immission: " + noiseImmission + " - noise immission plus one "
                                        + vehicleType.getId() + immissionPlusOne + ". This should not happen. Aborting...");
                            }
                            linkId2IsolatedImmissionPlusOneVehicle.computeIfAbsent(vehicleType, type -> new TObjectDoubleHashMap<>(rp.getRelevantLinks().size())).put(linkId, immissionPlusOne);
                        }
                    }
                }
                if (noiseImmission > 0.) {
                    sumTmp += (Math.pow(10, (0.1 * noiseImmission)));
                }
            }
            if (sumTmp > 0) {
                resultingNoiseImmission = 10 * Math.log10((sumTmp));
            }
        }
        rp.setCurrentImmission(resultingNoiseImmission, currentTimeBinEndTime);
        rp.setLinkId2IsolatedImmission(linkId2IsolatedImmission);
        rp.setLinkId2IsolatedImmissionPlusOneVehicle(linkId2IsolatedImmissionPlusOneVehicle);
    }

    @Override
    //TODO: add height of immission point (z-coordinate for shielding and ground dampening)
    public double calculateCorrection(double projectedDistance, NoiseReceiverPoint nrp, Link candidateLink) {

        if (projectedDistance < MINIMUM_DISTANCE) {
            projectedDistance = MINIMUM_DISTANCE;
            log.debug("Distance between " + candidateLink.getId() + " and " + nrp.getId() + " is too small. The calculation of the correction term Ds requires a distance > 0. Therefore, setting the distance to a minimum value of " + MINIMUM_DISTANCE + ".");
        }

        double geometricDivergence = 20 * Math.log10(projectedDistance) + 10 * Math.log10(2 * Math.PI);
        double airDampeningFactor =  projectedDistance / 200.;
        double groundDampening = Math.max(4.8 - (AVERAGE_GROUND_HEIGHT / projectedDistance) * (34 + 600 / projectedDistance), 0);

        double shielding = 0.;
        if (noiseParams.isConsiderNoiseBarriers()) {
            Coord projectedSourceCoord = CoordUtils.orthogonalProjectionOnLineSegment(
                    candidateLink.getFromNode().getCoord(), candidateLink.getToNode().getCoord(), nrp.getCoord());
            shielding = this.shielding.determineShieldingValue(nrp, candidateLink, projectedSourceCoord);
        }

        double dampeningCorrection = geometricDivergence + airDampeningFactor + groundDampening + shielding;

        //TODO: implement reflection - if someone is looking for a (bachelor) thesis...
        double firstReflectionCorrection = 0;
        double secondReflectionCorrection = 0;

        return dampeningCorrection + firstReflectionCorrection + secondReflectionCorrection;
    }

    private double calculateIsolatedLinkImmission(NoiseReceiverPoint rp, NoiseLink noiseLink) {

        double noiseImmission = 0.;
        if (!(noiseLink.getEmission() == 0.)) {
            double correction = rp.getLinkCorrection(noiseLink.getId());
            Link link = network.getLinks().get(noiseLink.getId());
            //this is actually different from RLS 90 and accounts for length of the link
            noiseImmission = noiseLink.getEmission() + 10 * Math.log10(link.getLength()) - correction;
            if (noiseImmission < 0.) {
                noiseImmission = 0.;
            }
        }
        return noiseImmission;
    }

    private double calculateIsolatedLinkImmissionPlusOneVehicle(NoiseReceiverPoint rp, NoiseLink noiseLink, NoiseVehicleType type) {
        double plusOne = 0;
        if (!(noiseLink.getEmissionPlusOneVehicle(type) == 0.)) {
            double correction = rp.getLinkCorrection(noiseLink.getId());
            Link link = network.getLinks().get(noiseLink.getId());
            //this is actually different from RLS 90 and accounts for length of the link
            plusOne = noiseLink.getEmissionPlusOneVehicle(type) + 10 * Math.log10(link.getLength()) - correction;
            if (plusOne < 0.) {
                plusOne = 0.;
            }
        }
        return plusOne;
    }


}
