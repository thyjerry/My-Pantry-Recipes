#!/usr/bin/env python3
"""One-off helper (kept for reference): adds a 6th header field with meal types to recipes/*.txt.
Rules derive meal types from the category, then explicit per-title adjustments (reviewed by hand).
Re-running is safe: recipes that already have a 6th field are left alone unless --force is given."""
import os, re, sys

ORDER = ["Breakfast", "Brunch", "Lunch", "Dinner", "Main dish", "Side dish", "Appetizer", "Snack", "Soup", "Salad",
         "Dessert", "Baked goods", "Drink", "Sauce/Condiment"]
BASE = {"Breakfast": ["Breakfast", "Brunch"], "Main": ["Main dish", "Dinner"], "Soup": ["Soup", "Lunch", "Dinner"],
        "Salad": ["Salad", "Lunch"], "Side": ["Side dish", "Dinner"], "Snack": ["Snack"], "Dessert": ["Dessert"],
        "Baking": ["Baked goods"], "Drink": ["Drink"], "Sauce": ["Sauce/Condiment"]}
def S(s): return [x.strip() for x in s.split(";") if x.strip()]
ADD = {
  "Lunch": S("""Shakshuka; Huevos Rancheros; Breakfast Burritos; Avocado Toast with Egg; Chilaquiles Rojos; Turkish-Style Eggs with Yogurt;
    Breakfast Quesadillas; Congee with Ginger and Green Onion; Masala Omelette; Poha with Peas and Peanuts; Breakfast Fried Rice;
    Sweet Potato Breakfast Hash with Black Beans; Bacon and Egg Breakfast Sandwiches; Mushroom and Thyme Toast; Smoked Salmon Bagels;
    Ham and Cheese Breakfast Strata; Polenta Breakfast Bowls with Fried Eggs; Egg and Avocado Breakfast Tacos; Gallo Pinto;
    Caribbean Fish and Egg Scramble; Smashed Chickpea Avocado Toast; Vegetable Omelette; Tofu Scramble; Huevos a la Mexicana;
    Tamagoyaki-Style Rolled Omelette; Grilled Cheese Sandwich; Onigiri Rice Balls; Cheese Quesadilla Triangles for Kids; Hummus with Spiced Lamb;
    Crispy Baked Chicken Tenders; Fish Cakes; Salmon Patties; Chili con Carne; Veggie Chili; Turkey Chili; Vegan Chili Sin Carne; Slow Cooker Beef Chili;
    Mujaddara; Koshari; Baked Potatoes with Chili Beans; Stuffed Sweet Potatoes; Broccoli Cheddar Baked Potatoes; Pasta e Ceci; Zucchini Fritters;
    Spanakopita Pie; Shakshuka with Feta and Chickpeas; Ratatouille Omelette Pie; Lemon Chicken Orzo; Orzo with Spinach and Feta; Sesame Peanut Noodles;
    Lo Mein with Vegetables; Garlic Butter Noodles; Egg Fried Noodles; Egg and Pea Fried Noodles; Mac and Cheese on the Stove; Baked Macaroni and Cheese;
    Cauliflower Mac and Cheese; Pesto Pasta with Peas; Lemon Ricotta Pasta; Pasta Primavera; Tuna and Tomato Spaghetti; Spaghetti Aglio e Olio"""),
  "Brunch": S("""Spinach and Feta Quiche; Vegetable Frittata; Shakshuka with Feta and Chickpeas; Ratatouille Omelette Pie; Croque Monsieur;
    Savory Cheddar Scones; Banana Bread; Blueberry Muffins; Pumpkin Bread; Zucchini Bread; Cinnamon Rolls; Buttermilk Biscuits; Simple Pancake Crepes;
    Zucchini Fritters; Smoked Salmon Bagels"""),
  "Breakfast": S("""Banana Bread; Blueberry Muffins; Pumpkin Bread; Zucchini Bread; Cinnamon Rolls; Buttermilk Biscuits; Simple Pancake Crepes;
    Green Breakfast Smoothie; Strawberry Banana Smoothie; Blueberry Oat Smoothie; Chicken Congee-Style Rice Soup"""),
  "Dinner": S("""Shakshuka; Huevos Rancheros; Breakfast Fried Rice; Congee with Ginger and Green Onion; Gallo Pinto;
    Loaded Sheet Pan Nachos with Chicken; Cornbread; Focaccia; Naan; No-Knead Bread; Buttermilk Biscuits; Flour Tortillas; Potstickers;
    Chicken Caesar Salad; Quinoa Salad with Roasted Vegetables; Tuna Nicoise Salad; Cold Soba Noodle Salad; Farro Salad with Roasted Vegetables; Gado Gado;
    Cobb Salad; Asian Chicken Salad; Lentil Salad with Feta; Thai Beef Salad; Greek Chicken Salad Bowls;
    Greek Salad; Asian Cabbage Slaw; Caprese Salad; Cucumber Yogurt Salad; Warm Potato Salad; Tabbouleh; Waldorf Salad; Fattoush; Panzanella;
    Kale Caesar Salad; Black Bean and Corn Salad; Three Bean Salad; Classic Coleslaw; Pasta Salad with Italian Dressing; Watermelon Feta Salad;
    Beet and Goat Cheese Salad; Smashed Cucumber Salad"""),
  "Main dish": S("""Chicken Caesar Salad; Quinoa Salad with Roasted Vegetables; Tuna Nicoise Salad; Cold Soba Noodle Salad; Farro Salad with Roasted Vegetables;
    Gado Gado; Cobb Salad; Asian Chicken Salad; Lentil Salad with Feta; Thai Beef Salad; Greek Chicken Salad Bowls; Chickpea Salad;
    Chicken and Dumplings; Moroccan Lentil Stew; Irish Beef and Stout-Style Stew; Sancocho-Style Chicken Stew; Pozole Rojo; Chicken Ramen Bowls;
    Beef Pho-Style Noodle Soup; Kimchi Jjigae; Beef and Barley Soup; Pasta Fagioli; Harira-Style Chickpea Soup; Slow Cooker Pork Ramen Broth;
    Loaded Sheet Pan Nachos with Chicken"""),
  "Side dish": S("""Greek Salad; Asian Cabbage Slaw; Caprese Salad; Cucumber Yogurt Salad; Warm Potato Salad; Tabbouleh; Waldorf Salad; Fattoush; Panzanella;
    Kale Caesar Salad; Black Bean and Corn Salad; Three Bean Salad; Classic Coleslaw; Pasta Salad with Italian Dressing; Watermelon Feta Salad;
    Beet and Goat Cheese Salad; Smashed Cucumber Salad; Cornbread; Focaccia; Naan; No-Knead Bread; Buttermilk Biscuits; Flour Tortillas;
    Gigantes-Style Baked Beans; Garlic Bread"""),
  "Appetizer": S("""Classic Hummus; Guacamole; Bruschetta; Deviled Eggs; Loaded Nachos; Spring Rolls in Lettuce Cups; Crispy Air Fryer Chicken Wings;
    Lemon Pepper Chicken Wings; Thai Fish Cakes; Mac and Cheese Bites; Spicy Garlic Edamame; Baba Ganoush; Caramelized Onion Dip; Spinach Artichoke Dip;
    Seven Layer Dip; Stuffed Mushrooms; Pigs in Blankets; Caprese Skewers; Hummus with Spiced Lamb; Vegetable Spring Rolls; Potstickers; Air Fryer Falafel;
    Air Fryer Tofu Bites; Loaded Sheet Pan Nachos with Chicken; Garlic Bread; Tostones; Caprese Salad; Beet and Goat Cheese Salad;
    Gazpacho; Miso Soup with Tofu; Egg Drop Soup; Wonton Soup; Hot and Sour Soup; French Onion Soup;
    Sweet and Sour Pork Belly Bites; Air Fryer Meatballs; Air Fryer Shrimp; Chicken Satay with Peanut Sauce; Beef Kofta Kebabs; Suya-Spiced Beef Skewers;
    Chicken Tikka Skewers; Tzatziki; Pico de Gallo; Restaurant-Style Blender Salsa; Focaccia"""),
  "Snack": S("""Granola with Nuts and Honey; Yogurt Berry Parfait; Coconut Chia Pudding; Cinnamon Sugar French Toast Sticks; Fruit and Nut Muesli;
    Elote-Style Corn; Tostones; Garlic Parmesan Air Fryer Fries; Air Fryer Sweet Potato Wedges;
    Banana Bread; Pumpkin Bread; Zucchini Bread; Blueberry Muffins; Savory Cheddar Scones; Chocolate Chip Cookies; Oatmeal Raisin Cookies;
    Peanut Butter Cookies; Snickerdoodles; Shortbread; Coconut Macaroons; Vegan Oat Cookies;
    Easy Fruit Yogurt Bark; Dark Chocolate Bark with Nuts; Rice Krispie-Style Marshmallow Treats; Banana Nice Cream;
    Pico de Gallo; Restaurant-Style Blender Salsa; Honey Mustard Dip"""),
  "Dessert": S("""Chocolate Chip Cookies; Vanilla Cupcakes; Oatmeal Raisin Cookies; Peanut Butter Cookies; Snickerdoodles; Shortbread; Carrot Cake;
    Vegan Chocolate Cake; Lemon Drizzle Cake; Coconut Macaroons; Vanilla Sheet Cake; Vegan Oat Cookies; Hot Chocolate; Vanilla Milkshake"""),
  "Baked goods": S("""Brownies; Lemon Bars; Apple Pie; Pecan Pie Bars; Tres Leches Cake; Baklava; Strawberry Shortcake"""),
}
REMOVE = {}

def meal_types(cat, title):
    out = list(BASE[cat])
    for m, titles in ADD.items():
        if title in titles and m not in out: out.append(m)
    return [m for m in ORDER if m in out]

def main():
    force = '--force' in sys.argv
    d = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'recipes')
    used = set(); n = 0
    for f in sorted(os.listdir(d)):
        if not f.endswith('.txt'): continue
        p = os.path.join(d, f); lines = open(p, encoding='utf-8').read().split('\n'); title = None
        for i, line in enumerate(lines):
            t = line.strip()
            if t.startswith('# '): title = t[2:].strip(); continue
            if title and t and not t.startswith(('- ', '> ')) and '|' in t:
                parts = [x.strip() for x in t.split('|')]
                if len(parts) == 6 and not force: title = None; continue
                parts = parts[:5]
                lines[i] = ' | '.join(parts + [', '.join(meal_types(parts[0], title))])
                used.add(title); n += 1; title = None
        open(p, 'w', encoding='utf-8').write('\n'.join(lines))
    missing = [t for v in ADD.values() for t in v if t not in used]
    print('updated', n, 'headers; unknown titles in rules:', sorted(set(missing)))

if __name__ == '__main__':
    main()
