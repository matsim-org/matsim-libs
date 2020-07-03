package org.matsim.contrib.noise;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;

import java.util.*;

import static org.matsim.contrib.noise.RLS90VehicleType.car;
import static org.matsim.contrib.noise.RLS90VehicleType.hgv;

public class NoiseDamageCalculation {

    private final static Logger log = Logger.getLogger(NoiseDamageCalculation.class);

    private static final boolean printLog = false;
    private boolean collectNoiseEvents = true;

    private String outputDirectory;


    @Inject
    private NoiseContext noiseContext;

    @Inject private EventsManager events;


    private int cWarn3 = 0;
    private int cWarn4 = 0;

    private List<NoiseEventCaused> noiseEventsCaused = new ArrayList<NoiseEventCaused>();
    private List<NoiseEventAffected> noiseEventsAffected = new ArrayList<NoiseEventAffected>();

    private double totalCausedNoiseCost = 0.;
    private double totalAffectedNoiseCost = 0.;

    private int iteration;

    void reset(int iteration) {
        this.iteration = iteration;
        this.totalCausedNoiseCost = 0.;
        this.totalAffectedNoiseCost = 0.;
        this.noiseEventsCaused.clear();
        this.noiseEventsAffected.clear();
    }


    void calculateDamages(NoiseReceiverPoint rp, NoiseReceiverPointImmision immisions) {
        if (this.noiseContext.getNoiseParams().isComputePopulationUnits()) {
            calculateAffectedAgentUnits(rp);
            if (this.noiseContext.getNoiseParams().isComputeNoiseDamages()) {
                calculateDamagePerReceiverPoint(rp);
            }
            if (this.noiseContext.getNoiseParams().isComputeCausingAgents()) {
                computeAverageDamageCost(rp, immisions);
                calculateMarginalDamageCost(rp, immisions);
            }
        }
        calculateCostsPerVehiclePerLinkPerTimeInterval();
    }

    private void calculateAffectedAgentUnits(NoiseReceiverPoint rp) {

        double affectedAgentUnits = 0.;
        if (!(rp.getPersonId2actInfos().isEmpty())) {

            for (Id<Person> personId : rp.getPersonId2actInfos().keySet()) {

                for (PersonActivityInfo actInfo : rp.getPersonId2actInfos().get(personId)) {
                    double unitsThisPersonActivityInfo = actInfo.getDurationWithinInterval(noiseContext.getCurrentTimeBinEndTime(), noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation()) / noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation();
                    affectedAgentUnits = affectedAgentUnits + ( unitsThisPersonActivityInfo * noiseContext.getNoiseParams().getScaleFactor() );
                }
            }
        }
        rp.setAffectedAgentUnits(affectedAgentUnits);
    }

    void finishNoiseDamageCosts() {

        if (writeOutput()) {
            NoiseWriter.writeDamageInfoPerHour(noiseContext, outputDirectory);
        }

        if (this.noiseContext.getNoiseParams().isThrowNoiseEventsAffected()) {

            if (printLog) {
                log.info("Throwing noise events for the affected agents...");
            }
            throwNoiseEventsAffected();
            if (printLog) {
                log.info("Throwing noise events for the affected agents... Done.");
            }
        }

        if (this.noiseContext.getNoiseParams().isComputeCausingAgents()) {
            calculateCostsPerVehiclePerLinkPerTimeInterval();
            if (writeOutput()) {
                NoiseWriter.writeLinkDamageInfoPerHour(noiseContext, outputDirectory);
            }
            if (writeOutput()) {
                NoiseWriter.writeLinkAvgCarDamageInfoPerHour(noiseContext, outputDirectory);
            }
            if (writeOutput()) {
                NoiseWriter.writeLinkAvgHgvDamageInfoPerHour(noiseContext, outputDirectory);
            }

            if (this.noiseContext.getNoiseParams().isThrowNoiseEventsCaused()) {
                if (printLog) {
                    log.info("Throwing noise events for the causing agents...");
                }
                throwNoiseEventsCaused();
                if (printLog) {
                    log.info("Throwing noise events for the causing agents... Done.");
                }

                if (this.noiseContext.getNoiseParams().isComputeAvgNoiseCostPerLinkAndTime()) {
                    this.noiseContext.storeTimeInterval();
                }
            }
        }
    }


