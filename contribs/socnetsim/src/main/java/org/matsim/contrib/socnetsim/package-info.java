/**
 * <p>
 * This package contains all functionality related to simulating joint behavior, as described in
 * <a href="https://doi.org/10.3929/ethz-b-000165685">this dissertation</a>.
 * It is recommended to skim at least Chapter 3 of the dissertation before looking at the code.
 * Chapters 4 and 6 are recommended to understand the specifics of the applications to households and friendship networks.
 * Here is a short guide of the different sub-packages:
 * </p>
 *
 * <h1>Framework</h1>
 *
 * <p>
 * {@link org.matsim.contrib.socnetsim.framework} contains the "framework" that allows the simulation of joint behavior.
 * The following elements are the most important:
 * </p>
 * <ul>
 *     <li>Data Structures to store the required information, in particular the {@link org.matsim.contrib.socnetsim.framework.population.SocialNetwork}
 *     and {@link org.matsim.contrib.socnetsim.framework.population.JointPlan}</li>
 *     <li>The machinery to replace the MATSim replanning process by a {@link org.matsim.contrib.socnetsim.framework.replanning.GroupStrategyManager},
 *     provided by the {@link org.matsim.contrib.socnetsim.framework.controller.JointDecisionProcessModule}.
 *     In general, the {@link org.matsim.contrib.socnetsim.framework.controller} package contains "infrastructure" needed
 *     to adapt the MATSim evolutionary process to take joint decisions into account, but leaves the actual implementation
 *     of the operators empty.</li>
 *     <li>{@link org.matsim.contrib.socnetsim.framework.replanning} provides specific implementation of the group replanning,
 *     including {@link org.matsim.contrib.socnetsim.framework.replanning.GroupStrategyManager} and various selectors.</li>
 * </ul>
 *
 * <h1>Specific Applications</h1>
 *
 * <p>
 * Additional packages provide implementation of classes geared towards specific applications.
 * In particular:
 * </p>
 *
 * <ul>
 *     <li>{@link org.matsim.contrib.socnetsim.jointactivities} contains classes relative to simulating the decision to
 *     participate in joint leisure activities (see Chapter 6)</li>
 *     <li>{@link org.matsim.contrib.socnetsim.jointtrips} contains classes relative to simulating the decision to
 *     perform joint car trips (several agents in one vehicle, see Chapter 4)</li>
 *     <li>{@link org.matsim.contrib.socnetsim.sharedvehicles} contains implementation of decisions relative to using
 *     shared household vehicles. This was not part of the thesis.</li>
 * </ul>
 *
 * <h1>Examples</h1>
 *
 * <p>
 * {@link org.matsim.contrib.socnetsim.examples.RunExampleSocialSimulation} demonstrates how to plug all elements together.
 * </p>
 *
 */
package org.matsim.contrib.socnetsim;