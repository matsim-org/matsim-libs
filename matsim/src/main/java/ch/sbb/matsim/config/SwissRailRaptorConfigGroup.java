/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2018.
 */

package ch.sbb.matsim.config;

import org.apache.log4j.Logger;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.utils.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author mrieser / SBB
 */
public class SwissRailRaptorConfigGroup extends ReflectiveConfigGroup {

	private static final Logger log = Logger.getLogger(SwissRailRaptorConfigGroup.class);
    public static final String GROUP = "swissRailRaptor";

    private static final String PARAM_USE_RANGE_QUERY = "useRangeQuery";
    private static final String PARAM_USE_INTERMODAL_ACCESS_EGRESS = "useIntermodalAccessEgress";
    private static final String PARAM_INTERMODAL_ACCESS_EGRESS_MODE_SELECTION = "intermodalAccessEgressModeSelection";
    private static final String PARAM_USE_MODE_MAPPING = "useModeMappingForPassengers";
    private static final String PARAM_SCORING_PARAMETERS = "scoringParameters";
    private static final String PARAM_TRANSFER_PENALTY_BASE = "transferPenaltyBaseCost";
    private static final String PARAM_TRANSFER_PENALTY_MIN = "transferPenaltyMinCost";
    private static final String PARAM_TRANSFER_PENALTY_MAX = "transferPenaltyMaxCost";
    private static final String PARAM_TRANSFER_PENALTY_PERHOUR = "transferPenaltyCostPerTravelTimeHour";

    private boolean useRangeQuery = false;
    private boolean useIntermodality = false;
    private IntermodalAccessEgressModeSelection intermodalAccessEgressModeSelection = IntermodalAccessEgressModeSelection.CalcLeastCostModePerStop;
    private boolean useModeMapping = false;

    private double transferPenaltyBaseCost = 0;
    private double transferPenaltyMinCost = Double.NEGATIVE_INFINITY;
    private double transferPenaltyMaxCost = Double.POSITIVE_INFINITY;
    private double transferPenaltyHourlyCost = 0;

    private ScoringParameters scoringParameters = ScoringParameters.Default;

    private final Map<String, RangeQuerySettingsParameterSet> rangeQuerySettingsPerSubpop = new HashMap<>();
    private final Map<String, RouteSelectorParameterSet> routeSelectorPerSubpop = new HashMap<>();
    private final List<IntermodalAccessEgressParameterSet> intermodalAccessEgressSettings = new ArrayList<>();
    private final Map<String, ModeMappingForPassengersParameterSet> modeMappingForPassengersByRouteMode = new HashMap<>();
    
    public enum IntermodalAccessEgressModeSelection {
    	CalcLeastCostModePerStop, RandomSelectOneModePerRoutingRequestAndDirection
    }
    
    public enum ScoringParameters {
    	Default, Individual
    }

