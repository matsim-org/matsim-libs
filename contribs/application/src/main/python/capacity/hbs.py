import math
import matplotlib.pyplot as plt
import plotly.graph_objects as go


def capacity_estimate(v):
    # headway
    tT = 1.2
    # car length
    lL = 7.0
    Qc = v / (v * tT + lL)
    return 3600 * Qc


def merge(qp=400):
    tf = 3.5
    tg = 6.36
    return math.exp((-qp / 3600) * (tg - tf / 2)) * 3600 / tf


class Street:
    def __init__(self, osm_type, lanes, speed, curvature):
        self.osm_type = osm_type
        self.lanes = lanes
        self.speed = speed
        self.curvature = curvature


def calc_capacity_stadtstrasse(street):
    # Source: HSB S3
    k = 0
    a = 54
    b = 0.850

    if street.lanes == 1:
        f = 1.0
        # Assume middle-high "Erschließungsintensität"
        if street.speed == 30:
            k = 45
            a = 38
            b = 0.715
        elif street.speed == 50:
            k = 45
            a = 54
            b = 0.850
        elif street.speed == 70:
            k = 40
            a = 89
            b = 0.846

        return (-1.0 * b * math.pow(k, 3 / 2) * math.sqrt(4 * a * f + b * b * k) + 2 * a * f * k + b * b * k * k) / (
                2 * f * f)
    elif street.lanes == 2:
        f = 0.7
        if street.speed == 50:
            k = 45
            a = -0.009
            b = 55.58
        elif street.speed == 70:
            f = 0.5
            k = 40
            a = -0.008
            b = 80

        return (b * k) / (f - a * k)


def calc_capacity_landstrasse(street):
    # Source: HSB table L3-4
    if street.lanes == 1:
        k = 20

        a = 0
        b = 0

        # Table L3-4
        if street.curvature == 1:
            a = 98.73
            b = 0.8175
        elif street.curvature == 2:
            a = 83.88
            b = 0.8384
        elif street.curvature == 3:
            a = 74.41
            b = 0.6788
        elif street.curvature == 4:
            a = 68.02
            b = 0.6539
        else:
            raise ValueError(f"Unknown curvature {street.curvature}")

        return 0.5 * (-b * math.pow(k, 3 / 2) * math.sqrt(4 * a + b * b * k) + 2 * a * k + b * b * k * k)
    if street.lanes == 2:
        k = 48
        a = 55.5
        b = -0.614

        # Divide by 2 because the formula is for both directions
        return k * (2 * a + b * k) / 2


def calc_capacity_autobahn(street):
    # Source: HSB A3
    anzahl_fahrstreifen = 1
    if street.lanes <= 2:
        if street.speed >= 130:
            return 3700 / anzahl_fahrstreifen
        elif street.speed == 120:
            return 3800 / anzahl_fahrstreifen
        elif street.speed <= 120:
            return 3750 / anzahl_fahrstreifen
    elif street.lanes == 3:
        if street.speed >= 130:
            return 5300 / anzahl_fahrstreifen
        elif street.speed == 120:
            return 5400 / anzahl_fahrstreifen
        elif street.speed <= 120:
            return 5350 / anzahl_fahrstreifen
    else:
        if street.speed >= 130:
            return 7300 / anzahl_fahrstreifen
        elif street.speed == 120:
            return 7400 / anzahl_fahrstreifen
        elif street.speed <= 120:
            return 7400 / anzahl_fahrstreifen


osm_types = {
    'residential': 'Stadtstraßen',
    'unclassified': 'Landstraßen',
    'motorway': 'Autobahnen'
}

speeds = [30, 50, 70, 80, 90, 100, 120, 130]
curvatures = [1, 2, 3, 4]

capacities = {}
streets = []

for osm_type, street_type in osm_types.items():
    capacities[osm_type] = {}
    for lanes in range(1, 5):
        capacities[osm_type][lanes] = {}
        for curvature in curvatures:
            capacities[osm_type][lanes][curvature] = {}
            for speed in speeds:
                if (osm_type == 'residential' and speed > 70) or (osm_type == 'residential' and speed < 30) or (
                        osm_type == 'residential' and lanes > 2):
                    continue
                if (osm_type == 'unclassified' and speed > 100) or (osm_type == 'unclassified' and speed <= 50) or (
                        osm_type == 'unclassified' and lanes > 2):
                    continue
                if (osm_type == 'motorway' and lanes == 1) or (osm_type == 'motorway' and speed < 80):
                    continue
                street = Street(osm_type, lanes, speed, curvature)
                streets.append(street)
                capacities[osm_type][lanes][curvature][speed] = 0

