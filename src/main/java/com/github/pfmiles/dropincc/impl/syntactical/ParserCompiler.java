package com.github.pfmiles.dropincc.impl.syntactical;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import com.github.pfmiles.dropincc.CC;
import com.github.pfmiles.dropincc.DropinccException;
import com.github.pfmiles.dropincc.Element;
import com.github.pfmiles.dropincc.Grule;
import com.github.pfmiles.dropincc.TokenDef;
import com.github.pfmiles.dropincc.impl.Alternative;
import com.github.pfmiles.dropincc.impl.AndSubRule;
import com.github.pfmiles.dropincc.impl.CAlternative;
import com.github.pfmiles.dropincc.impl.ConstructingGrule;
import com.github.pfmiles.dropincc.impl.EleType;
import com.github.pfmiles.dropincc.impl.GruleType;
import com.github.pfmiles.dropincc.impl.OrSubRule;
import com.github.pfmiles.dropincc.impl.PredictingGrule;
import com.github.pfmiles.dropincc.impl.SpecialType;
import com.github.pfmiles.dropincc.impl.TokenType;
import com.github.pfmiles.dropincc.impl.TypeMappingParam;
import com.github.pfmiles.dropincc.impl.kleene.AbstractKleeneNode;
import com.github.pfmiles.dropincc.impl.kleene.CKleeneNode;
import com.github.pfmiles.dropincc.impl.kleene.KleeneType;
import com.github.pfmiles.dropincc.impl.llstar.LlstarAnalysis;
import com.github.pfmiles.dropincc.impl.util.SetStack;
import com.github.pfmiles.dropincc.impl.util.Util;

/**
 * Util, to analysis & check, manipulate Grammar rules.
 * 
 * @author pf-miles
 * 
 */
public class ParserCompiler {

    /**
     * rewrite sub rules, return generated grules during rewriting.
     * 
     * @param grules
     * @return generated grules while rewrite
     */
    public static List<Grule> rewriteSubRules(List<Grule> grules) {
        if (grules != null && !grules.isEmpty()) {
            List<Grule> genGrules = new ArrayList<Grule>();
            Set<Grule> examinedGrules = new HashSet<Grule>();
            for (Grule g : grules)
                rewriteSubRulesForGrule(g, examinedGrules, genGrules);
            return genGrules;
        } else {
            throw new DropinccException("No grammar rules defined, error!");
        }
    }

    private static void rewriteSubRulesForGrule(Grule g, Set<Grule> examinedGrules, List<Grule> genGrules) {
        if (examinedGrules.contains(g))
            return;
        examinedGrules.add(g);
        for (Alternative alt : g.getAlts()) {
            rewriteSubRuleForElements(alt.getElements(), examinedGrules, genGrules);
        }
    }

    private static void rewriteSubRuleForElements(List<Element> eles, Set<Grule> examinedGrules, List<Grule> genGrules) {
        ListIterator<Element> iter = eles.listIterator();
        while (iter.hasNext()) {
            Element e = iter.next();
            Class<?> eleCls = e.getClass();
            // andSubRule or orSubRule should be rewritten currently
            if (AndSubRule.class.isAssignableFrom(eleCls)) {
                iter.remove();
                AndSubRule asr = (AndSubRule) e;
                Grule genGrule = new GenedGrule();
                List<Alternative> asrAlts = asr.getAlts();
                genGrule.setAlts(asrAlts);
                iter.add(genGrule);
                genGrules.add(genGrule);
                for (Alternative alt : asrAlts)
                    rewriteSubRuleForElements(alt.getElements(), examinedGrules, genGrules);
            } else if (OrSubRule.class.isAssignableFrom(eleCls)) {
                iter.remove();
                OrSubRule osr = (OrSubRule) e;
                Grule genGrule = new GenedGrule();
                List<Alternative> osrAlts = osr.getAlts();
                genGrule.setAlts(osrAlts);
                iter.add(genGrule);
                genGrules.add(genGrule);
                for (Alternative alt : osrAlts)
                    rewriteSubRuleForElements(alt.getElements(), examinedGrules, genGrules);
            } else if (ConstructingGrule.class.isAssignableFrom(eleCls)) {
                throw new DropinccException("Something must be wrong, ConstructingGrule shouldn't appear here");
            } else if (Grule.class.isAssignableFrom(eleCls)) {
                rewriteSubRulesForGrule((Grule) e, examinedGrules, genGrules);
            } else if (TokenDef.class.isAssignableFrom(eleCls)) {
                continue;
            } else if (CC.NOTHING.equals(e)) {
                continue;
            } else if (AbstractKleeneNode.class.isAssignableFrom(eleCls)) {
                rewriteSubRuleForElements(((AbstractKleeneNode) e).getElements(), examinedGrules, genGrules);
            } else {
                throw new DropinccException("Unhandled element: " + e);
            }
        }
    }

