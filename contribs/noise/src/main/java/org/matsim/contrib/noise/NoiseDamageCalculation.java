package org.matsim.contrib.noise;

import com.google.common.collect.Range;
import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class NoiseDamageCalculation {

    private final static Logger log = Logger.getLogger(NoiseDamageCalculation.class);

    private enum DayTime {NIGHT, DAY, EVENING}

    private static final boolean printLog = false;
    private boolean collectNoiseEvents = true;

    private String outputDirectory;

    @Inject
    private NoiseContext noiseContext;

    @Inject private EventsManager events;

    @Inject
    private NoiseEmission emission;

    @Inject
    private NoiseVehicleIdentifier noiseVehicleIdentifier;

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


    void calculateDamages(NoiseReceiverPoint rp) {
        if (this.noiseContext.getNoiseParams().isComputePopulationUnits()) {
            calculateAffectedAgentUnits(rp);
            if (this.noiseContext.getNoiseParams().isComputeNoiseDamages()) {
                calculateDamagePerReceiverPoint(rp);
            }
            if (this.noiseContext.getNoiseParams().isComputeCausingAgents()) {
                computeAverageDamageCost(rp);
                calculateMarginalDamageCost(rp);
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

        final NoiseConfigGroup noiseParams = this.noiseContext.getNoiseParams();
        if (noiseParams.isThrowNoiseEventsAffected()) {

            if (printLog) {
                log.info("Throwing noise events for the affected agents...");
            }
            throwNoiseEventsAffected();
            if (printLog) {
                log.info("Throwing noise events for the affected agents... Done.");
            }
        }

        if (noiseParams.isComputeCausingAgents()) {
            calculateCostsPerVehiclePerLinkPerTimeInterval();
            if (writeOutput()) {
                NoiseWriter.writeLinkDamageInfoPerHour(noiseContext, outputDirectory);
            }
            if (writeOutput()) {
                for(NoiseVehicleType vehicleType: noiseParams.getNoiseComputationMethod().noiseVehiclesTypes) {
                    NoiseWriter.writeLinkAvgDamagePerVehicleTypeInfoPerHour(noiseContext, outputDirectory, vehicleType);
                }
            }

            if (noiseParams.isThrowNoiseEventsCaused()) {
                if (printLog) {
                    log.info("Throwing noise events for the causing agents...");
                }
                throwNoiseEventsCaused();
                if (printLog) {
                    log.info("Throwing noise events for the causing agents... Done.");
                }

                if (noiseParams.isComputeAvgNoiseCostPerLinkAndTime()) {
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

        double noiseImmission = rp.getCurrentImmission().immission;
        double affectedAgentUnits = rp.getAffectedAgentUnits();

        double damageCost = calculateDamageCosts(noiseImmission, affectedAgentUnits, currentTimeBinEndTime, annualCostRate, timeBinsSize);
        double damageCostPerAffectedAgentUnit = calculateDamageCosts(noiseImmission, 1., currentTimeBinEndTime, annualCostRate, timeBinsSize);

        rp.setDamageCosts(damageCost);
        rp.setDamageCostsPerAffectedAgentUnit(damageCostPerAffectedAgentUnit);
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
    private void computeAverageDamageCost(NoiseReceiverPoint rp) {
        calculateCostSharesPerLinkPerTimeInterval(rp);
//		calculateCostsPerVehiclePerLinkPerTimeInterval();
    }

    /*
     * Noise allocation approach: AverageCost
     */
    private void calculateCostSharesPerLinkPerTimeInterval(NoiseReceiverPoint rp) {

        Map<Id<Link>, Double> linkId2costShare = new HashMap<Id<Link>, Double>();

//		for (NoiseReceiverPoint rp : this.noiseContext.getReceiverPoints().values()) {


        if (rp.getDamageCosts() != 0.) {
            for (Id<Link> linkId : rp.getCurrentImmission().getLinkId2IsolatedImmission().keySet()) {

                double linkImmission = rp.getCurrentImmission().getLinkId2IsolatedImmission().get(linkId);
                double costs = 0.;

                if (!(linkImmission == 0.)) {
                    double costShare = NoiseEquations.calculateShareOfResultingNoiseImmission(linkImmission, rp.getCurrentImmission().immission);
                    costs = costShare * rp.getDamageCosts();
                }
                linkId2costShare.put(linkId, costs);
            }
        }

//		}

        // summing up the link-based costs
//		for (NoiseReceiverPoint rp : this.noiseContext.getReceiverPoints().values()) {

        if (rp.getDamageCosts() != 0.) {

            for (Id<Link> linkId : rp.getRelevantLinks()) {
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

        final NoiseVehicleType[] noiseVehiclesTypes = noiseContext.getNoiseParams().getNoiseComputationMethod().noiseVehiclesTypes;
        for (Id<Link> linkId : this.noiseContext.getScenario().getNetwork().getLinks().keySet()) {
            NoiseLink noiseLink = this.noiseContext.getNoiseLinks().get(linkId);
            if (noiseLink != null) {
                double damageCostSum = noiseLink.getDamageCost();

                int[] counts = new int[noiseVehiclesTypes.length];
                double[] levels = new double[noiseVehiclesTypes.length];

                for(int i = 0; i < noiseVehiclesTypes.length; i++) {
                    NoiseVehicleType type = noiseVehiclesTypes[i];
                    counts[i] = noiseLink.getAgentsEntering(type.getId());
                    levels[i] = emission.calculateSingleVehicleLevel(type, noiseLink);
                }

                double[] shares = NoiseEquations.calculateShare(counts, levels);

                NoiseConfigGroup noiseParams = this.noiseContext.getNoiseParams();

                for(int i = 0; i < noiseVehiclesTypes.length; i++) {
                    NoiseVehicleType type = noiseVehiclesTypes[i];
                    double damageCostSumVehicle = shares[i] * damageCostSum;
                    if (!(counts[i] == 0)) {
                        double damageCostPerVehicle = damageCostSumVehicle / (counts[i] * noiseParams.getScaleFactor());
                        if (damageCostPerVehicle > 0.) {
                            noiseLink.setAverageDamageCostPerVehicle(type.getId(), damageCostPerVehicle);
                        }
                    }
                }
            }
        }
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
    private void calculateMarginalDamageCost(NoiseReceiverPoint rp) {

        if (rp.getAffectedAgentUnits() != 0.) {
            for (Id<Link> thisLink : rp.getCurrentImmission().getLinkId2IsolatedImmission().keySet()) {
                for(NoiseVehicleType type: noiseContext.getNoiseParams().getNoiseComputationMethod().noiseVehiclesTypes) {
                    double noiseImmissionPlusOneVehicleThisLink = NoiseEquations.calculateResultingNoiseImmissionPlusOneVehicle(rp.getCurrentImmission().immission, rp.getCurrentImmission().getLinkId2IsolatedImmission().get(thisLink), rp.getCurrentImmission().getLinkId2IsolatedImmissionPlusOneVehicle().get(type.getId()).get(thisLink));
                    double damageCostsPlusOneVehicleThisLink = calculateDamageCosts(noiseImmissionPlusOneVehicleThisLink, rp.getAffectedAgentUnits(), this.noiseContext.getCurrentTimeBinEndTime(), this.noiseContext.getNoiseParams().getAnnualCostRate(), this.noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation());
                    double marginalDamageCostVehicleThisLink = (damageCostsPlusOneVehicleThisLink - rp.getDamageCosts()) / this.noiseContext.getNoiseParams().getScaleFactor();

                    if (marginalDamageCostVehicleThisLink < 0.0) {
                        if (Math.abs(marginalDamageCostVehicleThisLink) < 0.0000000001) {
                            marginalDamageCostVehicleThisLink = 0.;
                        } else {
                            if (cWarn3 == 0) {
                                log.warn("The marginal damage cost per car on link " + thisLink.toString() + " for receiver point " + rp.getId().toString() + " is " + marginalDamageCostVehicleThisLink + ".");
                                log.warn("final immission: " + rp.getCurrentImmission() + " - immission plus one car " + noiseImmissionPlusOneVehicleThisLink + " - marginal damage cost car: " + marginalDamageCostVehicleThisLink);
                                log.warn("Setting the marginal damage cost per car to 0.");
                                log.warn("This message is only given once.");
                                cWarn3++;
                            }

                            marginalDamageCostVehicleThisLink = 0.;
                        }
                    }
                    NoiseLink noiseLink = this.noiseContext.getNoiseLinks().get(thisLink);
                    double marginalDamageCostCarSum = noiseLink.getMarginalDamageCostPerVehicle(type.getId()) + marginalDamageCostVehicleThisLink;
                    noiseLink.setMarginalDamageCostPerVehicle(type.getId(), marginalDamageCostCarSum);
                }
            }
        }
    }

    private void throwNoiseEventsCaused() {
        double eventTime = this.noiseContext.getEventTime();
        double currentTimeBinEndTime = this.noiseContext.getCurrentTimeBinEndTime();
        NoiseConfigGroup.NoiseAllocationApproach noiseAllocationApproach = this.noiseContext.getNoiseParams().getNoiseAllocationApproach();

        for (Id<Link> linkId : this.noiseContext.getScenario().getNetwork().getLinks().keySet()) {
            NoiseLink noiseLink = this.noiseContext.getNoiseLinks().get(linkId);
            if (noiseLink != null) {

                for (Id<Vehicle> vehicleId : noiseLink.getEnteringVehicleIds()) {

                    final Id<NoiseVehicleType> noiseVehicleTypeId = noiseVehicleIdentifier.identifyVehicle(vehicleId);
                    double amountVehicle;

                    if (noiseAllocationApproach == NoiseConfigGroup.NoiseAllocationApproach.AverageCost) {
                        amountVehicle = noiseLink.getAverageDamageCostPerVehicle(noiseVehicleTypeId);
                    } else if (noiseAllocationApproach == NoiseConfigGroup.NoiseAllocationApproach.MarginalCost) {
                        amountVehicle = noiseLink.getMarginalDamageCostPerVehicle(noiseVehicleTypeId);
                    } else {
                        throw new RuntimeException("Unknown noise allocation approach. Aborting...");
                    }

                    if (amountVehicle != 0.) {

                        if (this.noiseContext.getNotConsideredTransitVehicleIDs().contains(vehicleId)) {
                            // skip
                        } else {
                            NoiseEventCaused noiseEvent = new NoiseEventCaused(
                                    eventTime,
                                    currentTimeBinEndTime,
                                    this.noiseContext.getLinkId2vehicleId2lastEnterTime().get(linkId).get(vehicleId),
                                    this.noiseContext.getVehicleId2PersonId().get(vehicleId),
                                    vehicleId, amountVehicle, linkId);
                            events.processEvent(noiseEvent);

                            if (this.collectNoiseEvents) {
                                this.noiseEventsCaused.add(noiseEvent);
                            }
                            totalCausedNoiseCost = totalCausedNoiseCost + amountVehicle;
                        }
                    }
                }
            }
        }
    }

    static double calculateDamageCosts(double noiseImmission, double affectedAgentUnits, double timeInterval, double annualCostRate, double timeBinSize) {

        DayTime daytimeType = DayTime.NIGHT;

        if (timeInterval > 6 * 3600 && timeInterval <= 18 * 3600) {
            daytimeType = DayTime.DAY;
        } else if (timeInterval > 18 * 3600 && timeInterval <= 22 * 3600) {
            daytimeType = DayTime.EVENING;
        }

        double lautheitsgewicht = 0;

        switch (daytimeType) {
            case DAY:
                if (noiseImmission >= 50) {
                    lautheitsgewicht = Math.pow(2.0 , 0.1 * (noiseImmission - 50));
                }
                break;
            case EVENING:
                if (noiseImmission >= 45) {
                    lautheitsgewicht = Math.pow(2.0 , 0.1 * (noiseImmission - 45));
                }
                break;
            case NIGHT:
                if (noiseImmission >= 40) {
                    lautheitsgewicht = Math.pow(2.0 , 0.1 * (noiseImmission - 40));
                }
                break;
            default:
        }


        double laermEinwohnerGleichwert = lautheitsgewicht * affectedAgentUnits;
        double damageCosts = ( annualCostRate * laermEinwohnerGleichwert / 365. ) * ( timeBinSize / (24.0 * 3600) );

        return damageCosts;
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
