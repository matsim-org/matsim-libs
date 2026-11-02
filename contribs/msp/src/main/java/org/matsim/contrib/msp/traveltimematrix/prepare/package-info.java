/**
 * Preparing the network the matrix is built over.
 * <p>
 * A matrix over a Germany-wide network is neither affordable nor meaningful when the question is about
 * one city, so
 * {@link org.matsim.contrib.msp.traveltimematrix.prepare.StudyAreaNetwork} cuts the car subnetwork of
 * the study area out of the run's network - keeping the link ids, so that the travel times and volumes
 * measured on the full run still apply to it.
 */
package org.matsim.contrib.msp.traveltimematrix.prepare;
