/*
 * Created on 13.10.2003
 *  
 */
package de.schlund.pfixxml.targets.cachestat;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Timer;

import org.apache.log4j.Category;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import de.schlund.pfixxml.XMLException;
import de.schlund.pfixxml.targets.SPCache;
import de.schlund.pfixxml.targets.SPCacheFactory;
import de.schlund.pfixxml.targets.SharedLeaf;
import de.schlund.pfixxml.targets.Target;
import de.schlund.pfixxml.targets.TargetGenerator;
import de.schlund.pfixxml.util.XPath;
import de.schlund.pfixxml.util.Xml;
import de.schlund.util.FactoryInit;

/**
 * Class managing information on the hits
 * and misses in the SPCache. Currently it 
 * is used by TargetImpl to register
 * cache hits and misses.
 * @author Joerg Haecker <haecker@schlund.de>
 *  
 */
public class SPCacheStatistic implements FactoryInit {
    
    private static SPCacheStatistic theInstance = new SPCacheStatistic();
    private static int REGISTER_MISS = 0;
    private static int REGISTER_HIT = 1;
    private static String PROP_PRODUCTDATA = "editorproductfactory.productdata";
    private static String PROP_QUEUESIZE =  "cachestatistic.queuesize";
    private static String PROP_QUEUETICKS = "cachestatistic.queueticks";
    private static Category CAT = Category.getInstance(SPCacheStatistic.class.getName());
    private int queueSize = 0;
    private int queueTicks = 0;
    
    /** Maps TargetGenerators to AdvanceCacheStatistic */
    private Hashtable targetGen2AdvanceStatMapping;
    /** Maps config files (depend.xml.in) for  TargetGenerators to product names */ 
    private HashMap dependXMLToProductnameMapping;
    /** Format for hitrate */
    private DecimalFormat hitrateFormat = new DecimalFormat("##0.00");
    /** Timer used for AdvanceCacheStatistic */
    private Timer tickTimer = new Timer(true);

    public static void reset() {
        theInstance = new SPCacheStatistic();
    }

    
     /**
     * Retrieve information which maps the config file
     * of an TargetGenerator to a product name and get the
     * needed properties supplied by FactoryInit.
     * @throws XMLException if properties are not sensible.
     * @see de.schlund.util.FactoryInit#init(java.util.Properties)
     */
    public void init(Properties props) throws Exception {
        String queuesize = props.getProperty(PROP_QUEUESIZE);
        if(queuesize == null || queuesize.equals("")) {
            throw new XMLException("Need property '" + PROP_QUEUESIZE+ "'.");
        }
        try {
            queueSize = Integer.parseInt(queuesize);
        } catch(NumberFormatException e) {
            throw new XMLException("Property '"+PROP_QUEUESIZE+"' is not a number but: "+queuesize);
        }
        if(CAT.isDebugEnabled()) CAT.debug("Got property '"+PROP_QUEUESIZE+"' ="+queueSize);
        
        
        String queueticks = props.getProperty(PROP_QUEUETICKS);
        if(queueticks == null || queueticks.equals("")) {
            throw new XMLException("Need property '" + PROP_QUEUETICKS+ "'.");
        }
        try {
            queueTicks = Integer.parseInt(queueticks);
        } catch(NumberFormatException e) {
            throw new XMLException("Property '"+PROP_QUEUETICKS+"' is not a number but: "+queueticks);
        }
        if(CAT.isDebugEnabled()) CAT.debug("Got property '"+PROP_QUEUETICKS+"' ="+queueTicks);
        
        String productdatafile = props.getProperty(PROP_PRODUCTDATA);
        if (productdatafile == null || productdatafile.equals("")) {
            throw new XMLException("Need property '" + PROP_PRODUCTDATA + "' for retrieving product data.");
        }
        Document doc = Xml.parse(new File(productdatafile));
        List nl = XPath.select(doc, "/projects/project");
        for (int i = 0; i < nl.size(); i++) {
            Node project_node = (Node) nl.get(i);
            String prjname = ((Element) project_node).getAttribute("name");
            Node dependxml_node = XPath.selectNode(project_node, "./depend");
            String dependname = ((Text) ((Element) dependxml_node).getFirstChild()).getData();
            if(CAT.isDebugEnabled()) CAT.debug("Init: putting "+dependname+" = "+prjname);
            dependXMLToProductnameMapping.put(dependname, prjname);
        }
    }

    
    
    
    /**
     * Private constructor of a singleton.
     */
    private SPCacheStatistic() {
        targetGen2AdvanceStatMapping = new Hashtable();
        dependXMLToProductnameMapping = new HashMap();
    }

