import pickle, re, sys, json
foods=pickle.load(open('foods.pkl','rb'))
keys=[l.split('\t') for l in open('/tmp/kf/freq.txt')]
Q=json.load(open('queries.json')) if len(sys.argv)>1 else {}
def words(s): return re.findall(r'[a-z]+', s.lower())
def stem(w): return w[:-3]+'y' if w.endswith('ies') else (w[:-2] if w.endswith('oes') else (w[:-1] if w.endswith('s') and not w.endswith('ss') else w))
idx=[(fid, f['desc'], set(stem(w) for w in words(f['desc']))) for fid,f in foods.items() if '1008' in f['n']]
def search(q, n=1):
    if q.startswith('#'): return [(0,q[1:],foods[q[1:]]['desc'])]
    ex=[(0,fid,d) for fid,d,ws in idx if d.lower()==q.lower()]
    if ex: return ex
    pos=' '.join(t for t in q.split() if not t.startswith('-'))
    must=[stem(w) for w in words(pos)]
    neg=[stem(w[1:]) for w in q.split() if w.startswith('-')]
    out=[]
    for fid,d,ws in idx:
        if all(m in ws for m in must) and not any(m in ws for m in neg):
            first=stem(words(d)[0])
            sc=len(d) - (25 if 'raw' in ws else 0) - (15 if first==must[0] else 0) - (8 if 'fresh' in ws else 0)+ (30 if any(b in d for b in ('Babyfood','Fast food','Restaurant','McDONALD','KRAFT','Infant')) else 0)
            out.append((sc,fid,d))
    out.sort(); return out[:n]
for k in keys:
    if int(k[1])<2: continue
    key=k[3]; q=Q.get(key,key)
    if q=='-': continue
    r=search(q,1)
    print(key,'=>',(r[0][2]+' #'+r[0][1]) if r else '???')
