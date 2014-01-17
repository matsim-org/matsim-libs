/**
 * This package provides location choice for discretionary activities.
 * 
 * <h2>Package Maintainer:</h2>
 * <ul>
 *   <li>Andreas Horni</li>
 * </ul>
 * 
 * <h2>Parameters:<a name="locationchoice_parameters"></a></h2>

 * <h3>General Parameters</h3>
 * <ul>
 *  <li><strong>algorithm</strong><br>
 * 		Type and range: String (localSearchRecursive, localSearchSingleAct, random, bestResponse)<br>
 * 		Default: bestResponse<br>
 * 		Description: Specifies which version of destination choice module should be applied. <br>
 * 		localSearchRecursive = time geography (deprecated)<br>
 * 		localSearchSingleAct = the same as simple_tg (deprecated)<br>
 * 		random = random mutation (deprecated)<br>
 * 		bestResponse = best response (recommended) destination choice module <br>
 *  </li>
 * 	<li><strong>flexible_types</strong><br>
 * 		Type and range: String <br>
 * 		Default: null <br>
 * 		Description: This parameter globally (for all agents) specifies the activity types for which location choice is performed. For demand v1 instead of x0.5 ... x24, simply x has to be given.
 *  </li>
 *  <li><strong>planSelector</strong><br>
 * 		Type and range: String (BestScore, SelectExpBeta, ChangeExpBeta, SelectRandom)<br>
 * 		Default: SelectExpBeta <br>
 * 		Description: Specifies which plan should be replanned.
 *  </li>
 *  <li><strong>randomSeed</strong><br>
 * 		Type and range: long <br>
 * 		Default: 221177 <br>
 * 		Description: Initial seed for generation of random error terms
 *  </li>
 * </ul>
 * 
 * <h3>Diluted Scenarios</h3>
 * <ul>
 * 	<li><strong>centerNode</strong><br>
 * 		Type and range: String <br> 
 * 		Default: null<br>
 * 		Description: If the destinations have to be chosen from an additionally restrained region a center node and a radius can be set. This is only useful for diluted scenarios
 * </li>
 *	<li><strong>radius</strong><br>
 *		Type and range: float > 0 <br> 
 *		Default: null<br>
 *		Description: see above (centerNode) </li>
 * </ul>
 * 
 * <h3>Spatial Competition (Capacity Restraint Function)</h3>
 * <ul>
 * 	<li><strong>restraintFcnFactor</strong><br>
 * 		Type and range: float >= 0.0 <br> 
 * 		Default: 0.0<br>
 * 		Description: Factor of the fac.cap.restraint function
 * 	</li>
 * 	<li><strong>restraintFcnExp</strong><br>
 * 		Type and range: float >= 0.0 <br> 
 * 		Default: 0.0<br>
 * 		Description: Exponent of the fac.cap.restraint function
 * 	</li>
 *	<li><strong>scaleFactor</strong><br>
 * 		Type and range: int > 0 <br> 
 * 		Default: 1<br>
 * 		Description: For sample scenarios, i.e. if 10% population then scaleFactor := 10
 * 	</li>
 * </ul>
 * 
 * <h3>Utility Function Specification</h3>
 * <ul>
 * 	<li><strong>epsilonDistribution</strong><br>
 * 		Type and range: String (gumbel/gaussian) <br> 
 * 		Default: gumbel <br>
 * 		Description: Either use Normal or Gumbel distribution for error terms
 * 	</li>
 * <li><strong>epsilonScaleFactors</strong><br>
 * 		Type and range: float > array of doubles separated by ',' <br> 
 * 		Default: null <br>
 * 		Description: Scale the activities epsilons
 * 	</li>
 * <li><strong>pkValuesFile</strong><br>
 * 		Type and range: String <br> 
 * 		Default: null <br>
 * 		Description: specifies the location of the persons k values file (created during previous pre-processing)
 * 	</li>
 * <li><strong>fkValuesFile</strong><br>
 * 		Type and range: String <br> 
 * 		Default: null <br>
 * 		Description: specifies the location of the facilities k values file (created during previous pre-processing)
 * 	</li>
 * <li><strong>prefsFile</strong><br>
 * 		Type and range: String <br>
 * 		Default: null <br>
 * 		Description: 
 * 		Specifies the agents' earliestEndTime, latestStartTime, minimalDuration and typicalDuration. 
 * 		If no prefs file is found, the module tries to get the typicalDuration from the desires and the rest from the config.
 * 		If also no desires are available, then everything is tried to be taken from the config.
 * 		And finally, if also in the config not everything is there, you need to help yourself ;)
 * 		This should sooner or later go somewhere else, more into the core.
 *  </li>
 *  <li><strong>pBetasFileName</strong><br>
 * 		Type and range: String <br>
 * 		Default: null <br>
 * 		Description: Specifies the beta parameters for additional destination attributes specified in fAttributesFileName below
 *  </li>
 *  <li><strong>fAttributesFileName</strong><br>
 * 		Type and range: String <br>
 * 		Default: null <br>
 * 		Description: see pBetasFileName
 *  </li>
 * </ul>
 * 
 * <h3>Search Space Construction</h3>
 * <ul>
 * 	<li><strong>tt_approximationLevel</strong><br>
 * 		Type and range: int (0,1,2)<br> 
 * 		Default: 0 <br>
 * 		Description: 
 * 		0: no approximation, routing for every OD-pair (for real-world scenarios not applicable as it is much too slow)<br>
 * 		1: Dijkstra trees forward and backwards. See working paper Horni, Nagel, Axhausen 2011, page 13<br>
 * 		2: based on travel distances
 * 	</li>
 * <li><strong>maxDistanceDCScore</strong><br>
 * 		Type and range: float (-1 or > 0.0) <br> 
 * 		Default: -1 <br>
 * 		Description: Maximum search space radius [m]. "-1" means no explicit fixed search space limitation. 
 * 	</li>
 * <li><strong>probChoiceSetSize</strong><br>
 * 		Type and range: int > 0 <br> 
 * 		Default: 5 <br>
 * 		Description: Number of alternatives taken into account for the probabilistic choice. See working paper Horni, Nagel, Axhausen 2011, page 13
 * 	</li>
 * <li><strong>maxDCScoreFile</strong><br>
 * 		Type and range: String <br> 
 * 		Default: null <br>
 * 		Description: specifies the location of the unscaled max dc score file (created during pre-processing). If the k values have not changed from one run to another, the (unscaled) max dc score values can be reused and the pre-processing can be skipped.
 * 	</li>
 *  <li><strong>idExclusion (deprecated)</strong><br>
 * 		Type and range: int > 0<br> 
 * 		Default: max int <br>
 * 		Description: Better use the subpopulation specifications in the config strategy section!
 * 		But here used to exclude all agents from destination choice and analysis with id > analysisIdExclusion. (e.g., border crossers)
 * 	</li>
 * <li><strong>travelSpeed_car</strong><br>
 * 		Type and range: double > 0.0<br> 
 * 		Default: 8.5 m/s<br>
 * 		Description: approximated assumed car travel speed to convert time into distance for search space construction.
 * 	</li>
 * <li><strong>travelSpeed_pt</strong><br>
 * 		Type and range: double > 0.0<br> 
 * 		Default: 5 m/s<br>
 * 		Description: approximated assumed pt travel speed to convert time into distance for search space construction.
 * 	</li>
 * <li><strong>destinationSamplePercent</strong><br>
 * 		Type and range: double, 3 possible values: 100.0, 10.0, and 1.0<br> 
 * 		Default: 100.0<br>
 * 		Description: specifies how many percent of the destinations in the choice set should be used for the final choice
 * 	</li>
 * </ul>
 * 
 * <h3>Analysis</h3>
 * <ul>
 * 	<li><strong>analysisBoundary</strong><br>
 * 		Type and range: double > 0.0<br> 
 * 		Default: 200km <br>
 * 		Description: maximum distance in distance stats. Greater values go into the last bin.
 * 	</li>
 * <li><strong>analysisBinSize</strong><br>
 * 		Type and range: double > 0.0<br> 
 * 		Default: 20km <br>
 * 		Description: steps (bin sizes) for distance stats
 * 	</li>
 * </ul>
 */
package org.matsim.contrib.locationchoice;