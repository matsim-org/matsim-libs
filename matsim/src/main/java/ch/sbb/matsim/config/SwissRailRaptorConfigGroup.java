/* *********************************************************************** *
 * project: org.matsim.* 												   *
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package ch.sbb.matsim.config;

import com.google.common.base.Verify;

import ch.sbb.matsim.routing.pt.raptor.RaptorStaticConfig.RaptorTransferCalculation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup.HandlingOfPlansWithoutRoutingMode;
import org.matsim.core.utils.collections.CollectionUtils;

/**
 * @author mrieser / SBB
 */
public class SwissRailRaptorConfigGroup extends ReflectiveConfigGroup {

    public static final String GROUP = "swissRailRaptor";

    private static final String PARAM_USE_RANGE_QUERY = "useRangeQuery";
    private static final String PARAM_USE_INTERMODAL_ACCESS_EGRESS = "useIntermodalAccessEgress";
    private static final String PARAM_INTERMODAL_ACCESS_EGRESS_MODE_SELECTION = "intermodalAccessEgressModeSelection";
    private static final String PARAM_INTERMODAL_ACCESS_EGRESS_MODE_SELECTION_DESC = "Sets whether intermodal access and egress modes are selected by " +
            "least cost (default) or randomly chosen out of the available access / egress modes.";
    private static final String PARAM_USE_MODE_MAPPING = "useModeMappingForPassengers";
    private static final String PARAM_SCORING_PARAMETERS = "scoringParameters";
    private static final String PARAM_TRANSFER_PENALTY_BASE = "transferPenaltyBaseCost";
    private static final String PARAM_TRANSFER_PENALTY_MIN = "transferPenaltyMinCost";
    private static final String PARAM_TRANSFER_PENALTY_MAX = "transferPenaltyMaxCost";
    private static final String PARAM_TRANSFER_PENALTY_PERHOUR = "transferPenaltyCostPerTravelTimeHour";

    private static final String PARAM_USE_CAPACITY_CONSTRAINTS = "useCapacityConstraints";
    private static final String PARAM_USE_CAPACITY_CONSTRAINTS_DESC = "If true, SwissRailRaptor tries to detect when agents cannot board a vehicle in the previous iteration because it is already full and tries to find an alternative route instead.";

    private static final String PARAM_TRANSFER_WALK_MARGIN = "transferWalkMargin";
    private static final String PARAM_TRANSFER_WALK_MARGIN_DESC = "time deducted from transfer walk leg during transfers between pt legs in order to avoid missing a vehicle by a few seconds due to delays.";
    private static final String PARAM_INTERMODAL_LEG_ONLYHANDLING = "intermodalLegOnlyHandling";
    private static final String PARAM_INTERMODAL_LEG_ONLYHANDLING_DESC = "Define how routes containing only intermodal legs are handled: Useful options: alllow, avoid, forbid";
    private static final String PARAM_TRANSFER_CALCULATION = "transferCalculation";
    private static final String PARAM_TRANFER_CALCULATION_DESC = "Defines whether all potential transfers are precomputed at the beginning of the simulation (Initial) or whether they are constructed on-demand when needed (Adaptive). The former incurs potentially long up-front caclulations, but quicker routing. The latter avoids any initial computation, but may require longer routing time. Additionally, you may use Online, which will not cache adaptively calculated transfers. This will lead to largely reduced memory use, but drastically increased routing times.";

    private boolean useRangeQuery = false;
    private boolean useIntermodality = false;
    private IntermodalAccessEgressModeSelection intermodalAccessEgressModeSelection = IntermodalAccessEgressModeSelection.CalcLeastCostModePerStop;
    private boolean useModeMapping = false;
    private boolean useCapacityConstraints = false;

    private double transferPenaltyBaseCost = 0;
    private double transferPenaltyMinCost = Double.NEGATIVE_INFINITY;
    private double transferPenaltyMaxCost = Double.POSITIVE_INFINITY;
    private double transferPenaltyHourlyCost = 0;
    private double transferWalkMargin = 5;
	private IntermodalLegOnlyHandling intermodalLegOnlyHandling = IntermodalLegOnlyHandling.forbid;
	private RaptorTransferCalculation transferCalculation = RaptorTransferCalculation.Initial;