    /*
     * Damage cost for each receiver point
     */
    private void calculateDamagePerReceiverPoint(NoiseReceiverPoint rp) {
        double currentTimeBinEndTime = this.noiseContext.getCurrentTimeBinEndTime();
        double annualCostRate = this.noiseContext.getNoiseParams().getAnnualCostRate();
        double timeBinsSize = this.noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation();

//		for (NoiseReceiverPoint rp : this.noiseContext.getReceiverPoints().values()) {

        double noiseImmission = rp.getCurrentImmission();
        double affectedAgentUnits = rp.getAffectedAgentUnits();

        double damageCost = NoiseEquations.calculateDamageCosts(noiseImmission, affectedAgentUnits, currentTimeBinEndTime, annualCostRate, timeBinsSize);
        double damageCostPerAffectedAgentUnit = NoiseEquations.calculateDamageCosts(noiseImmission, 1., currentTimeBinEndTime, annualCostRate, timeBinsSize);

        rp.setDamageCosts(damageCost);
        rp.setDamageCostsPerAffectedAgentUnit(damageCostPerAffectedAgentUnit);
//		}
    }

    private void throwNoiseEventsAffected() {
        double currentTimeBinEndTime = this.noiseContext.getCurrentTimeBinEndTime();
        double eventTime = this.noiseContext.getEventTime();
        double timeBinSizeNoiseComputation = this.noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation();

        for (NoiseReceiverPoint rp : this.noiseContext.getReceiverPoints().values()) {

            if (!(rp.getPersonId2actInfos().isEmpty())) {

                for (Id<Person> personId : rp.getPersonId2actInfos().keySet()) {

                    for (PersonActivityInfo actInfo : rp.getPersonId2actInfos().get(personId)) {

                        double factor = actInfo.getDurationWithinInterval(currentTimeBinEndTime, timeBinSizeNoiseComputation) /  timeBinSizeNoiseComputation;
                        double amount = factor * rp.getDamageCostsPerAffectedAgentUnit();

                        if (amount != 0.) {
                            NoiseEventAffected noiseEventAffected = new NoiseEventAffected(eventTime, currentTimeBinEndTime, personId, amount, rp.getId(), actInfo.getActivityType());
                            events.processEvent(noiseEventAffected);

                            if (this.collectNoiseEvents) {
                                this.noiseEventsAffected.add(noiseEventAffected);
                            }

                            totalAffectedNoiseCost = totalAffectedNoiseCost + amount;
                        }
                    }
                }
            }
        }
    }

    /*
     * Noise allocation approach: AverageCost
     */
    private void computeAverageDamageCost(NoiseReceiverPoint rp, NoiseReceiverPointImmision immisions) {
        calculateCostSharesPerLinkPerTimeInterval(rp, immisions);
//		calculateCostsPerVehiclePerLinkPerTimeInterval();
    }

    /*
     * Noise allocation approach: AverageCost
     */
    private void calculateCostSharesPerLinkPerTimeInterval(NoiseReceiverPoint rp, NoiseReceiverPointImmision immisions) {

        Map<Id<Link>, Double> linkId2costShare = new HashMap<Id<Link>, Double>();

//		for (NoiseReceiverPoint rp : this.noiseContext.getReceiverPoints().values()) {


        if (rp.getDamageCosts() != 0.) {
            for (Id<Link> linkId : immisions.getLinkId2IsolatedImmission().keySet()) {

                double noiseImmission = immisions.getLinkId2IsolatedImmission().get(linkId);
                double costs = 0.;

                if (!(noiseImmission == 0.)) {
                    double costShare = NoiseEquations.calculateShareOfResultingNoiseImmission(noiseImmission, rp.getCurrentImmission());
                    costs = costShare * rp.getDamageCosts();
                }
                linkId2costShare.put(linkId, costs);
            }
        }

//		}

        // summing up the link-based costs
//		for (NoiseReceiverPoint rp : this.noiseContext.getReceiverPoints().values()) {

        if (rp.getDamageCosts() != 0.) {

            for (Id<Link> linkId : rp.getLinkId2distanceCorrection().keySet()) {
                NoiseLink noiseLink = this.noiseContext.getNoiseLinks().get(linkId);
                if ( noiseLink != null) {
                    double sum = noiseLink.getDamageCost() + linkId2costShare.get(linkId);
                    noiseLink.setDamageCost(sum);
                }
            }
        }
//		}
    }

