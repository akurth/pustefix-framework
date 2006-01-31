package de.schlund.pfixcore.util;



import de.schlund.pfixxml.PathFactory;
import de.schlund.pfixxml.util.Path;
import de.schlund.pfixxml.util.Xml;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.log4j.xml.DOMConfigurator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.util.regex.Matcher;

/**
 * DelLang.java
 *
 * @author <a href="mailto:jtl@schlund.de">Jens Lautenbacher</a>
 * @version 1.0
 */

public class DelLang {
    private static DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
    private String docroot;
    private Pattern pattern = Pattern.compile("^\\s*$");
    int onelangcount = 0;
    int multilangcount = 0;
            
    static {
        dbfac.setNamespaceAware(true);
    }
    
    public DelLang(String docroot) {
        this.docroot = docroot;
    }

    public static void main(String[] args) throws Exception {
        String    docrt = args[0];
        String    files = args[1];
        if (files == null || docrt == null) {
            System.err.println("Usage: java DelLang DOCROOT includefilelist");
            System.exit(0);
        }
        PathFactory.getInstance().init(docrt);
        DelLang dellang   = new DelLang(docrt);
        DOMConfigurator.configure("core/conf/generator_quiet.xml");
        dellang.transform(files);
    }

    public void transform(String files) throws Exception {

        File           list   = new File(files);
        BufferedReader input  = new BufferedReader(new FileReader(files));
        Set<Path>      inames = new TreeSet<Path>();
        String         line;
        
        while ((line = input.readLine()) != null) {
            inames.add(PathFactory.getInstance().createPath(line.substring(2)));
        }
        input.close();
        

        Document doc;
        
        for (Iterator<Path> i = inames.iterator(); i.hasNext();) {
            Path path = i.next();
            
            System.out.print(path.getRelative() + ":");
            doc = Xml.parseMutable(path.resolve());
            handleDoc(doc);
            
            File out = new File (path.resolve().getAbsolutePath() + ".TMPFILE");
            out.getParentFile().mkdirs();
            Xml.serialize(doc, out, false, true);

            out.renameTo(path.resolve());
            System.out.println("");
        }
        System.out.println("Multilang: " + multilangcount + " Only default lang: " + onelangcount);
    }
    
    
    public void handleDoc(Document doc) {
        
        Element  root                = doc.getDocumentElement();
        // System.out.println("NN: " + root.getNodeName());
        NodeList rootchildren        = root.getChildNodes();
        int      localmultilangcount = 0;
        
        for (int i = 0; i < rootchildren.getLength(); i++) {
            Node rootchild = rootchildren.item(i);
            if (rootchild.getNodeType() == Node.ELEMENT_NODE && rootchild.getNodeName().equals("part")) {
                String   partname       = ((Element) rootchild).getAttribute("name");
                NodeList partchildren   = rootchild.getChildNodes();
                for (int j = 0; j < partchildren.getLength(); j++) {
                    Node partchild = partchildren.item(j);
                    if (partchild.getNodeType() == Node.ELEMENT_NODE && partchild.getNodeName().equals("product")) {
                        String   productname     = ((Element) partchild).getAttribute("name");
                        NodeList prodchildren    = partchild.getChildNodes();
                        int      count           = prodchildren.getLength();
                        boolean  multilang       = false;
                        // First of all we check if there is any other lang node than "<lang name="default">".
                        for (int k = 0; k < count; k++) {
                            Node tmp = prodchildren.item(k);
                            if (tmp.getNodeType() == Node.ELEMENT_NODE) {
                                if (tmp.getNodeName().equals("lang")) {
                                    Element tmpelem    = (Element) tmp;
                                    if (!tmpelem.getAttribute("name").equals("default")) {
                                        multilang = true;
                                    }
                                } else {
                                    System.out.println("*** Wrong element " + tmp.getNodeName() +
                                                       " under part/product " + partname + "/" + productname);
                                    System.exit(1);
                                }
                            }
                        }
                        if (multilang) {
                                multilangcount++;
                                localmultilangcount++;
                        } else {
                            onelangcount++;
                        }
                        Element  theme = doc.createElement("theme");
                        theme.setAttribute("name", ((Element) partchild).getAttribute("name"));
                        
                        Element pfxlang = null;
                        if (multilang) {
                            pfxlang = doc.createElement("pfx:langselect");
                            theme.appendChild(pfxlang);
                        }
                        
                        for (int k = 0; k < count; k++) {
                            Node prodchild = prodchildren.item(0); // Stupid stuff...
                            partchild.removeChild(prodchild);
                            if (prodchild.getNodeType() == Node.ELEMENT_NODE && !multilang) {
                                NodeList langchildren = prodchild.getChildNodes();
                                int      langcount    = langchildren.getLength();
                                for (int l = 0; l < langcount ; l++) {
                                    Node langchild = langchildren.item(0);
                                    prodchild.removeChild(langchild);
                                    theme.appendChild(langchild);
                                }
                            } else if (!multilang) {
                                if ((k == 0 || k == (count - 1)) && prodchild.getNodeType() == Node.TEXT_NODE) {
                                    Matcher matcher = pattern.matcher(prodchild.getNodeValue());
                                    if (!matcher.matches()) {
                                        System.out.println("==>" + partname + "/" + productname + ":" + prodchild.getNodeValue());
                                        theme.appendChild(prodchild);
                                    }
                                }
                            } else if (prodchild.getNodeType() == Node.ELEMENT_NODE) {
                                Element langinst = doc.createElement("pfx:lang");
                                langinst.setAttribute("name", ((Element) prodchild).getAttribute("name"));
                                NodeList langchildren = prodchild.getChildNodes();
                                int      langcount    = langchildren.getLength();
                                for (int l = 0; l < langcount ; l++) {
                                    Node langchild = langchildren.item(0); // 
                                    prodchild.removeChild(langchild);
                                    langinst.appendChild(langchild);
                                }
                                pfxlang.appendChild(langinst);
                            } else {
                                pfxlang.appendChild(prodchild);
                            }
                            
                        }
                        rootchild.replaceChild(theme, partchild);
                    }
                }
            }
        }
        System.out.print(" (" + localmultilangcount + ") "); 
    }
    
    
} 
