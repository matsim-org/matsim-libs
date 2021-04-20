package org.matsim.contrib.noise;

import com.google.inject.Inject;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.log4j.Logger;
import org.locationtech.jts.algorithm.Angle;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.CoordUtils;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;



class RLS90NoiseImmission implements NoiseImmission {

    private final static Logger log = Logger.getLogger(RLS90NoiseImmission.class);

    private final NoiseConfigGroup noiseParams;
    private final NoiseContext noiseContext;
    private final ShieldingContext shielding;

    @Inject
    RLS90NoiseImmission(NoiseContext noiseContext, ShieldingContext shielding) {
        this.noiseParams = noiseContext.getNoiseParams();
        this.noiseContext = noiseContext;
        this.shielding = shielding;
    }

    @Override
    public void calculateImmission(NoiseReceiverPoint rp, double currentTimeBinEndTime) {

        double resultingNoiseImmission = 0.;
        double sumTmp = 0.;

        Map<RLS90VehicleType, TObjectDoubleMap<Id<Link>>> linkId2IsolatedImmissionPlusOneVehicle = new EnumMap<>(RLS90VehicleType.class);
        TObjectDoubleMap<Id<Link>> linkId2IsolatedImmission = new TObjectDoubleHashMap<>(rp.getRelevantLinks().size());
        if(!rp.getRelevantLinks().isEmpty()) {
            for (Id<Link> linkId : rp.getRelevantLinks()) {
                double noiseImmission = 0;
                if (noiseParams.getTunnelLinkIDsSet().contains(linkId)) {
                    linkId2IsolatedImmissionPlusOneVehicle.computeIfAbsent(RLS90VehicleType.car, type -> new TObjectDoubleHashMap<>()).put(linkId, 0.);
                    linkId2IsolatedImmissionPlusOneVehicle.computeIfAbsent(RLS90VehicleType.hgv, type -> new TObjectDoubleHashMap<>()).put(linkId, 0.);
                } else {
                    NoiseLink noiseLink = this.noiseContext.getNoiseLinks().get(linkId);
                    if (noiseLink != null) {
                        noiseImmission = calculateIsolatedLinkImmission(rp, noiseLink);
                        linkId2IsolatedImmission.put(linkId, noiseImmission);
                        for (RLS90VehicleType vehicleType : RLS90VehicleType.values()) {
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

    private double calculateIsolatedLinkImmission(NoiseReceiverPoint rp, NoiseLink noiseLink) {

        double correction = rp.getLinkCorrection(noiseLink.getId());

        double noiseImmission = 0.;
        if (!(noiseLink.getEmission() == 0.)) {
            noiseImmission = noiseLink.getEmission() + correction;
            if (noiseImmission < 0.) {
                noiseImmission = 0.;
            }
        }
        return noiseImmission;
    }

    static double calculateResultingNoiseImmission(Collection<Double> collection){
        double resultingNoiseImmission = 0.;

        if (collection.size() > 0) {
            double sumTmp = 0.;
            for (double noiseImmission : collection) {
                if (noiseImmission > 0.) {
                    sumTmp = sumTmp + (Math.pow(10, (0.1 * noiseImmission)));
                }
            }
            resultingNoiseImmission = 10 * Math.log10(sumTmp);
            if (resultingNoiseImmission < 0) {
                resultingNoiseImmission = 0.;
            }
        }
        return resultingNoiseImmission;
    }


    private double calculateIsolatedLinkImmissionPlusOneVehicle(NoiseReceiverPoint rp, NoiseLink noiseLink, NoiseVehicleType type) {
        double plusOne = 0;
        if (!(noiseLink.getEmissionPlusOneVehicle(type) == 0.)) {
            double correction = rp.getLinkCorrection(noiseLink.getId());
            plusOne = noiseLink.getEmissionPlusOneVehicle(type)
                    + correction;
        }
        return plusOne;
    }

    @Override
    public double calculateCorrection(double projectedDistance, NoiseReceiverPoint nrp, Link candidateLink) {
        // wouldn't it be good to check distance < minDistance here? DR20180215
        if (projectedDistance == 0) {
            double minimumDistance = 5.;
            projectedDistance = minimumDistance;
            log.warn("Distance between " + candidateLink.getId() + " and " + nrp.getId() + " is 0. The calculation of the correction term Ds requires a distance > 0. Therefore, setting the distance to a minimum value of " + minimumDistance + ".");
        }
        double correctionTermDs = calculateDistanceCorrection(projectedDistance);
        double correctionTermAngle = calculateAngleImmissionCorrection(nrp.getCoord(),
                noiseContext.getScenario().getNetwork().getLinks().get(candidateLink.getId()));
        double correction = correctionTermAngle + correctionTermDs;

        if (noiseParams.isConsiderNoiseBarriers()) {
            Coord projectedSourceCoord = CoordUtils.orthogonalProjectionOnLineSegment(
                    candidateLink.getFromNode().getCoord(), candidateLink.getToNode().getCoord(), nrp.getCoord());
            double correctionTermShielding =
                    shielding.determineShieldingValue(nrp, candidateLink, projectedSourceCoord);
            correction -= correctionTermShielding;
        }
        return correction;
    }

    @Override
    public void setCurrentRp(NoiseReceiverPoint nrp) {

    }

    static double calculateDistanceCorrection(double distance) {
        double correctionTermDs = 15.8 - (10 * Math.log10(distance)) - (0.0142 * (Math.pow(distance, 0.9)));
        return correctionTermDs;
    }

    static double calculateAngleCorrection(double angle) {
        double angleCorrection = 10 * Math.log10((angle) / (180));
        return angleCorrection;
    }

    private double calculateAngleImmissionCorrection(Coord receiverPointCoord, Link link) {

        double angle = 0;

        double pointCoordX = receiverPointCoord.getX();
        double pointCoordY = receiverPointCoord.getY();

        double fromCoordX = link.getFromNode().getCoord().getX();
        double fromCoordY = link.getFromNode().getCoord().getY();
        double toCoordX = link.getToNode().getCoord().getX();
        double toCoordY = link.getToNode().getCoord().getY();

        if (pointCoordX == fromCoordX && pointCoordY == fromCoordY) {
            // receiver point is situated on the link (fromNode)
            // assume a maximum angle immission correction for this case
            angle = 0;

        } else if (pointCoordX == toCoordX && pointCoordY == toCoordY) {
            // receiver point is situated on the link (toNode)
            // assume a zero angle immission correction for this case
            angle = 180;

        } else {
            // all other cases
            angle = Angle.toDegrees(Angle.angleBetween(
                    CoordUtils.createGeotoolsCoordinate(link.getFromNode().getCoord()),
                    CoordUtils.createGeotoolsCoordinate(receiverPointCoord),
                    CoordUtils.createGeotoolsCoordinate(link.getToNode().getCoord())));
        }

        // since zero is not defined
        if (angle == 0.) {
            // zero degrees is not defined
            angle = 0.0000000001;
        }

//		System.out.println(receiverPointCoord + " // " + link.getId() + "(" + link.getFromNode().getCoord() + "-->" + link.getToNode().getCoord() + " // " + angle);
        return RLS90NoiseImmission.calculateAngleCorrection(angle);
    }
}
