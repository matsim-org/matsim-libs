/**
 * 
 * Contains the scoring functions, i.e. the implementations of ScoringFunctionFactory, used in the
 * project.
 * 
 * The classes ending with Scoring (and not with ScoringFunction) are "sum terms" which you
 * can use independently when you write your own ScoringFunction, see {@link org.matsim.core.scoring.SumScoringFunction}.
 * On the other hand, it may be more straight-forward to just implement ScoringFunction yourself and copy/paste some of the
 * math to your own class.
 *
 * @see org.matsim.core.scoring
 */
package org.matsim.core.scoring.functions;