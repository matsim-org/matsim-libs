
-- COPY links (gid, source, target, length, x1, y1, x2, y2) FROM '/Users/michaelzilske/workspace/detailedEval/Net/network.csv' WITH CSV



-- SELECT AddGeometryColumn('trips', 'the_geom', '32632', 'LINESTRING', 2);
-- SELECT dropgeometrycolumn('trips', 'the_geom')
-- select astar_sp_delta_cc_directed('edges', 666, 777, 10.0, 'length', true, false)

-- select dijkstra_sp('edges', 666, 777)

-- select *, setsrid(makepoint(x1,y1),32632) from trips limit 1000

-- select g1.* from (select t.person_id, (pgis_fn_nn(setsrid(makepoint(t.x1,t.y1),32632), 1000, 1, 500, 'edges', 'true', 'gid', 'the_geom')).*  from (select * from trips limit 1000) t) as g1

-- update trips set link1=g1.nn_gid from (select t.person_id, (pgis_fn_nn(setsrid(makepoint(t.x1,t.y1),32632), 1000, 1, 500, 'edges', 'true', 'gid', 'the_geom')).*  from (select * from trips where link1 is null) t) as g1 where trips.person_id = g1.person_id
-- Query returned successfully: 896497 rows affected, 4674190 ms execution time.

-- truncate trips;
--  COPY trips (gid, person_id, plan_element_idx, x1, y1, x2, y2, mode) FROM '/Users/michaelzilske/workspace/detailedEval/pop/trips.csv' WITH CSV;
--  update trips set the_geom=setsrid(makeline(makepoint(x1,y1),makepoint(x2,y2)),32632);
--  update trips set start_geom=st_startpoint(the_geom);
--  update trips set end_geom = st_endpoint(the_geom);

-- Query returned successfully: 908497 rows affected, 30139 ms execution time.

-- select * from trips where link1 is not null

-- select astext(makepoint(5.0, 4.0))

-- CREATE INDEX geom_idx ON links USING gist (the_geom);

-- select mode from trips

-- select nn.*, trips.*
-- from (
-- select 
--  (pgis_fn_nn(input.startpoint, 10000, 5000, 1, '(select gid, mode, startpoint(the_geom) as the_geom from trips)', 'mode = ''car''', 'gid', 'the_geom')).* ,
--   input.endpoint as input_endpoint 
-- from 
--  (select startpoint(the_geom) as startpoint, endpoint(the_geom) as endpoint from trips where gid = 10155) as input) as nn, trips
-- where nn_gid = trips.gid and st_distance(st_endpoint(trips.the_geom), input_endpoint) <= nn_dist 


-- select count(*) from trips




-- select * from next_best_car_trip where nn_dist is not null order by nn_dist

-- select * from trips where start_geom = end_geom order by mode, person_id 

select st_length(the_geom) from trips

-- select * from next_best_car_trip order by pt_trip_idx
-- select * from trips where mode = 'pt'

-- select start_geom, st_union(start_geom,end_geom), geometrytype(st_union(start_geom,end_geom)) from trips where gid = 8
-- select * from trips where start_geom && st_buffer((select start_geom from trips where gid = 8), 1000)



-- select (mz_cluster_car_alternative(the_geom)).* from trips where gid = 8

-- select pgis_fn_nn((select startpoint(the_geom) from trips where gid = 10155), 10000, 1000, 20, '(select gid, mode, startpoint(the_geom) as the_geom from trips)', 'mode = ''car''', 'gid', 'the_geom') 
-- 
-- select * from trips where    distance((select startpoint(the_geom) from trips where gid = 1172), startpoint(the_geom))   < 1000
-- 			 and distance((select endpoint(the_geom) from trips where gid = 1172), endpoint(the_geom)) < 1000
--			or distance((select startpoint(the_geom) from trips where gid = 1172), endpoint(the_geom))   < 1000
--			 and distance((select endpoint(the_geom) from trips where gid = 1172), startpoint(the_geom)) < 1000