    private ScoringParameters scoringParameters = ScoringParameters.Default;

    private final Map<String, RangeQuerySettingsParameterSet> rangeQuerySettingsPerSubpop = new HashMap<>();
    private final Map<String, RouteSelectorParameterSet> routeSelectorPerSubpop = new HashMap<>();
    private final List<IntermodalAccessEgressParameterSet> intermodalAccessEgressSettings = new ArrayList<>();
    private final List<ModeToModeTransferPenalty> modeToModeTransferPenaltyParameterSets = new ArrayList<>();
    private final Map<String, ModeMappingForPassengersParameterSet> modeMappingForPassengersByRouteMode = new HashMap<>();


    public enum IntermodalAccessEgressModeSelection {
    	CalcLeastCostModePerStop, RandomSelectOneModePerRoutingRequestAndDirection
    }

	public enum IntermodalLegOnlyHandling {
		/**
		 * allows transit routes that only consist of intermodal feeder legs if these have the lowest cost.
		 */
		allow,
		/**
		 * avoids transit routes that only consist of feeder routes, unless no route containing at least one pt leg is found
		 */
		avoid,
		/**
		 * explicitly forbids such routes, tries to find a pt route and returns null if nothing is found
		 */
		forbid,
		/**
		 * mimics the behaviour implemented between 2019 and 2023. Returns null if a purely intermodal route has the lowest cost, does not check if a real pt route exists.
		 */
		@Deprecated
		returnNull
	}

    public enum ScoringParameters {
    	Default, Individual
    }

    public SwissRailRaptorConfigGroup() {
        super(GROUP);
    }

	@StringSetter(PARAM_INTERMODAL_LEG_ONLYHANDLING)
	public void setIntermodalLegOnlyHandling(String intermodalLegOnlyHandling) {
		this.intermodalLegOnlyHandling = IntermodalLegOnlyHandling.valueOf(intermodalLegOnlyHandling);
	}
	public void setIntermodalLegOnlyHandling(IntermodalLegOnlyHandling intermodalLegOnlyHandling) {
		this.intermodalLegOnlyHandling = intermodalLegOnlyHandling;
	}

	@StringGetter(PARAM_INTERMODAL_LEG_ONLYHANDLING)
	public String getIntermodalLegOnlyHandlingString() {
		return intermodalLegOnlyHandling.toString();
	}
	
	public IntermodalLegOnlyHandling getIntermodalLegOnlyHandling() {
		return intermodalLegOnlyHandling;
	}	
	
	@StringSetter(PARAM_TRANSFER_CALCULATION)
	public void setTransferCalculation(RaptorTransferCalculation transferCalculation) {
		this.transferCalculation = transferCalculation;
	}

	@StringGetter(PARAM_TRANSFER_CALCULATION)
	public RaptorTransferCalculation getTransferCalculation() {
		return transferCalculation;
	}

	@StringGetter(PARAM_USE_RANGE_QUERY)
    public boolean isUseRangeQuery() {
        return this.useRangeQuery;
    }

    @StringSetter(PARAM_USE_RANGE_QUERY)
    public void setUseRangeQuery(boolean useRangeQuery) {
        this.useRangeQuery = useRangeQuery;
    }

    @StringGetter(PARAM_USE_INTERMODAL_ACCESS_EGRESS)
    public boolean isUseIntermodalAccessEgress() {
        return this.useIntermodality;
    }

    @StringSetter(PARAM_USE_INTERMODAL_ACCESS_EGRESS)
    public void setUseIntermodalAccessEgress(boolean useIntermodality) {
        this.useIntermodality = useIntermodality;
    }

    @StringGetter(PARAM_INTERMODAL_ACCESS_EGRESS_MODE_SELECTION)
    public IntermodalAccessEgressModeSelection getIntermodalAccessEgressModeSelection() {
        return this.intermodalAccessEgressModeSelection;
    }

