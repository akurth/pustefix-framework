package de.schlund.pfixxml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.pustefixframework.util.LocaleUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Tenant {

    private String name;
    private String defaultLanguage;
    private boolean useLangPrefix = true;
    private List<String> supportedLanguages = new ArrayList<String>();
    private Map<String, String> languageCodeToLanguage = new HashMap<String, String>();
    private Pattern hostPattern;
    
    public Tenant(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDefaultLanguage() {
        if(defaultLanguage == null && supportedLanguages.size() > 0) {
            return supportedLanguages.get(0);
        }
        return defaultLanguage;
    }
    
    public void setDefaultLanguage(String language) {
        this.defaultLanguage = language;
    }

    public boolean useLangPrefix() {
        return useLangPrefix;
    }

    public void setUseLangPrefix(boolean useLangPrefix) {
        this.useLangPrefix = useLangPrefix;
    }

    public List<String> getSupportedLanguages() {
        return supportedLanguages;
    }
    
    public String getSupportedLanguageByCode(String languageCode) {
        return languageCodeToLanguage.get(languageCode);
    }
    
    public void addSupportedLanguage(String language) {
        supportedLanguages.add(language);
        languageCodeToLanguage.put(LocaleUtils.getLanguagePart(language), language);
    }
    
    public void setHostPattern(String hostPattern) {
        this.hostPattern = Pattern.compile(hostPattern);
    }

    public boolean matches(HttpServletRequest req) {
        if(hostPattern != null) {
            String host = req.getServerName();
            if(!hostPattern.matcher(host).matches()) {
                return false;
            }
        }
        return true;
    }
    
    public Element toXML(Element root) {
        Document doc = root.getOwnerDocument();
        Element tenantElem = doc.createElement("tenant");
        tenantElem.setAttribute("name", getName());
        tenantElem.setAttribute("langprefix", String.valueOf(useLangPrefix()));
        for(String lang : supportedLanguages) {
            Element elem = doc.createElement("lang");
            if(lang.equals(getDefaultLanguage())) {
                elem.setAttribute("default", "true");
            }
            elem.setTextContent(lang);
            tenantElem.appendChild(elem);
        }
        root.appendChild(tenantElem);
        return tenantElem;
    }
    
    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Tenant) {
            return ((Tenant)obj).getName().equals(name);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return name.hashCode();
    }
    
}