    public SwissRailRaptorConfigGroup() {
        super(GROUP);
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

    @StringGetter(PARAM_USE_MODE_MAPPING)
    public boolean isUseModeMappingForPassengers() {
        return this.useModeMapping;
    }

    @StringSetter(PARAM_USE_MODE_MAPPING)
    public void setUseModeMappingForPassengers(boolean useModeMapping) {
        this.useModeMapping = useModeMapping;
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
        if (RangeQuerySettingsParameterSet.TYPE.equals(type)) {
            return new RangeQuerySettingsParameterSet();
        } else if (RouteSelectorParameterSet.TYPE.equals(type)) {
            return new RouteSelectorParameterSet();
        } else if (IntermodalAccessEgressParameterSet.TYPE.equals(type)) {
            return new IntermodalAccessEgressParameterSet();
        } else if (ModeMappingForPassengersParameterSet.TYPE.equals(type)) {
            return new ModeMappingForPassengersParameterSet();
        } else {
            throw new IllegalArgumentException("Unsupported parameterset-type: " + type);
        }
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
            addModeMappingForPassengers((ModeMappingForPassengersParameterSet) set);
        } else {
            throw new IllegalArgumentException("Unsupported parameterset: " + set.getClass().getName());
        }
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

        private String mode;
        private double maxRadius;
        private double initialSearchRadius = Double.POSITIVE_INFINITY;
        private double searchExtensionRadius = 1000.0;
        private String linkIdAttribute;
        private String personFilterAttribute;
        private String personFilterValue;
        private String stopFilterAttribute;
        private String stopFilterValue;

        public IntermodalAccessEgressParameterSet() {
            super(TYPE);
        }

        @StringGetter(PARAM_MODE)
        public String getMode() {
            return mode;
        }

        @StringSetter(PARAM_MODE)
        public void setMode(String mode) {
            this.mode = mode;
        }

        @StringGetter(PARAM_MAX_RADIUS)
        public double getMaxRadius() {
            return maxRadius;
        }

        @StringSetter(PARAM_MAX_RADIUS)
        public void setMaxRadius(double maxRadius) {
            this.maxRadius = maxRadius;
        }
        
        @StringGetter(PARAM_INITIAL_SEARCH_RADIUS)
        public double getInitialSearchRadius() {
            return initialSearchRadius;
        }

        @StringSetter(PARAM_INITIAL_SEARCH_RADIUS)
        public void setInitialSearchRadius(double initialSearchRadius) {
            this.initialSearchRadius = initialSearchRadius;
        }
        
        @StringGetter(PARAM_SEARCH_EXTENSION_RADIUS)
        public double getSearchExtensionRadius() {
            return searchExtensionRadius;
        }

        @StringSetter(PARAM_SEARCH_EXTENSION_RADIUS)
        public void setSearchExtensionRadius(double searchExtensionRadius) {
            this.searchExtensionRadius = searchExtensionRadius;
        }

        @StringGetter(PARAM_LINKID_ATTRIBUTE)
        public String getLinkIdAttribute() {
            return linkIdAttribute;
        }

        @StringSetter(PARAM_LINKID_ATTRIBUTE)
        public void setLinkIdAttribute(String linkIdAttribute) {
            this.linkIdAttribute = linkIdAttribute;
        }

        @StringGetter(PARAM_PERSON_FILTER_ATTRIBUTE)
        public String getPersonFilterAttribute() {
            return this.personFilterAttribute;
        }

        @StringSetter(PARAM_PERSON_FILTER_ATTRIBUTE)
        public void setPersonFilterAttribute(String personFilterAttribute) {
            this.personFilterAttribute = personFilterAttribute;
        }

        @StringGetter(PARAM_PERSON_FILTER_VALUE)
        public String getPersonFilterValue() {
            return this.personFilterValue;
        }

        @StringSetter(PARAM_PERSON_FILTER_VALUE)
        public void setPersonFilterValue(String personFilterValue) {
            this.personFilterValue = personFilterValue;
        }

        @StringGetter(PARAM_STOP_FILTER_ATTRIBUTE)
        public String getStopFilterAttribute() {
            return stopFilterAttribute;
        }

        @StringSetter(PARAM_STOP_FILTER_ATTRIBUTE)
        public void setStopFilterAttribute(String stopFilterAttribute) {
            this.stopFilterAttribute = stopFilterAttribute;
        }

        @StringGetter(PARAM_STOP_FILTER_VALUE)
        public String getStopFilterValue() {
            return stopFilterValue;
        }

        @StringSetter(PARAM_STOP_FILTER_VALUE)
        public void setStopFilterValue(String stopFilterValue) {
            this.stopFilterValue = stopFilterValue;
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
            map.put(PARAM_SEARCH_EXTENSION_RADIUS, "If less than 2 stops were found in initialSearchRadius take the distance of the closest transit stop and add this extension radius to search again.The search radius will not exceed the maximum search radius set in param radius.");
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
}