    @StringSetter(PARAM_INTERMODAL_ACCESS_EGRESS_MODE_SELECTION)
    public void setIntermodalAccessEgressModeSelection(IntermodalAccessEgressModeSelection intermodalAccessEgressModeSelection) {
        this.intermodalAccessEgressModeSelection = intermodalAccessEgressModeSelection;
    }

    @StringGetter(PARAM_TRANSFER_WALK_MARGIN)
    public double getTransferWalkMargin() {
        return transferWalkMargin;
    }

    @StringSetter(PARAM_TRANSFER_WALK_MARGIN)
    public void setTransferWalkMargin(double transferWalkMargin) {
        this.transferWalkMargin = transferWalkMargin;
    }

    @StringGetter(PARAM_USE_MODE_MAPPING)
    public boolean isUseModeMappingForPassengers() {
        return this.useModeMapping;
    }

    @StringSetter(PARAM_USE_MODE_MAPPING)
    public void setUseModeMappingForPassengers(boolean useModeMapping) {
        this.useModeMapping = useModeMapping;
    }

    @StringGetter(PARAM_USE_CAPACITY_CONSTRAINTS)
    public boolean isUseCapacityConstraints() {
        return this.useCapacityConstraints;
    }

    @StringSetter(PARAM_USE_CAPACITY_CONSTRAINTS)
    public void setUseCapacityConstraints(boolean useCapacityConstraints) {
        this.useCapacityConstraints = useCapacityConstraints;
    }

    @StringGetter(PARAM_SCORING_PARAMETERS)
    public ScoringParameters getScoringParameters() {
        return this.scoringParameters;
    }

    @StringSetter(PARAM_SCORING_PARAMETERS)
    public void setScoringParameters(ScoringParameters scoringParameters) {
        this.scoringParameters = scoringParameters;
    }

    @StringGetter(PARAM_TRANSFER_PENALTY_BASE)
    public double getTransferPenaltyBaseCost() {
        return this.transferPenaltyBaseCost;
    }

    @StringSetter(PARAM_TRANSFER_PENALTY_BASE)
    public void setTransferPenaltyBaseCost(double baseCost) {
        this.transferPenaltyBaseCost = baseCost;
    }

    @StringGetter(PARAM_TRANSFER_PENALTY_MIN)
    public double getTransferPenaltyMinCost() {
        return this.transferPenaltyMinCost;
    }

    @StringSetter(PARAM_TRANSFER_PENALTY_MIN)
    public void setTransferPenaltyMinCost(double minCost) {
        this.transferPenaltyMinCost = minCost;
    }

    @StringGetter(PARAM_TRANSFER_PENALTY_MAX)
    public double getTransferPenaltyMaxCost() {
        return this.transferPenaltyMaxCost;
    }

    @StringSetter(PARAM_TRANSFER_PENALTY_MAX)
    public void setTransferPenaltyMaxCost(double maxCost) {
        this.transferPenaltyMaxCost = maxCost;
    }

    @StringGetter(PARAM_TRANSFER_PENALTY_PERHOUR)
    public double getTransferPenaltyCostPerTravelTimeHour() {
        return this.transferPenaltyHourlyCost;
    }

    @StringSetter(PARAM_TRANSFER_PENALTY_PERHOUR)
    public void setTransferPenaltyCostPerTravelTimeHour(double hourlyCost) {
        this.transferPenaltyHourlyCost = hourlyCost;
    }


	@Override
    public ConfigGroup createParameterSet(String type) {
        return switch (type){
			case RangeQuerySettingsParameterSet.TYPE -> new RangeQuerySettingsParameterSet();
			case RouteSelectorParameterSet.TYPE -> new RouteSelectorParameterSet();
			case IntermodalAccessEgressParameterSet.TYPE -> new IntermodalAccessEgressParameterSet();
			case ModeMappingForPassengersParameterSet.TYPE -> new ModeMappingForPassengersParameterSet();
			case ModeToModeTransferPenalty.TYPE -> new ModeToModeTransferPenalty();
			default -> throw new IllegalArgumentException("Unsupported parameterset-type: " + type);

		};

    }

