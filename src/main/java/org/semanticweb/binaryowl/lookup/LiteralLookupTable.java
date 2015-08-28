/*
 * This file is part of the OWL API.
 *
 * The contents of this file are subject to the LGPL License, Version 3.0.
 *
 * Copyright (C) 2011, The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0
 * in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 *
 * Copyright 2011, The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.semanticweb.binaryowl.lookup;

import com.google.common.base.Charsets;
import com.google.common.collect.MinMaxPriorityQueue;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import org.jetbrains.annotations.NotNull;
import org.semanticweb.binaryowl.doc.OWLOntologyDocument;
import org.semanticweb.binaryowl.stream.BinaryOWLOutputStream;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.OWLObjectVisitorAdapter;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.simmetrics.StringMetric;
import org.simmetrics.StringMetrics;
import org.simmetrics.metrics.OverlapCoefficient;
import org.simmetrics.tokenizers.Tokenizers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.manchester.cs.owl.owlapi.OWLDatatypeImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLLiteralImplNoCompression;

import java.io.DataInput;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Author: Matthew Horridge<br>
 * Stanford University<br>
 * Bio-Medical Informatics Research Group<br>
 * Date: 06/04/2012
 */
public class LiteralLookupTable implements Comparator<OWLLiteral> {
   private static Logger logger = LoggerFactory.getLogger(LiteralLookupTable.class);


    private static final byte INTERNING_NOT_USED_MARKER = 0;

    private static final byte INTERNING_USED_MARKER = 1;
    

    public static final String UTF_8 = "UTF-8";
    
    public static final byte NOT_INDEXED_MARKER = -2;

    private static final byte RDF_PLAIN_LITERAL_MARKER = 0;

    private static final byte XSD_STRING_MARKER = 1;

    private static final byte XSD_BOOLEAN_MARKER = 2;

    private static final byte OTHER_DATATYPE_MARKER = 3;

    private static final byte LANG_MARKER = 1;

    private static final byte NO_LANG_MARKER = 0;

    private boolean useInterning = false;

    //private Map<OWLLiteral, Integer> indexMap = new LinkedHashMap<OWLLiteral, Integer>();
    private Map<OWLLiteral, Integer> indexMap;// = new TreeMap<>();

    private List<OWLLiteral> tableList = new ArrayList<OWLLiteral>(0);

    private IRILookupTable iriLookupTable;

    private static final OWLDatatype RDF_PLAIN_LITERAL_DATATYPE = new OWLDatatypeImpl(OWL2Datatype.RDF_PLAIN_LITERAL.getIRI());

    private static final OWLDatatype XSD_STRING_DATATYPE = new OWLDatatypeImpl(OWL2Datatype.XSD_STRING.getIRI());

    private static final OWLDatatype XSD_BOOLEAN_DATATYPE = new OWLDatatypeImpl(OWL2Datatype.XSD_BOOLEAN.getIRI());



    private static final OWLLiteral BOOLEAN_TRUE = new OWLLiteralImplNoCompression("true", null, XSD_BOOLEAN_DATATYPE);

    private static final OWLLiteral BOOLEAN_FALSE = new OWLLiteralImplNoCompression("false", null, XSD_BOOLEAN_DATATYPE);

    public LiteralLookupTable(IRILookupTable iriLookupTable, boolean useInterning) {
        this.iriLookupTable = iriLookupTable;
        this.useInterning = useInterning;
    }

    public LiteralLookupTable(boolean useInterning) {
        this(new IRILookupTable(), useInterning);

    }

    public LiteralLookupTable() {
        this(false);
    }

    public LiteralLookupTable(IRILookupTable iriLookupTable, DataInput dis, OWLDataFactory df, boolean useInterning) throws IOException {
        this(iriLookupTable, useInterning);
        read(dis, df);
    }


    public LiteralLookupTable(OWLOntologyDocument ontology, IRILookupTable lookupTable, boolean useInterning) {
        this.iriLookupTable = lookupTable;
        this.useInterning = useInterning;
        if (useInterning) {
            internLiterals(ontology);
            checkTheSimilarity();
        }

    }

    private static class Sim implements Comparable {
        OWLLiteral literal;
        int similarity;
        int index;

