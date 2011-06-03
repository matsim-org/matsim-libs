package playground.mzilske.withinday;

import org.matsim.api.core.v01.Id;

public interface DrivingWorld {

    /**Design thoughts:<ul>
     * <li> I have never understood why the agent should be able to get the time from the global world, but nothing else.  
     * Either knowledge is local.  Or it is not.  Or we decide that time is the only exception to local knowledge.
     * </ul>
     */
    double getTime();

	/**Design thoughts:<ul>
	 * <li> I don't find "park" a particularly meaningful statement within the matsim mobsim logic.  You can, at best,
	 * ``parkAtNextPossibility''.  In the current qsim, this will be the next link2buffer ... but on a freeway it might have
	 * to wait for the next exit.  kai, jun'11
	 * </ul>
	 * 
	 */
	void park();

	/**Design thoughts:<ul>
	 * <li> Maybe "setNextLinkId()"?  The meaning would be that there will be a pre-cached result for the next call to
	 * ``chooseNextLinkId().  On the other hand, it is not fully clear why we do not just pass the call to "chooseNextLinkId()"
	 * to the corresponding computation.  This is not very intuitive in terms of autonomous agent design (with internal
	 * clock), but it is computationally far more efficient.  kai, jun'11
	 * </ul>
	 */
	void nextTurn(Id poll);

	/**Design thoughts:<ul>
	 * <li> This means in practice that the nextLinkId is not known.  Given that the agent could also just try to park,
	 * I don't find this very meaningful.  Could also be "isKnowningNextLinkId()".  If so, we could as well return null from 
	 * chooseNextLinkId().  kai, jun'11
	 * </ul>
	 */
	boolean requiresAction();

}
