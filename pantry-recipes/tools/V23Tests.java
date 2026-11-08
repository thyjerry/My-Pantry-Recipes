import org.pantryrecipes.app.core.*;
import java.util.*;
/** Focused substitution regression checks for v2.3. */
public final class V23Tests {
  public static void main(String[] args) {
    Substitutions s;
    try { s = Substitutions.parse(new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(args[0])), "UTF-8")); } catch(Exception e) { throw new RuntimeException(e); }
    int checked=0;
    for (Substitutions.Entry e : s.entries) for(Substitutions.Option o : e.options) {
      if(!SwapAdvice.caution(o, null, null, Arrays.asList("vegan")).isEmpty() == o.vegan) throw new AssertionError("vegan guard " + e.name);
      if (SwapAdvice.quantity(o, -1, "g").startsWith("Replacement estimate")) throw new AssertionError("unknown amount");
      for(java.util.List<String> g:o.needs) for(String k:g) if(SwapAdvice.caution(o, Arrays.asList(k), null, null).isEmpty()) throw new AssertionError("dislike " + k);
      if(SwapAdvice.quantity(o, 100, "g").startsWith("Replacement estimate")) throw new AssertionError("mass conversion");
      if(o.needs.size()>1 && SwapAdvice.quantity(o, 100,"g").startsWith("Replacement estimate")) throw new AssertionError("compound conversion");
      for(String id:o.allergens) if(SwapAdvice.caution(o,null,Arrays.asList(id),null).isEmpty()) throw new AssertionError("allergen guard " + id);
      Double ratio = Substitutions.ratioValue(o.ratio);
      if(o.needs.size()==1 && ratio != null && ratio > 0 && !SwapAdvice.quantity(o,2,"cup").contains(Quantity.format(2*ratio,"cup",1))) throw new AssertionError("scaled volume");
      checked++;
    }
    for(Substitutions.Entry e:s.search("chicken thighs")) if(e.name.toLowerCase().contains("pork")) throw new AssertionError("unrelated protein");
    System.out.println("V23Tests: " + checked + " substitution options checked; 0 failures");
  }
}
