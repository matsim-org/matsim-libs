/**
 * QSim needs to be able to plug in the engines.  In consequence, the engines need to have public methods (preferably interfaces).
 * <br/>
 * But also the engines need to access the QSim.  In consequence, the QSim needs to have public methods (preferably interfaces).
 * <br/>
 * In extension, one should be able to compose the module QSim from anywhere (i.e. set the engines).  This implies that the
 * "setXXXEngine"-methods need to be public.
 * <br/>
 * Something like the pt engine needs to be able to get to the network engine.  This imples that the "getXXXEngine" methods need 
 * to be public.
 * <br/>
 * Yet, core.mobsim.queuesim is not pluggable, so none of this stuff exists and should exist.  So we need an additional interface
 * "PluggableMobsim" or "ModularMobsim", similar to (or replacing) "AcceptsFeatures".  
 * <br/>
 * kai, jun'10
 */
package org.matsim.ptproject.qsim;