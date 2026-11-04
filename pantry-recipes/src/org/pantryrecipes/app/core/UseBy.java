package org.pantryrecipes.app.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Pantry use-by dates: status labels, the "use soon" set and the ranking boost. */
public final class UseBy {
    private UseBy() { }

    /** Items due within this many days (today included) count as "use soon". */
    public static final int SOON_DAYS = 3;

    public static final String EXPIRED = "expired", TODAY = "today", SOON = "soon", OK = "ok";

    public static String status(int daysLeft) {
        if (daysLeft < 0) return EXPIRED;
        if (daysLeft == 0) return TODAY;
        if (daysLeft <= SOON_DAYS) return SOON;
        return OK;
    }

    public static String label(int daysLeft) {
        if (daysLeft < -1) return "Past use-by (" + (-daysLeft) + " days ago)";
        if (daysLeft == -1) return "Past use-by (yesterday)";
        if (daysLeft == 0) return "Use today";
        if (daysLeft == 1) return "Use by tomorrow";
        if (daysLeft <= SOON_DAYS) return "Use within " + daysLeft + " days";
        return daysLeft + " days left";
    }

    /** Days from today until the use-by date (negative once it has passed); Integer.MIN_VALUE if not set/invalid. */
    public static int daysLeft(String useBy, String today) {
        if (!Dates.valid(useBy) || !Dates.valid(today)) return Integer.MIN_VALUE;
        return Dates.between(today, useBy);
    }

    /** Pantry keys whose use-by date is today or within SOON_DAYS (expired items are not boosted). */
    public static Set<String> soonKeys(Map<String, String> useByByKey, String today) {
        Set<String> out = new LinkedHashSet<String>();
        for (Map.Entry<String, String> e : useByByKey.entrySet()) {
            int d = daysLeft(e.getValue(), today);
            if (d != Integer.MIN_VALUE && d >= 0 && d <= SOON_DAYS) out.add(e.getKey());
        }
        return out;
    }

    /** Distinct required ingredients of {@code r} covered by a use-soon pantry item. */
    public static int count(Recipe r, Matcher soon) {
        Set<String> seen = new HashSet<String>();
        int n = 0;
        for (Ingredient i : r.ingredients) {
            if (i.optional || i.key.isEmpty() || !seen.add(i.key)) continue;
            if (soon.has(i.key)) n++;
        }
        return n;
    }

    public static Matcher soonMatcher(Collection<String> soonKeys) {
        return new Matcher(soonKeys, Collections.<String>emptyList());
    }

    private static int bucket(Matcher.Result r) {
        if (r.haveNonStaple == 0) return 3;
        if (r.missingCount() == 0) return 0;
        return r.missingCount() <= 2 ? 1 : 2;
    }

    /**
     * Sorts best match first. With no use-soon items this is exactly Matcher.compare. Otherwise recipes are
     * grouped (cook now / missing 1-2 / the rest / nothing from the pantry) and, inside the first two groups,
     * recipes that use more use-soon items come first.
     */
    public static void sort(List<Matcher.Result> list, Collection<String> soonKeys) {
        if (soonKeys == null || soonKeys.isEmpty()) {
            Collections.sort(list, new Comparator<Matcher.Result>() {
                public int compare(Matcher.Result a, Matcher.Result b) { return Matcher.compare(a, b); }
            });
            return;
        }
        Matcher soon = soonMatcher(soonKeys);
        final Map<Matcher.Result, Integer> c = new IdentityHashMap<Matcher.Result, Integer>();
        for (Matcher.Result r : list) c.put(r, count(r.recipe, soon));
        Collections.sort(list, new Comparator<Matcher.Result>() {
            public int compare(Matcher.Result a, Matcher.Result b) {
                int ba = bucket(a), bb = bucket(b);
                if (ba != bb) return ba - bb;
                if (ba <= 1) {
                    int d = c.get(b) - c.get(a);
                    if (d != 0) return d;
                }
                return Matcher.compare(a, b);
            }
        });
    }

    /** "Use it up": recipes that use at least one use-soon item, boosted order. */
    public static List<Matcher.Result> useItUp(List<Matcher.Result> all, Collection<String> soonKeys, int limit) {
        List<Matcher.Result> out = new ArrayList<Matcher.Result>();
        if (soonKeys == null || soonKeys.isEmpty()) return out;
        Matcher soon = soonMatcher(soonKeys);
        for (Matcher.Result r : all) if (r.haveNonStaple > 0 && count(r.recipe, soon) > 0) out.add(r);
        sort(out, soonKeys);
        return out.size() > limit ? new ArrayList<Matcher.Result>(out.subList(0, limit)) : out;
    }
}
