/**
 * This package provides location choice for discretionary activities.
 * 
 * <h2>Package Maintainer:</h2>
 * <ul>
 *   <li>Andreas Horni</li>
 * </ul>
 * 
 * <h2>Parameters:<a name="locationchoice_parameters"></a></h2>
 * Two basic versions of the destination choice module exist, one (v0) based on either global random search or local search and Haegerstrand's time geography and the other (v1) based on best response including error terms. v0 includes <i>localSearchRecursive, localSearchSingleAct</i> and global <i>random</i> search to be specified by the <strong><font color="black">algorithm</font></strong> attribute. v1 includes <i>bestResponse</i> also to be specified by the <strong><font color="black">algorithm</font></strong> attribute. 
 * Parameters required for v0 are given in <strong><font color="blue">blue</font></strong>, the ones used in v1 are given in <strong><font color="red">red</font></strong> and parameters applied in both versions are given in <strong>black</strong>.
 * <br/>
 * <h3>General Parameters</h3>
 * <ul>
 *  <li><strong><font color="black">algorithm</font></strong><br>
 * 		Type and range: String (localSearchRecursive, localSearchSingleAct, random, bestResponse)<br>
 * 		Default: null<br>
 * 		Description: Specifies which version of destination choice module should be applied. <br>
 * 		localSearchRecursive = time geography <br>
 * 		localSearchSingleAct = the same as simple_tg <br>
 * 		random = random mutation <br>
 * 		bestResponse = best response (recommended) destination choice module <br>
 *  </li>
 * 	<li><strong><font color="black">flexible_types</font></strong><br>
 * 		Type and range: String <br>
 * 		Default: null <br>
 * 		Description: This parameter globally (for all agents) specifies the activity types for which location choice is performed. For demand v1 instead of x0.5 ... x24, simply x can be given.
 *  </li>
 *  <li><strong><font color="black">planSelector</font></strong><br>
 * 		Type and range: String (BestScore, SelectExpBeta, ChangeExpBeta, SelectRandom)<br>
 * 		Default: SelectExpBeta <br>
 * 		Description: Specifies which plan should be replanned.
 *  </li>
 * 
 *  <li><strong><font color="red">randomSeed</font></strong><br>
 * 		Type and range: long <br>
 * 		Default: 221177 <br>
 * 		Description: Initial seed for generation of random error terms
 *  </li>
 * </ul>
 * 
 * <h3>Diluted Scenarios</h3>
 * <ul>
 * 	<li><strong><font color="blue">centerNode</font></strong><br>
 * 		Type and range: String <br> 
 * 		Default: null<br>
 * 		Description: If the locations have to be chosen from an additionally restrained region a center node and a radius can be set. This is only useful for diluted scenarios
 * </li>
 *	<li><strong><font color="blue">radius</font></strong><br>
 *		Type and range: float > 0 <br> 
 *		Default: null<br>
 *		Description: see above (centerNode) </li>
 * </ul>
 * 
 * <h3>Time Geography</h3>
 * <ul>
 * 	<li><strong><font color="blue">recursionTravelSpeedChange</font></strong><br> 
 * 		Type and range: float > 0 <br> 
 *		Default: 0.1 <br>
 *		Description: This factor is used to grow or shrink the approximate set of locations (see above).<br>
 *					 radius<sub>i+1</sub> = ttb / 2 x recurisionTravelSpeed x (1.0 -+ recursionTravelSpeedChange)<sup>i</sup>, with i: recursion number 
 *	</li>
 *	<li><strong><font color="black">recursionTravelSpeed</font></strong><br> 
 *		Type and range: float > 0 <br> 
 *		Default: 8.5 <br>
 *		Description: Initial assumed travel speed for the investigation area in [m/s]. This value is also used to compute the search space for version 1.
 *	</li>
 *	<li><strong><font color="blue">maxRecursions</font></strong><br>
 *		Type and range: int >= 0 <br> 
 *		Default: 1 <br>
 *		Description: Max. number of trials to search for a location according to the travel time budget (if 0 := universal choice set)
 *	</li> 
 * </ul>
 *
 * <h3>Spatial Competition (Capacity Restraint Function)</h3>
 * <ul>
 * 	<li><strong><font color="blue">restraintFcnFactor</font></strong><br>
 * 		Type and range: float >= 0.0 <br> 
 * 		Default: 0.0<br>
 * 		Description: Factor of the fac.cap.restraint function
 * 	</li>
 * 	<li><strong><font color="blue">restraintFcnExp</font></strong><br>
 * 		Type and range: float >= 0.0 <br> 
 * 		Default: 0.0<br>
 * 		Description: Exponent of the fac.cap.restraint function
 * 	</li>
 *	<li><strong><font color="blue">scaleFactor</font></strong><br>
 * 		Type and range: int > 0 <br> 
 * 		Default: 1<br>
 * 		Description: For sample scenarios, i.e. if 10% population then scaleFactor := 10
 * 	</li>
 * </ul>
 * 
 * <h3>Utility Function Specification</h3>
 * <ul>
 * 	<li><strong><font color="red">epsilonDistribution</font></strong><br>
 * 		Type and range: String (gumbel/gaussian) <br> 
 * 		Default: gumbel <br>
 * 		Description: Either use Normal or Gumbel distribution for error terms
 * 	</li>
 * <li><strong><font color="red">epsilonScaleFactors</font></strong><br>
 * 		Type and range: float > array of double separated by ',' <br> 
 * 		Default: null <br>
 * 		Description: Scale the activities epsilons, separated by comma
 * 	</li>
 * <li><strong><font color="red">pkValuesFile</font></strong><br>
 * 		Type and range: String <br> 
 * 		Default: null <br>
 * 		Description: specifies the location of the persons k values file (created during previous pre-processing)
 * 	</li>
 * <li><strong><font color="red">fkValuesFile</font></strong><br>
 * 		Type and range: String <br> 
 * 		Default: null <br>
 * 		Description: specifies the location of the facilities k values file (created during previous pre-processing)
 * 	</li>
 * </ul>
 * 
 * <h3>Search Space Construction</h3>
 * <ul>
 * 	<li><strong><font color="red">tt_approximationLevel</font></strong><br>
 * 		Type and range: int (0,1,2)<br> 
 * 		Default: 0 <br>
 * 		Description: 0: no approximation, routing for every OD-pair <br>
 * 		1: Dijkstra trees forward and backwards See working paper Horni,Nagel,Axhausen 2011, page 13<br>
 * 		2: based on travel distances
 * 	</li>
 * <li><strong><font color="red">maxDistanceEpsilon</font></strong><br>
 * 		Type and range: float (-1 or > 0.0) <br> 
 * 		Default: -1 <br>
 * 		Description: Maximum search space radius [m]. "-1" means no explicit fixed search space limitation. 
 * 	</li>
 * <li><strong><font color="red">probChoiceSetSize</font></strong><br>
 * 		Type and range: int > 0 <br> 
 * 		Default: 10 <br>
 * 		Description: Number of alternatives taken into account for the probabilistic choice. See working paper Horni,Nagel,Axhausen 2011, page 13
 * 	</li>
 *  <li><strong><font color="red">probChoiceExponent</font></strong><br>
 * 		Type and range: float <br> 
 * 		Default: 3.0 <br>
 * 		Description: Weighting of scores in the reduced choice set subject to probabilistic choice. See working paper Horni,Nagel,Axhausen 2011, page 13
 * 	</li>
 * <li><strong><font color="red">maxEpsFile</font></strong><br>
 * 		Type and range: String <br> 
 * 		Default: null <br>
 * 		Description: specifies the location of the maps eps file (created during pre-processing)
 * 	</li>
 *  <li><strong><font color="red">idExclusion</font></strong><br>
 * 		Type and range: int > 0<br> 
 * 		Default: max int <br>
 * 		Description: exclude all agents from destination choice and analysis with id > analysisIdExclusion. (e.g., border crossers)
 * 	</li>
 * </ul>
 * 
 * * <h3>Analysis</h3>
 * <ul>
 * 	<li><strong><font color="red">analysisBoundary</font></strong><br>
 * 		Type and range: double > 0.0<br> 
 * 		Default: 200km <br>
 * 		Description: boundary (radius) of analysis region (used for distance stats creation)
 * 	</li>
 * <li><strong><font color="red">analysisBinSize</font></strong><br>
 * 		Type and range: double > 0.0<br> 
 * 		Default: 20km <br>
 * 		Description: steps (bin sizes) for analysis (used for distance stats creation)
 * 	</li>
 * </ul>
 */
package org.matsim.contrib.locationchoice;