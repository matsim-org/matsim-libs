-- drop table next_best_car_trip;
-- create table next_best_car_trip (pt_trip_idx integer, car_trip_idx integer, nn_dist double precision);
-- 

-- truncate next_best_car_trip;
-- insert into next_best_car_trip select nexts.car_gid, (nexts.nn).nn_gid, (nexts.nn).nn_dist from (select trips.gid as car_gid, mz_cluster_alternative(trips.the_geom, 'pt') as nn from trips where mode = 'car') as nexts
-- 

-- create view clustering as
-- select pt_trip.person_id as pt_person, pt_trip.plan_element_idx as pt_idx, car_trip.person_id as car_person, car_trip.plan_element_idx as car_idx from next_best_car_trip as joint, trips as pt_trip, trips as car_trip
-- where joint.pt_trip_idx = pt_trip.gid and joint.car_trip_idx = car_trip.gid

copy (select * from clustering) to '/Users/michaelzilske/workspace/detailedEval/pop/befragte-personen/clustering.txt' with header delimiter ';' csv
-- select * from trips where mode = 'car'

-- select avg(factor), min(factor), max(factor), stddev(factor) from (select survey_time / routed_time as factor from pt_leg_times) factors

select factor from (select survey_time / routed_time as factor from pt_leg_times) factors
