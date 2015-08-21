package org.semanticweb.binaryowl.lookup;/**
 * Created by ses on 8/20/15.
 */

import com.google.common.base.Optional;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

public class PrimarySortKeyFinder implements OWLAxiomVisitorEx<OWLObject> {
    @SuppressWarnings("UnusedDeclaration")
    private static Logger logger = LoggerFactory.getLogger(PrimarySortKeyFinder.class);
    private static PrimarySortKeyFinder sortKeyFinder = new PrimarySortKeyFinder();

    public static Optional<IRI> getPrimarySortKey(OWLAxiom axiom) {
        OWLObject key = axiom.accept(sortKeyFinder);
        if(key instanceof IRI) {
            return Optional.of((IRI)key);
        }
        if (key instanceof HasIRI) {
            HasIRI hasIRI = (HasIRI) key;
            return Optional.of(hasIRI.getIRI());
        }  else {
            return Optional.absent();
        }
    }

    @Nonnull
    @Override
    public OWLObject visit(@Nonnull OWLDeclarationAxiom owlDeclarationAxiom) {
        return owlDeclarationAxiom.getEntity();
    }

    @Nonnull
    @Override
    public OWLObject visit(@Nonnull OWLDatatypeDefinitionAxiom owlDatatypeDefinitionAxiom) {
        return owlDatatypeDefinitionAxiom.getDatatype();
    }

    @Nonnull
    @Override
    public OWLObject visit(@Nonnull OWLAnnotationAssertionAxiom owlAnnotationAssertionAxiom) {
        return owlAnnotationAssertionAxiom.getSubject();
    }

    @Nonnull
    @Override
    public OWLObject visit(@Nonnull OWLSubAnnotationPropertyOfAxiom owlSubAnnotationPropertyOfAxiom) {
        return owlSubAnnotationPropertyOfAxiom.getSubProperty();
    }

    @Nonnull
    @Override
    public OWLObject visit(@Nonnull OWLAnnotationPropertyDomainAxiom owlAnnotationPropertyDomainAxiom) {
        return owlAnnotationPropertyDomainAxiom.getProperty();
    }

    @Nonnull
    @Override
    public OWLObject visit(@Nonnull OWLAnnotationPropertyRangeAxiom owlAnnotationPropertyRangeAxiom) {
        return owlAnnotationPropertyRangeAxiom.getProperty();
    }

    @Nonnull
    @Override
    public OWLObject visit(@Nonnull OWLSubClassOfAxiom owlSubClassOfAxiom) {
        return owlSubClassOfAxiom.getSubClass();
    }

    @Nonnull
    @Override
    public OWLObject visit(@Nonnull OWLNegativeObjectPropertyAssertionAxiom owlNegativeObjectPropertyAssertionAxiom) {
        return owlNegativeObjectPropertyAssertionAxiom.getSubject();
    }

    @Nonnull
    @Override
    public OWLObject visit(@Nonnull OWLAsymmetricObjectPropertyAxiom owlAsymmetricObjectPropertyAxiom) {
        return owlAsymmetricObjectPropertyAxiom.getProperty().getNamedProperty();
    }

    @Nonnull
    @Override
    public OWLObject visit(@Nonnull OWLReflexiveObjectPropertyAxiom owlReflexiveObjectPropertyAxiom) {
        return owlReflexiveObjectPropertyAxiom.getProperty().getNamedProperty();
    }

    @Nonnull
    @Override
    public OWLObject visit(@Nonnull OWLDisjointClassesAxiom owlDisjointClassesAxiom) {
        for (OWLClassExpression expression : owlDisjointClassesAxiom.getClassExpressions()) {
            if(!expression.isAnonymous()) {
                return expression;
            }
        }
        return owlDisjointClassesAxiom.getClassExpressions().iterator().next();
    }

    @Nonnull
    @Override
    public OWLObject visit(@Nonnull OWLDataPropertyDomainAxiom owlDataPropertyDomainAxiom) {
        return owlDataPropertyDomainAxiom.getProperty();
    }

    @Nonnull
    @Override
    public OWLObject visit(@Nonnull OWLObjectPropertyDomainAxiom owlObjectPropertyDomainAxiom) {
        return owlObjectPropertyDomainAxiom.getProperty();
    }

    @Nonnull
    @Override
    public OWLObject visit(@Nonnull OWLEquivalentObjectPropertiesAxiom owlEquivalentObjectPropertiesAxiom) {
        return  owlEquivalentObjectPropertiesAxiom.getProperties().iterator().next().getNamedProperty();
    }

    @Nonnull
    @Override
    public OWLObject visit(@Nonnull OWLNegativeDataPropertyAssertionAxiom owlNegativeDataPropertyAssertionAxiom) {
        return owlNegativeDataPropertyAssertionAxiom.getProperty();
    }

    @Nonnull
    @Override
    public OWLObject visit(@Nonnull OWLDifferentIndividualsAxiom owlDifferentIndividualsAxiom) {
         return owlDifferentIndividualsAxiom.getIndividuals().iterator().next();
    }

    @Nonnull
    @Override
    public OWLObject visit(@Nonnull OWLDisjointDataPropertiesAxiom owlDisjointDataPropertiesAxiom) {
        return owlDisjointDataPropertiesAxiom.getProperties().iterator().next();
    }

    @Nonnull
    @Override
    public OWLObject visit(@Nonnull OWLDisjointObjectPropertiesAxiom owlDisjointObjectPropertiesAxiom) {
        return owlDisjointObjectPropertiesAxiom.getProperties().iterator().next();
    }

