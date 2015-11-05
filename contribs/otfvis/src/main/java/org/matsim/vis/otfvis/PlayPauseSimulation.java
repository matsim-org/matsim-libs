/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.vis.otfvis;

import java.util.concurrent.Semaphore;

import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.vis.otfvis.interfaces.PlayPauseSimulationI;

/**
 * Extracted the play/pause functionality from otfvis to make it available for other purposes (specifically, RMITs 
 * CombineSim approach).
 * 
 * @author nagel
 */
public class PlayPauseSimulation implements PlayPauseSimulationI {
/*	Hallo Kai,

	auch “main” läuft in einem Thread, welcher angehalten werden kann. 

	Wenn man etwas stoppen und weiterführen will, braucht es einfach einen zweiten Thread, welcher nicht gestoppt wird, und der dann den Befehl zum weiter machen geben kann. Das bedeutet, man könnte entweder: 

	(1) Die QSim in einem Thread packen und aus dem Main-Thread (oder einem anderen, dritten Thread) heraus doStep()/notifyAll() aufrufen.

	oder: 

	(2) Den Steuerungs-Teil in einen Thread packen, und die QSim im Main-Thread lassen. So wird der Main-Thread angehalten. Ist technisch gesehen absolut gleichwertig, da der Main-Thread in Java auch einfach ein normaler Thread ist. (die JVM beendet normalerweise, sobald kein Thread (resp. kein Daemon-Thread) mehr läuft. Das heisst, man könnte im Main-Thread auch einfach einen anderen Thread starten und dann die ganze Arbeit da drin machen, und die main-Methode gleich wieder verlassen. Das Programm würde weiterlaufen, bis der gestartete Thread beendet).



	Da in der GUI-Programmierung sowieso sehr viel mit Threads gearbeitet werden muss (das UI sollte bei längeren Berechnungen ja nicht blockieren), war es beim OTFVis wohl sinnvoller/einfacher, die QSim im Main-Thread zu lassen und die Steuerung separat zu haben. Zudem: sobald man ein Swing-GUI erstellt, läuft dieses in einem eigenen Thread (dem sogenannten Events Dispatch Thread). Das heisst, es muss gar nicht explizit ein Thread erzeugt werden, ein Swing-Fenster anzeigen mit Buttons drin, und schon werden alle Button-Clicks in einem separaten Thread ausgeführt.


	Für allgemeine Anwendungsfälle (also auch ohne GUI) könnte es aber einfacher sein, wenn die PlayPauseSimulation in einem eigenen Thread abläuft, und man die Steuerung dann quasi aus dem Main-Thread machen könnte. Ansonsten müssten die Anwender selber einen eigenen Thread erzeugen, um die Simulation zu steuern, was zwar machbar, aber komplexer, fehleranfälliger und weniger intuitiv ist.


	Gruss
	Marcel
*/
	public static enum Status {
		PAUSE, PLAY, STEP, FINISHED;
	}

	public class AccessToBlockingEtc {
		public final void blockOtherUpdates() {
			try {			
				accessToQNetwork.acquire();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		public void unblockOtherUpdates() {
			accessToQNetwork.release();
		}
		public void updateStatus(double time) {
			localTime = (int) time ;
			if ( status == Status.STEP) {
				// Time and Iteration reached?
				if ( stepToTime <= localTime ) {
					synchronized (stepDone) {
						stepDone.notifyAll();
						status = Status.PAUSE ;
					}
				}
			}
			synchronized(paused) {
				while (status == Status.PAUSE) {
					try {
						paused.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	private volatile Status status = Status.PAUSE;
	private Semaphore accessToQNetwork = new Semaphore(1, true);
	private volatile int localTime = 0;
	private volatile double stepToTime = 0;

	private final Object paused = new Object();
	private final Object stepDone = new Object();
	private final Object updateFinished = new Object();
	private AccessToBlockingEtc internalInterface = new AccessToBlockingEtc() ;
	
	@Override
	public final void doStep(int time) {
		// leave Status on pause but let one step run (if one is waiting)
		synchronized(paused) {
			setStepToTime(time);
			status = Status.STEP ;
			paused.notifyAll();
		}
		synchronized (stepDone) {
			if (status == Status.PAUSE) return;
			try {
				stepDone.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public final void pause() {
		if (status != Status.FINISHED) {
			synchronized (updateFinished) {
				status = Status.PAUSE ;
			}
		}
	}

	@Override
	public final void play() {
		if (status != Status.FINISHED) {
			synchronized(paused) {
				status = Status.PLAY ;
				paused.notifyAll();
			}
		}
	}

	public final void setListener(PlayPauseMobsimListener queueSimulationFeature) {
		queueSimulationFeature.setInternalInterface( internalInterface ) ;
	}
	
	// for everything below here, I am not yet sure which of these need to be there. kai, mar'15

	void setStepToTime(double stepToTime) {
		this.stepToTime = stepToTime;
	}

	public int getLocalTime() {
		return localTime;
	}

	Semaphore getAccessToQNetwork() {
		return accessToQNetwork;
	}

	Status getStatus() {
		return status;
	}

	void setStatus(Status status) {
		this.status = status;
	}

}