        public Sim(OWLLiteral literal, float similarity, int index) {
            this.literal = literal;
            this.index = index;
            this.similarity = Math.round((1 - similarity) * 1000);
        }

        @Override
        public int compareTo(Object o) {
            assert o instanceof Sim;
            return similarity - ((Sim) o).similarity;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Sim{");
            sb.append(literal.getLiteral());
            sb.append(String.format(" @ %,d", index));
            sb.append(", ").append(String.format("%0,3f", getSim()));
            sb.append('}');
            return sb.toString();
        }

        public double getSim() {
            double sim = ((float) (similarity)) / 1000;
            sim = 1.0 - sim;
            return sim;
        }
    }

    class MetricTask implements Runnable {
        int i;
        OWLLiteral iLiteral;
        private final PrintWriter pr;
        StringMetric stringMetric;
        MinMaxPriorityQueue<Sim> topTen;

        public MetricTask(int i, StringMetric stringMetric, MinMaxPriorityQueue topTen, OWLLiteral iLiteral, PrintWriter pr) {
            this.i = i;
            this.stringMetric = stringMetric;
            this.topTen = topTen;
            this.iLiteral = iLiteral;
            this.pr = pr;
        }

        @Override
        public void run() {
            for (int j = 0; j < tableList.size(); j++) {
                if (i != j) {
                    OWLLiteral jLiteral = tableList.get(j);
                    float sim = stringMetric.compare(iLiteral.getLiteral(), jLiteral.getLiteral());
                    topTen.add(new Sim(jLiteral, sim, j));
                }
            }
            if (true || i % 10 == 0) {
                logger.info("{} - {} ({}) : {}", Thread.currentThread(), i, iLiteral.getLiteral(), topTen);
            }
            synchronized (pr) {
                while (!topTen.isEmpty()) {
                    Sim piglet = topTen.removeFirst();
                    pr.format("%,d: %s - %s (%,d) = %01.3f\n", i, iLiteral.getLiteral(), piglet.literal.getLiteral(), piglet.index, piglet.getSim());
                }
                pr.flush();
            }
        }
    }

    private void checkTheSimilarity() {
        ExecutorService executor = Executors.newFixedThreadPool(4);

        try (PrintWriter pr = new PrintWriter("/tmp/sims.txt")) {

            MinMaxPriorityQueue.Builder<Comparable> comparableBuilder = MinMaxPriorityQueue.maximumSize(10);
            StringMetric stringMetric = getOverlapQgramMetric();
            // stringMetric= new JaroWinkler(0.7f, 0.1f, 4);
            for (int i = 0; i < tableList.size(); i++) {
                OWLLiteral iLiteral = tableList.get(i);
                MetricTask task = new MetricTask(i, stringMetric, comparableBuilder.create(), iLiteral, pr);
                executor.execute(task);
            }
            executor.shutdown();
            while (!executor.isTerminated()) {
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    logger.error("Interrupted sleep", e); //To change body of catch statement use File | Settings | File Templates.
                }
            }
        } catch (FileNotFoundException e) {
            logger.error("Caught Exception", e); //To change body of catch statement use File | Settings | File Templates.
        }

    }

    @NotNull
    private StringMetric getOverlapQgramMetric() {
        return StringMetrics.createForSetMetric(new OverlapCoefficient<String>(),
                Tokenizers.qGram(5));
    }

    public int compare(OWLLiteral o1, OWLLiteral o2) {
        int i1 = getIndex(o1);
        int i2 = getIndex(o2);
        if(i1 < 0 || i2 < 0) {
            return o1.compareTo(o2);
        }  else {
            return i1 - i2;
        }
    }
    private boolean shouldSortLiterals = false;

