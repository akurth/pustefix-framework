package de.schlund.pfixxml.util;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import de.schlund.pfixxml.util.Xml;
import junit.framework.TestCase;

public class XmlTest extends TestCase {
    public void testCreateDocument() {
        assertNotNull(Xml.createDocument());
    }

    //-- parse tests 
    
    public void testXmlReaderConfig() throws Exception {
        XMLReader reader;
        
        reader = Xml.createXMLReader();
        assertFalse(reader.getFeature("http://xml.org/sax/features/validation"));
        assertTrue(reader.getFeature("http://xml.org/sax/features/namespaces"));
    }
    
    public void testDocumentBuilderConfig() {
        DocumentBuilder builder;
        
        builder = Xml.createDocumentBuilder();
        assertFalse(builder.isValidating());
        assertTrue(builder.isNamespaceAware());
    }

    public void testParse() throws Exception {
        parse("<ok/>");
        try {
            parse("<wrong>");
            fail();
        } catch (SAXException e) {
            // ok
        }
    }
    public void testParseTailingWhitespaceRemoved() throws Exception {
        Document doc;
        
        doc = parse("<ok/> \n");
        assertEquals(1, doc.getChildNodes().getLength());
    }
    
    public void testComments() throws Exception {
        // make sure to get comments
    	Document doc = parse("<hello><!-- commend --></hello>");
    	Node ele = doc.getDocumentElement();
    	NodeList lst = ele.getChildNodes();
    	assertEquals(1, lst.getLength());
    	assertTrue(lst.item(0) instanceof Comment);
    }

    public void testNamespaceListInTinyTree() throws Exception {
        Transformer t;
        
        // make sure to get comments
    	Document doc = Xml.parseString("<pfx:include xmlns:pfx='foo'><a/></pfx:include>");
    	Element root = doc.getDocumentElement();
    	NamedNodeMap lst = root.getAttributes();
    	assertEquals(0, lst.getLength());
    	t = Xslt.createIdentityTransformer();
    	t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
    	t.setOutputProperty(OutputKeys.INDENT, "no");
    	t.transform(new DOMSource(doc), new StreamResult(System.out));
    }
    
    //-- serialize tests
    
    public void testSerializeSimple() throws Exception {
        assertEquals("<ok/>", serialize("<ok/>", false, false));
    }

    public void testSerializePreserveInnerWhitespace() throws Exception {
        final String STR = "<a>\t<b/>  \n<c/></a>";
        assertEquals(STR, serialize(STR, false, false));
    }
    public void testSerializeRemoveTailingWhitespace() throws Exception {
        assertEquals("<a/>", serialize("<a/>\t \n", false, false));  // the parser removes it!
    }
    public void testSerializeMergeEvenWithPreserve() throws Exception {
        assertEquals("<c/>", serialize("<c></c>", false, false));
    }

    public void testSerializePP() throws Exception {
        assertEquals("<a>\n  <b/>\n  <c/>\n</a>", serialize("<a><b/><c/></a>", true, false));
    }

    public void testSerializePPTailingWhitespaceRemoved() throws Exception {
        assertEquals("<ok/>", serialize("<ok/>\n ", true, false));
    }
    
    public void testSerializeDecl() throws Exception {
        assertEquals("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><a/>", serialize("<a/>", false, true));
    }

    public void testSerializeElement() throws Exception {
        Document doc = parse("<a><b>foo</b></a>");
        assertEquals("<b>foo</b>", Xml.serialize(XPath.selectNode(doc, "/a/b"), true, false));
    }

    public void testSerializeText() throws Exception {
        Document doc = parse("<a>foo</a>");
        assertEquals("foo", Xml.serialize(XPath.selectNode(doc, "/a/text()"), false, false));
    }

    public void testSerializeComment() throws Exception {
        Document doc = parse("<a><!-- hi --></a>");
        assertEquals("<!-- hi -->", Xml.serialize(XPath.selectNode(doc, "/a/comment()"), false, false));
    }

    public void testSerializeAttribute() throws Exception {
        Document doc = parse("<a b='foo'/>");
        try {
            Xml.serialize(XPath.selectNode(doc, "/a/@b"), false, false);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    public void testSerializeExplicitNamespace() throws Exception {
        Document doc = parse(serialize("<ns:ok xmlns:ns='foo'/>", false, false));
        assertEquals("foo", doc.getDocumentElement().getNamespaceURI());
    }
    public void testSerializeImplicitNamespace() throws Exception {
        Document doc = parse(serialize("<ok xmlns='bar'/>", false, false));
        assertEquals("bar", doc.getDocumentElement().getNamespaceURI());
    }
    
    public void testSerializeNoImplicitNamespaceDeclaration() {
        // saxon does not represent ns decls as attributes 
        Document doc = Xml.createDocument();
        doc.appendChild(doc.createElementNS("myuri", "ab:cd"));
        assertEquals("<ab:cd xmlns:ab=\"myuri\"/>", Xml.serialize(doc, true, false));
    }

    //--
    
    public void testStrip() {
        assertEquals("foo", Xml.stripElement("<a>foo</a>"));
        assertEquals("foo", Xml.stripElement("<b bar='xy'>foo</b>"));
        assertEquals("", Xml.stripElement("<c/>"));
    }
    
    //-- helper code
    
    private static String serialize(String doc, boolean pp, boolean decl) throws Exception {
        return Xml.serialize(parse(doc), pp, decl);
    }
    
    private static Document parse(String str) throws Exception {
        return Xml.parseStringMutable(str);
    }
}
