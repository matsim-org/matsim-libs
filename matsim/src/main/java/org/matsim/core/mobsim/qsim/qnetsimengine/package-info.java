
/* *********************************************************************** *
 * project: org.matsim.*
 * package-info.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 /**
 * <h3> From a recent email </h3>
 * 
 * Hallo,

Habe QNetworkFactory und related restrukturiert.  

Im Grunde sieht's jetzt m.E. ganz gut aus, aber es ging wie meistens nicht ganz so, wie ich mir das gedacht hatte, und dann wurde die Zeit etwas knapp.

Im Prinzip:

QLaneI

... wird hineingesteckt in ...

QLinkI

Die zentralen Service-Implementations davon sind

 QueueWithBuffer

und

 AbstractQLink bzw. QLinkImpl

In beiden Fällen gibt es Builder, um die (jetzt recht vielen) constructor-parameters abzufangen.

In den meisten Fällen baut die QNetworkFactory erst eine QLaneI-Factory, und übergibt die an den QLinkI-Creator.  Das geht u.a. deswegen nicht anders, weil QLinkLanesImpl mehr als eine Instanz von QLane braucht.

Siehe DefaultQNetworkFactory und ConfigurableQNetworkFactory (bei letzterer könnte man bei Bedarf noch den einen oder anderen setter einbauen).

---

Ich habe versucht, zwischen 

* zentralen Objekten eines runs (Scenario, EventsManager)
* zentralen Objekten der Mobsim (in jeder Iteration neu)
* QLinkI-spezifischen Objekten

zu unterscheiden.

Zu letzterem habe ich auch VehicleQ und LinkSpeedCalculator gezählt; die können somit nun im Prinzip für jede Kante separat gesetzt werden.

---

Weiterhin bin ich davon ausgegangen, dass Objekte back pointer (also z.B. QLaneI auf QLinkI) haben, aber diese die back pointer nicht rausgeben.  Somit sind Verkettungen von back pointers nun nicht mehr möglich; QLane kommt bis auf QLink hoch und dann ist Schluss.  "Weiter oben" habe ich das allerdings noch nicht durchgeführt.  Generell scheint das aber zu reichen; die nötigen zentralen Objekte sind im NetsimEngineContext.

---

Wenn gewünscht, ist QNetworkFactory nun über die addOverridingModule-Syntax per guice setzbar.  Siehe {@link LinkSpeedCalculatorIntegrationTest} sowie Beispiele unten.

---

Gregor: M.E. könnte es sein, dass Du für "CA" nur noch QLaneI und gar nicht mehr QLink austauschen musst.  Auf jeden Fall sollte es möglich sein, "AbstractQLink" zu verwenden und damit auf zentrale Infrastruktur zurückzugreifen.  Weiß nicht, wie viel Du hier noch Zeit hast.

---

Derzeit ist das alles immer noch package-protected, weil ich es gerne ein wenig ruhen lassen würde.  

Ich hoffe, dass das alles so funktioniert, oder Ihr Euch zurecht findet.  Ich schaue morgen noch in die Email, danach erstmal nicht mehr.

Viele Grüße

Kai
 * 
 * <h3> OLD MATERIAL, NOT CHECKED </h3>
 * 
 * This package is <i>ancient</i> and correspondingly <i>archaic.</i>  The queue simulation was (evidently) one of the first pieces
 * which had to be in place.  It then got patched up and patched up, for example by becoming more deterministic, including the public transit
 * dynamics, including lanes, including signals, including "additional agents on link", etc. etc.
 * <p>
 * Eventually, the qnetsimengine was carved out of the monolithic mess.  Now the qnetsimengine attempts to provide a somewhat meaningful
 * interface to the outside world, but internally it is still more complex than is good.
 * <p>
 * With respect to that public interface: The QSim needs one simengine which provides a "service network" to other pieces, where for example
 * agents at activities or waiting for pt can be located, in particular for the visualizer.  This "service network" capability could, in principle, be
 * moved to a separate simengine.  This would result in cleaner code ... but the provision for visualization (otfvis) would need to be
 * extracted as well.
 * 
 * @see RunConfigurableQNetworkFactoryExample
 * @see RunFlexibleQNetworkFactoryExample
 */
package org.matsim.core.mobsim.qsim.qnetsimengine;