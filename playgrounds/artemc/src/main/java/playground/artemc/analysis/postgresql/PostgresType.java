package playground.artemc.analysis.postgresql;

public enum PostgresType {
	FLOAT8(128), INT(64), TEXT(1000), BOOLEAN(8);
	private final int size;

	PostgresType(int size) {
		this.size = size;
	}

	public int size() {
		return size;
	}
}
