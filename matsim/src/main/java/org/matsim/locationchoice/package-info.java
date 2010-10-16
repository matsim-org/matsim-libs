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
 * 	<li><strong><font color="blue">constrained</font></strong> <br>
 * 		Type and range: boolean (true/false)<br>
 * 		Default: false <br>
 * 		Description: Random location choice ("<i>false</i>") or constrained location choice ("<i>true</i>") based on travel time budgets (time geography).
 * 	</li>
 *	<li><strong><font color="blue">simple_tg</font></strong> <br>
 *		Type and range: boolean (true/false) <br>
 *		Default: false <br>
 *		Description: If simple_tg is set true, a simplified but more efficient version of location choice is used. Only one discretionary activity per plan is relocated in this version, instead of recursively taking into account the complete activity chain. Do only use this version if performance is crucial.
 *	</li>	
 * 	<li><strong><font color="blue">flexible_types</font></strong><br>
 * 		Type and range: String <br>
 * 		Default: null <br>
 * 		Description: This parameter globally (for all agents) specifies the activity types for which location choice is performed. If flexible_types==null the individualized information of the plan->knowledge is used
 * </li>
 * </ul>
 * 
 * <h3>Diluted Scenarios</h3>
 * <ul>
 * 	<li><strong><font color="blue">centerNode</font></strong>
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
 *	<li><strong><font color="blue">recursionTravelSpeed</font></strong><br> 
 *		Type and range: float > 0 <br> 
 *		Default: 8.5 <br>
 *		Description: Initial assumed travel speed for the investigation area in [m/s]
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
 */
package org.matsim.locationchoice;