    @Override
    public void addParameterSet(ConfigGroup set) {
        if (set instanceof RangeQuerySettingsParameterSet) {
            addRangeQuerySettings((RangeQuerySettingsParameterSet) set);
        } else if (set instanceof RouteSelectorParameterSet) {
            addRouteSelector((RouteSelectorParameterSet) set);
        } else if (set instanceof IntermodalAccessEgressParameterSet) {
            addIntermodalAccessEgress((IntermodalAccessEgressParameterSet) set);
        } else if (set instanceof ModeMappingForPassengersParameterSet) {
            addModeMappingForPassengers((ModeMappingForPassengersParameterSet) set);}
		else if (set instanceof ModeToModeTransferPenalty) {
			addModeToModeTransferPenalty((ModeToModeTransferPenalty) set);
        } else {
            throw new IllegalArgumentException("Unsupported parameterset: " + set.getClass().getName());
        }
    }

	public void addModeToModeTransferPenalty(ModeToModeTransferPenalty set) {
		this.modeToModeTransferPenaltyParameterSets.add(set);
		super.addParameterSet(set);

	}

	public List<ModeToModeTransferPenalty> getModeToModeTransferPenaltyParameterSets() {
		return modeToModeTransferPenaltyParameterSets;
	}

	public void addRangeQuerySettings(RangeQuerySettingsParameterSet settings) {
        Set<String> subpops = settings.getSubpopulations();
        if (subpops.isEmpty()) {
            this.rangeQuerySettingsPerSubpop.put(null, settings);
        } else {
            for (String subpop : subpops) {
                this.rangeQuerySettingsPerSubpop.put(subpop, settings);
            }
        }
        super.addParameterSet(settings);
    }

    public RangeQuerySettingsParameterSet getRangeQuerySettings(String subpopulation) {
        return this.rangeQuerySettingsPerSubpop.get(subpopulation);
    }

    public RangeQuerySettingsParameterSet removeRangeQuerySettings(String subpopulation) {
        RangeQuerySettingsParameterSet paramSet = this.rangeQuerySettingsPerSubpop.remove(subpopulation);
        super.removeParameterSet(paramSet);
        return paramSet;
    }

    public void addRouteSelector(RouteSelectorParameterSet settings) {
        Set<String> subpops = settings.getSubpopulations();
        if (subpops.isEmpty()) {
            this.routeSelectorPerSubpop.put(null, settings);
        } else {
            for (String subpop : subpops) {
                this.routeSelectorPerSubpop.put(subpop, settings);
            }
        }
        super.addParameterSet(settings);
    }

    public RouteSelectorParameterSet getRouteSelector(String subpopulation) {
        return this.routeSelectorPerSubpop.get(subpopulation);
    }

    public RouteSelectorParameterSet removeRouteSelector(String subpopulation) {
        RouteSelectorParameterSet paramSet = this.routeSelectorPerSubpop.remove(subpopulation);
        super.removeParameterSet(paramSet);
        return paramSet;
    }

    public void addIntermodalAccessEgress(IntermodalAccessEgressParameterSet paramSet) {
        this.intermodalAccessEgressSettings.add(paramSet);
        super.addParameterSet(paramSet);
    }

    public List<IntermodalAccessEgressParameterSet> getIntermodalAccessEgressParameterSets() {
        return this.intermodalAccessEgressSettings;
    }

    public void addModeMappingForPassengers(ModeMappingForPassengersParameterSet paramSet) {
        this.modeMappingForPassengersByRouteMode.put(paramSet.getRouteMode(), paramSet);
        super.addParameterSet(paramSet);
    }

    public ModeMappingForPassengersParameterSet getModeMappingForPassengersParameterSet(String routeMode) {
        return this.modeMappingForPassengersByRouteMode.get(routeMode);
    }

    public Collection<ModeMappingForPassengersParameterSet> getModeMappingForPassengers() {
        return this.modeMappingForPassengersByRouteMode.values();
    }


	public static class RangeQuerySettingsParameterSet extends ReflectiveConfigGroup {

