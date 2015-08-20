package org.semanticweb.binaryowl.lookup;

import org.semanticweb.binaryowl.owlobject.OWLObjectBinaryTypeSelector;
import org.semanticweb.owlapi.model.*;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;

public class LookupTableBasedAxiomComparator implements Comparator<OWLAxiom> {

    private final LookupTable table;
    private final static OWLObjectBinaryTypeSelector selector = new OWLObjectBinaryTypeSelector();

    private class ComparisonVisitor implements OWLObjectVisitorEx<Integer> {
        OWLObject o2;

        public ComparisonVisitor(OWLObject o2) {
            this.o2 = o2;
        }

        // Ontology 

        @Nonnull
        @Override
        public Integer visit(@Nonnull OWLOntology o1) {
            int cmp;
            cmp = compareObjectTypes(o1, o2);
            if(cmp != 0) {
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
            return compareAnnotation((OWLAnnotation) o2, a1);
        }

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
            ComparisonVisitor drComparator = new ComparisonVisitor(d2.getDataRange());
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
            ComparisonVisitor compareDomain = new ComparisonVisitor(a2.getDomain());
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
            ComparisonVisitor compareRange = new ComparisonVisitor(a2.getRange());
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
            ComparisonVisitor compareDomain = new ComparisonVisitor(a2.getDomain());
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
            ComparisonVisitor compareRange = new ComparisonVisitor(a2.getRange());
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

        // intersection
        @Nonnull
        @Override
        public Integer visit(@Nonnull OWLObjectIntersectionOf owlObjectIntersectionOf) {
            throw new NoSuchMethodError("NOT DONE");// TODO
        }

        @Nonnull
        @Override
        public Integer visit(@Nonnull OWLDataIntersectionOf owlDataIntersectionOf) {
            throw new NoSuchMethodError("NOT DONE");// TODO
        }

        // union 
        @Nonnull
        @Override
        public Integer visit(@Nonnull OWLObjectUnionOf owlObjectUnionOf) {
            throw new NoSuchMethodError("NOT DONE");// TODO
        }

        @Nonnull
        @Override
        public Integer visit(@Nonnull OWLDataUnionOf owlDataUnionOf) {
            throw new NoSuchMethodError("NOT DONE");// TODO
        }

        // complement
        @Nonnull
        @Override
        public Integer visit(@Nonnull OWLObjectComplementOf owlObjectComplementOf) {
            throw new NoSuchMethodError("NOT DONE");// TODO
        }

        @Nonnull
        @Override
        public Integer visit(@Nonnull OWLDataComplementOf owlDataComplementOf) {
            throw new NoSuchMethodError("NOT DONE");// TODO
        }

        // one of 
        @Nonnull
        @Override
        public Integer visit(@Nonnull OWLObjectOneOf owlObjectOneOf) {
            throw new NoSuchMethodError("NOT DONE");// TODO
        }

        @Nonnull
        @Override
        public Integer visit(@Nonnull OWLDataOneOf owlDataOneOf) {
            throw new NoSuchMethodError("NOT DONE");// TODO
        }

        // some values from
        @Nonnull
        @Override
        public Integer visit(@Nonnull OWLObjectSomeValuesFrom owlObjectSomeValuesFrom) {
            throw new NoSuchMethodError("NOT DONE");// TODO
        }

        @Nonnull
        @Override
        public Integer visit(@Nonnull OWLDataSomeValuesFrom owlDataSomeValuesFrom) {
            throw new NoSuchMethodError("NOT DONE");// TODO
        }

        // All values from 
        @Nonnull
        @Override
        public Integer visit(@Nonnull OWLObjectAllValuesFrom owlObjectAllValuesFrom) {
            throw new NoSuchMethodError("NOT DONE");// TODO
        }

        @Nonnull
        @Override
        public Integer visit(@Nonnull OWLDataAllValuesFrom owlDataAllValuesFrom) {
            throw new NoSuchMethodError("NOT DONE");// TODO
        }
        // has value

        @Nonnull
        @Override
        public Integer visit(@Nonnull OWLObjectHasValue owlObjectHasValue) {
            throw new NoSuchMethodError("NOT DONE");// TODO
        }

        @Nonnull
        @Override
        public Integer visit(@Nonnull OWLDataHasValue owlDataHasValue) {
            throw new NoSuchMethodError("NOT DONE");// TODO
        }

        // selfie

        @Nonnull
        @Override
        public Integer visit(@Nonnull OWLObjectHasSelf owlObjectHasSelf) {
            throw new NoSuchMethodError("NOT DONE");// TODO
        }

        // cardinality constraints 


        @Nonnull
        @Override
        public Integer visit(@Nonnull OWLObjectMinCardinality owlObjectMinCardinality) {
            throw new NoSuchMethodError("NOT DONE");// TODO
        }

        @Nonnull
        @Override
        public Integer visit(@Nonnull OWLDataMinCardinality owlDataMinCardinality) {
            throw new NoSuchMethodError("NOT DONE");// TODO
        }

        @Nonnull
        @Override
        public Integer visit(@Nonnull OWLObjectMaxCardinality owlObjectMaxCardinality) {
            throw new NoSuchMethodError("NOT DONE");// TODO
        }

        @Nonnull
        @Override
        public Integer visit(@Nonnull OWLDataMaxCardinality owlDataMaxCardinality) {
            throw new NoSuchMethodError("NOT DONE");// TODO
        }

        @Nonnull
        @Override
        public Integer visit(@Nonnull OWLObjectExactCardinality owlObjectExactCardinality) {
            throw new NoSuchMethodError("NOT DONE");// TODO
        }

        @Nonnull
        @Override
        public Integer visit(@Nonnull OWLDataExactCardinality owlDataExactCardinality) {
            throw new NoSuchMethodError("NOT DONE");// TODO
        }

        // datatype / facet restriction

        @Nonnull
        @Override
        public Integer visit(@Nonnull OWLDatatypeRestriction owlDatatypeRestriction) {
            throw new NoSuchMethodError("NOT DONE");// TODO
        }

        @Nonnull
        @Override
        public Integer visit(@Nonnull OWLFacetRestriction owlFacetRestriction) {
            throw new NoSuchMethodError("NOT DONE");// TODO
        }

        // object property expression

        @Nonnull
        @Override
        public Integer visit(@Nonnull OWLObjectInverseOf owlObjectInverseOf) {
            throw new NoSuchMethodError("NOT DONE");// TODO
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

    public LookupTableBasedAxiomComparator(LookupTable table) {

        this.table = table;
    }

    @Override
    public int compare(OWLAxiom o1, OWLAxiom o2) {
        ComparisonVisitor comparisonVisitor = new ComparisonVisitor(o2);
        return o1.accept(comparisonVisitor);
    }


    private int compareAnnotations(Collection<OWLAnnotation> a1, Collection<OWLAnnotation> a2) {
        Iterator<OWLAnnotation> it1 = a1.iterator();
        Iterator<OWLAnnotation> it2 = a2.iterator();

        while (it1.hasNext() && it2.hasNext()) {
            int cmp = compareAnnotation(it1.next(), it2.next());

            if (cmp != 0) {
                return cmp;
            }
        }
        if (it2.hasNext()) return -1;
        if (it1.hasNext()) return +1;
        return 0;
    }

    private int compareAnnotation(OWLAnnotation a1, OWLAnnotation a2) {
        int cmp;
        cmp = compareAnnotationValue(a1.getValue(), a2.getValue());
        if (cmp != 0) {
            return cmp;
        }
        cmp = compareEntity(a1.getProperty(), a2.getProperty());
        if (cmp != 0) {
            return cmp;
        }

        return compareAnnotations(a1.getAnnotations(), a2.getAnnotations());
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
        return table.getIRILookupTable().getIndex(iri1) - table.getIRILookupTable().getIndex(iri2);
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

}
