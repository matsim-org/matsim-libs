/**
 * Implements custom road pricing: the toll agents have to pay is dependent
 * on the vehicle type of the agent. The vehicle type is retrieved from the
 * vehicle Id, see {@link playground.jjoubert.roadpricing.senozon.SanralTollFactor}
 * for the calculation of the factor from the vehicle id.
 *
 * To use it, it is important to:
 * <ul>
 * <li>Use the special {@link playground.jjoubert.roadpricing.senozon.SanralControler}</li>
 * <li><em>Not</em> to have "useRoadPricing" enabled in the configuration in the scenario module.
 *     This is required in order to not load the default road pricing, but that the
 *     SanralControler can inject its own special road pricing implementation.</li>
 * </ul>
 */
package playground.jjoubert.roadpricing.senozon;
