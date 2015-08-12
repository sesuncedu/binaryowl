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
import org.semanticweb.binaryowl.doc.OWLOntologyDocument;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.OWLObjectVisitorAdapter;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.manchester.cs.owl.owlapi.OWLDatatypeImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLLiteralImplNoCompression;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Author: Matthew Horridge<br>
 * Stanford University<br>
 * Bio-Medical Informatics Research Group<br>
 * Date: 06/04/2012
 */
public class LiteralLookupTable {
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
    private Map<OWLLiteral, Integer> indexMap = new TreeMap<>();

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
        }
    }


    private void internLiterals(OWLOntologyDocument ontology) {
        InternLiteralsVisitor interner = new InternLiteralsVisitor();
        for (OWLAnnotation annotation : ontology.getAnnotations()) {
            annotation.accept(interner);
        }
        for (AxiomType<?> axiomType : AxiomType.AXIOM_TYPES) {
            Set<? extends OWLAxiom> axioms = ontology.getAxioms(axiomType);
            ArrayList<? extends OWLAxiom> orderedAxioms = new ArrayList<>(axioms);
            Collections.sort(orderedAxioms);
            for (OWLAxiom axiom : orderedAxioms) {
                axiom.accept(interner);
            }
        }

        //renumberLiterals();

    }

    private void renumberLiterals() {
        int n=0;
        for (Map.Entry<OWLLiteral, Integer> entry : indexMap.entrySet()) {
            entry.setValue(n++);
        }
    }

    private void internLiteral(OWLLiteral value) {
        int newIndex = indexMap.size();
        Integer prev = indexMap.put(value, newIndex);
        if (prev != null) {
            indexMap.put(value, prev);
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

    public void write(DataOutputStream os) throws IOException {
        if(useInterning) {
            os.writeByte(INTERNING_USED_MARKER);
            os.writeInt(indexMap.size());

            for (OWLLiteral literal : indexMap.keySet()) {
                iriLookupTable.writeIRI(literal.getDatatype().getIRI(), os);
            }
            for (OWLLiteral literal : indexMap.keySet()) {
                if(literal.getDatatype().isRDFPlainLiteral()) {
                    os.writeUTF(literal.getLang());
                }
            }

            for (OWLLiteral literal : indexMap.keySet()) {
                byte[] utf8Bytes = literal.getLiteral().getBytes(Charsets.UTF_8);
                os.write(utf8Bytes);
                os.writeByte(0);
                //os.writeUTF(literal.getLiteral());
            }


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

    private DeltaHistoryTable deltaHistoryTable = new DeltaHistoryTable(4);

    private int indexCount = 0;
    private int bcdCount = 0;
    private int scdCount = 0;
    private int icdCount = 0;

    public void logDeltaCounts() {
        logger.info("{}",
                String.format("index count = %,d, bcd = %,d, scd = %,d, icd = %,d",
                        indexCount, bcdCount, scdCount, icdCount));
    }

    public void writeLiteral(DataOutput os, OWLLiteral literal) throws IOException {
        if(useInterning) {
            indexCount++;
            int index = getIndex(literal);
            if(index == -1) {
                os.writeInt(NOT_INDEXED_MARKER);
                writeRawLiteral(os, literal);
            }
            else {
                long codedDelta = deltaHistoryTable.getCodedDelta(index);
                long delta = deltaHistoryTable.decodeDelta(codedDelta);
                long deltaBaseId = deltaHistoryTable.decodeDeltaBase(codedDelta);
                byte flagByte = (byte)deltaBaseId;

                byte bcd = (byte) (delta);
                if (bcd == delta) {
                    int v = flagByte | 0xc0;
                    os.writeByte(v);
                    os.writeByte(bcd);
                    bcdCount++;
                    return;
                }

                short scd = (short) delta;
                if (scd == delta) {
                    int v = flagByte | 0x80;
                    os.writeByte(v);
                    os.writeShort(scd);
                    scdCount++;
                    return;
                }

                int icd = (int) delta;
                if (icd == delta) {
                    int v = flagByte | 0x40;
                    os.writeByte(v);
                    os.writeInt((int) delta);
                    icdCount++;
                    return;
                }

                os.writeByte(4);
                os.writeInt(index);
            }
        }
        else {
            writeRawLiteral(os, literal);
        }

    }

    private void writeRawLiteral(DataOutput os, OWLLiteral literal) throws IOException {
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

    private void writeString(String s, DataOutput os) throws IOException {
        os.writeUTF(s);
    }


    private void writeBytes(byte[] bytes, DataOutput os) throws IOException {
        os.writeShort(bytes.length);
        os.write(bytes);
    }


    private byte[] readBytes(DataInput is) throws IOException {
        int length = is.readShort();
        byte[] bytes = new byte[length];
        is.readFully(bytes);
        return bytes;
    }

    private class InternLiteralsVisitor extends OWLObjectVisitorAdapter {

        @Override
        protected void handleDefault(OWLObject object) {
            handleAnnotations(object);

            if (object instanceof HasFiller) {
                HasFiller hasFiller = (HasFiller) object;
                hasFiller.getFiller().accept(this);
            }
        }

        private void handleAnnotations(OWLObject object) {
            if (object instanceof HasAnnotations) {
                Collection<OWLAnnotation> annotations = ((HasAnnotations) object).getAnnotations();
                if (annotations != null && annotations.size() > 0) {
                    for (OWLAnnotation annotation : annotations) {
                        annotation.accept(this);
                    }
                }
            }

        }

        @Override
        public void visit(OWLAnnotation node) {
            handleAnnotations(node);
            if (node.getValue() instanceof OWLLiteral) {
                OWLLiteral value = (OWLLiteral) node.getValue();
                value.accept(this);
            }
        }

        @Override
        public void visit(OWLLiteral node) {
            internLiteral(node);
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
    }
}