        private static final String TYPE = "rangeQuerySettings";

        private static final String PARAM_SUBPOPS = "subpopulations";
        private static final String PARAM_MAX_EARLIER_DEPARTURE = "maxEarlierDeparture_sec";
        private static final String PARAM_MAX_LATER_DEPARTURE = "maxLaterDeparture_sec";

        private final Set<String> subpopulations = new HashSet<>();
        private int maxEarlierDeparture = 600;
        private int maxLaterDeparture = 900;

        public RangeQuerySettingsParameterSet() {
            super(TYPE);
        }

        @StringGetter(PARAM_SUBPOPS)
        public String getSubpopulationsAsString() {
            return CollectionUtils.setToString(this.subpopulations);
        }

        public Set<String> getSubpopulations() {
            return this.subpopulations;
        }

        @StringSetter(PARAM_SUBPOPS)
        public void setSubpopulations(String subpopulation) {
            this.setSubpopulations(CollectionUtils.stringToSet(subpopulation));
        }

        public void setSubpopulations(Set<String> subpopulations) {
            this.subpopulations.clear();
            this.subpopulations.addAll(subpopulations);
        }

        @StringGetter(PARAM_MAX_EARLIER_DEPARTURE)
        public int getMaxEarlierDeparture() {
            return maxEarlierDeparture;
        }

        @StringSetter(PARAM_MAX_EARLIER_DEPARTURE)
        public void setMaxEarlierDeparture(int maxEarlierDeparture) {
            this.maxEarlierDeparture = maxEarlierDeparture;
        }

        @StringGetter(PARAM_MAX_LATER_DEPARTURE)
        public int getMaxLaterDeparture() {
            return maxLaterDeparture;
        }

        @StringSetter(PARAM_MAX_LATER_DEPARTURE)
        public void setMaxLaterDeparture(int maxLaterDeparture) {
            this.maxLaterDeparture = maxLaterDeparture;
        }
    }

    public static class RouteSelectorParameterSet extends ReflectiveConfigGroup {

        private static final String TYPE = "routeSelector";

        private static final String PARAM_SUBPOPS = "subpopulations";
        private static final String PARAM_BETA_TRAVELTIME = "betaTravelTime";
        private static final String PARAM_BETA_DEPARTURETIME = "betaDepartureTime";
        private static final String PARAM_BETA_TRANSFERS = "betaTransferCount";

        private final Set<String> subpopulations = new HashSet<>();
        private double betaTravelTime = 1;
        private double betaDepartureTime = 1;
        private double betaTransfers = 300;

        public RouteSelectorParameterSet() {
            super(TYPE);
        }

        @StringGetter(PARAM_SUBPOPS)
        public String getSubpopulationsAsString() {
            return CollectionUtils.setToString(this.subpopulations);
        }

        public Set<String> getSubpopulations() {
            return this.subpopulations;
        }

        @StringSetter(PARAM_SUBPOPS)
        public void setSubpopulations(String subpopulation) {
            this.setSubpopulations(CollectionUtils.stringToSet(subpopulation));
        }

        public void setSubpopulations(Set<String> subpopulations) {
            this.subpopulations.clear();
            this.subpopulations.addAll(subpopulations);
        }

        @StringGetter(PARAM_BETA_TRAVELTIME)
        public double getBetaTravelTime() {
            return this.betaTravelTime;
        }

        @StringSetter(PARAM_BETA_TRAVELTIME)
        public void setBetaTravelTime(double betaTravelTime) {
            this.betaTravelTime = betaTravelTime;
        }

        @StringGetter(PARAM_BETA_DEPARTURETIME)
        public double getBetaDepartureTime() {
            return betaDepartureTime;
        }

        @StringSetter(PARAM_BETA_DEPARTURETIME)
        public void setBetaDepartureTime(double betaDepartureTime) {
            this.betaDepartureTime = betaDepartureTime;
        }

        @StringGetter(PARAM_BETA_TRANSFERS)
        public double getBetaTransfers() {
            return betaTransfers;
        }

