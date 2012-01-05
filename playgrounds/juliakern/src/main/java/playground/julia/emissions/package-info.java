package playground.julia.emissions;


/**
 * This package provides a tool for exhaust emission calculation based on
 * the ``Handbook on Emission Factors for Road Transport'' (HBEFA), version 3.1 (see <link>{@link http://www.hbefa.net}</link>).
 * 
 * <h2>Usage</h2>
 * Run RunEmissionToolOnline or RunEmissionToolOffline from the example package.
 * <ul>
 * 	<li> RunEmissionToolOnline: Works with network and plan files.  </li>
 * 	<li> RunEmissionToolOffline: Uses already calculated trips from an eventsfile. </li>
 * </ul>
 * In both cases some files concerning the network, emission values, road and vehicle types need to be provided.
 * See the RunEmissionTool classes for details. 
 *
 * <h2>Input files</h2>
 * Required files are 
 * <ul>
 * <li>roadTypeMappingFile: This file can be directly exported from Hbefa3.1 
 * ( <link>{@link http://www.hbefa.net}</link>). Its header should look like this
 * <code> VISUM_RT_NR;VISUM_RT_NAME;HBEFA_RT_NAME,</code> and this would be a typical line: <code>16;1ao 2FS 120;RUR/MW/120 </code> </li> 
 * <li>emissionVehicleFile: This data type is defined in the VspExperimentalConfigGroup, 
 * see {@link org.matsim.core.config.groups.VspExperimentalConfigGroup} and described as "definition of a vehicle
 *  for every person (who is allowed to choose a vehicle in the simulation):" + "\n" +
 *  " - REQUIRED: vehicle type Id must start with the respective HbefaVehicleCategory followed by `;'" + "\n" +
 *  " - OPTIONAL: if detailed emission calculation is switched on, vehicle type Id should aditionally contain" +
 *  " HbefaVehicleAttributes (`Technology;SizeClasse;EmConcept'), corresponding to the strings in " + EMISSION_FACTORS_WARM_FILE_DETAILED);"

 * <li>averageFleetWarmEmissionFactorsFile: This file can be exported from Hbefa3.1 
 * ( <link>{@link http://www.hbefa.net}</link>). Its header should look like this
 * <code> Case;VehCat;Year;TrafficScenario;Component;RoadCat;TrafficSit;Gradient;V_weighted;EFA_weighted </code>
 * and this would be a typical line: <code> 2005average[3.1];pass. car;2005;"BAU" (D);FC;MW;RUR/MW/80/Freeflow;0%;82.8;47.69 </code></li> 
 * 
 * <li>averageFleetColdEmissionFactorsFile: This file can be exported from Hbefa3.1 
 * ( <link>{@link http://www.hbefa.net}</link>).Its header should look like this
 * <code> Case;VehCat;Year;TrafficScenario;Component;RoadCat;AmbientCondPattern;EFA_weighted;EFA_km_weighted </code>
 * and this would be a typical line: <code> 2005average[3.1];pass. car;2005;"BAU" (D);FC;Urban;TØ,0-1h,0-1km;0.99; </code> </li>
 * </ul>
 * 
 * Optional: To use detailed emission calculation set isUsingDetailedEmissionCalculation to <code>true</code> in the 
 * RunEmissionToolOnline or RunEmissionToolOffline class. Define the input paths for
 * <ul>
 * <li>detailedWarmEmissionFactorsFile: With <code>detailedWarmEmissionFactorsFileCase;VehCat;Year;TrafficScenario;Component;RoadCat;TrafficSit;Gradient;IDSubsegment;Subsegment;Technology;SizeClasse;EmConcept;KM;%OfSubsegment;V;V_0%;V_100%;EFA;EFA_0%;EFA_100%;V_weighted;V_weighted_0%;V_weighted_100%;EFA_weighted;EFA_weighted_0%;EFA_weighted_100% 
 * </code>
 * 
 * as header and lines like this 
 * 
 * <code> PC[3.1];pass. car;2010;;FC;;RUR/MW/80/Freeflow;0%;111100;PC petrol <1,4L <ECE;petrol (4S);<1,4L;PC-P-Euro-0;50000,00;1,00;82,80;;;45,03;;;;;;;; </code>
 * </code>  </li>
 * 
 * <li>and detailedColdEmissionFactorsFile: With 
 * <code> detailedColdEmissionFactorsFileCase;VehCat;Year;TrafficScenario;Component;RoadCat;AmbientCondPattern;IDSubsegment;Subsegment;Technology;SizeClasse;EmConcept;KM;%OfSubsegment;EFA;EFA_weighted;EFA_km;EFA_km_weighted 
 * </code>
 * as header and lines like this <code>
 * 2005detailed[3.1];pass. car;2010;;FC;;TØ,0-1h,0-1km;111100;PC petrol <1,4L <ECE;petrol (4S);<1,4L;PC-P-Euro-0;50000.00;1.00;0.92;;;
 *  </li>
 * </code>
 * 
 * <h2>Model description</h2>
 * The emissions package contains four subpackages:
 * <ul>	<li><code>events</code></li>
 * 		<li><code>example</code></li> 
 *  	<li><code>test</code></li>
 *   	<li><code>types</code></li>
 * </ul>
 * 
 * <h3> Emissions </h3>
 * The main package contains classes and methods to handle the emission input data and create
 * maps to associate the emissions with corresponding vehicle types, speed, parking time, ...
 * 
 * <h3> Events </h3>
 * This class contains extensions of {@link org.matsim.core.api.experimental.events.Event} 
 * to handle different types of emissions as events. <code> ColdEmissionAnalysisModule</code> 
 * calculates emissions after a cold start, <code> WarmEmissionAnalysisModule</code> calculates
 * warm emissions.
 * 
 * <h3> Example</h3>
 * This class contains the RunEmissionTool classes and a control listener which implements
 * some functions from {@link org.matsim.core.controler}.
 * 
 * <h3> Types </h3>
 *  
 * ColdPollutant/WarmPollutant are enumerations of emission factors. Currently FC, NOx, NO2, PM, CO2(total) 
 * are considered as warm pollutants and FC, NOx, NO2, PM, CO and HC as cold pollutants. 
 * These enumerations are expandable. 
 * The HbefaVehicleAttributes class contains a default constructor, setting all values to average. 
 * This way unknown vehicle types can be handled and emissions calculated.
 * Any instance of the HbefaWarmEmissionFactorKey class contains a vehicle category, a warm pollutant;a road category,
 * a traffic situation and three vehicle attributes (technology, size class, em concept).
 * Instances of the HbefaColdEmissionFactorKey class contain a vehicle category, a cold pollutant, a parking time range,
 * a distance, which is driven after parking and vehicle attributes.
 * The cold/warm factor keys are mapped to the values of cold/warm emissions, the cold/warm emission factors. 
 * 
 * 
 * @author benjamin
 */


class Heinz{
	
}