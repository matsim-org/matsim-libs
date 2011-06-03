package playground.mzilske.withinday;


public interface TeleportationWorld {

	double getTime();

	/**Design thoughts:<ul>
	 * <li>stopping is supported nearly nowhere, since it does not make sense.  An airplane can also not
	 * just stop.  Shouldn't we get rid of this method?  kai, jun'11
     * </ul>
	 */
	void stop();

}