        @StringSetter(PARAM_BETA_TRANSFERS)
        public void setBetaTransfers(double betaTransfers) {
            this.betaTransfers = betaTransfers;
        }
    }

    public static class IntermodalAccessEgressParameterSet extends ReflectiveConfigGroup {

        private static final String TYPE = "intermodalAccessEgress";

        private static final String PARAM_MODE = "mode";
        private static final String PARAM_MAX_RADIUS = "maxRadius";
        private static final String PARAM_INITIAL_SEARCH_RADIUS = "initialSearchRadius";
        private static final String PARAM_SEARCH_EXTENSION_RADIUS = "searchExtensionRadius";
        private static final String PARAM_LINKID_ATTRIBUTE = "linkIdAttribute";
        private static final String PARAM_PERSON_FILTER_ATTRIBUTE = "personFilterAttribute";
        private static final String PARAM_PERSON_FILTER_VALUE = "personFilterValue";
        private static final String PARAM_STOP_FILTER_ATTRIBUTE = "stopFilterAttribute";
        private static final String PARAM_STOP_FILTER_VALUE = "stopFilterValue";
        private static final String PARAM_SHARE_TRIP_SEARCH_RADIUS = "shareTripSearchRadius";

        private String mode;
        private double maxRadius;
        private double initialSearchRadius = Double.NEGATIVE_INFINITY;
        private double searchExtensionRadius = 200;
        private String linkIdAttribute;
        private String personFilterAttribute;
        private String personFilterValue;
        private String stopFilterAttribute;
        private String stopFilterValue;
        private double shareTripSearchRadius = Double.POSITIVE_INFINITY;

        public IntermodalAccessEgressParameterSet() {
            super(TYPE);
        }

        @StringGetter(PARAM_MODE)
        public String getMode() {
            return mode;
        }

        @StringSetter(PARAM_MODE)
        public IntermodalAccessEgressParameterSet setMode(String mode) {
            this.mode = mode;
            return this ;
        }

        @StringGetter(PARAM_MAX_RADIUS)
        public double getMaxRadius() {
            return maxRadius;
        }

        @StringSetter(PARAM_MAX_RADIUS)
        public IntermodalAccessEgressParameterSet setMaxRadius(double maxRadius) {
            this.maxRadius = maxRadius;
            return this ;
        }

        @StringGetter(PARAM_INITIAL_SEARCH_RADIUS)
        public double getInitialSearchRadius() {
            return initialSearchRadius;
        }

        @StringSetter(PARAM_INITIAL_SEARCH_RADIUS)
        public IntermodalAccessEgressParameterSet setInitialSearchRadius(double initialSearchRadius) {
            this.initialSearchRadius = initialSearchRadius;
            return this ;
        }

        @StringGetter(PARAM_SEARCH_EXTENSION_RADIUS)
        public double getSearchExtensionRadius() {
            return searchExtensionRadius;
        }

        @StringSetter(PARAM_SEARCH_EXTENSION_RADIUS)
        public IntermodalAccessEgressParameterSet setSearchExtensionRadius(double searchExtensionRadius) {
            this.searchExtensionRadius = searchExtensionRadius;
            return this ;
        }

        @StringGetter(PARAM_LINKID_ATTRIBUTE)
        public String getLinkIdAttribute() {
            return linkIdAttribute;
        }

        @StringSetter(PARAM_LINKID_ATTRIBUTE)
        public IntermodalAccessEgressParameterSet setLinkIdAttribute(String linkIdAttribute) {
            this.linkIdAttribute = linkIdAttribute;
            return this ;
        }

        @StringGetter(PARAM_PERSON_FILTER_ATTRIBUTE)
        public String getPersonFilterAttribute() {
            return this.personFilterAttribute;
        }

        @StringSetter(PARAM_PERSON_FILTER_ATTRIBUTE)
        public IntermodalAccessEgressParameterSet setPersonFilterAttribute(String personFilterAttribute) {
            this.personFilterAttribute = personFilterAttribute;
            return this ;
        }

        @StringGetter(PARAM_PERSON_FILTER_VALUE)
        public String getPersonFilterValue() {
            return this.personFilterValue;
        }

