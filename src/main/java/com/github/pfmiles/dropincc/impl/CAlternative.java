package com.github.pfmiles.dropincc.impl;

import java.util.ArrayList;
import java.util.List;

import com.github.pfmiles.dropincc.Action;
import com.github.pfmiles.dropincc.Predicate;
import com.github.pfmiles.dropincc.impl.util.Util;

/**
 * Compiled rule alternative, with matching element sequence and look-aheads,
 * action
 * 
 * @author pf-miles
 * 
 */
public class CAlternative {

    private List<EleType> matchSequence = new ArrayList<EleType>();
    private Action action = null;
    private Predicate predicate = null;

    public CAlternative(List<EleType> ms, Action action, Predicate pred) {
        this.matchSequence = ms;
        this.action = action;
        this.predicate = pred;
    }

    public List<EleType> getMatchSequence() {
        return matchSequence;
    }

    public void setMatchSequence(List<EleType> matchSequence) {
        this.matchSequence = matchSequence;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    // same hashCode method as Object.class needed
    public int hashCode() {
        return super.hashCode();
    }

    // same equals method as Object.class needed
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CAlternative(").append(this.matchSequence.toString());
        if (this.action != null)
            sb.append(", ").append(Util.resolveActionName(this.action));
        sb.append(")");
        return sb.toString();
    }

    public Predicate getPredicate() {
        return predicate;
    }

    public void setPredicate(Predicate predicate) {
        this.predicate = predicate;
    }
}
