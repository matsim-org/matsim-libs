package vrp.algorithms.ruinAndRecreate.recreation;

public interface RecreationListener {
	public void inform(RecreationEvent event);
	public void finish();
}
