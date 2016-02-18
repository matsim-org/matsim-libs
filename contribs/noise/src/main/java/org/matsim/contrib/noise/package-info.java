/**
 * A package which provides some tools to compute...
 * <ul>
 * <li> noise (emissions) levels on network links (based on traffic volume, share of heavy goods vehicles, speed level)
 * <li> noise (immission) levels at receiver points (based on the surrounding links' emissions and distances)
 * <li> population units (= population densities) at receiver points (mapping agents to the nearest receiver point) 
 * <li> noise damages (= noise exposure costs) at receiver points (based on population units and noise levels)
 * </ul>
 * 
 * All relevant parameters such as the temporal and spatial resolution are specified in {@link org.matsim.contrib.noise.NoiseConfigGroup}.
 * <p>
 * 
 * There are two possible use cases:
 * <ul>
 * <li> Run an offline noise computation for analysis purposes, see {@link org.matsim.contrib.noise.examples.NoiseOfflineCalculationExample}. 
 * <li> Run an online noise computation, see {@link org.matsim.contrib.noise.examples.NoiseOnlineControlerExample}. Noise damages may be internalized applying different allocation approaches, see {@link org.matsim.contrib.noise.data.NoiseAllocationApproach}.
 * </ul>
 * 
 * The computation of noise levels is based on the German RLS 90 approach (Richtlinien fuer den Laermschutz and Strassen, Forschungsgesellschaft fuer Strassen- und Verkehrswesen), but applies some simplifications and minor modifications.
 * Thus, noise levels should be considered as estimates rather than exact values. The conversion of noise exposures into monetary costs follows a slightly modified version of the German EWS approach (Empfehlungen fuer Wirtschaftlichkeitsuntersuchungen an Strassen, Forschungsgesellschaft fuer Strassen- und Verkehrswesen).
 * A more detailed description of the computation methodology and the model limitations is provided in the related publications.
 * <p>
 * Related publications (list not regularly updated):
 * <ul>
 * <li> I. Kaddoura, L. Kroeger, and K. Nagel. User-specific and dynamic internalization of road traffic noise exposures. Networks and Spatial Economics, 2016.
 * Preprint available from <a href="https://svn.vsp.tu-berlin.de/repos/public-svn/publications/vspwp/2015/15-12/"> </a>
 * <li> I. Kaddoura and K. Nagel. Activity-based computation of marginal noise exposure costs: Impacts for traffic management. Annual Meeting Preprint 16-3437, Transportation Research Board, Washington D.C., January 2016.
 * Preprint available from <a href="https://svn.vsp.tu-berlin.de/repos/public-svn/publications/vspwp/2015/15-13/"> </a>
 * </ul>
 * 
 * 
 * @author ikaddoura, lkroeger
 *
 */
package org.matsim.contrib.noise;