
import pandas as pd
import numpy as np
from scipy.optimize import minimize

from sklearn.metrics import mean_squared_error

#%%

def read_tsv(f):        
    return pd.read_csv(f, dtype={"person": "str"}, sep="\t")


#%%

df = read_tsv(r"C:\Users\chris\Development\matsim-scenarios\matsim-kelheim\analysis-act.tsv")
df = df[~df.type.isna()]

df["dist"] = df[df.columns[df.columns.str.contains("dist")]].sum(axis=1)


#%%

def p(x):
    d = {}
    
    m_est = x.loc[x.estimate.idxmax()]
    m_score = x.loc[x.score.idxmax()]
        
    d["dist"] = m_est["dist"]
    d["diff"] = m_est.estimate - m_score.estimate
    d["trips"] = m_est["type"].count("-") + 1
    d["type"] = m_est["type"]
    
    for m in ("bike", "walk", "car", "ride", "pt"):
        d[m] = m_est["type"].count(m)
        d[m + "_dist"] = m_est[m + "_dist"]
    
    return pd.Series(d)

df_diff = df.groupby("person").apply(p)

#%%

q = 0.999

d = df_diff[df_diff["diff"] > 0]

print("Score quantile", d["diff"].quantile(q))

#d = d[d["diff"] < d["diff"].quantile(q)]

def stats(x):
    """ Distance based objective """
    
    score =  x[0] + x[1] * df_diff.dist / 1000 - df_diff["diff"]
    
    c = (score < 0).sum()
    
    p =  c * 100 / len(df_diff)
    
    print("Score",  score.abs().sum())
    print("Miss %d / %d (%.2f%%)" % (c, len(df_diff), p))
    
    return p


def stats2(x):
    """ Mode distance based objective """
    score = x[0] - d["diff"] 
    
    score += x[1] * d.bike_dist / 1000
    score += x[2] * d.walk_dist / 1000
    score += x[3] * d.car_dist / 1000
    score += x[4] * d.ride_dist / 1000
    score += x[5] * d.pt_dist / 1000

    c = (score < 0).sum()
    p =  c * 100 / len(df_diff)
    
    print("Score",  score.abs().sum())
    print("Miss %d / %d (%.2f%%)" % (c, len(df_diff), p))
    
    return p
    

#%% 

def opt(scale):
    
    def f(x):            
        score = x[0] + x[1] * d.dist / 1000 - d["diff"]
        
        ltz = score < 0
        score[ltz] = score[ltz] * scale
        
        #if (score < 0).any():
        #    return np.inf
        
        return score.abs().sum()
    
    x0 = [1, 0.1]    
    bnds = tuple((0, np.inf) for x in x0)    

    res = minimize(f, x0, method='L-BFGS-B', bounds=bnds)

    p = stats(res.x)

    return p, res


p, res = opt(1)

print(res)

#%%

def opt2(scale):
    
    def f(x):            
        score = x[0] - d["diff"] 
        
        score += x[1] * d.bike_dist / 1000
        score += x[2] * d.walk_dist / 1000
        score += x[3] * d.car_dist / 1000
        score += x[4] * d.ride_dist / 1000
        score += x[5] * d.pt_dist / 1000
        
        ltz = score < 0
        score[ltz] = score[ltz] * scale    
  
        return score.abs().sum()
    
    x0 = [1] + [0.1] * 5
    bnds = tuple((0, np.inf) for x in x0)    

    res = minimize(f, x0, method='L-BFGS-B', bounds=bnds)

    p = stats2(res.x)

    return p, res


p, res = opt2(24.40)

print(res)


#%%

# Higher level objective
hf = opt
q = 0.1
x0 = 2

def hopt(s):

    p, res = hf(s[0])    
    
    # First argument ist the percentile 1 = 99% percentile
    err = mean_squared_error([q], [p])
    
    # Minimize scale of s if error is equal    
    return err - err/abs(s)


hres = minimize(hopt, [x0], method='Nelder-Mead', bounds=[(1, np.inf)])
print(hres)

p, res = hf(hres.x[0])
print(res)
