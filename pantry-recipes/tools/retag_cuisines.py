#!/usr/bin/env python3
"""v2.0: replaces lumped cuisines ("Asian", "Caribbean", "Middle Eastern", ...) with a country (or a named
sub-region where the dish's country is honestly shared or disputed, e.g. Levantine, West African) where the dish is
known; fusion dishes that only borrow flavours become "International". Idempotent; edits recipes/*.txt headers."""
import glob, re, sys
T = {
 # Caribbean -> by country
 'Tostones': 'Puerto Rican', 'Rice and Peas with Jerk Tofu': 'Jamaican', 'Homemade Ginger Beer (Quick)': 'Jamaican',
 'Coconut Rice and Beans': 'International', 'Caribbean Fish and Egg Scramble': 'International', 'Sweet Potato Coconut Soup': 'International',
 'Sweet Potato and Black Bean Curry': 'International', 'Air Fryer Coconut Shrimp': 'American', 'Caramelized Pineapple with Lime': 'International',
 'Tropical Mango Pineapple Smoothie': 'International', 'Pineapple Coconut Cooler': 'International', 'Pineapple Salsa': 'International',
 'Blender Hot Sauce': 'International', 'Vegan Coconut Chickpea Stew with Kale': 'International',
 # Asian
 'Honey Garlic Chicken': 'Chinese-American', 'Asian Cabbage Slaw': 'American', 'Crispy Tempeh Rice Bowls': 'International',
 'Breakfast Fried Rice': 'Chinese-American', 'Singapore-Style Curry Noodles': 'Cantonese', 'Chicken Congee-Style Rice Soup': 'Chinese',
 'Slow Cooker Honey Garlic Chicken Thighs': 'Chinese-American', 'Ham and Pineapple Fried Rice': 'Chinese-American', 'Asian Chicken Salad': 'American',
 'Sriracha Mayo': 'American', 'Air Fryer Tofu Bites': 'International', 'Egg and Pea Fried Noodles': 'Chinese-American',
 'Sheet Pan Tofu and Broccoli': 'International', 'Leftover Rice Fried Egg Bowl': 'International', 'Chicken and Cabbage Stir-Fry': 'Chinese-American',
 'Edamame Vegetable Stir-Fry': 'International', 'Air Fryer Crispy Tofu Rice Bowls': 'International', 'Bacon and Cabbage Fried Rice': 'Chinese-American',
 'Tofu Steaks with Ginger Glaze': 'International', 'Asian Sesame Chicken Salad': 'American', 'Peanut Ginger Slaw': 'American',
 'Asian Sesame Slaw': 'American', 'Carrot Ginger Sesame Salad': 'American',
 # Middle Eastern
 'Shakshuka': 'Tunisian', 'Shakshuka with Feta and Chickpeas': 'Tunisian', 'Instant Pot Shakshuka': 'Tunisian', 'Green Shakshuka': 'Israeli',
 'Falafel Wraps': 'Levantine', 'Falafel Bowls with Tahini': 'Levantine', 'Falafel Pita Pockets': 'Levantine', 'Air Fryer Falafel': 'Levantine',
 'Classic Hummus': 'Levantine', 'Instant Pot Hummus': 'Levantine', 'Hummus with Spiced Lamb': 'Levantine', 'Roasted Red Pepper Hummus': 'Levantine',
 'Beet Hummus': 'International', 'Baba Ganoush': 'Levantine', 'Instant Pot Baba Ganoush': 'Levantine', 'Mujaddara': 'Levantine',
 'Chicken Shawarma Wraps': 'Levantine', 'Chicken Shawarma': 'Levantine', 'Chicken Shawarma Bowls': 'Levantine', 'Instant Pot Chicken Shawarma Bowls': 'Levantine',
 'Beef Kofta Kebabs': 'Turkish', 'Beef Kofta': 'Turkish', 'Lamb Kofta with Mint Yogurt': 'Turkish', 'Baklava': 'Turkish',
 'Lemony Lentil and Spinach Soup': 'Lebanese', 'Rice Pilaf with Vermicelli': 'Lebanese', 'Freekeh Salad with Herbs and Pomegranate': 'Levantine',
 'Tomato and Cucumber Salad': 'Levantine', "Za'atar Roasted Carrots": 'International', 'Pita Bread': 'Levantine', 'Mint Limeade': 'Levantine',
 'Tahini Halva': 'Levantine', "Labneh with Za'atar and Olive Oil": 'Levantine', 'Palestinian Maqluba': 'Palestinian', 'Quinoa Tabbouleh': 'International',
 'Lentil and Vegetable Soup': 'International', 'Halloumi and Chickpea Traybake': 'International', 'Roasted Cauliflower with Tahini': 'International',
 'Cumin Roasted Carrots with Yogurt': 'International', 'Garlic Yogurt Sauce': 'Levantine', 'Tahini Sauce': 'Levantine', 'Hummus Toast with Cucumber': 'International',
 'Chickpea Breakfast Hash': 'International', 'Spiced Lamb Couscous Bowls': 'International', 'Sheet Pan Chickpea Shawarma with Cauliflower': 'International',
 'Pistachio Cardamom Loaf': 'International', 'Lemon Tahini Dressing': 'International', 'Homemade Pita Chips': 'American',
 'Air Fryer Falafel-Spiced Cauliflower Steaks': 'International', 'Sheet-Pan Chickpea Shawarma': 'International', 'Banana Date Shake': 'International',
 'Sweet Potato and Chickpea Buddha Traybake': 'International',
 # Latin American / South American
 'Arroz con Pollo': 'Cuban', 'Instant Pot Arroz con Pollo': 'Cuban', 'Tres Leches Cake': 'Mexican', 'Slow Cooker Black Bean Soup': 'Cuban',
 'Instant Pot Black Bean Soup': 'Cuban', 'Instant Pot Black Beans from Dry': 'International', 'Instant Pot Quinoa': 'International',
 'Instant Pot Quinoa Vegetable Soup': 'International',
 # West / North African
 'Egusi-Inspired Spinach Peanut Stew': 'Nigerian', 'Pineapple Ginger Juice': 'Nigerian',
 'Herbed Couscous with Chickpeas': 'International', 'Couscous with Roasted Vegetables': 'International', 'Millet Porridge with Dates': 'International',
 'Sheet Pan Harissa Chicken with Carrots': 'International', 'Lemon Herb Couscous Salad': 'International', 'Harissa Cauliflower and Lentil Bowl': 'International',
 'Spiced Roasted Chickpeas and Carrots': 'International', 'Herbed Couscous': 'International',
 # Eastern European / Scandinavian / European
 'Stuffed Cabbage Rice Bake': 'Polish', 'Budget Cabbage and Sausage Skillet': 'Polish', 'Buckwheat Porridge with Apple': 'Russian',
 'Slow Cooker Mushroom Barley Stew': 'Polish', 'Instant Pot Mushroom Barley Soup': 'Polish', 'Beet and Apple Soup with Dill': 'International',
 'Cabbage Roll Soup': 'Polish', 'Creamy Cucumber Dill Salad': 'Polish', 'Roasted Cabbage Wedges': 'International',
 'Radish and Cucumber Yogurt Salad': 'International', 'Potato Latkes': 'Jewish',
 'Dill Gravlax': 'Swedish', 'Baked Salmon with Lemon and Dill': 'International', 'Smoked Salmon and Dill Frittata': 'International',
 'Smoked Salmon Cream Cheese Toast': 'American', 'Barley Breakfast Porridge with Pear': 'International', 'Pan-Fried Salmon with Dill Yogurt': 'International',
 'Smoked Salmon Cucumber Bites': 'International', 'Instant Pot Salmon with Lemon and Dill': 'International', 'Smoked Mackerel and Potato Salad': 'International',
 'Marbled Chocolate Loaf Cake': 'German', 'Thumbprint Jam Cookies': 'International',
}
LUMPS = {'Caribbean', 'Asian', 'Middle Eastern', 'Latin American', 'North African', 'Eastern European', 'Scandinavian', 'South American', 'European', 'Mediterranean'}
SPELL = {'Argentinian': 'Argentine', 'Trinidadian': 'Trinidadian & Tobagonian'}
MED = {'One-Pot Chicken and Orzo': 'Greek', 'Mediterranean Orzo Salad': 'Greek', 'One-Pot Lemon Garlic Shrimp Orzo': 'Greek',
       'Halloumi and Roasted Pepper Wraps': 'Cypriot', 'Halloumi Burgers with Roasted Peppers': 'Cypriot', 'Watermelon Feta Salad': 'Greek',
       'Watermelon Feta Mint Salad': 'Greek', 'Lentil Salad with Feta': 'Greek', 'Instant Pot Lentil Soup': 'Greek', 'Slow Cooker Lentil Soup': 'Greek'}
T.update(MED)
changed = 0; left = []
for f in sorted(glob.glob('recipes/*.txt')):
    lines = open(f, encoding='utf-8').read().split('\n')
    for i in range(len(lines) - 1):
        if not lines[i].startswith('# '): continue
        title = lines[i][2:].strip(); h = lines[i + 1].split('|')
        if len(h) != 6: continue
        c = h[1].strip(); n = T.get(title) if c in LUMPS or c in ('West African',) else None
        if n is None: n = SPELL.get(c)
        if n is None and c in LUMPS: n = 'International'
        if n and n != c:
            h[1] = ' ' + n + ' '; lines[i + 1] = '|'.join(h); changed += 1
    open(f, 'w', encoding='utf-8').write('\n'.join(lines))
print('retagged', changed)
