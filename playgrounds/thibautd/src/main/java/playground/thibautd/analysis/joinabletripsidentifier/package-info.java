package playground.thibautd.analysis.joinabletripsidentifier;

/**
 * improved event analyser to identify possible joint trips.
 * This module takes basically the same approach as the possiblesharedride package,
 * but:
 *
 * <ul>
 * <li> more complete and structured information is collected and written, to allow
 * a wider range of analysis with one data collection pass. Particularly, the joinable
 * trips are not only counted but identified, and the data is exported in XML rather
 * than flat text.
 * <li> the event exploration is intended to be faster (hopefully)
 * <li> the plan files are no longer analysis is purely event-based,
 * even though the information exported allows to use information from the plan
 * a posteriori).
 * </ul>
 */
