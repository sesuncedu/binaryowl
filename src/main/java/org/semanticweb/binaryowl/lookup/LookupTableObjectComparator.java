package org.semanticweb.binaryowl.lookup;

import org.semanticweb.owlapi.model.OWLObject;

import java.util.Comparator;

public class LookupTableObjectComparator implements Comparator<OWLObject> {

    private final LookupTable table;


    public LookupTableObjectComparator(LookupTable table) {

        this.table = table;
    }

    @Override
    public int compare(OWLObject o1, OWLObject o2) {
        ObjectComparisonVisitor comparisonVisitor = new ObjectComparisonVisitor(o2,table);
        return o1.accept(comparisonVisitor);
    }



}
