# CHANGELOG

**1.0.10-dev**

- Add trip index to DiscreteModeChoiceTrip
- Make NonSelectedPlanSelector raise exception if too many plans are available
- Add preprouted modes to `AbstractTripRouterEstimator`
- Interpret an undefined start time as 00:00:00
- Changed `ActivityTourFinder` and `ActivityHomeFinder`to not only check for a single activity type, but now a list of activity types can be provided through the `activityTypes` parameter
- Added `HomeTourFinder` which determines tours by using the location from the home finder
- Added `HomeFinder` as a general component that can be bound in `AbstractDiscreteModeChoiceExtension`
- `MATSimTripCandidate` was removed because now all `TripCandidate`s provide the trip duration
- Make delay accumulation optional through `accumulateEstimationDelays`
- BC: Improve time calculation and propagation of delays during estimation
- Include duration in estimated routed trips
- Remove unintended print in TripListConverter
- Fix integer overflow exceptions for tours with many trips in DefaultModeChainGenerator
- Add `accumulateDelays` option to sum up delays in TripListConverter (default `false`)
- Fix bug in TripListConverter when estimating trip durations for maximum duration activities
- Improve/add tests for ScheduleWaitingTimeEstimator
- Add Apollo reader

**1.0.9**

- Make PlanBuilder consistent with SubtourModeChoice
- Improve ModeAvailability documentation
- Fix behaviour of DMC with maximum duration activities
- Add filters to DiscreteModeChoiceModule
- Use "routing mode" instead of MainModeIdentifier
- Allow ReRoute strategy in combination with DCM
- Add TourLengthFilter and document filters
- BC: Remove over-complicated generics for UtilitySelector/Factory
- Update MATSim version, fix MainModeIdentifier and remove StageActivityTypes
- Make initial plan elements available to mode choice process

**1.0.8**

- Fix duplicate config parametersets
- Add getter to DiscreteModeChoiceConfigGroup
- Add Sioux Falls as integration test for SubtourModeChoiceReplacement
- Fix error messages in EstimatorModule
- Fix wrong initialization of array size in DiscreteModeChoiceAlgorithm
- Add IndexUtils to clarify how to calculate trip indices
- Rename index variables in DiscreteModeChoiceTrip to avoid confusion

**1.0.7**

- Switch to GitFlow repository model
- Update to MATSim 12
- Change version scheme to be in line with MATSim

**1.0.5**

- Fix cases where no trip/tour constraints are given
- Add problem filters (TripFilter, TourFilter)

**1.0.4**

- Fix bug in MATSimDayScoringEstimator
- Better handling of max_dur in activities
- Switch to weekly SNAPSHOT instead of continuous SNAPSHOT of MATSim
- Add check and warning for NaN utilities

**1.0.3**

- Fix HomeActivityFinder (was just based on links rather than BasicLocation before)
- Fix buggy vehicle constraints
- Make inference of origin/destination facility in AbstractTripRouterEstimator compatible with PlanRouter
- Fix MNL selection

**1.0.2**

- Fix 'restricted mode' setters for LinkAttributeConstraint and ShapeFileConstraint configuration
- Put in caching of trips again (it fell out accidentally during refactoring)
- Attach sources to maven artifacts

**1.0.1**

- Fix MATSimScoringEstimator parallelization
- Generalize MATSimTripScoringEstimator to all modes in scoring config
- Fix: Multinomial logit was filtering for < -minimumUtility
- Fix initial choice fallback for TourModel

**1.0.0**

- First stable release after refactoring
