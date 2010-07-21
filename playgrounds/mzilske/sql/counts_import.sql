-- create temp table export_counts as
-- select vnets.messstelle, vnets.richtung, hour_counts.h, sum(hour_counts.sum), links.link_id
-- from (select detektor, zeit / (60*60) as h, sum(qkfz)  from counts group by detektor,h order by detektor,h) as hour_counts, vnets, aggr_vnets, links
-- where hour_counts.detektor = vnets.detektor_i
-- and vnets.messstelle = aggr_vnets.messstelle
-- and vnets.richtung = aggr_vnets.richtung
-- and links.gid = aggr_vnets.link_id
-- group by vnets.messstelle,vnets.richtung,h,links.link_id
-- order by vnets.messstelle,vnets.richtung,h

select * from export_counts where sum = 0

-- copy export_counts to '/Users/michaelzilske/workspace/detailedEval/net/counts.csv' WITH DELIMITER ';' CSV