    public static Map<Grule, GruleType> buildGruleTypeMapping(List<Grule> grules, List<Grule> genGrules) {
        Map<Grule, GruleType> gruleTypeMapping = new LinkedHashMap<Grule, GruleType>();
        if (grules != null && !grules.isEmpty()) {
            for (int i = 0; i < grules.size(); i++) {
                gruleTypeMapping.put(grules.get(i), new GruleType(i));
            }
            if (genGrules.size() != 0) {
                int base = grules.size();
                for (int i = 0; i < genGrules.size(); i++) {
                    gruleTypeMapping.put(genGrules.get(i), new GenedGruleType(base + i));
                }
            }
        } else {
            throw new DropinccException("No grammar rules defined, error!");
        }
        return gruleTypeMapping;
    }

    public static Map<GruleType, List<CAlternative>> buildRuleTypeToAlts(TypeMappingParam param) {
        Map<GruleType, List<CAlternative>> ruleTypeToAlts = new HashMap<GruleType, List<CAlternative>>();
        for (Map.Entry<Grule, GruleType> entry : param.getGruleTypeMapping().entrySet()) {
            ruleTypeToAlts.put(entry.getValue(), resolveCAlts(entry.getKey().getAlts(), param));
        }
        return ruleTypeToAlts;
    }

    private static List<CAlternative> resolveCAlts(List<Alternative> alts, TypeMappingParam param) {
        List<CAlternative> ret = new ArrayList<CAlternative>();
        for (Alternative a : alts) {
            List<EleType> ms = eleListToTypeList(a.getElements(), param);
            ret.add(new CAlternative(ms, a.getAction(), a.getPred()));
        }
        return ret;
    }

    private static List<EleType> eleListToTypeList(List<Element> eles, TypeMappingParam param) {
        List<EleType> ret = new ArrayList<EleType>();
        for (Element e : eles) {
            EleType t = Util.resolveEleType(e, param);
            if (t == null)
                throw new DropinccException("Could not resolve element type for element: " + e + ", is this element defined in a proper manner?");
            ret.add(t);
        }
        return ret;
    }

    /**
     * Detect left-recursions, which is not allowed in LL parsing
     * 
     */
    public static void checkAndReportLeftRecursions(Map<GruleType, List<CAlternative>> ruleTypeToAlts, Map<KleeneType, CKleeneNode> kleeneTypeToNode) {
        SetStack<GruleType> path = new SetStack<GruleType>();
        for (Map.Entry<GruleType, List<CAlternative>> e : ruleTypeToAlts.entrySet())
            examineEleType(e.getKey(), path, ruleTypeToAlts, kleeneTypeToNode);
    }

    private static void examineEleType(EleType t, SetStack<GruleType> path, Map<GruleType, List<CAlternative>> ruleTypeToAlts,
            Map<KleeneType, CKleeneNode> kleeneTypeToNode) {
        if (t instanceof SpecialType) {
            return;
        } else if (t instanceof TokenType) {
            return;
        } else if (t instanceof GruleType) {
            if (path.contains(t)) {
                throw new DropinccException("Left recursion detected: " + Util.dumpCirclePath(path, t));
            } else {
                path.push((GruleType) t);
                List<CAlternative> alts = ruleTypeToAlts.get(t);
                for (CAlternative a : alts) {
                    // only the first eleType need to be checked for each
                    // alternative
                    examineEleType(a.getMatchSequence().get(0), path, ruleTypeToAlts, kleeneTypeToNode);
                }
                path.pop();
            }
        } else if (t instanceof KleeneType) {
            CKleeneNode knode = kleeneTypeToNode.get(t);
            examineEleType(knode.getContents().get(0), path, ruleTypeToAlts, kleeneTypeToNode);
        } else {
            throw new DropinccException("Unhandled element type: " + t);
        }
    }

    /**
     * Compute lookAheads for grules, prepare to generate parser code
     * 
     * @param ruleTypeToAlts
     * @param kleeneTypeToNode
     * @return
     */
    public static List<PredictingGrule> computePredictingGrules(Map<GruleType, List<CAlternative>> ruleTypeToAlts, Map<KleeneType, CKleeneNode> kleeneTypeToNode) {
        List<PredictingGrule> ret = new ArrayList<PredictingGrule>();
        LlstarAnalysis a = new LlstarAnalysis(ruleTypeToAlts, kleeneTypeToNode);
        for (Map.Entry<GruleType, List<CAlternative>> e : ruleTypeToAlts.entrySet()) {
            GruleType grule = e.getKey();
            ret.add(new PredictingGrule(grule, a.getLookAheadDfa(grule), e.getValue()));
        }
        return ret;
    }
}