    /*
     * Noise allocation approach: AverageCost
     */
    private void calculateCostsPerVehiclePerLinkPerTimeInterval() {

        for (Id<Link> linkId : this.noiseContext.getScenario().getNetwork().getLinks().keySet()) {

            double damageCostPerCar = 0.;
            double damageCostPerHgv = 0.;

            NoiseLink noiseLink = this.noiseContext.getNoiseLinks().get(linkId);

            double damageCostSum = 0.;
            int nCarAgents = 0;
            int nHdvAgents = 0;

            if (noiseLink != null) {
                damageCostSum = noiseLink.getDamageCost();
                nCarAgents = noiseLink.getAgentsEntering(car.getId());
                nHdvAgents = noiseLink.getAgentsEntering(hgv.getId());
            }

            Tuple<Double, Double> vCarVHdv = getV(linkId, noiseLink);
            double vCar = vCarVHdv.getFirst();
            double vHdv = vCarVHdv.getSecond();

            double lCar = RLS90NoiseComputation.calculateLCar(vCar);
            double lHdv = RLS90NoiseComputation.calculateLHdv(vHdv);

            double shareCar = 0.;
            double shareHdv = 0.;

            if ((nCarAgents > 0) || (nHdvAgents > 0)) {
                shareCar = NoiseEquations.calculateShare(nCarAgents, lCar, nHdvAgents, lHdv);
                shareHdv = NoiseEquations.calculateShare(nHdvAgents, lHdv, nCarAgents, lCar);
            }

            double damageCostSumCar = shareCar * damageCostSum;
            double damageCostSumHdv = shareHdv * damageCostSum;

            NoiseConfigGroup noiseParams = this.noiseContext.getNoiseParams();
            if (!(nCarAgents == 0)) {
                damageCostPerCar = damageCostSumCar / (nCarAgents * noiseParams.getScaleFactor());
            }

            if (!(nHdvAgents == 0)) {
                damageCostPerHgv = damageCostSumHdv / (nHdvAgents * noiseParams.getScaleFactor());
            }

            if (damageCostPerCar > 0.) {
                noiseLink.setAverageDamageCostPerVehicle(car.getId(), damageCostPerCar);
            }
            if (damageCostPerHgv > 0.) {
                noiseLink.setAverageDamageCostPerVehicle(hgv.getId(), damageCostPerHgv);
            }
        }
    }

    private Tuple<Double, Double> getV(Id<Link> linkId, NoiseLink noiseLink) {
        Link link = noiseContext.getScenario().getNetwork().getLinks().get(linkId);

        double vCar = (link.getFreespeed()) * 3.6;
        double vHdv = vCar;

        double freespeedCar = vCar;
        final NoiseConfigGroup noiseParams = noiseContext.getNoiseParams();

        if (noiseParams.isUseActualSpeedLevel()) {

            // use the actual speed level if possible
            if (noiseLink != null) {

                // Car
                if (noiseLink.getTravelTime_sec(car.getId()) == 0.
                        || noiseLink.getAgentsLeaving(car.getId()) == 0) {
                    // use the maximum speed level

                } else {
                    double averageTravelTimeCar_sec =
                            noiseLink.getTravelTime_sec(car.getId()) / noiseLink.getAgentsLeaving(car.getId());
                    vCar = 3.6 * (link.getLength() / averageTravelTimeCar_sec );
                }

                // HGV
                if (noiseLink.getTravelTime_sec(hgv.getId()) == 0. || noiseLink.getAgentsLeaving(hgv.getId()) == 0) {
                    // use the actual car speed level
                    vHdv = vCar;

                } else {
                    double averageTravelTimeHGV_sec = noiseLink.getTravelTime_sec(hgv.getId()) / noiseLink.getAgentsLeaving(hgv.getId());
                    vHdv = 3.6 * (link.getLength() / averageTravelTimeHGV_sec );
                }
            }
        }

        if (vCar > freespeedCar) {
            throw new RuntimeException(vCar + " > " + freespeedCar + ". This should not be possible. Aborting...");
        }

        if (!noiseParams.isAllowForSpeedsOutsideTheValidRange()) {

            // shifting the speed into the allowed range defined by the RLS-90 computation approach

            if (vCar < 30.) {
                vCar = 30.;
            }

            if (vHdv < 30.) {
                vHdv = 30.;
            }

            if (vCar > 130.) {
                vCar = 130.;
            }

            if (vHdv > 80.) {
                vHdv = 80.;
            }
        }
        return new Tuple<>(vCar, vHdv);
    }

