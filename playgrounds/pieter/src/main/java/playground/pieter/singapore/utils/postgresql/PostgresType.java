package playground.pieter.singapore.utils.postgresql;

public enum PostgresType {
	FLOAT8(64), INT(32), TEXT(500), BOOLEAN(4);
	private final int size;

	PostgresType(int size) {
		this.size = size;
	}

	public int size() {
		return size;
	}
}
