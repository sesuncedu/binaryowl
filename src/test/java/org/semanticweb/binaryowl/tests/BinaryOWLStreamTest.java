package org.semanticweb.binaryowl.tests;/**
 * Created by ses on 8/12/15.
 */

import org.junit.Test;
import org.semanticweb.binaryowl.BinaryOWLVersion;
import org.semanticweb.binaryowl.stream.BinaryOWLInputStream;
import org.semanticweb.binaryowl.stream.BinaryOWLOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class BinaryOWLStreamTest {
    @SuppressWarnings("UnusedDeclaration")
    private static Logger logger = LoggerFactory.getLogger(BinaryOWLStreamTest.class);

    @Test
    public void testWriteUnsignedInt() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        BinaryOWLOutputStream bowls = new BinaryOWLOutputStream(bos, BinaryOWLVersion.getVersion(1));

        bowls.writeUnsignedInt(0);
        bowls.writeUnsignedInt(126);
        bowls.writeUnsignedInt(127);
        bowls.writeUnsignedInt(128);
        bowls.writeUnsignedInt(1024);
        bowls.writeUnsignedInt(0xff  * 1024);
        bowls.writeUnsignedInt(0xff* 1024*1024);
        bowls.writeUnsignedInt(Integer.MAX_VALUE);

        byte[] buf = bos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(buf);
        BinaryOWLInputStream bois = new BinaryOWLInputStream(bais,new OWLDataFactoryImpl(),BinaryOWLVersion.getVersion(1 ));

        assertEquals(0,bois.readUnsignedInt());
        assertEquals(126,bois.readUnsignedInt());
        assertEquals(127,bois.readUnsignedInt());
        assertEquals(128,bois.readUnsignedInt());
        assertEquals(1024,bois.readUnsignedInt());
        assertEquals(0xff * 1024,bois.readUnsignedInt());
        assertEquals(0xff* 1024*1024,bois.readUnsignedInt());
        assertEquals(Integer.MAX_VALUE,bois.readUnsignedInt());
        try {
            bois.readByte();
            fail("should be at EOF");
        } catch (EOFException e) {

        }
    }
}