    private void internLiterals(OWLOntologyDocument ontology) {
        if (shouldSortLiterals) {
            indexMap = new TreeMap<>();
        } else {
            indexMap = new LinkedHashMap<>();
        }
        InternLiteralsVisitor interner = new InternLiteralsVisitor();


        Set<OWLAxiom> axiomSet = ontology.getOntology().getAxioms();
        Comparator<OWLAxiom> axiomComparator = new AxiomSorter(iriLookupTable);

        final Multimap<IRI, OWLAxiom> axiomsByIRI = MultimapBuilder.treeKeys(iriLookupTable).treeSetValues(axiomComparator).build();

        for (Iterator<OWLAxiom> iterator = axiomSet.iterator(); iterator.hasNext(); ) {
            OWLAxiom axiom = iterator.next();
            com.google.common.base.Optional<IRI> key = PrimarySortKeyFinder.getPrimarySortKey(axiom);
            if (key.isPresent()) {
                axiomsByIRI.put(key.get(), axiom);
                iterator.remove();
            }
        }
        ArrayList<OWLAxiom> generalAxioms = new ArrayList<>(axiomSet);
        Collections.sort(generalAxioms, axiomComparator);
        internAxioms(ontology, interner, axiomsByIRI, generalAxioms, true);
        internAxioms(ontology, interner, axiomsByIRI, generalAxioms, false);

        if (shouldSortLiterals) {
            //logger.warn("not renumbering literal ids");
            renumberLiterals();
        }
        deltaHistoryTable = new DeltaHistoryTable(6, indexMap.size(), -2, 16);
    }

    private void internAxioms(OWLOntologyDocument ontology, InternLiteralsVisitor interner, Multimap<IRI, OWLAxiom> axiomsByIRI, ArrayList<OWLAxiom> generalAxioms, boolean doAnnotations) {
        interner.setVisitingAnnotations(doAnnotations);
        ontology.getOntology().accept(interner);
        for (OWLAxiom axiom : axiomsByIRI.values()) {
              axiom.accept(interner);
        }
        for (OWLAxiom axiom : generalAxioms) {
            axiom.accept(interner);
        }
    }

    private void renumberLiterals() {
        int n=0;
        for (Map.Entry<OWLLiteral, Integer> entry : indexMap.entrySet()) {
            entry.setValue(n);
            tableList.set(n, entry.getKey());
            n++;
        }
    }

    private void internLiteral(OWLLiteral value) {
        if (!indexMap.containsKey(value)) {
            int newIndex = indexMap.size();
            indexMap.put(value, newIndex);
            tableList.add(value);
        }
    }


    public OWLLiteral getLiteral(int index) {
        return tableList.get(index);
    }

    public int getIndex(OWLLiteral literal) {
        if(!useInterning) {
            return -1;
        }
        Integer i = indexMap.get(literal);
        if (i != null) {
            return i;
        }
        else {
            return -1;
        }
    }

    public void write(BinaryOWLOutputStream os) throws IOException {
        if(useInterning) {
            os.writeByte(INTERNING_USED_MARKER);
            os.writeInt(indexMap.size());
            logger.info("datatype table start at {}",os.size());

            for (OWLLiteral literal : indexMap.keySet()) {
                iriLookupTable.writeIRI(literal.getDatatype().getIRI(), os);
            }
            logger.info("datatype  table done at {}, langtags start",os.size());
            for (OWLLiteral literal : indexMap.keySet()) {
                if(literal.getDatatype().isRDFPlainLiteral()) {
                    os.writeUTF(literal.getLang());
                }
            }
            logger.info("langtag  table done at {}, strings start",os.size());

          /*  DeltaHistoryTable indexDelta = new DeltaHistoryTable(6,indexMap.size());
            for (int index : indexMap.values()) {
                long codedDelta = indexDelta.getCodedDelta(index);
                int deltaBase = (int) indexDelta.decodeDeltaBase(codedDelta);
                os.writeVarInt(deltaBase);
                int delta = (int) indexDelta.decodeDelta(codedDelta);
                os.writeVarInt(delta);
            }
          */
            PrintWriter pw = new PrintWriter("/tmp/literals.txt");
            for (OWLLiteral literal : indexMap.keySet()) {
                byte[] utf8Bytes = literal.getLiteral().getBytes(Charsets.UTF_8);
                os.write(utf8Bytes);
                os.writeByte('\n');
                //os.writeUTF(literal.getLiteral());
                pw.format("%s\n",literal.getLiteral());
            }
            pw.close();
            logger.info("strings done at {}",os.size());


        }
        else {
            os.writeByte(INTERNING_NOT_USED_MARKER);
        }

    }

