"""v2.2 dinner generator: cuts of meat and fish, with cut-specific amounts, times and doneness wording.
Temperatures follow common food-safety guidance (USDA/FSA): poultry 74 °C (165 °F), ground beef/pork/lamb 71 °C (160 °F),
whole cuts of pork, beef and lamb 63 °C (145 °F) plus a 3-minute rest, fish 63 °C (145 °F)."""
POULTRY = "cooked through, with no pink in the middle (74 °C / 165 °F)"
PORK = "63 °C (145 °F) in the centre"
STEAK = "done to your liking (63 °C / 145 °F for medium)"
FISH = "the fish flakes easily (63 °C / 145 °F)"
SHRIMP = "pink and opaque all the way through"
GROUND_RED = "no longer pink inside (71 °C / 160 °F)"
GROUND_POULTRY = "no longer pink inside (74 °C / 165 °F)"

# key: (title name, protein group, ingredient line, methods, {method: params})
# params: stirfry (minutes per batch), skillet (minutes per side, rest), roast (oven °C, minutes, marinate),
#         braise (brown minutes, simmer minutes, tender phrase)
CUTS = {
 "cthigh": ("Chicken Thigh", "chicken", "800 g|chicken thighs, boneless and skinless", POULTRY,
            {"stirfry": 4, "skillet": (5, "cook 5 to 6 minutes per side"), "roast": (200, 25, "30 minutes or up to overnight")}),
 "cbreast": ("Chicken Breast", "chicken", "700 g|chicken breasts", POULTRY,
            {"stirfry": 3, "skillet": (6, "butterfly or pound to an even 2 cm, then cook 5 to 6 minutes per side"), "roast": (200, 22, "30 minutes or up to 4 hours")}),
 "cbonein": ("Chicken Thighs", "chicken", "1.2 kg|chicken thighs, bone-in, skin-on", POULTRY,
            {"braise": (8, 40, "the meat pulls easily from the bone"), "roast": (210, 40, "1 hour or up to overnight")}),
 "cdrum": ("Chicken Drumsticks", "chicken", "1.2 kg|chicken drumsticks", POULTRY,
            {"braise": (8, 45, "the meat is tender and pulls from the bone"), "roast": (210, 40, "1 hour or up to overnight")}),
 "bstrip": ("Beef", "beef", "500 g|beef sirloin, thinly sliced against the grain", STEAK,
            {"stirfry": 2}),
 "flank": ("Flank Steak", "beef", "700 g|flank steak", STEAK,
            {"roast": ("grill", 5, "1 to 4 hours")}),
 "sirloin": ("Sirloin Steak", "beef", "4|sirloin steaks, about 200 g each", STEAK,
            {"skillet": (3, "cook 3 to 4 minutes per side, turning once")}),
 "chuck": ("Beef", "beef", "1.2 kg|beef chuck, cut into 4 cm pieces", "",
            {"braise": (12, 150, "the beef is very tender")}),
 "sribs": ("Short Ribs", "beef", "1.6 kg|beef short ribs, bone-in", "",
            {"braise": (12, 180, "the meat is falling off the bone")}),
 "gbeef": ("Beef", "beef", "600 g|ground beef", GROUND_RED, {"ground": 1}),
 "ptend": ("Pork Tenderloin", "pork", "700 g|pork tenderloin, trimmed", PORK,
            {"roast": (220, 20, "30 minutes or up to 8 hours"), "stirfry": 3}),
 "pchop": ("Pork Chops", "pork", "4|pork chops, boneless, about 2 cm thick", PORK,
            {"skillet": (4, "cook 4 to 5 minutes per side"), "roast": ("grill", 5, "30 minutes or up to 8 hours")}),
 "pshoulder": ("Pork Shoulder", "pork", "1.2 kg|pork shoulder, boneless, cut into 5 cm pieces", "",
            {"braise": (12, 120, "the pork is tender enough to pull apart")}),
 "pbelly": ("Pork Belly", "pork", "1 kg|pork belly, skin off, cut into 4 cm cubes", "",
            {"braise": (12, 90, "the pork is soft and glossy")}),
 "gpork": ("Pork", "pork", "600 g|ground pork", GROUND_RED, {"ground": 1}),
 "lshoulder": ("Lamb", "lamb", "1.2 kg|lamb shoulder, boneless, cut into 4 cm pieces", "",
            {"braise": (12, 120, "the lamb is very tender")}),
 "lchop": ("Lamb Chops", "lamb", "8|lamb chops (loin or cutlets)", STEAK,
            {"roast": ("grill", 3, "1 to 8 hours"), "skillet": (3, "cook 3 to 4 minutes per side")}),
 "glamb": ("Lamb", "lamb", "600 g|ground lamb", GROUND_RED, {"ground": 1}),
 "gturkey": ("Turkey", "turkey", "600 g|ground turkey", GROUND_POULTRY, {"ground": 1}),
 "tcutlet": ("Turkey Steaks", "turkey", "600 g|turkey breast, cut into 1 cm steaks", POULTRY,
            {"skillet": (3, "cook 3 to 4 minutes per side")}),
 "gchicken": ("Chicken", "chicken", "600 g|ground chicken", GROUND_POULTRY, {"ground": 1}),
 "salmon": ("Salmon", "fish", "4|salmon fillets, about 150 g each", FISH,
            {"roast": (200, 12, "15 minutes (no longer, or the acid starts to cook it)"), "skillet": (4, "cook skin side down 4 minutes, then 2 to 3 minutes on the other side")}),
 "cod": ("Cod", "fish", "4|cod fillets, about 150 g each", FISH,
            {"skillet": (3, "cook 3 to 4 minutes per side, turning once gently"), "braise": (0, 10, "")}),
 "shrimp": ("Shrimp", "shrimp", "500 g|shrimp, peeled and deveined", SHRIMP,
            {"stirfry": 2, "skillet": (2, "cook 1 to 2 minutes per side")}),
}
