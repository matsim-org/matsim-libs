package org.matsim.modechoice;

import com.google.common.base.Joiner;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import jakarta.validation.constraints.Positive;
import org.matsim.application.Category;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.utils.objectattributes.attributable.Attributes;

import java.util.Map;
import java.util.Objects;

/**
 * Target parameters for mode shares.
 * This group allows arbitrary attributes to be defined, which are matched against person attributes.
 */
public final class ModeTargetParameters extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "modeTargetParameters";

	private static final String SHARES = "shares";

	private static Joiner.MapJoiner JOINER = Joiner.on(",").withKeyValueSeparator("=");

	@Parameter
	@Positive
	@Comment("Allowed tolerance for mode share.")
	public double tolerance = 0.01;

	private Object2DoubleMap<String> shares = new Object2DoubleOpenHashMap<>();

	public ModeTargetParameters() {
		super(GROUP_NAME, true);
	}

	public ModeTargetParameters(String subpopulation, Map<String, Double> shares) {
		super(GROUP_NAME, true);
		this.shares.putAll(shares);
		this.addParam("subpopulation", subpopulation);
	}

	/**
	 * Name of the config group.
	 */
	public String getGroupName() {
		Map<String, String> params = getParams();
		params.remove("shares");
		params.remove("tolerance");
		return JOINER.join(params);
	}

	public Object2DoubleMap<String> getShares() {
		return shares;
	}

	@StringSetter(SHARES)
	void setShares(String shares) {
		this.shares = new Object2DoubleOpenHashMap<>();
		String[] parts = shares.split(",");
		for (String part : parts) {
			String[] kv = part.split("=");
			this.shares.put(kv[0], Double.parseDouble(kv[1]));
		}
	}

	@StringGetter(SHARES)
	String getSharesString() {
		return JOINER.join(shares);
	}


	/**
	 * Match attributes from an object with parameters defined in config.
	 */
	public boolean matchPerson(Attributes attr, Map<String, Category> categories) {

		for (Map.Entry<String, String> e : getParams().entrySet()) {
			if (e.getKey().equals(SHARES) || e.getKey().equals("tolerance"))
				continue;

			// might be null if not defined
			Object objValue = attr.getAttribute(e.getKey());
			String category = categories.get(e.getKey()).categorize(objValue);

			// compare as string
			if (!Objects.toString(category).equals(e.getValue()))
				return false;
		}

		return true;
	}

}
