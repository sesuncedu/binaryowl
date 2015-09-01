package org.semanticweb.binaryowl.lookup;/**
 * Created by ses on 8/18/15.
 */

import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.TLinkedHashSet;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

public class RightToLeftEntityOrderer {
    @SuppressWarnings("UnusedDeclaration")
    private static Logger logger = LoggerFactory.getLogger(RightToLeftEntityOrderer.class);
    private final OWLOntology ontology;

    public static class Node {
        OWLEntity entity;

        public OWLEntity getEntity() {
            return entity;
        }

        public String getLabel() {
            return label;
        }

        public Set<Node> getRhsNodes() {
            return rhsNodes;
        }

        public Set<Node> getLhsNodes() {
            return lhsNodes;
        }

        public LinkedList<Node> getPath() {
            return path;
        }

        LinkedList<Node> path = new LinkedList<>();

        String label;
        Set<Node> rhsNodes = new TLinkedHashSet<>();
        Set<Node> lhsNodes = new TLinkedHashSet<>();

        public Node(OWLEntity entity) {
            this.entity = entity;
        }

        @Override
        public String toString() {
            return "'" + label + "'";
        }
    }

    Map<OWLEntity, Node> nodeMap = new THashMap<>();

    @Nonnull
    Node getNode(OWLEntity entity) {
        Node result = nodeMap.get(entity);
        if (result == null) {
            result = new Node(entity);
            nodeMap.put(entity, result);
        }
        return result;
    }

    public RightToLeftEntityOrderer(OWLOntology ontology) {
        this.ontology = ontology;

        for (OWLSubClassOfAxiom subClassOfAxiom : ontology.getAxioms(AxiomType.SUBCLASS_OF)) {
            addLeftRights(subClassOfAxiom.getSubClass(), subClassOfAxiom.getSuperClass());
        }

        for (OWLEquivalentClassesAxiom equivalentClassesAxiom : ontology.getAxioms(AxiomType.EQUIVALENT_CLASSES)) {
            Set<OWLClassExpression> unamedExpressions = new TLinkedHashSet<>(equivalentClassesAxiom.getClassExpressions());
            Set<OWLClass> namedClasses = equivalentClassesAxiom.getNamedClasses();

            unamedExpressions.removeAll(namedClasses);

            for (OWLClass namedClass : namedClasses) {

                for (OWLClassExpression unamedExpression : unamedExpressions) {

                    addLeftRights(namedClass, unamedExpression);
                }
            }
        }

        for (OWLEntity entity : ontology.getSignature()) {
            Node node = getNode(entity);
            for (OWLAnnotationAssertionAxiom axiom : ontology.getAnnotationAssertionAxioms(node.entity.getIRI())) {
                if (axiom.getProperty().isLabel()) {
                    node.label = axiom.getValue().asLiteral().get().getLiteral();
                }
            }
        }

    }

    private void addLeftRights(OWLClassExpression lhs, OWLClassExpression rhs) {
        Set<OWLClass> lhsSignature = lhs.getClassesInSignature();
        Set<OWLClass> rhsSignature = rhs.getClassesInSignature();
        for (OWLEntity rhsEntity : rhsSignature) {
            for (OWLEntity lhsEntity : lhsSignature) {
                if (lhsEntity.getIRI().getFragment().contains("GO_0008152")) {
                    logger.trace("should be a root");
                }
                if (lhsEntity != rhsEntity) {
                    Node rhsNode = getNode(rhsEntity);
                    Node lhsNode = getNode(lhsEntity);
                    lhsNode.rhsNodes.add(rhsNode);
                    rhsNode.lhsNodes.add(lhsNode);
                }
            }
        }
    }

    public Set<Node> getRoots() {
        Set<Node> roots = new TLinkedHashSet<>();
        for (Node node : nodeMap.values()) {
            if (node.rhsNodes.isEmpty()) {
                roots.add(node);
            }
        }
        return roots;
    }

    public Set<Node> getNodes() {
        Set<Node> result = new TLinkedHashSet<>();
        Set<Node> todo = new TLinkedHashSet<>(nodeMap.values());
        for (Node node : getRoots()) {
            addNode(result, node, todo);
        }
        if (!todo.isEmpty()) {
            logger.info("non empty todo");
            result.addAll(todo);
        }
        return result;
    }


    private void addNode(Set<Node> result, Node node, Set<Node> todo) {
        if (todo.remove(node)) {
            result.add(node);
     /*for (Node rhsNode : node.rhsNodes) {
                if(result.add(rhsNode)) {
                    rhsNode.getPath().add(node);
                    rhsNode.getPath().addAll(node.getPath());
                }
            }  */
            result.addAll(node.lhsNodes);
            for (Node lhsNode : node.lhsNodes) {
                if (todo.contains(lhsNode)) {
                    lhsNode.getPath().add(node);
                    lhsNode.getPath().addAll(node.getPath());
                    addNode(result, lhsNode, todo);
                }
            }

        }

    }
}

