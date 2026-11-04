import csv, collections, pickle
D='FoodData_Central_sr_legacy_food_csv_2018-04/'
foods={}
for r in csv.DictReader(open(D+'food.csv',encoding='utf-8')): foods[r['fdc_id']]={'desc':r['description'],'n':{},'p':[]}
for r in csv.DictReader(open(D+'food_nutrient.csv',encoding='utf-8')):
    if r['nutrient_id'] in ('1003','1004','1005','1008') and r['fdc_id'] in foods: foods[r['fdc_id']]['n'][r['nutrient_id']]=float(r['amount'])
mu={r['id']:r['name'] for r in csv.DictReader(open(D+'measure_unit.csv'))}
for r in csv.DictReader(open(D+'food_portion.csv',encoding='utf-8')):
    if r['fdc_id'] in foods: foods[r['fdc_id']]['p'].append((float(r['amount'] or 1), mu.get(r['measure_unit_id'],''), r['modifier'], float(r['gram_weight'])))
pickle.dump(foods,open('foods.pkl','wb')); print(len(foods))