    private void read(DataInput is, OWLDataFactory df) throws IOException {
        int interningMarker = is.readByte();
        if(interningMarker == INTERNING_USED_MARKER) {
            useInterning = true;
        }
        else if(interningMarker == INTERNING_NOT_USED_MARKER) {
            useInterning = false;
        }
        else {
            throw new IOException("Unexpected literal interning marker: " + interningMarker);
        }

        if (useInterning) {
            int size = is.readInt();
            tableList = new ArrayList<OWLLiteral>(size + 2);
            for (int i = 0; i < size; i++) {
                OWLLiteral literal = readRawLiteral(is, df);
                tableList.add(literal);
            }
        }
    }

    public OWLLiteral readLiteral(DataInput is, OWLDataFactory df) throws IOException {
        if (useInterning) {
            int index = is.readInt();
            if(index == NOT_INDEXED_MARKER) {
                return readRawLiteral(is, df);
            }
            else {
                return tableList.get(index);
            }
        }
        else {
            return readRawLiteral(is, df);
        }
    }

    private OWLLiteral readRawLiteral(DataInput is, OWLDataFactory df) throws IOException {
        int typeMarker = is.readByte();
        if (typeMarker == RDF_PLAIN_LITERAL_MARKER) {
            int langMarker = is.readByte();
            if (langMarker == LANG_MARKER) {
                String lang = is.readUTF();
                byte[] literalBytes = readBytes(is);
//                return new OWLLiteralImplNoCompression(literalBytes, lang, RDF_PLAIN_LITERAL_DATATYPE);
                return new OWLLiteralImplNoCompression(new String(literalBytes), lang, RDF_PLAIN_LITERAL_DATATYPE);
            }
            else if (langMarker == NO_LANG_MARKER) {
                byte[] literalBytes = readBytes(is);
//                return new OWLLiteralImplNoCompression(literalBytes, null, RDF_PLAIN_LITERAL_DATATYPE);
                return new OWLLiteralImplNoCompression(new String(literalBytes), null, RDF_PLAIN_LITERAL_DATATYPE);
            }
            else {
                throw new IOException("Unknown lang marker: " + langMarker);
            }
        }
        else if(typeMarker == XSD_STRING_MARKER) {
            byte[] literalBytes = readBytes(is);
//            return new OWLLiteralImplNoCompression(literalBytes, null, XSD_STRING_DATATYPE);
            return new OWLLiteralImplNoCompression(new String(literalBytes), null, XSD_STRING_DATATYPE);
        }
        else if(typeMarker == XSD_BOOLEAN_MARKER) {
            if(is.readBoolean()) {
                return BOOLEAN_TRUE;
            }
            else {
                return BOOLEAN_FALSE;
            }
        }
        else if (typeMarker == OTHER_DATATYPE_MARKER) {
            OWLDatatype datatype = iriLookupTable.readDataypeIRI(is);
            byte[] literalBytes = readBytes(is);
            return new OWLLiteralImplNoCompression(new String(literalBytes), null, datatype);
        }
        else {
            throw new RuntimeException("Unknown type marker: " + typeMarker);
        }


    }

    private DeltaHistoryTable deltaHistoryTable;

    private int indexCount = 0;
    private int bcdCount = 0;
    private int scdCount = 0;
    private int icdCount = 0;

    public void logDeltaCounts() {
        logger.info("{}",
                String.format("index count = %,d, bcd = %,d, scd = %,d, icd = %,d",
                        indexCount, bcdCount, scdCount, icdCount));
    }

    public void writeLiteral(BinaryOWLOutputStream dos, OWLLiteral literal) throws IOException {
        if(useInterning) {
            indexCount++;
            int index = getIndex(literal);
            if(index == -1) {
                dos.writeInt(NOT_INDEXED_MARKER);
                writeRawLiteral(dos, literal);
            }
            else {
                long codedDelta = deltaHistoryTable.getCodedDelta(index);
                long delta = deltaHistoryTable.decodeDelta(codedDelta);
                long deltaBaseId = deltaHistoryTable.decodeDeltaBase(codedDelta);
                byte flagByte = (byte)deltaBaseId;
                if (false) {
                    dos.writeVarInt((int)codedDelta);
                    return;
                }
                byte bcd = (byte) (delta);
                if (bcd == delta) {
                    int v = flagByte | 0x00;
                    dos.writeByte(v);
                    dos.writeByte(bcd);
                    bcdCount++;
                    return;
                }

                short scd = (short) delta;
                if (scd == delta) {
                    int v = flagByte | 0x40;
                    dos.writeByte(v);
                    dos.writeShort(scd);
                    scdCount++;
                    return;
                }

                int icd = (int) delta;
                if (icd == delta) {
                    int v = flagByte | 0x80;
                    dos.writeByte(v);
                    dos.writeInt(icd);
                    icdCount++;
                    return;
                }


                dos.writeByte(4);
                dos.writeInt(index);
            }
        }
        else {
            writeRawLiteral(dos, literal);
        }

    }

