package org.semanticweb.binaryowl.lookup;/**
 * Created by ses on 8/15/15.
 */

import org.semanticweb.owlapi.model.*;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

public class AxiomSorter implements Comparator<OWLAxiom> {

    private final Comparator<IRI> iriComparator;
    private final Comparator<OWLLiteral> literalComparator;


    public AxiomSorter(Comparator<IRI> iriComparator) {
        this.iriComparator = iriComparator;
        this.literalComparator = defaultLiteralComparator();
    }

    public AxiomSorter(Comparator<IRI> iriComparator,Comparator<OWLLiteral> literalComparator) {
        this.iriComparator = iriComparator;
        this.literalComparator = literalComparator;
    }

    private Comparator<OWLLiteral> defaultLiteralComparator() {
        return new Comparator<OWLLiteral>() {
            @Override
            public int compare(OWLLiteral o1, OWLLiteral o2) {
                return o1.compareTo(o2);
            }
        };
    }

    public AxiomSorter() {
        iriComparator = new Comparator<IRI>() {
            @Override
            public int compare(IRI o1, IRI o2) {
                return o1.compareTo(o2);
            }
        };
        literalComparator = defaultLiteralComparator();
    }

    public int compareIRI(IRI o1, IRI o2) {
        return iriComparator.compare(o1, o2);
    }

    public int compareLiteral(OWLLiteral o1, OWLLiteral o2) {
        return literalComparator.compare(o1,o2);
    }
    @Override
    public int compare(OWLAxiom o1, OWLAxiom o2) {
        if (o1 instanceof OWLSubClassOfAxiom && o2 instanceof OWLSubClassOfAxiom) {
            return compareSubClassAxioms((OWLSubClassOfAxiom) o1, (OWLSubClassOfAxiom) o2);

        } else if (o1 instanceof OWLAnnotationAssertionAxiom && o2 instanceof OWLAnnotationAssertionAxiom) {
            return compareAnnotationAssertionAxioms((OWLAnnotationAssertionAxiom) o1, (OWLAnnotationAssertionAxiom) o2);

        } else {
            return o1.compareTo(o2);
        }
    }

    private int compareAnnotationAssertionAxioms(OWLAnnotationAssertionAxiom a1, OWLAnnotationAssertionAxiom a2) {
        int cmp;

        cmp = compareAnnotationSubject(a1.getSubject(), a2.getSubject());

        if (cmp != 0) {
            return cmp;
        }

        cmp = compareEntity(a1.getProperty(), a2.getProperty());
        if (cmp != 0) {
            return cmp;
        }


        cmp = compareAnnotationValue(a1.getValue(), a2.getValue());

        if (cmp != 0) {
            return cmp;
        }


        cmp = compareAnnotations(a1.getAnnotations(), a2.getAnnotations());
        if (cmp != 0) {
            return cmp;
        }
        return cmp;

    }

    public int compareAnnotations(Set<OWLAnnotation> a1, Set<OWLAnnotation> a2) {
        Iterator<OWLAnnotation> it1 = new TreeSet<>(a1).iterator();
        Iterator<OWLAnnotation> it2 = new TreeSet<>(a2).iterator();
        int cmp = 0;
        while (it1.hasNext() && it2.hasNext()) {
            OWLAnnotation a11 = it1.next();
            OWLAnnotation a21 = it2.next();
            cmp = compareAnnotation(a11, a21);
            if (cmp != 0) {
                break;
            }
        }
        if (cmp == 0) {
            if (it1.hasNext()) {
                cmp = -1;
            } else if (it2.hasNext()) {
                cmp =  +1;
            }
        }
        return cmp;
    }

    public int compareAnnotationSubject(OWLAnnotationSubject a1Subject, OWLAnnotationSubject a2Subject) {
        int cmp;
        if (a1Subject instanceof IRI && a2Subject instanceof IRI) {
            cmp = compareIRI((IRI) a1Subject, (IRI) a2Subject);
        } else {
            cmp = a1Subject.compareTo(a2Subject);
        }
        return cmp;
    }

    public int compareAnnotationValue(OWLAnnotationValue v1, OWLAnnotationValue v2) {
        int cmp;
        if (v1 instanceof OWLLiteral) {
            if (v2 instanceof IRI) {
                cmp = -1;
            } else {
                cmp = compareLiteral((OWLLiteral) v1,(OWLLiteral) v2);
            }
        } else {
            if (v2 instanceof OWLLiteral) {
                cmp = +1;
            } else {
                cmp = iriComparator.compare((IRI) v1, (IRI) v2);
            }
        }
        return cmp;
    }

    public int compareEntity(OWLEntity e1, OWLEntity e2) {
        return compareIRI(e1.getIRI(), e2.getIRI());
    }

    private int compareSubClassAxioms(OWLSubClassOfAxiom sc1, OWLSubClassOfAxiom sc2) {
        int result;
        result = compareClassExpression(sc1.getSuperClass(), sc2.getSuperClass());
        if (result != 0) {
            return result;
        }
        result = compareClassExpression(sc1.getSubClass(), sc2.getSubClass());
        if (result != 0) {
            return result;
        }

        return compareAnnotations(sc1.getAnnotations(), sc2.getAnnotations());
    }

    private int compareClassExpression(OWLClassExpression c1, OWLClassExpression c2) {
        if (c1 instanceof OWLClass && c2 instanceof OWLClass) {
            return compareEntity(c1.asOWLClass(), c2.asOWLClass());
        } else {
            return c1.compareTo(c2);
        }
    }

    public int compareAnnotation(OWLAnnotation a1, OWLAnnotation a2) {
        int cmp;
        cmp = compareEntity(a1.getProperty(), a2.getProperty());
        if (cmp != 0) {
            return cmp;
        }
        cmp = compareAnnotationValue(a1.getValue(), a2.getValue());

        if (cmp != 0) {
            return cmp;
        }
        return compareAnnotations(a1.getAnnotations(), a2.getAnnotations());
    }

}
