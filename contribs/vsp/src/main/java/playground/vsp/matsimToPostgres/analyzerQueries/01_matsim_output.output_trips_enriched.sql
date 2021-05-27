Select *,
	ST_SetSRID(ST_MakePoint(start_x, start_y), 31468) as start_point,
	ST_SetSRID(ST_MakePoint(end_x, end_y), 31468) as end_point,
	ST_MakeLine(ST_SetSRID(ST_MakePoint(start_x, start_y), 31468), ST_SetSRID(ST_MakePoint(end_x, end_y), 31468)) as trip_line,
	CASE
		WHEN (EUCLIDEAN_DISTANCE < 500) THEN 'unter_500m'::text
		WHEN ((EUCLIDEAN_DISTANCE >= 500) AND (EUCLIDEAN_DISTANCE < 1000)) THEN '500m_bis_1km'::text
		WHEN ((EUCLIDEAN_DISTANCE >= 1000) AND (EUCLIDEAN_DISTANCE < 2000)) THEN '1km_bis_2km'::text
		WHEN ((EUCLIDEAN_DISTANCE >= 2000) AND (EUCLIDEAN_DISTANCE < 5000)) THEN '2km_bis_5km'::text
		WHEN ((EUCLIDEAN_DISTANCE >= 5000) AND (EUCLIDEAN_DISTANCE < 10000)) THEN '5km_bis_10km'::text
		WHEN ((EUCLIDEAN_DISTANCE >= 10000) AND (EUCLIDEAN_DISTANCE < 20000)) THEN '10km_bis_20km'::text
		WHEN ((EUCLIDEAN_DISTANCE >= 20000) AND (EUCLIDEAN_DISTANCE < 50000)) THEN '20km_bis_50km'::text
		WHEN ((EUCLIDEAN_DISTANCE >= 50000) AND (EUCLIDEAN_DISTANCE < 100000)) THEN '50km_bis_100km'::text
		ELSE 'ueber_100km'::text
	END as distance_group
FROM matsim_output.output_trips