    private void writeRawLiteral(BinaryOWLOutputStream os, OWLLiteral literal) throws IOException {
        if(literal.getDatatype().equals(XSD_BOOLEAN_DATATYPE)) {
            os.write(XSD_BOOLEAN_MARKER);
            os.writeBoolean(literal.parseBoolean());
            return;
        }
        else if (literal.isRDFPlainLiteral()) {
            os.write(RDF_PLAIN_LITERAL_MARKER);
            if (literal.hasLang()) {
                os.write(LANG_MARKER);
                writeString(literal.getLang(), os);
            }
            else {
                os.write(NO_LANG_MARKER);
            }
        }
        else if(literal.getDatatype().equals(XSD_STRING_DATATYPE)) {
            os.write(XSD_STRING_MARKER);
        }
        else {
            os.write(OTHER_DATATYPE_MARKER);
            iriLookupTable.writeIRI(literal.getDatatype().getIRI(), os);
        }

        byte[] literalBytes;
        if (literal instanceof OWLLiteralImplNoCompression) {
            literalBytes = ((OWLLiteralImplNoCompression) literal).getLiteral().getBytes(UTF_8);
        }
        else {
            literalBytes = literal.getLiteral().getBytes(UTF_8);
        }
        writeBytes(literalBytes, os);



    }

    private void writeString(String s, BinaryOWLOutputStream os) throws IOException {
        os.writeUTF(s);
    }


    private void writeBytes(byte[] bytes, BinaryOWLOutputStream os) throws IOException {
        os.writeShort(bytes.length);
        os.write(bytes);
    }


    private byte[] readBytes(DataInput is) throws IOException {
        int length = is.readShort();
        byte[] bytes = new byte[length];
        is.readFully(bytes);
        return bytes;
    }

    public DeltaHistoryTable getDeltaHistoryTable() {
        return deltaHistoryTable;
    }

    private class InternLiteralsVisitor extends OWLObjectVisitorAdapter {
        boolean visitingAnnotations;

        public boolean isVisitingAnnotations() {
            return visitingAnnotations;
        }

        @Override
        protected void handleDefault(OWLObject object) {
            if (isVisitingAnnotations()) {
                handleAnnotations(object);
            }

            if (object instanceof HasFiller) {
                HasFiller hasFiller = (HasFiller) object;
                hasFiller.getFiller().accept(this);
            }
        }

        public void handleAnnotations(OWLObject object) {
            if (object instanceof HasAnnotations) {
                Collection<OWLAnnotation> annotations = ((HasAnnotations) object).getAnnotations();
                if (annotations.size() > 0) {
                    List<OWLAnnotation> list = new ArrayList<>(annotations);
                    Collections.sort(list, new OWLAnnotationComparator());
                    for (OWLAnnotation annotation : annotations) {
                        annotation.accept(this);
                    }
                }
            }

        }

        @Override
        public void visit(OWLAnnotation node) {
            if (isVisitingAnnotations()) {
                handleDefault(node);
                if (node.getValue() instanceof OWLLiteral) {
                    OWLLiteral value = (OWLLiteral) node.getValue();
                    internLiteral(value);
                }
            }
        }

        @Override
        public void visit(OWLLiteral node) {
            if (!isVisitingAnnotations()) {
                internLiteral(node);
            }
        }

        @Override
        public void visit(OWLObjectIntersectionOf ce) {
            handleDefault(ce);
            for (OWLClassExpression expression : ce.getOperands()) {
                expression.accept(this);
            }
        }

