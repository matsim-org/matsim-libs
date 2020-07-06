package org.matsim.contrib.noise;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.CoordUtils;

import java.util.Collection;
import java.util.HashMap;
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
    public ImmissionInfo calculateImmission(NoiseReceiverPoint rp) {
        ImmissionInfo info = new ImmissionInfo();
        Map<Id<Link>, Double> linkId2IsolatedImmission = new HashMap<>(0);
        Map<Id<NoiseVehicleType>, Map<Id<Link>, Double>> linkId2IsolatedImmissionPlusOneVehicle = new HashMap<>(0);
        double finalNoiseImmission = 0;
        if (!rp.getRelevantLinks().isEmpty()) {
            for (Id<Link> linkId : rp.getRelevantLinks()) {
                double noiseImmission;
                if (noiseParams.getTunnelLinkIDsSet().contains(linkId)) {
                    linkId2IsolatedImmission.put(linkId, 0.);
                    linkId2IsolatedImmissionPlusOneVehicle.computeIfAbsent(pkw.getId(), type -> new HashMap<>()).put(linkId, 0.);
                    linkId2IsolatedImmissionPlusOneVehicle.computeIfAbsent(lkw1.getId(), type -> new HashMap<>()).put(linkId, 0.);
                    linkId2IsolatedImmissionPlusOneVehicle.computeIfAbsent(lkw2.getId(), type -> new HashMap<>()).put(linkId, 0.);
                } else {
                    NoiseLink noiseLink = this.noiseContext.getNoiseLinks().get(linkId);
                    if (noiseLink != null) {
                        noiseImmission = calculateIsolatedLinkImmission(rp, noiseLink);
                        linkId2IsolatedImmission.put(linkId, noiseImmission);
                        for (NoiseVehicleType vehicleType : noiseParams.getNoiseComputationMethod().noiseVehiclesTypes) {
                            double immissionPlusOne = calculateIsolatedLinkImmissionPlusOneVehicle(rp, noiseLink, vehicleType);
                            if (immissionPlusOne < 0.) {
                                immissionPlusOne = 0.;
                            }
                            if (immissionPlusOne < noiseImmission) {
                                throw new RuntimeException("noise immission: " + noiseImmission + " - noise immission plus one "
                                        + vehicleType.getId() + immissionPlusOne + ". This should not happen. Aborting...");
                            }
                            linkId2IsolatedImmissionPlusOneVehicle.computeIfAbsent(vehicleType.getId(), type -> new HashMap<>()).put(linkId, immissionPlusOne);
                        }
                    }
                }
            }
            finalNoiseImmission = calculateResultingNoiseImmission(linkId2IsolatedImmission.values());
        }
        info.setImmission(finalNoiseImmission);
        info.setLinkId2IsolatedImmission(linkId2IsolatedImmission);
        info.setLinkId2IsolatedImmissionPlusOneVehicle(linkId2IsolatedImmissionPlusOneVehicle);
        return info;
    }

    @Override
    //TODO: add height of immission point (z-coordinate for shielding and ground dampening)
    public double calculateCorrection(double projectedDistance, NoiseReceiverPoint nrp, Link candidateLink) {

        if (projectedDistance < MINIMUM_DISTANCE) {
            projectedDistance = MINIMUM_DISTANCE;
            log.warn("Distance between " + candidateLink.getId() + " and " + nrp.getId() + " is too small. The calculation of the correction term Ds requires a distance > 0. Therefore, setting the distance to a minimum value of " + MINIMUM_DISTANCE + ".");
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

    static double calculateResultingNoiseImmission(Collection<Double> collection) {
        double resultingNoiseImmission = 0.;

        double linkImmissions;

        if (collection.size() > 0) {
            double sumTmp = 0.;
            for (double noiseImmission : collection) {
                if (noiseImmission > 0.) {
                    sumTmp += (Math.pow(10, (0.1 * noiseImmission)));
                }
            }
            if (sumTmp > 0) {
                linkImmissions = 10 * Math.log10(sumTmp);
                resultingNoiseImmission = 10 * Math.log10(Math.pow(10, 0.1 * linkImmissions));
            }
        }
        return resultingNoiseImmission;
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
        if (!(noiseLink.getEmissionPlusOneVehicle(type.getId()) == 0.)) {
            double correction = rp.getLinkCorrection(noiseLink.getId());
            Link link = network.getLinks().get(noiseLink.getId());
            //this is actually different from RLS 90 and accounts for length of the link
            plusOne = noiseLink.getEmissionPlusOneVehicle(type.getId()) + 10 * Math.log10(link.getLength()) - correction;
            if (plusOne < 0.) {
                plusOne = 0.;
            }
        }
        return plusOne;
    }
}
