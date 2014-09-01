package playground.mrieser.core.ids;

public class NewIdentifiableUseCases {

	public static interface Facility<T extends Facility> extends NewIdentifiable<T> {
		@Override
		public NewId<T> getId();
	}

	public static class ActivityFacility implements Facility<ActivityFacility> {
		@Override
		public NewId<ActivityFacility> getId() {
			return null;
		}
	}

	public static class TransitStopFacility implements Facility<TransitStopFacility> {
		@Override
		public NewId<TransitStopFacility> getId() {
			return null;
		}
	}

	public static class SuperDuperActivityFacility extends ActivityFacility {
//		@Override
//		public NewId<ActivityFacility> getId() {
//			return null;
//		}
		
//		@Override
//		public NewId<SuperDuperActivityFacility> getId() {
//			return null;
//		}

		public NewId<SuperDuperActivityFacility> getId2() {
			return null;
		}
	}
}