        @Override
        public void visit(OWLObjectUnionOf ce) {
            handleDefault(ce);
            for (OWLClassExpression expression : ce.getOperands()) {
                expression.accept(this);
            }
        }

        @Override
        public void visit(OWLFacetRestriction node) {
            node.getFacetValue().accept(this);
        }


        @Override
        public void visit(OWLDataComplementOf node) {
            node.getDataRange().accept(this);
        }

        @Override
        public void visit(OWLDataIntersectionOf node) {
            for (OWLDataRange dataRange : node.getOperands()) {
                dataRange.accept(this);
            }
        }

        @Override
        public void visit(OWLDataOneOf node) {
            for (OWLLiteral owlLiteral : node.getValues()) {
                owlLiteral.accept(this);
            }
        }


        @Override
        public void visit(OWLDatatypeRestriction node) {
            for (OWLFacetRestriction facetRestriction : node.getFacetRestrictions()) {
                facetRestriction.accept(this);
            }
        }

        @Override
        public void visit(OWLDataUnionOf node) {
            for (OWLDataRange dataRange : node.getOperands()) {
                dataRange.accept(this);
            }

        }


        @Override
        public void visit(OWLAnnotationAssertionAxiom axiom) {
            handleDefault(axiom);
            axiom.getValue().accept(this);
        }


        @Override
        public void visit(SWRLLiteralArgument node) {
            handleDefault(node);
            node.getLiteral().accept(this);
        }


        @Override
        public void visit(SWRLDataRangeAtom node) {
            handleDefault(node);
            node.getPredicate().accept(this);
        }


        @Override
        public void visit(OWLDatatypeDefinitionAxiom axiom) {
            handleDefault(axiom);
            axiom.getDataRange().accept(this);
        }


        @Override
        public void visit(OWLClassAssertionAxiom axiom) {
            handleDefault(axiom);
            axiom.getClassExpression().accept(this);
        }

        @Override
        public void visit(OWLDataPropertyAssertionAxiom axiom) {
            handleDefault(axiom);
            axiom.getObject().accept(this);

        }

        @Override
        public void visit(OWLDataPropertyDomainAxiom axiom) {
            handleDefault(axiom);
            axiom.getDomain().accept(this);
        }

        @Override
        public void visit(OWLDataPropertyRangeAxiom axiom) {
            handleDefault(axiom);
            axiom.getRange().accept(this);
        }

        @Override
        public void visit(OWLHasKeyAxiom axiom) {
            handleDefault(axiom);
            axiom.getClassExpression().accept(this);
        }

        @Override
        public void visit(OWLNegativeDataPropertyAssertionAxiom axiom) {
            handleDefault(axiom);
            axiom.getObject().accept(this);
        }

        @Override
        public void visit(OWLObjectPropertyDomainAxiom axiom) {
            handleDefault(axiom);
            axiom.getDomain().accept(this);
        }

        @Override
        public void visit(OWLObjectPropertyRangeAxiom axiom) {
            handleDefault(axiom);
            axiom.getRange().accept(this);
        }

        public void setVisitingAnnotations(boolean vistingAnnotations) {
            this.visitingAnnotations = vistingAnnotations;
        }

        private class OWLAnnotationComparator implements Comparator<OWLAnnotation> {
            @Override
            public int compare(OWLAnnotation o1, OWLAnnotation o2) {
                int cmp;
                cmp = iriLookupTable.compare(o1.getProperty().getIRI(), o2.getProperty().getIRI());
                if (cmp != 0) {
                    return cmp;
                }
                cmp = o1.getValue().compareTo(o2.getValue());
                if (cmp != 0) {
                    return cmp;
                }
                Iterator<OWLAnnotation> i1 = o1.getAnnotations().iterator();
                Iterator<OWLAnnotation> i2 = o2.getAnnotations().iterator();
                while (i1.hasNext() && i2.hasNext()) {
                    cmp = compare(i1.next(), i2.next());
                    if (cmp != 0) {
                        return cmp;
                    }
                }
                if (i1.hasNext()) {
                    return +1;
                }
                if (i2.hasNext()) {
                    return -1;
                }
                return 0;

            }

        }
    }
}