    /**
     * Get the one and only instance.
     */
    public static SPCacheStatistic getInstance() {
        return theInstance;
    }
    
   
    /**
     * This is called from outside (TargetImpl) to
     * register a cache miss.
     */
    public void registerCacheMiss(Target target) {
        registerForTarget(target, REGISTER_MISS);
    }

    /**
     * This is called from outside (TargetImpl) to
     * register a cache hit.
     */
    public void registerCacheHit(Target target) {
        registerForTarget(target, REGISTER_HIT);
    }

    /**
	 * Create cache-statistic in XML-format.
	 */
    public Document getCacheStatisticAsXML() {
        
        // do clone or synchronize? We need a stable iterator.
        Hashtable targetgentoinfomap_clone =  (Hashtable) targetGen2AdvanceStatMapping.clone();

        Document doc = Xml.createDocument();
        Element top = doc.createElement("spcachestatistic");

        SPCache currentcache = SPCacheFactory.getInstance().getCache();

        Element ele_currentcache = doc.createElement("currentcache");
        setCacheAttributesXML(currentcache, ele_currentcache);
        top.appendChild(ele_currentcache);

        // Get information on the entries in the cache.
        TargetsInSPCache targetsincache = new TargetsInSPCache();
        targetsincache.inspectCache();

        Element ele_hitmiss = doc.createElement("products");
        attachTargetGenerators2XML(doc, targetsincache, ele_hitmiss, targetgentoinfomap_clone);
        attachShared2XML(doc, targetsincache, ele_hitmiss);
        top.appendChild(ele_hitmiss);

        doc.appendChild(top);

        return doc;
    }
    
    /**
     * Create cache-statistic in special format.
     */
    public String getCacheStatisticAsString() {
        StringBuffer sb = new StringBuffer(128);
        SPCache currentcache = SPCacheFactory.getInstance().getCache();

        // do clone or synchronize 
        Hashtable targetgentoinfomap_clone = (Hashtable) targetGen2AdvanceStatMapping.clone();

        long totalmisses = 0;
        long totalhits = 0;
        for (Iterator i = targetgentoinfomap_clone.keySet().iterator(); i.hasNext();) {
            TargetGenerator tgen = (TargetGenerator) i.next();
            AdvanceCacheStatistic stat = (AdvanceCacheStatistic) targetgentoinfomap_clone.get(tgen);
            String productname = getProductnameForTargetGenerator(tgen);
            long hits = stat.getHits();
            long misses = stat.getMisses();
            String hitrate = formatHitrate(hits, misses);
            sb.append("|" + productname + ":" + hits + "," + misses + "," + hitrate);
            totalmisses += misses;
            totalhits += hits;
        }

        String hitrate = formatHitrate(totalhits, totalmisses);
        sb.insert(0, "TOTAL:" + totalhits + "," + totalmisses + "," + hitrate);

        return sb.toString();
    }