        @StringSetter(PARAM_PERSON_FILTER_VALUE)
        public IntermodalAccessEgressParameterSet setPersonFilterValue(String personFilterValue) {
            this.personFilterValue = personFilterValue;
            return this ;
        }

        @StringGetter(PARAM_STOP_FILTER_ATTRIBUTE)
        public String getStopFilterAttribute() {
            return stopFilterAttribute;
        }

        @StringSetter(PARAM_STOP_FILTER_ATTRIBUTE)
        public IntermodalAccessEgressParameterSet setStopFilterAttribute(String stopFilterAttribute) {
            this.stopFilterAttribute = stopFilterAttribute;
            return this ;
        }

        @StringGetter(PARAM_STOP_FILTER_VALUE)
        public String getStopFilterValue() {
            return stopFilterValue;
        }

        @StringSetter(PARAM_STOP_FILTER_VALUE)
        public IntermodalAccessEgressParameterSet setStopFilterValue(String stopFilterValue) {
            this.stopFilterValue = stopFilterValue;
            return this ;
        }

        @StringGetter(PARAM_SHARE_TRIP_SEARCH_RADIUS)
        public double getShareTripSearchRadius() {
            return shareTripSearchRadius;
        }

        @StringSetter(PARAM_SHARE_TRIP_SEARCH_RADIUS)
        public IntermodalAccessEgressParameterSet setShareTripSearchRadius(double shareTripSearchRadius) {
            this.shareTripSearchRadius = shareTripSearchRadius;
            return this ;
        }

        @Override
        public Map<String, String> getComments() {
            Map<String, String> map = super.getComments();
            map.put(PARAM_LINKID_ATTRIBUTE, "If the mode is routed on the network, specify which linkId acts as access link to this stop in the transport modes sub-network.");
            map.put(PARAM_STOP_FILTER_ATTRIBUTE, "Name of the transit stop attribute used to filter stops that should be included in the set of potential stops for access and egress. The attribute should be of type String. 'null' disables the filter and all stops within the specified radius will be used.");
            map.put(PARAM_STOP_FILTER_VALUE, "Only stops where the filter attribute has the value specified here will be considered as access or egress stops.");
            map.put(PARAM_PERSON_FILTER_ATTRIBUTE, "Name of the person attribute used to figure out if this access/egress mode is available to the person.");
            map.put(PARAM_PERSON_FILTER_VALUE, "Only persons where the filter attribute has the value specified here can use this mode for access or egress. The attribute should be of type String.");
            map.put(PARAM_MAX_RADIUS, "Radius from the origin / destination coord in which transit stops are accessible by this mode.");
            map.put(PARAM_INITIAL_SEARCH_RADIUS, "Radius from the origin / destination coord in which transit stops are searched. Only if less than 2 transit stops are found the search radius is increased step-wise until the maximum search radius set in param radius is reached.");
            map.put(PARAM_SEARCH_EXTENSION_RADIUS, "If less than 2 stops were found in initialSearchRadius take the distance of the closest transit stop and add this extension radius to search again.The search radius will not exceed the maximum search radius set in param radius. Default is 200 meters.");
            map.put(PARAM_SHARE_TRIP_SEARCH_RADIUS, "The share of the trip crowfly distance within which the stops for access and egress will be searched for. This is a harder constraint than initial search radius. Default is positive infinity.");

            return map;
        }
    }

    public static class ModeMappingForPassengersParameterSet extends ReflectiveConfigGroup {

        private static final String TYPE = "modeMapping";

        private static final String PARAM_ROUTE_MODE = "routeMode";
        private static final String PARAM_PASSENGER_MODE = "passengerMode";

        private String routeMode = null;
        private String passengerMode = null;

        public ModeMappingForPassengersParameterSet() {
            super(TYPE);
        }

        public ModeMappingForPassengersParameterSet(String routeMode, String passengerMode) {
            super(TYPE);
            this.routeMode = routeMode;
            this.passengerMode = passengerMode;
        }