    //	/*
//	 * Noise allocation approach: MarginalCost
//	 */
//	private void computeMarginalDamageCost() {
//
//		// For each receiver point we have something like:
//		// Immission_linkA(n)
//		// Immission_linkA(n-1)
//		// Immission_linkB(n)
//		// Immission_linkB(n-1)
//		// Immission_linkC(n)
//		// Immission_linkC(n-1)
//		// ...
//
//		// resultingImmission = computeResultingImmission(Immission_linkA(n), Immission_linkB(n), Immission_linkC(n), ...)
//
//		// MarginalCostCar_linkA = damageCost(resultingImmission) - damageCost(X)
//		// X = computeResultingImmission(Immission_linkA(n-1), Immission_linkB(n), Immission_linkC(n), ...)
//
//		// MarginalCostCar_linkB = damageCost(resultingImmission) - damageCost(Y)
//		// Y = computeResultingImmission(Immission_linkA(n), Immission_linkB(n-1), Immission_linkC(n), ...)
//
//		if (printLog) log.info("Computing the marginal damage cost for each link and receiver point...");
//		calculateMarginalDamageCost();
//		if (writeOutput()) NoiseWriter.writeLinkMarginalCarDamageInfoPerHour(noiseContext, outputDirectory);
//		if (writeOutput()) NoiseWriter.writeLinkMarginalHgvDamageInfoPerHour(noiseContext, outputDirectory);
//		if (printLog) log.info("Computing the marginal damage cost for each link and receiver point... Done.");
//	}

    /*
     * Noise allocation approach: MarginalCost
     */
    private void calculateMarginalDamageCost(NoiseReceiverPoint rp, NoiseReceiverPointImmision immision) {
//		for (NoiseReceiverPoint rp : this.noiseContext.getReceiverPoints().values()) {

        if (rp.getAffectedAgentUnits() != 0.) {
            for (Id<Link> thisLink : immision.getLinkId2IsolatedImmission().keySet()) {

                double noiseImmissionPlusOneCarThisLink = NoiseEquations.calculateResultingNoiseImmissionPlusOneVehicle(rp.getCurrentImmission(), immision.getLinkId2IsolatedImmission().get(thisLink), immision.getLinkId2IsolatedImmissionPlusOneCar().get(thisLink));
                double noiseImmissionPlusOneHGVThisLink = NoiseEquations.calculateResultingNoiseImmissionPlusOneVehicle(rp.getCurrentImmission(), immision.getLinkId2IsolatedImmission().get(thisLink), immision.getLinkId2IsolatedImmissionPlusOneHGV().get(thisLink));

                double damageCostsPlusOneCarThisLink = NoiseEquations.calculateDamageCosts(noiseImmissionPlusOneCarThisLink, rp.getAffectedAgentUnits(), this.noiseContext.getCurrentTimeBinEndTime(), this.noiseContext.getNoiseParams().getAnnualCostRate(), this.noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation());
                double marginalDamageCostCarThisLink = (damageCostsPlusOneCarThisLink - rp.getDamageCosts()) / this.noiseContext.getNoiseParams().getScaleFactor();

                if (marginalDamageCostCarThisLink < 0.0) {
                    if (Math.abs(marginalDamageCostCarThisLink) < 0.0000000001) {
                        marginalDamageCostCarThisLink = 0.;
                    } else {
                        if (cWarn3 == 0) {
                            log.warn("The marginal damage cost per car on link " + thisLink.toString() + " for receiver point " + rp.getId().toString() + " is " + marginalDamageCostCarThisLink + ".");
                            log.warn("final immission: " + rp.getCurrentImmission() + " - immission plus one car " + noiseImmissionPlusOneCarThisLink + " - marginal damage cost car: " + marginalDamageCostCarThisLink);
                            log.warn("Setting the marginal damage cost per car to 0.");
                            log.warn("This message is only given once.");
                            cWarn3++;
                        }

                        marginalDamageCostCarThisLink = 0.;
                    }
                }

                double damageCostsPlusOneHGVThisLink = NoiseEquations.calculateDamageCosts(noiseImmissionPlusOneHGVThisLink, rp.getAffectedAgentUnits(), this.noiseContext.getCurrentTimeBinEndTime(), this.noiseContext.getNoiseParams().getAnnualCostRate(), this.noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation());
                double marginalDamageCostHGVThisLink = (damageCostsPlusOneHGVThisLink - rp.getDamageCosts()) / this.noiseContext.getNoiseParams().getScaleFactor();

                if (marginalDamageCostHGVThisLink < 0.0) {
                    if (Math.abs(marginalDamageCostHGVThisLink) < 0.0000000001) {
                        marginalDamageCostHGVThisLink = 0.;
                    } else {
                        if (cWarn4 == 0) {
                            log.warn("The marginal damage cost per HGV on link " + thisLink.toString() + " for receiver point " + rp.getId().toString() + " is " + marginalDamageCostHGVThisLink + ".");
                            log.warn("final immission: " + rp.getCurrentImmission() + " - immission plus one car " + noiseImmissionPlusOneCarThisLink + " - marginal damage cost car: " + marginalDamageCostHGVThisLink);
                            log.warn("Setting the marginal damage cost per HGV to 0.");
                            log.warn("This message is only given once.");
                            cWarn4++;
                        }

                        marginalDamageCostHGVThisLink = 0.;
                    }
                }
                NoiseLink noiseLink = this.noiseContext.getNoiseLinks().get(thisLink);
                double marginalDamageCostCarSum = noiseLink.getMarginalDamageCostPerVehicle(car.getId()) + marginalDamageCostCarThisLink;
                noiseLink.setMarginalDamageCostPerVehicle(car.getId(), marginalDamageCostCarSum);

                double marginalDamageCostHGVSum = noiseLink.getMarginalDamageCostPerVehicle(hgv.getId()) + marginalDamageCostHGVThisLink;
                noiseLink.setMarginalDamageCostPerVehicle(hgv.getId(), marginalDamageCostHGVSum);
            }
        }
//		}
    }

