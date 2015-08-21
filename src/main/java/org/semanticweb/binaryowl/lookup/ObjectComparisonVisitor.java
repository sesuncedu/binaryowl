package org.semanticweb.binaryowl.lookup;/**
 * Created by ses on 8/21/15.
 */

import org.semanticweb.binaryowl.owlobject.OWLObjectBinaryTypeSelector;
import org.semanticweb.owlapi.model.*;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Iterator;

class ObjectComparisonVisitor implements OWLObjectVisitorEx<Integer> {
    private final static OWLObjectBinaryTypeSelector selector = new OWLObjectBinaryTypeSelector();
    OWLObject o2;
    LookupTable table;

    public ObjectComparisonVisitor(OWLObject o2, LookupTable table) {
        this.o2 = o2;
        this.table = table;
    }

    // Ontology

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLOntology o1) {
        int cmp;
        cmp = compareObjectTypes(o1, o2);
        if (cmp != 0) {
            return cmp;
        }
        OWLOntology o21 = (OWLOntology) this.o2;
        return o1.getOntologyID().compareTo(o21.getOntologyID());

    }

    // Values

    @Nonnull
    @Override
    public Integer visit(@Nonnull IRI i1) {
        int cmp;
        cmp = compareObjectTypes(i1, o2);
        if (cmp != 0) {
            return cmp;
        }
        return compareIRI(i1, (IRI) o2);
    }


    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLLiteral l1) {
        int cmp;
        cmp = compareObjectTypes(l1, o2);
        if (cmp != 0) {
            return cmp;
        }
        return compareLiteral(l1, (OWLLiteral) o2);
    }

    // Entities

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLClass i1) {
        return compareEntity(i1, (OWLEntity) o2);
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLDatatype i1) {
        return compareEntity(i1, (OWLEntity) o2);
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLObjectProperty i1) {
        return compareEntity(i1, (OWLEntity) o2);
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLDataProperty i1) {
        return compareEntity(i1, (OWLEntity) o2);
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLAnnotationProperty i1) {
        return compareEntity(i1, (OWLEntity) o2);
    }

    // Individuals
    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLNamedIndividual i1) {
        return compareEntity(i1, (OWLEntity) o2);
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLAnonymousIndividual a1) {
        return a1.compareTo(o2);
    }

    // Annotation
    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLAnnotation a1) {
        int cmp;
        cmp = compareObjectTypes(a1, o2);
        if (cmp != 0) {
            return cmp;
        }
        OWLAnnotation a2 = (OWLAnnotation)o2;
        return table.getAnnotationLookupTable().compare(a1,a2);
    }
    /*public Integer visit(@Nonnull OWLAnnotation a1) {
        int cmp;
        cmp = compareObjectTypes(a1, o2);
        if (cmp != 0) {
            return cmp;
        }
        int cmp1;
        cmp1 = compareEntity(((OWLAnnotation) o2).getProperty(), a1.getProperty());
        if (cmp1 != 0) {
            return cmp1;
        }

        cmp1 = compareAnnotationValue(((OWLAnnotation) o2).getValue(), a1.getValue());
        if (cmp1 != 0) {
            return cmp1;
        }

        return compareAnnotations(((OWLAnnotation) o2).getAnnotations(), a1.getAnnotations());
    }  */


    // Declaration
    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLDeclarationAxiom d1) {
        int cmp = compareObjectTypes(d1, o2);
        if (cmp != 0) {
            return cmp;
        }
        OWLDeclarationAxiom d2 = (OWLDeclarationAxiom) o2;

        cmp = compareEntity(d1.getEntity(), d2.getEntity());
        if (cmp != 0) {
            return cmp;
        }
        return compareAnnotations(d1.getAnnotations(), d2.getAnnotations());
    }

    // Assertion Axioms

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLAnnotationAssertionAxiom a1) {
        int cmp = compareObjectTypes(a1, o2);
        if (cmp != 0) {
            return cmp;
        }

        OWLAnnotationAssertionAxiom a2 = (OWLAnnotationAssertionAxiom) o2;
        cmp = compareAnnotationValue(a1.getValue(), a2.getValue());
        if (cmp != 0) {
            return cmp;
        }
        cmp = compareEntity(a1.getProperty(), a2.getProperty());
        if (cmp != 0) {
            return cmp;
        }
        cmp = compareAnnotationSubject(a1.getSubject(), a2.getSubject());
        if (cmp != 0) {
            return cmp;
        }
        return compareAnnotations(a1.getAnnotations(), a2.getAnnotations());
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLDataPropertyAssertionAxiom a1) {
        int cmp = compareObjectTypes(a1, o2);
        if (cmp != 0) {
            return cmp;
        }
        OWLDataPropertyAssertionAxiom a2 = (OWLDataPropertyAssertionAxiom) o2;

        cmp = compareLiteral(a1.getObject(), a2.getObject());
        if (cmp != 0) {
            return cmp;
        }
        cmp = compareEntity(a1.getProperty().asOWLDataProperty(), a2.getProperty().asOWLDataProperty());
        if (cmp != 0) {
            return cmp;
        }
        cmp = compareIndividual(a1.getSubject(), a2.getSubject());
        if (cmp != 0) {
            return cmp;
        }
        return compareAnnotations(a1.getAnnotations(), a2.getAnnotations());
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLNegativeDataPropertyAssertionAxiom a1) {
        int cmp = compareObjectTypes(a1, o2);
        if (cmp != 0) {
            return cmp;
        }
        OWLNegativeDataPropertyAssertionAxiom a2 = (OWLNegativeDataPropertyAssertionAxiom) o2;

        cmp = compareLiteral(a1.getObject(), a2.getObject());
        if (cmp != 0) {
            return cmp;
        }
        cmp = compareEntity(a1.getProperty().asOWLDataProperty(), a2.getProperty().asOWLDataProperty());
        if (cmp != 0) {
            return cmp;
        }
        cmp = compareIndividual(a1.getSubject(), a2.getSubject());
        if (cmp != 0) {
            return cmp;
        }
        return compareAnnotations(a1.getAnnotations(), a2.getAnnotations());
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLObjectPropertyAssertionAxiom a1) {
        int cmp = compareObjectTypes(a1, o2);
        if (cmp != 0) {
            return cmp;
        }
        OWLObjectPropertyAssertionAxiom a2 = (OWLObjectPropertyAssertionAxiom) o2;

        cmp = compareIndividual(a1.getSubject(), a2.getSubject());
        if (cmp != 0) {
            return cmp;
        }

        cmp = compareObjectPropertyExpression(a1.getProperty(), a2.getProperty());
        if (cmp != 0) {
            return cmp;
        }

        cmp = compareIndividual(a1.getObject(), a2.getObject());
        if (cmp != 0) {
            return cmp;
        }
        return compareAnnotations(a1.getAnnotations(), a2.getAnnotations());
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLNegativeObjectPropertyAssertionAxiom a1) {
        int cmp = compareObjectTypes(a1, o2);
        if (cmp != 0) {
            return cmp;
        }
        OWLNegativeObjectPropertyAssertionAxiom a2 = (OWLNegativeObjectPropertyAssertionAxiom) o2;

        cmp = compareIndividual(a1.getSubject(), a2.getSubject());
        if (cmp != 0) {
            return cmp;
        }

        cmp = compareObjectPropertyExpression(a1.getProperty(), a2.getProperty());
        if (cmp != 0) {
            return cmp;
        }

        cmp = compareIndividual(a1.getObject(), a2.getObject());
        if (cmp != 0) {
            return cmp;
        }
        return compareAnnotations(a1.getAnnotations(), a2.getAnnotations());
    }

    // Datatype definition
    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLDatatypeDefinitionAxiom d1) {
        int cmp = compareObjectTypes(d1, o2);
        if (cmp != 0) {
            return cmp;
        }
        OWLDatatypeDefinitionAxiom d2 = (OWLDatatypeDefinitionAxiom) o2;

        cmp = compareEntity(d1.getDatatype(), d2.getDatatype());
        if (cmp != 0) {
            return cmp;
        }
        ObjectComparisonVisitor drComparator = new ObjectComparisonVisitor(d2.getDataRange(), table);
        cmp = d1.getDataRange().accept(drComparator);
        if (cmp != 0) {
            return cmp;
        }
        return compareAnnotations(d1.getAnnotations(), d2.getAnnotations());
    }


    // Domain and Range Axioms

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLAnnotationPropertyDomainAxiom a1) {
        int cmp = compareObjectTypes(a1, o2);
        if (cmp != 0) {
            return cmp;
        }

        OWLAnnotationPropertyDomainAxiom a2 = (OWLAnnotationPropertyDomainAxiom) o2;
        cmp = compareEntity(a1.getProperty(), a2.getProperty());
        if (cmp != 0) {
            return cmp;
        }
        cmp = compareIRI(a1.getDomain(), a2.getDomain());
        if (cmp != 0) {
            return cmp;
        }
        return compareAnnotations(a1.getAnnotations(), a2.getAnnotations());
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLAnnotationPropertyRangeAxiom a1) {
        int cmp = compareObjectTypes(a1, o2);
        if (cmp != 0) {
            return cmp;
        }

        OWLAnnotationPropertyRangeAxiom a2 = (OWLAnnotationPropertyRangeAxiom) o2;

        cmp = compareEntity(a1.getProperty(), a2.getProperty());
        if (cmp != 0) {
            return cmp;
        }
        cmp = compareIRI(a1.getRange(), a2.getRange());
        if (cmp != 0) {
            return cmp;
        }
        return compareAnnotations(a1.getAnnotations(), a2.getAnnotations());
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLDataPropertyDomainAxiom a1) {
        int cmp = compareObjectTypes(a1, o2);
        if (cmp != 0) {
            return cmp;
        }
        OWLDataPropertyDomainAxiom a2 = (OWLDataPropertyDomainAxiom) o2;

        cmp = compareEntity(a1.getProperty().asOWLDataProperty(), a2.getProperty().asOWLDataProperty());
        if (cmp != 0) {
            return cmp;
        }
        ObjectComparisonVisitor compareDomain = new ObjectComparisonVisitor(a2.getDomain(), table);
        cmp = a1.getDomain().accept(compareDomain);
        if (cmp != 0) {
            return cmp;
        }
        return compareAnnotations(a1.getAnnotations(), a2.getAnnotations());
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLDataPropertyRangeAxiom a1) {
        int cmp = compareObjectTypes(a1, o2);
        if (cmp != 0) {
            return cmp;
        }
        OWLDataPropertyRangeAxiom a2 = (OWLDataPropertyRangeAxiom) o2;

        cmp = compareEntity(a1.getProperty().asOWLDataProperty(), a2.getProperty().asOWLDataProperty());
        if (cmp != 0) {
            return cmp;
        }
        ObjectComparisonVisitor compareRange = new ObjectComparisonVisitor(a2.getRange(), table);
        cmp = a1.getRange().accept(compareRange);
        if (cmp != 0) {
            return cmp;
        }
        return compareAnnotations(a1.getAnnotations(), a2.getAnnotations());
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLObjectPropertyDomainAxiom a1) {
        int cmp = compareObjectTypes(a1, o2);
        if (cmp != 0) {
            return cmp;
        }
        OWLObjectPropertyDomainAxiom a2 = (OWLObjectPropertyDomainAxiom) o2;

        cmp = compareObjectPropertyExpression(a1.getProperty(), a2.getProperty());
        if (cmp != 0) {
            return cmp;
        }
        ObjectComparisonVisitor compareDomain = new ObjectComparisonVisitor(a2.getDomain(), table);
        cmp = a1.getDomain().accept(compareDomain);
        if (cmp != 0) {
            return cmp;
        }
        return compareAnnotations(a1.getAnnotations(), a2.getAnnotations());
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLObjectPropertyRangeAxiom a1) {
        int cmp = compareObjectTypes(a1, o2);
        if (cmp != 0) {
            return cmp;
        }
        OWLObjectPropertyRangeAxiom a2 = (OWLObjectPropertyRangeAxiom) o2;

        cmp = compareObjectPropertyExpression(a1.getProperty(), a2.getProperty());
        if (cmp != 0) {
            return cmp;
        }
        ObjectComparisonVisitor compareRange = new ObjectComparisonVisitor(a2.getRange(), table);
        cmp = a1.getRange().accept(compareRange);
        if (cmp != 0) {
            return cmp;
        }
        return compareAnnotations(a1.getAnnotations(), a2.getAnnotations());
    }

    // SubProperty Axioms

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLSubAnnotationPropertyOfAxiom a1) {
        int cmp = compareObjectTypes(a1, o2);
        if (cmp != 0) {
            return cmp;
        }
        OWLSubAnnotationPropertyOfAxiom a2 = (OWLSubAnnotationPropertyOfAxiom) o2;

        cmp = compareEntity(a1.getSuperProperty(), a2.getSuperProperty());
        if (cmp != 0) {
            return cmp;
        }
        cmp = compareEntity(a1.getSubProperty(), a2.getSubProperty());
        if (cmp != 0) {
            return cmp;
        }
        return compareAnnotations(a1.getAnnotations(), a2.getAnnotations());
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLSubObjectPropertyOfAxiom a1) {
        int cmp = compareObjectTypes(a1, o2);
        if (cmp != 0) {
            return cmp;
        }
        OWLSubObjectPropertyOfAxiom a2 = (OWLSubObjectPropertyOfAxiom) o2;

        cmp = compareObjectPropertyExpression(a1.getSuperProperty(), a2.getSuperProperty());
        if (cmp != 0) {
            return cmp;
        }
        cmp = compareObjectPropertyExpression(a1.getSubProperty(), a2.getSubProperty());
        if (cmp != 0) {
            return cmp;
        }
        return compareAnnotations(a1.getAnnotations(), a2.getAnnotations());
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLSubDataPropertyOfAxiom a1) {
        int cmp = compareObjectTypes(a1, o2);
        if (cmp != 0) {
            return cmp;
        }

        OWLSubDataPropertyOfAxiom a2 = (OWLSubDataPropertyOfAxiom) o2;
        cmp = compareEntity(a1.getSuperProperty().asOWLDataProperty(), a2.getSuperProperty().asOWLDataProperty());
        if (cmp != 0) {
            return cmp;
        }
        cmp = compareEntity(a1.getSubProperty().asOWLDataProperty(), a2.getSubProperty().asOWLDataProperty());
        if (cmp != 0) {
            return cmp;
        }
        return compareAnnotations(a1.getAnnotations(), a2.getAnnotations());
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLSubPropertyChainOfAxiom owlSubPropertyChainOfAxiom) {
        throw new NoSuchMethodError("NOT DONE");// TODO
    }

    // Class Assertion Axiom TODO Move when done

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLClassAssertionAxiom owlClassAssertionAxiom) {
        throw new NoSuchMethodError("NOT DONE");// TODO
    }

    // SubClass Axiom

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLSubClassOfAxiom owlSubClassOfAxiom) {
        throw new NoSuchMethodError("NOT DONE");// TODO
    }


    // object property attribute axioms

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLFunctionalObjectPropertyAxiom owlFunctionalObjectPropertyAxiom) {
        throw new NoSuchMethodError("NOT DONE");// TODO
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLInverseFunctionalObjectPropertyAxiom owlInverseFunctionalObjectPropertyAxiom) {
        throw new NoSuchMethodError("NOT DONE");// TODO
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLSymmetricObjectPropertyAxiom owlSymmetricObjectPropertyAxiom) {
        throw new NoSuchMethodError("NOT DONE");// TODO
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLAsymmetricObjectPropertyAxiom owlAsymmetricObjectPropertyAxiom) {
        throw new NoSuchMethodError("NOT DONE");// TODO
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLReflexiveObjectPropertyAxiom owlReflexiveObjectPropertyAxiom) {
        throw new NoSuchMethodError("NOT DONE");// TODO
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLTransitiveObjectPropertyAxiom owlTransitiveObjectPropertyAxiom) {
        throw new NoSuchMethodError("NOT DONE");// TODO
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLIrreflexiveObjectPropertyAxiom owlIrreflexiveObjectPropertyAxiom) {
        throw new NoSuchMethodError("NOT DONE");// TODO
    }


    // data property attribute axioms

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLFunctionalDataPropertyAxiom owlFunctionalDataPropertyAxiom) {
        throw new NoSuchMethodError("NOT DONE");// TODO
    }


    // disjointness axioms

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLDisjointClassesAxiom owlDisjointClassesAxiom) {
        throw new NoSuchMethodError("NOT DONE");// TODO
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLDisjointUnionAxiom owlDisjointUnionAxiom) {
        throw new NoSuchMethodError("NOT DONE");// TODO
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLDisjointDataPropertiesAxiom owlDisjointDataPropertiesAxiom) {
        throw new NoSuchMethodError("NOT DONE");// TODO
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLDisjointObjectPropertiesAxiom owlDisjointObjectPropertiesAxiom) {
        throw new NoSuchMethodError("NOT DONE");// TODO
    }

    // Inverse property axiom

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLInverseObjectPropertiesAxiom owlInverseObjectPropertiesAxiom) {
        throw new NoSuchMethodError("NOT DONE");// TODO
    }


    // Equivalence axioms

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLEquivalentObjectPropertiesAxiom owlEquivalentObjectPropertiesAxiom) {
        throw new NoSuchMethodError("NOT DONE");// TODO
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLEquivalentDataPropertiesAxiom owlEquivalentDataPropertiesAxiom) {
        throw new NoSuchMethodError("NOT DONE");// TODO
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLEquivalentClassesAxiom owlEquivalentClassesAxiom) {
        throw new NoSuchMethodError("NOT DONE");// TODO
    }

    // Individuals axioms

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLDifferentIndividualsAxiom owlDifferentIndividualsAxiom) {
        throw new NoSuchMethodError("NOT DONE");// TODO
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLSameIndividualAxiom owlSameIndividualAxiom) {
        throw new NoSuchMethodError("NOT DONE");// TODO
    }

    // Key Axiom

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLHasKeyAxiom owlHasKeyAxiom) {
        throw new NoSuchMethodError("NOT DONE");// TODO
    }


    // ****  Class and Data Expressions


    // utility methods
    private int compareNaryDataRange(OWLNaryDataRange r1) {
        int cmp = compareObjectTypes(r1, o2);
        if (cmp != 0) {
            return cmp;
        }
        OWLNaryDataRange r2 = (OWLNaryDataRange) o2;
        return compareObjects(r1.getOperands(), r2.getOperands());

    }

    private int compareNaryBooleanClassExpression(OWLNaryBooleanClassExpression e1) {
        int cmp = compareObjectTypes(e1, o2);
        if (cmp != 0) {
            return cmp;
        }
        OWLNaryBooleanClassExpression e2 = (OWLNaryBooleanClassExpression) o2;
        return compareObjects(e1.getOperands(), e2.getOperands());

    }

    private int compareHasFiller(HasFiller f1) {
        int cmp = compareObjectTypes((OWLObject) f1, o2);
        if (cmp != 0) {
            return cmp;
        }
        HasFiller f2 = (HasFiller) o2;
        ObjectComparisonVisitor cv = new ObjectComparisonVisitor(f2.getFiller(), table);
        return f1.getFiller().accept(cv);
    }

    private int compareQuantifiedRestriction(OWLQuantifiedRestriction q1) {
        int cmp = compareHasFiller(q1);
        return cmp;

    }

    private int compareHasValueRestriction(OWLHasValueRestriction v1) {
        int cmp = compareHasFiller(v1);
        return cmp;
    }

    private int compareCardinalityRestriction(OWLCardinalityRestriction c1) {
        int cmp = compareHasFiller(c1);
        if(cmp != 0) {
            return cmp;
        }
        OWLCardinalityRestriction c2 = (OWLCardinalityRestriction)o2;
        return c1.getCardinality() - c2.getCardinality();
    }

    // intersection
    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLObjectIntersectionOf i1) {
        return compareNaryBooleanClassExpression(i1);
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLDataIntersectionOf r1) {
        return compareNaryDataRange(r1);
    }

    // union
    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLObjectUnionOf u1) {
        return compareNaryBooleanClassExpression(u1);
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLDataUnionOf r1) {
        return compareNaryDataRange(r1);
    }

    // complement
    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLObjectComplementOf e1) {
        int cmp = compareObjectTypes(e1, o2);
        if (cmp != 0) {
            return cmp;
        }
        OWLObjectComplementOf e2 = (OWLObjectComplementOf) o2;
        ObjectComparisonVisitor cv = new ObjectComparisonVisitor(e2.getOperand(), table);
        return e1.getOperand().accept(cv);
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLDataComplementOf r1) {
        int cmp = compareObjectTypes(r1, o2);
        if (cmp != 0) {
            return cmp;
        }
        OWLDataComplementOf r2 = (OWLDataComplementOf) o2;
        ObjectComparisonVisitor cv = new ObjectComparisonVisitor(r2.getDataRange(), table);
        return r1.getDataRange().accept(cv);
    }

    // one of
    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLObjectOneOf e1) {
        int cmp = compareObjectTypes(e1, o2);
        if (cmp != 0) {
            return cmp;
        }
        OWLObjectOneOf e2 = (OWLObjectOneOf) o2;
        return compareObjects(e1.getIndividuals(), e2.getIndividuals());
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLDataOneOf r1) {
        int cmp = compareObjectTypes(r1, o2);
        if (cmp != 0) {
            return cmp;
        }
        OWLDataOneOf r2 = (OWLDataOneOf) o2;
        return compareObjects(r1.getValues(), r2.getValues());
    }

    // some values from
    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLObjectSomeValuesFrom q1) {
        int cmp = compareQuantifiedRestriction(q1);
        return cmp;

    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLDataSomeValuesFrom q1) {
        int cmp = compareQuantifiedRestriction(q1);
        return cmp;
    }

    // All values from
    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLObjectAllValuesFrom q1) {
        int cmp = compareQuantifiedRestriction(q1);
        return cmp;
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLDataAllValuesFrom q1) {
        int cmp = compareQuantifiedRestriction(q1);
        return cmp;
    }
    // has value

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLObjectHasValue v1) {
        int cmp = compareHasValueRestriction(v1);
        return cmp;

    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLDataHasValue v1) {
        int cmp = compareHasValueRestriction(v1);
        return cmp;
    }

    // selfie

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLObjectHasSelf s1) {
        int cmp = compareObjectTypes(s1, o2);
        return cmp;
    }

    // cardinality constraints


    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLObjectMinCardinality c1) {
        int cmp = compareCardinalityRestriction(c1);
        return cmp;
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLDataMinCardinality c1) {
        int cmp = compareCardinalityRestriction(c1);
        return cmp;
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLObjectMaxCardinality c1) {
        int cmp = compareCardinalityRestriction(c1);
        return cmp;
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLDataMaxCardinality c1) {
        int cmp = compareCardinalityRestriction(c1);
        return cmp;
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLObjectExactCardinality c1) {
        int cmp = compareCardinalityRestriction(c1);
        return cmp;
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLDataExactCardinality c1) {
        int cmp = compareCardinalityRestriction(c1);
        return cmp;
    }

    // datatype / facet restriction

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLDatatypeRestriction r1) {
        int cmp = compareObjectTypes(r1, o2);
        if (cmp != 0) {
            return cmp;
        }
        OWLDatatypeRestriction r2 = (OWLDatatypeRestriction) o2;
        return compareObjects(r1.getFacetRestrictions(), r2.getFacetRestrictions());
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLFacetRestriction r1) {
        int cmp = compareObjectTypes(r1, o2);
        if (cmp != 0) {
            return cmp;
        }
        OWLFacetRestriction r2 = (OWLFacetRestriction) o2;
        cmp = compareLiteral(r1.getFacetValue(), r2.getFacetValue());
        if(cmp != 0) {
            return cmp;
        }

        cmp = compareIRI(r1.getFacet().getIRI(), r2.getFacet().getIRI());
        if(cmp != 0) {
            return cmp;
        }
        cmp = compareLiteral(r1.getFacetValue(), r2.getFacetValue());

        return cmp;
    }

    // object property expression

    @Nonnull
    @Override
    public Integer visit(@Nonnull OWLObjectInverseOf p1) {
        int cmp = compareObjectTypes(p1, o2);
        if (cmp != 0) {
            return cmp;
        }
        OWLObjectInverseOf p2 = (OWLObjectInverseOf)o2;
        return compareEntity(p1.getSimplified().getNamedProperty(),p2.getSimplified().getNamedProperty());
    }

    // swrlie

    @Nonnull
    @Override
    public Integer visit(@Nonnull SWRLRule swrlRule) {
        throw new NoSuchMethodError("NOT DONE");// TODO
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull SWRLClassAtom swrlClassAtom) {
        throw new NoSuchMethodError("NOT DONE");// TODO
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull SWRLDataRangeAtom swrlDataRangeAtom) {
        throw new NoSuchMethodError("NOT DONE");// TODO
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull SWRLDataPropertyAtom swrlDataPropertyAtom) {
        throw new NoSuchMethodError("NOT DONE");// TODO
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull SWRLObjectPropertyAtom swrlObjectPropertyAtom) {
        throw new NoSuchMethodError("NOT DONE");// TODO
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull SWRLBuiltInAtom swrlBuiltInAtom) {
        throw new NoSuchMethodError("NOT DONE");// TODO
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull SWRLVariable swrlVariable) {
        throw new NoSuchMethodError("NOT DONE");// TODO
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull SWRLIndividualArgument swrlIndividualArgument) {
        throw new NoSuchMethodError("NOT DONE");// TODO
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull SWRLSameIndividualAtom swrlSameIndividualAtom) {
        throw new NoSuchMethodError("NOT DONE");// TODO
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull SWRLLiteralArgument swrlLiteralArgument) {
        throw new NoSuchMethodError("NOT DONE");// TODO
    }

    @Nonnull
    @Override
    public Integer visit(@Nonnull SWRLDifferentIndividualsAtom swrlDifferentIndividualsAtom) {
        throw new NoSuchMethodError("NOT DONE");// TODO
    }

    private int compareObjects(Collection<? extends OWLObject> os1, Collection<? extends OWLObject> os2) {
        int cmp = 0;
        Iterator<? extends OWLObject> it1 = os1.iterator();
        Iterator<? extends OWLObject> it2 = os2.iterator();
        while (it1.hasNext() && it2.hasNext()) {
            OWLObject n1 = it1.next();
            OWLObject n2 = it2.next();
            ObjectComparisonVisitor cv = new ObjectComparisonVisitor(n2, table);
            cmp = n1.accept(cv);
            if (cmp != 0) {
                break;
            }
        }
        if (cmp != 0) {
            return cmp;
        }
        if (it1.hasNext()) {
            return -1;
        }
        if (it2.hasNext()) {
            return +1;
        }
        return 0;
    }

    private int compareAnnotations(Collection<OWLAnnotation> a1, Collection<OWLAnnotation> a2) {
        return compareObjects(a1, a2);
    }

    private int compareEntity(OWLEntity e1, OWLEntity e2) {
        int cmp;
        cmp = compareObjectTypes(e1, e2);
        if (cmp != 0) {
            return cmp;
        }
        return compareIRI(e1.getIRI(), e2.getIRI());
    }

    private int compareObjectTypes(OWLObject o1, OWLObject o2) {
        int cmp = o1.accept(selector).getMarker() - o2.accept(selector).getMarker();
        return cmp;
    }


    private int compareLiteral(OWLLiteral l1, OWLLiteral l2) {
        return table.getLiteralLookupTable().getIndex(l1) - table.getLiteralLookupTable().getIndex(l2);
    }

    private int compareIRI(IRI iri1, IRI iri2) {
        return table.getIRILookupTable().compare(iri1, iri2);
    }

    private int compareAnnotationValue(OWLAnnotationValue v1, OWLAnnotationValue v2) {
        int cmp;
        if (v1 instanceof IRI) {
            if (!(v2 instanceof IRI)) {
                cmp = -1;
            } else {
                cmp = compareIRI((IRI) v1, (IRI) v2);
            }
        } else {
            if (v2 instanceof IRI) {
                cmp = +1;
            } else {
                cmp = compareLiteral((OWLLiteral) v1, (OWLLiteral) v2);
            }
        }
        return cmp;
    }

    private int compareObjectPropertyExpression(OWLObjectPropertyExpression p1, OWLObjectPropertyExpression p2) {
        if (p1 instanceof OWLObjectProperty) {
            if (!(p2 instanceof OWLObjectProperty)) {
                return +1;
            } else {
                return compareEntity(p1.asOWLObjectProperty(), p2.asOWLObjectProperty());
            }
        } else {
            if (!(p2 instanceof OWLObjectInverseOf)) {
                return +1;
            } else {
                OWLObjectInverseOf i1 = (OWLObjectInverseOf) p1;
                OWLObjectInverseOf i2 = (OWLObjectInverseOf) p2;
                return compareEntity(i1.getInverse().asOWLObjectProperty(), i2.getInverse().asOWLObjectProperty());
            }
        }
    }

    private int compareIndividual(OWLIndividual s1, OWLIndividual s2) {
        if (s1 instanceof OWLAnonymousIndividual) {
            if (!(s2 instanceof OWLAnonymousIndividual)) {
                return -1;
            } else {
                return s1.compareTo(s2);
            }
        } else {
            if (!(s2 instanceof OWLNamedIndividual)) {
                return +1;
            } else {
                return compareIRI(s1.asOWLNamedIndividual().getIRI(), s2.asOWLNamedIndividual().getIRI());
            }
        }
    }

    private int compareAnnotationSubject(OWLAnnotationSubject s1, OWLAnnotationSubject s2) {
        if (s1 instanceof OWLAnonymousIndividual) {
            if (!(s2 instanceof OWLAnonymousIndividual)) {
                return -1;
            } else {
                return s1.compareTo(s2);
            }
        } else {
            if (!(s2 instanceof IRI)) {
                return +1;
            } else {
                return compareIRI((IRI) s1, (IRI) s2);
            }
        }
    }

}