        @StringGetter(PARAM_ROUTE_MODE)
        public String getRouteMode() {
            return routeMode;
        }

        @StringSetter(PARAM_ROUTE_MODE)
        public void setRouteMode(String routeMode) {
            this.routeMode = routeMode;
        }

        @StringGetter(PARAM_PASSENGER_MODE)
        public String getPassengerMode() {
            return passengerMode;
        }

        @StringSetter(PARAM_PASSENGER_MODE)
        public void setPassengerMode(String passengerMode) {
            this.passengerMode = passengerMode;
        }
    }

	public static class ModeToModeTransferPenalty extends ReflectiveConfigGroup{
		private static final String TYPE = "modeToModeTransferPenalty";
		@Parameter
		@Comment("from Transfer PT Sub-Mode")
		public String fromMode;
		@Parameter
		@Comment("to Transfer PT Sub-Mode")
		public String toMode;
		@Parameter
		@Comment("Transfer Penalty per Transfer between modes")
		public double transferPenalty = 0.0;

		public ModeToModeTransferPenalty() {
			super(TYPE);
		}

		public ModeToModeTransferPenalty(String fromMode, String toMode, double transferPenalty) {
			super(TYPE);
			this.fromMode = fromMode;
			this.toMode = toMode;
			this.transferPenalty = transferPenalty;
		}
	}



	@Override
    public Map<String, String> getComments() {
        Map<String, String> comments = super.getComments();
        comments.put(PARAM_INTERMODAL_ACCESS_EGRESS_MODE_SELECTION, PARAM_INTERMODAL_ACCESS_EGRESS_MODE_SELECTION_DESC);
        comments.put(PARAM_USE_CAPACITY_CONSTRAINTS, PARAM_USE_CAPACITY_CONSTRAINTS_DESC);
        comments.put(PARAM_TRANSFER_WALK_MARGIN, PARAM_TRANSFER_WALK_MARGIN_DESC);
		comments.put(PARAM_INTERMODAL_ACCESS_EGRESS_MODE_SELECTION,PARAM_INTERMODAL_ACCESS_EGRESS_MODE_SELECTION_DESC);
		comments.put(PARAM_TRANSFER_CALCULATION, PARAM_TRANFER_CALCULATION_DESC);
        return comments;
    }

    // TODO: add more
	@Override
	protected void checkConsistency(Config config) {

		if (useIntermodality) {

            Verify.verify(config.plans().getHandlingOfPlansWithoutRoutingMode().equals(HandlingOfPlansWithoutRoutingMode.reject), "Using intermodal access and egress in "
                    + "combination with plans without a routing mode is not supported.");
            Verify.verify(!intermodalAccessEgressSettings.isEmpty(), "Using intermodal routing, but there are no access/egress "
                    + "modes defined. Add at least one parameterset with an access/egress mode and ensure "
                    + "SwissRailRaptorConfigGroup is loaded correctly.");

            for (IntermodalAccessEgressParameterSet paramset : intermodalAccessEgressSettings) {
                Verify.verifyNotNull(paramset.mode, "mode of an IntermodalAccessEgressParameterSet "
                        + "is undefined. Please set a value in the config.");
                Verify.verify(paramset.maxRadius > 0.0, "maxRadius of IntermodalAccessEgressParameterSet "
                        + "for mode " + paramset.mode + " is negative or 0. Please set a positive value in the config.");
                Verify.verify(paramset.initialSearchRadius > 0.0, "initialSearchRadius of IntermodalAccessEgressParameterSet "
                        + "for mode " + paramset.mode + " is negative or 0. Please set a positive value in the config.");
                Verify.verify(paramset.searchExtensionRadius > 0.0, "searchExtensionRadius of IntermodalAccessEgressParameterSet "
                        + "for mode " + paramset.mode + " is negative or 0. Please set a positive value in the config.");

                Verify.verify(paramset.maxRadius >= paramset.initialSearchRadius, "maxRadius of IntermodalAccessEgressParameterSet "
                        + "for mode " + paramset.mode + " is smaller than initialSearchRadius. This is inconsistent.");

            }
        }
	}
}