    @Nonnull
    @Override
    public OWLObject visit(@Nonnull OWLObjectPropertyRangeAxiom owlObjectPropertyRangeAxiom) {
        return owlObjectPropertyRangeAxiom.getProperty().getNamedProperty();
    }

    @Nonnull
    @Override
    public OWLObject visit(@Nonnull OWLObjectPropertyAssertionAxiom owlObjectPropertyAssertionAxiom) {
        return owlObjectPropertyAssertionAxiom.getSubject();
    }

    @Nonnull
    @Override
    public OWLObject visit(@Nonnull OWLFunctionalObjectPropertyAxiom owlFunctionalObjectPropertyAxiom) {
        return owlFunctionalObjectPropertyAxiom.getProperty();
    }

    @Nonnull
    @Override
    public OWLObject visit(@Nonnull OWLSubObjectPropertyOfAxiom owlSubObjectPropertyOfAxiom) {
        return owlSubObjectPropertyOfAxiom.getSubProperty().getNamedProperty();
    }

    @Nonnull
    @Override
    public OWLObject visit(@Nonnull OWLDisjointUnionAxiom owlDisjointUnionAxiom) {
        for (OWLClassExpression expression : owlDisjointUnionAxiom.getClassExpressions()) {
            if(!expression.isAnonymous()) {
                return expression;
            }
        }
        return owlDisjointUnionAxiom.getClassExpressions().iterator().next();
    }

    @Nonnull
    @Override
    public OWLObject visit(@Nonnull OWLSymmetricObjectPropertyAxiom owlSymmetricObjectPropertyAxiom) {
        return owlSymmetricObjectPropertyAxiom.getProperty().getNamedProperty();
    }

    @Nonnull
    @Override
    public OWLObject visit(@Nonnull OWLDataPropertyRangeAxiom owlDataPropertyRangeAxiom) {
        return owlDataPropertyRangeAxiom.getProperty();
    }

    @Nonnull
    @Override
    public OWLObject visit(@Nonnull OWLFunctionalDataPropertyAxiom owlFunctionalDataPropertyAxiom) {
        return owlFunctionalDataPropertyAxiom.getProperty();
    }

    @Nonnull
    @Override
    public OWLObject visit(@Nonnull OWLEquivalentDataPropertiesAxiom owlEquivalentDataPropertiesAxiom) {
        return owlEquivalentDataPropertiesAxiom.getProperties().iterator().next();
    }

    @Nonnull
    @Override
    public OWLObject visit(@Nonnull OWLClassAssertionAxiom owlClassAssertionAxiom) {
        return owlClassAssertionAxiom.getIndividual();
    }

    @Nonnull
    @Override
    public OWLObject visit(@Nonnull OWLEquivalentClassesAxiom owlEquivalentClassesAxiom) {
        for (OWLClassExpression expression : owlEquivalentClassesAxiom.getClassExpressions()) {
            if(!expression.isAnonymous()) {
                return expression;
            }
        }
        return owlEquivalentClassesAxiom.getClassExpressions().iterator().next();
    }

    @Nonnull
    @Override
    public OWLObject visit(@Nonnull OWLDataPropertyAssertionAxiom owlDataPropertyAssertionAxiom) {
        return owlDataPropertyAssertionAxiom.getSubject();
    }

    @Nonnull
    @Override
    public OWLObject visit(@Nonnull OWLTransitiveObjectPropertyAxiom owlTransitiveObjectPropertyAxiom) {
        return owlTransitiveObjectPropertyAxiom.getProperty().getNamedProperty();
    }

    @Nonnull
    @Override
    public OWLObject visit(@Nonnull OWLIrreflexiveObjectPropertyAxiom owlIrreflexiveObjectPropertyAxiom) {
        return owlIrreflexiveObjectPropertyAxiom.getProperty().getNamedProperty();
    }

    @Nonnull
    @Override
    public OWLObject visit(@Nonnull OWLSubDataPropertyOfAxiom owlSubDataPropertyOfAxiom) {
        return owlSubDataPropertyOfAxiom.getSubProperty();
    }

    @Nonnull
    @Override
    public OWLObject visit(@Nonnull OWLInverseFunctionalObjectPropertyAxiom owlInverseFunctionalObjectPropertyAxiom) {
        return owlInverseFunctionalObjectPropertyAxiom.getProperty().getNamedProperty();
    }

    @Nonnull
    @Override
    public OWLObject visit(@Nonnull OWLSameIndividualAxiom owlSameIndividualAxiom) {
        return owlSameIndividualAxiom.getIndividuals().iterator().next();
    }

    @Nonnull
    @Override
    public OWLObject visit(@Nonnull OWLSubPropertyChainOfAxiom owlSubPropertyChainOfAxiom) {
        return owlSubPropertyChainOfAxiom.getSuperProperty().getNamedProperty();
    }

    @Nonnull
    @Override
    public OWLObject visit(@Nonnull OWLInverseObjectPropertiesAxiom owlInverseObjectPropertiesAxiom) {
        return owlInverseObjectPropertiesAxiom.getFirstProperty().getNamedProperty();
    }

    @Nonnull
    @Override
    public OWLObject visit(@Nonnull OWLHasKeyAxiom owlHasKeyAxiom) {
        return owlHasKeyAxiom.getClassExpression();
    }

    @Nonnull
    @Override
    public OWLObject visit(@Nonnull SWRLRule swrlRule) {
        return swrlRule;
    }
}
