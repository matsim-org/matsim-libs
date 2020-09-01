package org.matsim.contrib.noise;

import com.google.inject.Inject;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineSegment;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
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

    private final ShieldingContext shielding;

    @Inject
    RLS19NoiseImmission(NoiseContext noiseContext, ShieldingContext shielding) {
        this.noiseParams = noiseContext.getNoiseParams();
        this.noiseContext = noiseContext;
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
                        noiseImmission = calculateLinkImmission(rp, noiseLink);
                        double isolatedLinkImmission = 10 * Math.log10(noiseImmission);
                        linkId2IsolatedImmission.put(linkId, isolatedLinkImmission);
                        for (RLS19VehicleType vehicleType : RLS19VehicleType.values()) {
                            double immissionPlusOne = calculateIsolatedLinkImmissionPlusOneVehicle(rp, noiseLink, vehicleType);
                            if (immissionPlusOne < 0.) {
                                immissionPlusOne = 0.;
                            }
                            if (immissionPlusOne < isolatedLinkImmission) {
                                throw new RuntimeException("noise immission: " + noiseImmission + " - noise immission plus one "
                                        + vehicleType.getId() + immissionPlusOne + ". This should not happen. Aborting...");
                            }
                            linkId2IsolatedImmissionPlusOneVehicle.computeIfAbsent(vehicleType, type -> new TObjectDoubleHashMap<>(rp.getRelevantLinks().size())).put(linkId, immissionPlusOne);
                        }
                    }
                }
                if (noiseImmission > 0.) {
                    sumTmp += noiseImmission;
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
        return getSectionsCorrection(nrp, candidateLink);
    }

    private double getSectionsCorrection(NoiseReceiverPoint nrp, Link link) {

        Coordinate nrpCoordinate = CoordUtils.createGeotoolsCoordinate(nrp.getCoord());
        LineSegment linkSegment = new LineSegment(CoordUtils.createGeotoolsCoordinate(link.getFromNode().getCoord()),
                CoordUtils.createGeotoolsCoordinate(link.getToNode().getCoord()));

        return getSubSectionsCorrection(nrpCoordinate, linkSegment);
    }

    private double getSubSectionsCorrection(Coordinate nrpCoordinate, LineSegment segment) {

        double correctionTemp = 0;
        final double length = segment.getLength();

        // "Als Faustregel sollte bei freier Schallausbreitung ueber ebenem
        // Boden die Länge l_i eines Fahrstreifenteilstücks
        // maximal die Hälfte der Weglänge si von der Mitte
        // des Teilstücks zum Immissionsort betragen (li ≤ si / 2)"
        double maxL = segment.midPoint().distance(nrpCoordinate) / 2;
        if (length <= maxL) {
            final double sectionCorrection = 10 * Math.log10(length) - calculateCorrection(nrpCoordinate, segment);
            correctionTemp += Math.pow(10, 0.1*sectionCorrection);
        } else {
            double lMid = length / 2;

            double leftLength = lMid - maxL / 2;
            Coordinate leftCoord = segment.pointAlong(leftLength / length);
            LineSegment leftRemaining = new LineSegment(segment.p0, leftCoord);

            double rightLength = lMid + maxL / 2;
            Coordinate rightCoord = segment.pointAlong(rightLength / length);
            LineSegment rightRemaining = new LineSegment(rightCoord, segment.p1);

            LineSegment central = new LineSegment(leftCoord, rightCoord);

            final double sectionCorrection = 10 * Math.log10(length) - calculateCorrection(nrpCoordinate, central);
            correctionTemp += Math.pow(10, 0.1*sectionCorrection);
            correctionTemp += getSubSectionsCorrection(nrpCoordinate, leftRemaining);
            correctionTemp += getSubSectionsCorrection(nrpCoordinate, rightRemaining);
        }
        return correctionTemp;
    }

    private double calculateCorrection(Coordinate nrp, LineSegment segment) {

        double distance = segment.midPoint().distance(nrp);
        if (distance < MINIMUM_DISTANCE) {
            distance = MINIMUM_DISTANCE;
        }

        double geometricDivergence = 20 * Math.log10(distance) + 10 * Math.log10(2 * Math.PI);
        double airDampeningFactor = distance / 200.;
        double groundDampening = Math.max(4.8 - (AVERAGE_GROUND_HEIGHT / distance) * (34 + 600 / distance), 0);

        if (noiseParams.isConsiderNoiseBarriers()) {
            return geometricDivergence + airDampeningFactor + Math.max(groundDampening, this.shielding.determineShieldingValue(nrp, segment));
        } else {
            return geometricDivergence + airDampeningFactor ;
        }

        //TODO: implement reflection - if someone is looking for a (bachelor) thesis...
//        double firstReflectionCorrection = 0;
//        double secondReflectionCorrection = 0;
//        return dampeningCorrection + firstReflectionCorrection + secondReflectionCorrection;
    }


    private double calculateLinkImmission(NoiseReceiverPoint rp, NoiseLink noiseLink) {
        if (!(noiseLink.getEmission() == 0.)) {
            double noiseImmission = Math.pow(10, 0.1 * noiseLink.getEmission()) * rp.getLinkCorrection(noiseLink.getId());
            if (noiseImmission < 0.) {
                noiseImmission = 0.;
            }
            return noiseImmission;
        } else {
            return 0;
        }
    }

    private double calculateIsolatedLinkImmissionPlusOneVehicle(NoiseReceiverPoint rp, NoiseLink noiseLink, NoiseVehicleType type) {
        if (!(noiseLink.getEmission() == 0.)) {
            double noiseImmission = 10 * Math.log10(Math.pow(10, 0.1 * noiseLink.getEmissionPlusOneVehicle(type)) * rp.getLinkCorrection(noiseLink.getId()));

            if (noiseImmission < 0.) {
                noiseImmission = 0.;
            }
            return noiseImmission;
        } else {
            return 0;
        }
    }
}
