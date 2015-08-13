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
import static org.junit.Assert.assertTrue;
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

    @Test
    public void testWriteInt() throws IOException {
        int batch = 100_000;
        for (int i = -10_000_000; i <= 10_000_000; i += batch) {
            logger.info("i = {}", String.format("%,d", i));
            ByteArrayOutputStream bos = new ByteArrayOutputStream(batch * 6);
            BinaryOWLOutputStream bowls = new BinaryOWLOutputStream(bos, BinaryOWLVersion.getVersion(1));
            double start = System.currentTimeMillis();
            for (int j = i; j < i + batch; j++) {
                bowls.writeVarInt(j);
            }
            double end = System.currentTimeMillis();
            double delta = end - start;
            delta /= 1000;
            byte[] buf = bos.toByteArray();
            logger.info("encoded {} values in {}: buf length is {}", batch, String.format("%,.3fs", delta), String.format("%,d", buf.length));
            ByteArrayInputStream bais = new ByteArrayInputStream(buf);
            BinaryOWLInputStream bois = new BinaryOWLInputStream(bais, new OWLDataFactoryImpl(), BinaryOWLVersion.getVersion(1));
            logger.info("start decode");
            start = System.currentTimeMillis();
            for (int j = i; j < i + batch; j++) {
                int val = bois.readVarInt();
                assertTrue("value mismatch", j == val);
            }
            end = System.currentTimeMillis();
            delta = end - start;
            delta /= 1000;
            logger.info("decoded {} values in {}",batch, String.format("%,.3fs", delta));
            logger.info("decode speed: {} values per second", String.format("%,.0f", batch / delta));


        }
    }
}
