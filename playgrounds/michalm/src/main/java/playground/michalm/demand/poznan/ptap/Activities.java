/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.michalm.demand.poznan.ptap;

import static playground.michalm.demand.poznan.ptap.Activities.ActivityType.*;


public class Activities
{

    static enum ActivityType
    {
        D, //dom
        P, //praca
        S, //szkola
        U, //uczelnia
        Z, //zakupy
        CH, //centrum handlowe
        W, //wypoczynek,
        R, //rozrywka
        I, //inne
        ND; //nie-dom
    }


    static enum ActivityPair
    {
        D_P(D, P), //
        P_D(P, D), //
        D_S(D, S), //
        S_D(S, D), //
        D_U(D, U), //
        U_D(U, D), //
        D_Z(D, Z), //
        Z_D(Z, D), //
        D_CH(D, CH), // 
        CH_D(CH, D), // 
        D_W(D, W), //
        W_D(W, D), //
        D_R(D, R), //
        R_D(R, D), //
        D_I(D, I), //
        I_D(I, D), //
        NzD(ND, ND); // niezwiazane z domem

        final ActivityType from, to;


        private ActivityPair(ActivityType from, ActivityType to)
        {
            this.from = from;
            this.to = to;
        }
    }
}