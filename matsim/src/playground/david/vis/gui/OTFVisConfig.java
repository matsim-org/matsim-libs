package playground.david.vis.gui;

import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.config.Module;
import org.matsim.config.groups.GlobalConfigGroup;

public class OTFVisConfig extends Module {
	public static final String GROUP_NAME = "otfvis";

	public OTFVisConfig() {
		super(GROUP_NAME);
	}

	public static final String AGENT_SIZE = "agentSize";
	public static final String MIDDLE_MOUSE_FUNC = "middleMouseFunc";
	public static final String LEFT_MOUSE_FUNC = "leftMouseFunc";
	public static final String RIGHT_MOUSE_FUNC = "rightMouseFunc";

	private  float agentSize = 100.f;
	private  String middleMouseFunc = "Pan";
	private  String leftMouseFunc = "Zoom";
	private  String rightMouseFunc = "Menu";

	private static final Logger log = Logger.getLogger(GlobalConfigGroup.class);

	@Override
	public String getValue(final String key) {
		if (AGENT_SIZE.equals(key)) {
			return Float.toString(getAgentSize());
		} else if (MIDDLE_MOUSE_FUNC.equals(key)) {
			return middleMouseFunc;
		} else if (LEFT_MOUSE_FUNC.equals(key)) {
			return leftMouseFunc;
		}  else if (RIGHT_MOUSE_FUNC.equals(key)) {
			return rightMouseFunc;
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public void addParam(final String key, final String value) {
		if (AGENT_SIZE.equals(key)) {
			agentSize = Float.parseFloat(value);
		} else if (MIDDLE_MOUSE_FUNC.equals(key)) {
			middleMouseFunc = value;
		} else if (LEFT_MOUSE_FUNC.equals(key)) {
			leftMouseFunc = value;
		}  else if (RIGHT_MOUSE_FUNC.equals(key)) {
			rightMouseFunc = value;
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	protected final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		map.put(AGENT_SIZE, getValue(AGENT_SIZE));
		map.put(LEFT_MOUSE_FUNC, getValue(LEFT_MOUSE_FUNC));
		map.put(MIDDLE_MOUSE_FUNC, getValue(MIDDLE_MOUSE_FUNC));
		map.put(RIGHT_MOUSE_FUNC, getValue(RIGHT_MOUSE_FUNC));
		return map;
	}

	/* direct access */

	public float getAgentSize() {
		return agentSize;
	}

	public void setAgentSize(float agentSize) {
		this.agentSize = agentSize;
	}

	public String getMiddleMouseFunc() {
		return middleMouseFunc;
	}

	public void setMiddleMouseFunc(String middleMouseFunc) {
		this.middleMouseFunc = middleMouseFunc;
	}

	public String getLeftMouseFunc() {
		return leftMouseFunc;
	}

	public void setLeftMouseFunc(String leftMouseFunc) {
		this.leftMouseFunc = leftMouseFunc;
	}

	public String getRightMouseFunc() {
		return rightMouseFunc;
	}

	public void setRightMouseFunc(String rightMouseFunc) {
		this.rightMouseFunc = rightMouseFunc;
	}

	
}
