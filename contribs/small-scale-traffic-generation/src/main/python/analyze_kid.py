# -*- coding: utf-8 -*-

from datetime import timedelta

import matplotlib.pyplot as plt
import matplotlib.ticker as ticker
import numpy as np
import pandas as pd
import seaborn as sns

# %%

sns.set_style("ticks")
sns.set_context("paper", font_scale=1.2, rc={"grid.linewidth": 1, "lines.linewidth": 2})
# sns.set_palette("Set2")

plt.rcParams['figure.dpi'] = 350
plt.rcParams['pdf.fonttype'] = 42
plt.rcParams['ps.fonttype'] = 42
plt.rcParams['font.family'] = 'Arial'

palette = sns.color_palette("Set2")

# %%

d = "/Users/rakow/Development/shared-svn/studies/countries/de/KiD_2002/Daten/"

Fahrten = pd.read_csv(d + "KiD_2002_(Einzel)Fahrten-Datei.txt", delimiter="\t", encoding="ISO-8859-1")
Ketten = pd.read_csv(d + "KiD_2002_Fahrtenketten-Datei.txt", delimiter="\t", encoding="ISO-8859-1")
Fahrzeug = pd.read_csv(d + "KiD_2002_Fahrzeug-Datei.txt", delimiter="\t", encoding="ISO-8859-1")

# %%

Fahrten["start"] = pd.to_datetime(Fahrten["F04"], errors="coerce")
Fahrten["end"] = pd.to_datetime(Fahrten["F10a"], errors="coerce")

# %%

# Filter commercial trips
df = Fahrten[Fahrten["F07b"] == 1]

df = df.merge(Fahrzeug, on="K00")
df = df.rename(columns={"K91": "w", "F14": "dist"})

df["type"] = df["F07a"].map(
    {1: "goodsTraffic", 2: "commercialPersonTraffic", 3: "commercialPersonTraffic", 4: "commercialPersonTraffic",
     5: "returnDepot"})
df = df[["K00", "F00", "start", "end", "w", "dist", "type"]]

df["start"] = df["start"].dt.hour * 60 + df["start"].dt.minute
df["end"] = df["end"].dt.hour * 60 + df["end"].dt.minute

# Filter valid ranges
df = df[df.start <= df.end]

df["duration"] = df.end - df.start

df = df.dropna()

purpose = df.groupby("K00").apply(
    lambda x: "goodsTraffic" if "goodsTraffic" in set(x["type"]) else "commercialPersonTraffic")

# %%

durations = [0, 10, 20, 30, 40, 50, 60, 75, 90, 105, 120, 150, 180, 240, 300, 420, 540, 660, np.inf]

df["dur_group"] = pd.cut(df.duration, durations)

starts = [0, 4 * 60, 5 * 60, 6 * 60, 7 * 60, 8 * 60, 9 * 60, 10 * 60, 11 * 60, 12 * 60, 13 * 60, 14 * 60, 15 * 60,
          16 * 60, 17 * 60, 18 * 60, 19 * 60, np.inf]
start_labels = ["0-4", "4-5", "5-6", "6-7", "7-8", "8-9", "9-10", "10-11", "11-12", "12-13", "13-14", "14-15", "15-16",
                "16-17", "17-18", "18-19", "19-24"]

df["start_group"] = pd.cut(df.start, starts)

# %%

x = np.arange(start=0, stop=24 * 60, step=15, dtype=np.float64)

# %%

y = np.zeros_like(x)

for t in df.itertuples():
    idx = np.searchsorted(x, [t.start, t.end])
    idx[1] += 1

    y[slice(*idx)] += t.w

y /= np.max(y / 1000)

# %%

fig, ax = plt.subplots(figsize=(8, 4))

sns.lineplot(x=x, y=y, ax=ax)

sns.despine()

ax.xaxis.set_major_locator(ticker.FixedLocator(np.arange(start=0, stop=24 * 60, step=120, dtype=np.float64)))
ax.xaxis.set_major_formatter(lambda x, y: str(timedelta(minutes=x))[:-3])

# %%


sns.histplot(data=df, x="duration", bins=durations)

# %%

# Only commercial tours

Ketten["start"] = pd.to_datetime(Ketten["T04"], errors="coerce")

tf = Ketten[Ketten.T07 == 1]

tf = tf.merge(Fahrzeug, on="K00").set_index("K00")
tf["type"] = purpose

tf["start"] = tf["start"].dt.hour * 60 + tf["start"].dt.minute

tf = tf.rename(columns={"K91": "w", "T01": "duration", "T05": "dist", "K03": "vWeight"})

tf = tf[tf.duration > 0]

tf = tf[["start", "w", "duration", "dist", "vWeight", "type"]]

tf = tf.dropna()

# %%

vehicles = [0, 2800, 3500, 7500, 12000, 100000]

tf["vClass"] = pd.cut(tf.vWeight, vehicles, labels=["vehType1", "vehType2", "vehType3", "vehType4", "vehType5"])

t_durations = np.hstack(([0, 30, 60, 90], np.arange(120, 15 * 60, step=60, dtype=np.float64), [18 * 60]))

tf["dur_group"] = pd.cut(tf.duration, t_durations)
tf["start_group"] = pd.cut(tf.start, starts, labels=start_labels)

tf = tf.dropna()

# %%

aggr = tf.groupby(["type", "start_group", "dur_group"]).agg(p=("w", "sum"))

for group in ("goodsTraffic", "commercialPersonTraffic"):
    sub = aggr.loc[group, :]
    sub.p /= sub.p.sum()

aggr = aggr.reset_index()

target = "goodsTraffic"
for a in aggr.itertuples():

    if a.type != target or a.p <= 0:
        continue

    f, t = a.start_group.split("-")
    lower, upper = a.dur_group.left, a.dur_group.right

    print(f"Pair.create(new TourStartAndDuration({f}, {t}, {lower}, {upper}), {a.p}),")

# %%

fig, ax = plt.subplots(figsize=(8, 4))

sns.histplot(data=tf, x="start", bins=starts, ax=ax)

sns.despine()

ax.xaxis.set_major_locator(ticker.FixedLocator(np.arange(start=0, stop=24 * 60, step=120, dtype=np.float64)))
ax.xaxis.set_major_formatter(lambda x, y: str(timedelta(minutes=x))[:-3])

# %%

fig, ax = plt.subplots(figsize=(8, 4))

sns.histplot(data=tf, x="duration", bins=t_durations, ax=ax)
sns.despine()

ax.xaxis.set_major_formatter(lambda x, y: int(x // 60))
ax.xaxis.set_major_locator(ticker.FixedLocator(np.arange(start=0, stop=24 * 60, step=60, dtype=np.float64)))

# %%

grid = sns.FacetGrid(tf, col="start_group", col_wrap=6, palette="tab20c")

grid.map_dataframe(sns.histplot, x="duration", bins=t_durations, stat="percent")

# %%