    /** 
     * Register a cache-hit or cache-miss for a given target.
     * For the TargetGenerator of the given target a AdvanceCacheStatistic
     * will be created (if not already exists) and will be stored in
     * the targetGen2AdvanceStatMapping map. Each hit or miss
     * for a target will be handled by the belonging AdvanceCacheStatistic.
     **/
    private void registerForTarget(Target target, int mode) {
        TargetGenerator tgen = target.getTargetGenerator();
        if (targetGen2AdvanceStatMapping.containsKey(tgen)) {
            AdvanceCacheStatistic stat = (AdvanceCacheStatistic) targetGen2AdvanceStatMapping.get(tgen);
            if(CAT.isDebugEnabled()) CAT.debug("Found: "+stat.hashCode()+" for target: "+target);
            if (mode == REGISTER_HIT) {
                stat.registerHit();
            } else {
                stat.registerMiss();
            }
        } else {
            AdvanceCacheStatistic stat = new AdvanceCacheStatistic(tickTimer, queueSize, queueTicks);
            if(CAT.isDebugEnabled())   CAT.debug("New: "+stat.hashCode()+" for target: "+target);
            if (mode == REGISTER_HIT) {
                stat.registerHit();
            } else {
                stat.registerMiss();
            }
            targetGen2AdvanceStatMapping.put(tgen, stat);
        }
    }

    /**
     *  Attach the cachestatistic for all known targetgenerators.
     **/
    private void attachTargetGenerators2XML(Document doc, TargetsInSPCache targetsincache, Element ele_hitmiss, Hashtable targetgentoinfomap) {

        for (Iterator i = targetgentoinfomap.keySet().iterator(); i.hasNext();) {
            TargetGenerator tgen = (TargetGenerator) i.next();
            Element ele_tg = doc.createElement("product");
            String configattr = getProductnameForTargetGenerator(tgen);

            ele_tg.setAttribute("name", configattr);
            AdvanceCacheStatistic stat = (AdvanceCacheStatistic) targetgentoinfomap.get(tgen);
            long hits = stat.getHits();
            long misses = stat.getMisses();
            String hitrate = formatHitrate(hits, misses) + "%";
            ele_tg.setAttribute("hitrate", hitrate);
            ele_tg.setAttribute("hits", "" + hits);
            ele_tg.setAttribute("misses", "" + misses);

            attachTargets2XML(doc, targetsincache, tgen, ele_tg);
            ele_hitmiss.appendChild(ele_tg);
        }
    }

    /**
     *  Attach all targets belonging to a given TargetGenerator.
     **/
    private void attachTargets2XML(Document doc, TargetsInSPCache targetsincache, TargetGenerator tgen, Element ele_tg) {
        if (targetsincache.containsTargetGenerator(tgen)) {
            List targets = targetsincache.getTargetsForTargetGenerator(tgen);
            for (Iterator j = targets.iterator(); j.hasNext();) {
                Element entry_ele = doc.createElement("target");
                Target t = (Target) j.next();
                entry_ele.setAttribute("id", t.getTargetKey());
                ele_tg.appendChild(entry_ele);
            }
        }
    }

    /**
     *  Attach the SharedLeafs-targets to the cachestatistic
     */
    private void attachShared2XML(Document doc, TargetsInSPCache targetsincache, Element ele_hitmiss) {
        if (!targetsincache.getSharedTargets().isEmpty()) {
            Element ele_shared = doc.createElement("shared");
            for (Iterator i = targetsincache.getSharedTargets().iterator(); i.hasNext();) {
                Element entry_ele = doc.createElement("sharedtarget");
                SharedLeaf sleaf = (SharedLeaf) i.next();
                entry_ele.setAttribute("id", sleaf.getPath());
                ele_shared.appendChild(entry_ele);
            }
            ele_hitmiss.appendChild(ele_shared);
        }
    }

