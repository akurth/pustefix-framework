/*
 * Created on 07.08.2003
 *
 */
package de.schlund.pfixxml.testenv;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Category;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.icl.saxon.TransformerFactoryImpl;

/**
 * @author Joerg Haecker <haecker@schlund.de>
 *
 */
public class TestcaseStepResult {

    private static String GNU_DIFF = "diff -u";
    private static Category CAT = Category.getInstance(TestcaseStepResult.class.getName());
    private int statusCode;
    private String diffString;
    private Document serverResponse;
    private Document recordedReferenceDoc;
    private boolean serverError = false;

    public void setRecordedReferenceDoc(Document reference_data) {
        if (reference_data == null) {
            throw new IllegalArgumentException("A NP as referencedata is NOT allowed here!");
        }
        recordedReferenceDoc = reference_data;
    }

    public Document getServerResponse() {
        return serverResponse;
    }

    public int getStatuscode() {
        return statusCode;
    }

    public void setStatuscode(int code) {
        statusCode = code;
    }

    public void setServerResponse(Document response) {
        if (response == null) {
            throw new IllegalArgumentException("A NP as response is not allowed here!");
        }
        serverResponse = response;
    }

    public void setReferenceDoc(Document doc) {
    }

    public void createDiff(String tmpdir, int count, String style_dir, String stylesheet) throws Exception {
        if (serverError) {
            diffString = ":-(";
            return;
        }

        doTransform(style_dir, stylesheet);

        String ref_path = tmpdir + "/_recorded" + count;
        String srv_path = tmpdir + "/_current" + count;
        XMLSerializeUtil.getInstance().serializeToFile(serverResponse, srv_path, 2, false);
        XMLSerializeUtil.getInstance().serializeToFile(recordedReferenceDoc, ref_path, 2, false);
        doDiff(srv_path, ref_path);
    }

    public String getDiffString() {
        return diffString;
    }

    /** remove the serial number from the result document */
    private void removeSerialNumbers() {
        Node node1 = recordedReferenceDoc.getFirstChild();
        ((Element) node1).setAttribute("serial", "0");

        Node node2 = serverResponse.getFirstChild();
        ((Element) node2).setAttribute("serial", "0");
    }

    /** start GNU diff process */
    private void doDiff(String path1, String path2) throws Exception {
        String diff = GNU_DIFF + " " + path2 + " " + path1;

        Process process = null;
        try {
            process = Runtime.getRuntime().exec(diff);
        } catch (IOException e) {
            throw new TestClientException("IOException occured!", e);
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String s;
        StringBuffer buf = new StringBuffer();

        while ((s = reader.readLine()) != null) {
            buf.append(s).append("\n");
        }

        reader.close();
        process.waitFor();
        diffString = buf.toString();
    }

    public boolean isServerError() {
        return serverError;
    }

    public void setServerError(boolean error) {
        serverError = error;
    }

    private void doTransform(String style_dir, String stylesheet) throws TestClientException {

        removeSerialNumbers();

        // saxon
        TransformerFactoryImpl trans_fac = (TransformerFactoryImpl) TransformerFactory.newInstance();
        String path = style_dir + "/" + stylesheet;
        File styesheet = new File(path);
        if (styesheet.exists()) {
            if (CAT.isInfoEnabled()) {
                CAT.info("  Transforming using stylesheet :"+path);
            }
            StreamSource stream_source = new StreamSource("file://" + path);
            Templates templates = null;
            try {
                templates = trans_fac.newTemplates(stream_source);
            } catch (TransformerConfigurationException e) {
                throw new TestClientException("TransformerConfigurationException occured!", e);
            }
            Transformer trafo = null;
            try {
                trafo = templates.newTransformer();
            } catch (TransformerConfigurationException e) {
                throw new TestClientException("TransformerConfigurationException occured!", e);
            }

            DOMSource dom_source1 = new DOMSource(serverResponse);
            DOMResult dom_result1 = new DOMResult();
            try {
                trafo.transform(dom_source1, dom_result1);
            } catch (TransformerException e) {
                throw new TestClientException("TransformerException occured!", e);
            }
            serverResponse = (Document) dom_result1.getNode();

            DOMSource dom_source2 = new DOMSource(recordedReferenceDoc);
            DOMResult dom_result2 = new DOMResult();
            try {
                trafo.transform(dom_source2, dom_result2);
            } catch (TransformerException e) {
                throw new TestClientException("TransformerException occured!", e);
            }
            recordedReferenceDoc = (Document) dom_result2.getNode();
        } else {
            CAT.info("Stylesheet "+path+" not found.");
        }
    }

}