for street in streets:
    capacity = 0
    if street.osm_type == 'residential':
        capacity = calc_capacity_stadtstrasse(street)
    elif street.osm_type == 'motorway':
        capacity = calc_capacity_autobahn(street)
    else:
        capacity = calc_capacity_landstrasse(street)
    capacities[street.osm_type][street.lanes][street.curvature][street.speed] = capacity

for street_type, lanes_capacity in capacities.items():
    for lanes, speeds_capacity in lanes_capacity.items():
        print(f"{osm_types[street_type]}, Fahrstreifen: {lanes}:")
        for curvature, capacity in speeds_capacity.items():
            for speed, capacity in capacity.items():
                print(f"Geschwindigkeit {speed} km/h: Kurvigkeit {curvature} Verkehrskapazität {capacity} ")

plt.figure(figsize=(14, 10))
for street_type, lanes_capacity in capacities.items():
    for lanes, speeds_capacity in lanes_capacity.items():
        for curvature, capacity in speeds_capacity.items():
            plt.plot(list(capacity.keys()), list(capacity.values()),
                     label=f"{osm_types[street_type]}, Fahrstreifen: {lanes} Kurvigkeit: {curvature}")

# plt.figure(figsize=(14, 10))
# for street_type, lanes_capacity in capacities.items():
#     for lanes, speeds_capacity in lanes_capacity.items():
#         for speed, curvatures_capacity in speeds_capacity.items():
#             for curvature, capacity in curvatures_capacity.items():
#                 plt.plot(speed, capacity,
#                          label=f"{osm_types[street_type]}, Fahrstreifen: {lanes}, Kurvigkeit: {curvature}")

speeds_compare = []
capacity_compare = []
for v in range(30, 130, 10):
    speeds_compare.append(v)
    capacity_compare.append(capacity_estimate(v))
plt.plot(speeds_compare, capacity_compare, label="Referenz")

plt.xlabel('Geschwindigkeit (km/h)')
plt.ylabel('Verkehrskapazität')
plt.title('Verkehrskapazität nach Straßentyp, Fahrstreifen und Geschwindigkeit')
plt.legend(loc='upper center', bbox_to_anchor=(0.5, -0.2), fancybox=True, ncol=4)
plt.subplots_adjust(bottom=0.3)
plt.show()

# fig = go.Figure()
#
# for street_type, lanes_capacity in capacities.items():
#     for lanes, speeds_capacity in lanes_capacity.items():
#         fig.add_trace(go.Scatter(x=list(speeds_capacity.keys()), y=list(speeds_capacity.values()),
#                                  mode='lines',
#                                  name=f"{osm_types[street_type]}, Fahrstreifen: {lanes}"))
#
# speeds_compare = []
# capacity_compare = []
# for v in range(50, 140, 10):
#     speeds_compare.append(v)
#     capacity_compare.append(capacity_estimate(v))
# fig.add_trace(go.Scatter(x=speeds_compare, y=capacity_compare, mode='lines', showlegend=True, name="REFERENZ"))
#
# for qp in (0, 200, 400, 600):
#     kontenpunkt_y = []
#     kontenpunkt_y.append(merge(qp))
#     kontenpunkt_y.append(merge(qp))
#     fig.add_trace(go.Scatter(x=(30, 50), y=kontenpunkt_y, mode='lines', showlegend=True, name="Knotenpunkt %d" % qp))
#
# fig.update_layout(
#     title='Verkehrskapazität nach Straßentyp, Fahrstreifen und Geschwindigkeit',
#     xaxis_title='Geschwindigkeit (km/h)',
#     yaxis_title='Verkehrskapazität',
#     legend=dict(x=0.5, y=-0.2, orientation='h'),
#     margin=dict(b=100)
# )
#
# fig.show()