    private void throwNoiseEventsCaused() {
        String[] hgvPrefixes = this.noiseContext.getNoiseParams().getHgvIdPrefixesArray();
        Set<Id<Vehicle>> busVehicleIds = this.noiseContext.getBusVehicleIDs();
        double eventTime = this.noiseContext.getEventTime();
        double currentTimeBinEndTime = this.noiseContext.getCurrentTimeBinEndTime();
        NoiseConfigGroup.NoiseAllocationApproach noiseAllocationApproach = this.noiseContext.getNoiseParams().getNoiseAllocationApproach();

        for (Id<Link> linkId : this.noiseContext.getScenario().getNetwork().getLinks().keySet()) {
            NoiseLink noiseLink = this.noiseContext.getNoiseLinks().get(linkId);
            if (noiseLink != null) {

                double amountCar = 0.;
                double amountHdv = 0.;

                if (noiseAllocationApproach == NoiseConfigGroup.NoiseAllocationApproach.AverageCost) {
                    amountCar = noiseLink.getAverageDamageCostPerVehicle(car.getId());
                    amountHdv = noiseLink.getAverageDamageCostPerVehicle(hgv.getId());

                } else if (noiseAllocationApproach == NoiseConfigGroup.NoiseAllocationApproach.MarginalCost) {
                    amountCar = noiseLink.getMarginalDamageCostPerVehicle(car.getId());
                    amountHdv = noiseLink.getMarginalDamageCostPerVehicle(hgv.getId());

                } else {
                    throw new RuntimeException("Unknown noise allocation approach. Aborting...");
                }

                for(Id<Vehicle> vehicleId : noiseLink.getEnteringVehicleIds()) {

                    double amount = 0.;

                    boolean isHGV = false;
                    for (String hgvPrefix : hgvPrefixes) {
                        if (vehicleId.toString().startsWith(hgvPrefix)) {
                            isHGV = true;
                            break;
                        }
                    }

                    if(isHGV || busVehicleIds.contains(vehicleId)) {
                        amount = amountHdv;
                    } else {
                        amount = amountCar;
                    }

                    if (amount != 0.) {

                        if (this.noiseContext.getNotConsideredTransitVehicleIDs().contains(vehicleId)) {
                            // skip
                        } else {
                            NoiseEventCaused noiseEvent = new NoiseEventCaused(
                                    eventTime,
                                    currentTimeBinEndTime,
                                    this.noiseContext.getLinkId2vehicleId2lastEnterTime().get(linkId).get(vehicleId),
                                    this.noiseContext.getVehicleId2PersonId().get(vehicleId),
                                    vehicleId, amount, linkId);
                            events.processEvent(noiseEvent);

                            if (this.collectNoiseEvents) {
                                this.noiseEventsCaused.add(noiseEvent);
                            }

                            totalCausedNoiseCost = totalCausedNoiseCost + amount;
                        }
                    }
                }
            }
        }
    }

    private boolean writeOutput() {
        if (this.noiseContext.getNoiseParams().getWriteOutputIteration() == 0) {
            return false;
        } else if (this.iteration % this.noiseContext.getNoiseParams().getWriteOutputIteration() == 0) {
            return true;
        } else {
            return false;
        }
    }

    List<NoiseEventCaused> getNoiseEventsCaused() {
        return noiseEventsCaused;
    }

    List<NoiseEventAffected> getNoiseEventsAffected() {
        return noiseEventsAffected;
    }

    public double getTotalCausedNoiseCost() {
        return totalCausedNoiseCost;
    }

    public double getTotalAffectedNoiseCost() {
        return totalAffectedNoiseCost;
    }

    public void setEvents(EventsManager events) {
        this.events = events;
    }

    public void setOutputFilePath(String outputFilePath) {
        this.outputDirectory = outputFilePath;
    }
}