    /**
     * Attach general information abou the cache and create the total rates
     * by iterating over all known targetGenerators in the targetGen2AdvanceStatMapping-map.
     */
    private void setCacheAttributesXML(SPCache currentcache, Element ele_currentcache) {
        ele_currentcache.setAttribute("class", currentcache.getClass().getName());
        ele_currentcache.setAttribute("capacity", "" + currentcache.getCapacity());
        ele_currentcache.setAttribute("size", "" + currentcache.getSize());

        long totalhits = 0;
        long totalmisses = 0;

        for (Iterator i = targetGen2AdvanceStatMapping.keySet().iterator(); i.hasNext();) {
            TargetGenerator tgen = (TargetGenerator) i.next();
            AdvanceCacheStatistic stat = (AdvanceCacheStatistic) targetGen2AdvanceStatMapping.get(tgen);

            String productname = getProductnameForTargetGenerator(tgen);
            long hits = stat.getHits();
            long misses = stat.getMisses();
            totalmisses += misses;
            totalhits += hits;
        }

        ele_currentcache.setAttribute("hits", "" + totalhits);
        ele_currentcache.setAttribute("misses", "" + totalmisses);
        String hitrate = formatHitrate(totalhits, totalmisses) + "%";
        ele_currentcache.setAttribute("hitrate", hitrate);
    }

    /**
     * Get the product-name for a given TargetGenerator by
     * inspecting the dependXMLToProductnameMapping.
     */
    private String getProductnameForTargetGenerator(TargetGenerator tgen) {
        String dependxml = tgen.getConfigname();
        if (dependXMLToProductnameMapping.containsKey(dependxml)) {
            return (String) dependXMLToProductnameMapping.get(dependxml);
        } else {
            if(CAT.isDebugEnabled()) CAT.debug("No productname found !"+dependxml);
            return dependxml;
        }
    }

    private String formatHitrate(double hits, double misses) {
        double rate = calcHitrate(hits, misses);
        return hitrateFormat.format(rate);
    }

    private double calcHitrate(double hits, double misses) {
        double rate = 0;
        if (hits != 0 && (hits + misses != 0)) {
            rate = (hits / (misses + hits)) * 100;
            if (rate > 100) {
                rate = 100;
            }
        }
        return rate;
    }
}




/**
 * Encapsulates information on targets in the cache.
 */
final class TargetsInSPCache {
    /**
     *  Maps tgen as key to List with targets as values 
     **/
    private HashMap targettgenMapping = new HashMap();
    /**
     * Includes all SharedLeafs in SPCache 
     **/
    private ArrayList sharedTargets = new ArrayList();

    /**
     * Trigger collection of cache information.
     */
    void inspectCache() {
        getTargetsForTargetGeneratorFromSPCache();
    }

    /**
     * Get all SharedLeaf-targets in the cache.
     */
    List getSharedTargets() {
        return sharedTargets;
    }

    /**
     * Get all targets for a given TargetGenerator.
     */
    List getTargetsForTargetGenerator(TargetGenerator tgen) {
        return (List) targettgenMapping.get(tgen);
    }

    boolean containsTargetGenerator(TargetGenerator tgen) {
        return targettgenMapping.containsKey(tgen);
    }

    private void addSharedTarget(SharedLeaf leaf) {
        sharedTargets.add(leaf);
    }

    private void setTargetsForTargetGenerator(TargetGenerator tgen, List targets) {
        targettgenMapping.put(tgen, targets);
    }

    /**
	 * Inspect SPCache and collect all Targets belonging to a TargetGenerator
	 * and all Shared Leafs
	 */
    private void getTargetsForTargetGeneratorFromSPCache() {
        SPCache cache = SPCacheFactory.getInstance().getCache();

        for (Iterator i = cache.getIterator(); i.hasNext();) {
            Object obj = i.next();
            if (obj instanceof Target) {
                Target target = (Target) obj;
                TargetGenerator tgen = target.getTargetGenerator();
                if (containsTargetGenerator(tgen)) {
                    List list = getTargetsForTargetGenerator(tgen);
                    list.add(target);
                } else {
                    ArrayList list = new ArrayList();
                    list.add(target);
                    setTargetsForTargetGenerator(tgen, list);
                }
            } else if (obj instanceof SharedLeaf) {
                addSharedTarget((SharedLeaf) obj);
            }
        }
    }
}
