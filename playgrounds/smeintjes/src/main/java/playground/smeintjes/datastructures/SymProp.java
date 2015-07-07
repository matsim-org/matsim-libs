/* 
 * Copyright (C) 2013 Maarten Houbraken
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Software available at https://github.com/mhoubraken/ISMAGS
 * Author : Maarten Houbraken (maarten.houbraken@intec.ugent.be)
 */
package playground.smeintjes.datastructures;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Groups all information on the symmetric properties of the motif
 */
public class SymProp {

    public int nrMotifNodes;
    List<int[]> perms;
    Map<Integer, Set<Integer>> smaller;
    Map<Integer, Set<Integer>> larger;

    public SymProp(int nrMotifNodes, Map<Integer, Set<Integer>> smaller, Map<Integer, Set<Integer>> larger) {
        this.nrMotifNodes = nrMotifNodes;
        perms = new ArrayList<int[]>();
        this.smaller = smaller;
        this.larger = larger;
    }

    public void addPermutation(int[] perm) {
        perms.add(perm);
    }

    /**
     * Extracts symmetry-breaking constraints from the orbit of the specified
     * motif node
     *
     * @param motifNodeID motif node to generate constraints for
     * @param orbits orbit partition
     */
    public void fix(int motifNodeID, int[] orbits) {
        int orbit = orbits[motifNodeID];
        if (orbit == -1) {
            return;
        }
        for (int i = motifNodeID + 1; i < orbits.length; i++) {
            int orbiti = orbits[i];
            if (orbit == orbiti) {
                addConstraint(motifNodeID, i);
            }
        }
    }

    /**
     * Adds a constraint of the form lowerID&lt;higherID to the symmetric
     * properties. Constraints are transitively propagated.
     *
     * @param lowerID
     * @param higherID
     */
    public void addConstraint(int lowerID, int higherID) {
        Set<Integer> as = smaller.get(lowerID);
        Set<Integer> bs = smaller.get(higherID);
        Set<Integer> al = larger.get(lowerID);
        Set<Integer> bl = larger.get(higherID);
        if (as == null) {
            as = new HashSet<Integer>();
            smaller.put(lowerID, as);
        }
        if (bs == null) {
            bs = new HashSet<Integer>();
            smaller.put(higherID, bs);
        }
        if (al == null) {
            al = new HashSet<Integer>();
            larger.put(lowerID, al);
        }
        if (bl == null) {
            bl = new HashSet<Integer>();
            larger.put(higherID, bl);
        }
        as.add(higherID);
        bl.add(lowerID);
        as.addAll(bs);
        bl.addAll(al);
        for (Integer i : bs) {
            larger.get(i).add(lowerID);
        }
        for (Integer i : al) {
            smaller.get(i).add(higherID);
        }
    }
}
