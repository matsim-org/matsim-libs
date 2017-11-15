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
package org.matsim.core.mobsim.framework;

/**
 * @author nagel
 *
 */
public interface PlayPauseSimulationControlI {
/*	Hallo Kai,

	auch “main” läuft in einem Thread, welcher angehalten werden kann. 

	Wenn man etwas stoppen und weiterführen will, braucht es einfach einen zweiten Thread, welcher nicht gestoppt wird, und der 
	dann den Befehl zum weiter machen geben kann. Das bedeutet, man könnte entweder: 

	(1) Die QSim in einem Thread packen und aus dem Main-Thread (oder einem anderen, dritten Thread) heraus 
	doStep()/notifyAll() aufrufen.

	oder: 

	(2) Den Steuerungs-Teil in einen Thread packen, und die QSim im Main-Thread lassen. So wird der Main-Thread angehalten. 
	Ist technisch gesehen absolut gleichwertig, da der Main-Thread in Java auch einfach ein normaler Thread ist. (die JVM 
	beendet normalerweise, sobald kein Thread (resp. kein Daemon-Thread) mehr läuft. Das heisst, man könnte im Main-Thread 
	auch einfach einen anderen Thread starten und dann die ganze Arbeit da drin machen, und die main-Methode gleich wieder 
	verlassen. Das Programm würde weiterlaufen, bis der gestartete Thread beendet).

	Da in der GUI-Programmierung sowieso sehr viel mit Threads gearbeitet werden muss (das UI sollte bei längeren 
	Berechnungen ja nicht blockieren), war es beim OTFVis wohl sinnvoller/einfacher, die QSim im Main-Thread zu lassen und die 
	Steuerung separat zu haben. Zudem: sobald man ein Swing-GUI erstellt, läuft dieses in einem eigenen Thread (dem 
	sogenannten Events Dispatch Thread). Das heisst, es muss gar nicht explizit ein Thread erzeugt werden, ein Swing-Fenster 
	anzeigen mit Buttons drin, und schon werden alle Button-Clicks in einem separaten Thread ausgeführt.

	Für allgemeine Anwendungsfälle (also auch ohne GUI) könnte es aber einfacher sein, wenn die PlayPauseSimulation in 
	einem eigenen Thread abläuft, und man die Steuerung dann quasi aus dem Main-Thread machen könnte. Ansonsten müssten 
	die Anwender selber einen eigenen Thread erzeugen, um die Simulation zu steuern, was zwar machbar, aber komplexer,
	fehleranfälliger und weniger intuitiv ist.

	Gruss
	Marcel
*/
	
	public void pause();
	
	public void play();

	void doStep(int time);
	
}
