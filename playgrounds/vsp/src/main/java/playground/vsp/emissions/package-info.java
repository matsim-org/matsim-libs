package playground.vsp.emissions;

/**
 * This package provides a tool for exhaust emission calculation based on
 * the ``Handbook on Emission Factors for Road Transport'' (HBEFA), version 3.1 (see <a href="http://www.hbefa.net">http://www.hbefa.net</a>).
 * 
 * <h2>Usage</h2>
 * Execute {@link playground.vsp.emissions.example.benjamin.emissions.example.RunEmissionToolOnline RunEmissionToolOnline} or {@link playground.vsp.emissions.example.benjamin.emissions.example.RunEmissionToolOffline RunEmissionToolOffline} from the example package.
 * <ul>
 * <li> {@link playground.vsp.emissions.example.benjamin.emissions.example.RunEmissionToolOnline RunEmissionToolOnline}: Produces an emission events file during the simulation. </li>
 * <li> {@link playground.vsp.emissions.example.benjamin.emissions.example.RunEmissionToolOffline RunEmissionToolOffline}: Produces an emission events file based on a standard MATSim eventsfile. </li>
 * </ul>
 * In both cases you will have to specify several input files. See the above classes as examples and the following section for details.
 *
 * <h2>Input files</h2>
 * Required files are:
 * <ul>
 * <li>roadTypeMappingFile: This file needs to map road types in your network to Hbefa 3.1 road types. 
 * See the parser {@link playground.vsp.emissions.EmissionModule#createRoadTypeMapping createRoadTypeMapping} 
 * at the {@link playground.vsp.emissions.EmissionModule EmissionModule} 
 * or see {@link org.matsim.core.config.groups.VspExperimentalConfigGroup VspExperimentalConfigGroup} for a detailed description.
 * 
 * <li>emissionVehicleFile: This data type is defined in the VspExperimentalConfigGroup, 
 * see {@link org.matsim.core.config.groups.VspExperimentalConfigGroup VspExperimentalConfigGroup} and described as "definition of a vehicle
 *  for every person (who is allowed to choose a vehicle in the simulation):
 *  <ul>
 *  <li> REQUIRED: vehicle type Id must start with the respective HbefaVehicleCategory followed by ";"
 *  <li> OPTIONAL: if detailed emission calculation is switched on, vehicle type Id should aditionally contain
 *  HbefaVehicleAttributes ("Technology;SizeClasse;EmConcept"), corresponding to the strings in detailedWarmEmissionFactorsFile (see below)) </li>
 * </ul>
 * <li>averageFleetWarmEmissionFactorsFile: This file can be exported from Hbefa 3.1. 
 * See the parser {@link playground.vsp.emissions.EmissionModule#createAvgHbefaWarmTable createAvgHbefaWarmTable}
 *  at the {@link playground.vsp.emissions.EmissionModule EmissionModule} 
 *  or see the {@link org.matsim.core.config.groups.VspExperimentalConfigGroup VspExperimentalConfigGroup} for a detailed description. </li>
 *
 * <li>averageFleetColdEmissionFactorsFile: This file can be exported from Hbefa 3.1.
 * See the parser {@link playground.vsp.emissions.EmissionModule#createAvgHbefaColdTable createAvgHbefaColdTable}
 * at the {@link playground.vsp.emissions.EmissionModule EmissionModule} 
 * or see the {@link org.matsim.core.config.groups.VspExperimentalConfigGroup VspExperimentalConfigGroup} for a detailed description. </li>
 * </ul>
 *
 * Optional: To use detailed emission calculation set isUsingDetailedEmissionCalculation to <code>true</code> in the
 * {@link playground.vsp.emissions.example.benjamin.emissions.example.RunEmissionToolOnline RunEmissionToolOnline} or 
 * {@link playground.vsp.emissions.example.benjamin.emissions.example.RunEmissionToolOffline RunEmissionToolOffline} class. 
 * Define the input paths for
 * <ul>
 * 
 * <li>detailedWarmEmissionFactorsFile: See the parser 
 * {@link playground.vsp.emissions.EmissionModule#createDetailedHbefaWarmTable createDetailedHbefaWarmTable}
 * at the {@link playground.vsp.emissions.EmissionModule EmissionModule} for details. </li>
 *
 * <li>and detailedColdEmissionFactorsFile: 
 * {@link playground.vsp.emissions.EmissionModule#createDetailedHbefaColdTable createDetailedHbefaColdTable}
 * at the {@link playground.vsp.emissions.EmissionModule EmissionModule} for details.
 * </li>
 * </ul>
 * 
 *  All emission factor files represent tables from Hbefa 3.1, which are read columnwise. 
 *  Their column headers need to match the parser definition 
 *  in the respective method of the {@link playground.vsp.emissions.EmissionModule EmissionModule}.
 *  Variable detailed vehicle types and traffic situations definitions are mapped to emission values and types. 
 *
 * <h2>Model description</h2>
 * 
 * <h3>Emissions</h3>
 * The main package contains classes and methods to handle the emission input data and create
 * maps to associate the emissions with corresponding vehicle types, speed, parking time, ... <br/>
 *
 * <h3>Events</h3>
 * This class contains extensions of {@link org.matsim.api.core.v01.events.Event.matsim.core.api.experimental.events.Event Event}
 * to handle different types of emissions as events. <code> ColdEmissionAnalysisModule</code>
 * calculates emissions after a cold start, <code> WarmEmissionAnalysisModule</code> calculates
 * warm emissions.
 *
 * <h3>Example</h3>
 * This class contains the "RunEmissionTool" classes and a control listener which implements
 * some functions from {@link org.matsim.core.controler Controler}.
 *
 * <h3>Types</h3>
 * {@link playground.benjamin.emissions.types.ColdPollutant ColdPollutant} and {@link playground.benjamin.emissions.types.WarmPollutant WarmPollutant} are enumerations of emission factors. 
 * {@link playground.benjamin.emissions.types.HbefaVehicleAttributes HbefaVehicleAttributes} contains a default constructor, setting all values to average.
 * This way a calculation of emissions for undefined vehicle types can be performed.
 * Any instance of {@link playground.benjamin.emissions.types.HbefaWarmEmissionFactorKey HbefaWarmEmissionFactorKey} contains a vehicle category, a warm pollutant; a road category,
 * a traffic situation and three vehicle attributes (technology, size class, em concept).
 * Instances of {@link playground.benjamin.emissions.types.HbefaColdEmissionFactorKey HbefaColdEmissionFactorKey} contain a vehicle category, a cold pollutant, a parking time range,
 * a distance, which is driven after parking and, again, vehicle attributes.
 * The cold/warm emission factor keys are mapped to the values of cold/warm emissions, the cold/warm emission factors.
 * <br/> <br/>
 * 
 * @author benjamin
 */

class EmissionPackageInfo{
	
}