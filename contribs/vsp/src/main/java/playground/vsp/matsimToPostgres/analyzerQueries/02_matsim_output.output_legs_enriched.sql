Select *,
	ST_SetSRID(ST_MakePoint(start_x, start_y), 31468) as start_point,
	ST_SetSRID(ST_MakePoint(end_x, end_y), 31468) as end_point,
	ST_MakeLine(ST_SetSRID(ST_MakePoint(start_x, start_y), 31468), ST_SetSRID(ST_MakePoint(end_x, end_y), 31468)) as leg_line
FROM matsim_output.output_legs