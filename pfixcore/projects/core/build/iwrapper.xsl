<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:iwrp="http://pustefix.sourceforge.net/interfacewrapper200401"
  >
  <xsl:output method="text" encoding="ISO-8859-1" indent="no"/>

  <xsl:param name="classname"/>
  <xsl:param name="package"/>
  
  <!--
  // TODO 2004-12-06 adam corrected spelling of occurence (was occurance).
  changed schema example/core/schema/interfacewrapper200401.xsd
  to allow both, correct: "occurence" old one with typo "occurence" 
  remove the "occurance" stuff herein, once it can be expected anyone has updated its files
  to the new spelling. Mind to update schema file.
  -->
  <xsl:variable name="ihandlerClassStr" select="/iwrp:interface/iwrp:ihandler/@class" />
  
  <xsl:template match="iwrp:interface" ><xsl:param name="extends">
  <xsl:if test="iwrp:interface/iwrp:param/@occurance">
    <xsl:message>The use of attribute occurance (with 'a')is deprecated, use occurence (with 'e') instead.</xsl:message>
  </xsl:if>
      <xsl:choose>
        <xsl:when test="@extends">
          <xsl:value-of select="@extends"/>
        </xsl:when>
        <xsl:otherwise>IWrapperImpl</xsl:otherwise>
      </xsl:choose>
    </xsl:param>/*
 * This file is part of PFIXCORE.
 *
 * PFIXCORE is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * PFIXCORE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with PFIXCORE; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
    
package <xsl:value-of select="$package"/>;
    
import de.schlund.pfixcore.generator.*;
    
/**
 * THIS CLASS IS AUTOGENERATED! DO NOT EDIT IN ANY WAY!
 */

public <xsl:if test="not(/iwrp:interface/iwrp:ihandler) and not(@extends)">abstract </xsl:if>class <xsl:value-of select="$classname"/> extends <xsl:value-of select="$extends"/> {

    public <xsl:value-of select="$classname"/>() {<xsl:choose>
      <xsl:when test="/iwrp:interface/iwrp:ihandler">
        <xsl:choose>
          <xsl:when test="starts-with($ihandlerClassStr, 'script:')">
        handler = new de.schlund.pfixcore.scripting.ScriptingIHandler("<xsl:value-of select="substring-after($ihandlerClassStr, 'script:')" />"); </xsl:when>
          <xsl:otherwise>
        handler = IHandlerFactory.getInstance().getIHandler("<xsl:value-of select="/iwrp:interface/iwrp:ihandler/@class"/>");</xsl:otherwise>
        </xsl:choose>      
        </xsl:when>
      <xsl:otherwise>
        super();</xsl:otherwise>
    </xsl:choose>
    }
    
    <xsl:call-template name="generatetostring">
      <xsl:with-param name="classname" select="$classname"/> 
    </xsl:call-template>
    
    @Override
    protected synchronized void registerParams() {
        super.registerParams();
        @SuppressWarnings("unused")
        IWrapperParam          pinfo;
        @SuppressWarnings("unused") 
        IWrapperIndexedParam   pindx;
        @SuppressWarnings("unused") 
        IWrapperParamCaster    caster;
        @SuppressWarnings("unused") 
        IWrapperParamPreCheck  pre;
        @SuppressWarnings("unused") 
        IWrapperParamPostCheck post;
    <xsl:for-each select="/iwrp:interface/iwrp:param">
      <xsl:variable name="freqparam">
        <xsl:choose>
          <xsl:when test="@frequency = 'multiple'">true</xsl:when>
          <xsl:otherwise>false</xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:variable name="occurence">
        <xsl:choose>
          <xsl:when test="@occurance = 'optional'">true</xsl:when>
          <xsl:when test="@occurance = 'indexed'">indexed</xsl:when>
          <xsl:when test="@occurence = 'optional'">true</xsl:when>
          <xsl:when test="@occurence = 'indexed'">indexed</xsl:when>
          <xsl:otherwise>false</xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:variable name="pname" select="@name"/>
      <xsl:choose>
        <xsl:when test="$occurence = 'indexed'">
        // <xsl:value-of select="$pname"/>
        pindx  = new IWrapperIndexedParam("<xsl:value-of select="$pname"/>", <xsl:value-of select="$freqparam"/>);
        idxprms.put("<xsl:value-of select="$pname"/>", pindx);
          <xsl:if test="./iwrp:caster">
            <xsl:call-template name="fmt_caster">
              <xsl:with-param name="node" select="./iwrp:caster"/>
              <xsl:with-param name="var">pindx</xsl:with-param>
            </xsl:call-template>
          </xsl:if>
          <xsl:for-each select="./iwrp:precheck">
            <xsl:call-template name="fmt_precheck">
              <xsl:with-param name="node" select="current()"/>
              <xsl:with-param name="var">pindx</xsl:with-param>
            </xsl:call-template>
          </xsl:for-each>
          <xsl:for-each select="./iwrp:postcheck">
            <xsl:call-template name="fmt_postcheck">
              <xsl:with-param name="node" select="current()"/>
              <xsl:with-param name="var">pindx</xsl:with-param>
            </xsl:call-template>
          </xsl:for-each>
        </xsl:when>

        <xsl:otherwise>
        // <xsl:value-of select="$pname"/>
        pinfo  = new IWrapperParam("<xsl:value-of select="$pname"/>", <xsl:value-of select="$freqparam"/>, <xsl:value-of select="$occurence"/><xsl:text>, </xsl:text>
          <xsl:choose>
            <xsl:when test="./iwrp:default">
              <xsl:text>new de.schlund.pfixxml.RequestParam[] {</xsl:text>
              <xsl:for-each select="./iwrp:default/iwrp:value">
                <xsl:text>new de.schlund.pfixxml.SimpleRequestParam("</xsl:text><xsl:value-of select="node()"/><xsl:text>")</xsl:text>
                <xsl:if test="count(following-sibling::iwrp:value) > 0">
                  <xsl:text>, </xsl:text>
                </xsl:if>
              </xsl:for-each>
              <xsl:text>}</xsl:text>
            </xsl:when>
            <xsl:otherwise>null</xsl:otherwise>
          </xsl:choose>);
          <xsl:if test="@missingscode and $occurence = 'false'">
        pinfo.setCustomSCode("<xsl:value-of select="@missingscode"/>");
          </xsl:if>
        params.put("<xsl:value-of select="$pname"/>", pinfo);
          <xsl:if test="./iwrp:caster">
            <xsl:call-template name="fmt_caster">
              <xsl:with-param name="node" select="./iwrp:caster"/>
              <xsl:with-param name="var">pinfo</xsl:with-param>
            </xsl:call-template>
          </xsl:if>
          <xsl:for-each select="./iwrp:precheck">
            <xsl:call-template name="fmt_precheck">
              <xsl:with-param name="node" select="current()"/>
              <xsl:with-param name="var">pinfo</xsl:with-param>
            </xsl:call-template>
          </xsl:for-each>
          <xsl:for-each select="./iwrp:postcheck">
            <xsl:call-template name="fmt_postcheck">
              <xsl:with-param name="node" select="current()"/>
              <xsl:with-param name="var">pinfo</xsl:with-param>
            </xsl:call-template>
          </xsl:for-each>
        </xsl:otherwise>
      </xsl:choose><xsl:text>
      </xsl:text>
    </xsl:for-each>
    }
    <xsl:for-each select="/iwrp:interface/iwrp:param">
      <xsl:variable name="occurence">
        <xsl:choose>
          <xsl:when test="@occurence = 'optional'">true</xsl:when>
          <xsl:when test="@occurance = 'optional'">true</xsl:when>
          <xsl:when test="@occurence = 'indexed'">indexed</xsl:when>
          <xsl:when test="@occurance = 'indexed'">indexed</xsl:when>
          <xsl:otherwise>false</xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:variable name="freq">
        <xsl:choose>
          <xsl:when test="@frequency = 'multiple'">[]</xsl:when>
          <xsl:otherwise></xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:variable name="pname" select="@name"/>
      <xsl:variable name="cpname" 
        select="concat(translate(substring($pname, 1, 1), 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'), substring($pname, 2))"/>  
      <xsl:variable name="ptype" select="@type"/>
      <xsl:choose>
        <xsl:when test="$occurence = 'indexed'">
    // <xsl:value-of select="$pname"/>
    public <xsl:value-of select="$ptype"/><xsl:value-of select="$freq"/> get<xsl:value-of select="$cpname"/>(String index) {
        return (<xsl:value-of select="$ptype"/><xsl:value-of select="$freq"/>) gimmeIndexedParamForKey("<xsl:value-of select="$pname"/>").getParamForIndex(index).getValue<xsl:if test="not(string($freq) = '')">Arr</xsl:if>();
    }

    public java.lang.String[] getKeys<xsl:value-of select="$cpname"/>() {
        return gimmeIndexedParamForKey("<xsl:value-of select="$pname"/>").getKeys();
    }
          
    public void setStringVal<xsl:value-of select="$cpname"/>(String<xsl:value-of select="$freq"/> v, String index) {
        gimmeIndexedParamForKey("<xsl:value-of select="$pname"/>").getParamForIndex(index).
          <xsl:choose><xsl:when test="string($freq) = ''">setStringValue(new String[] {v});</xsl:when>
            <xsl:otherwise>setStringValue(v);</xsl:otherwise></xsl:choose>
    }

    <xsl:if test="not($ptype = 'String') and not($ptype = 'java.lang.String')">
    public void setStringVal<xsl:value-of select="$cpname"/>(<xsl:value-of select="$ptype"/><xsl:value-of select="$freq"/> v, String index) {
        gimmeIndexedParamForKey("<xsl:value-of select="$pname"/>").getParamForIndex(index).
          <xsl:choose><xsl:when test="string($freq) = ''">setStringValue(new <xsl:value-of select="$ptype"/>[] {v});</xsl:when>
            <xsl:otherwise>setStringValue(v);</xsl:otherwise></xsl:choose>
    }
    </xsl:if>
    
    /**
      * @deprecated use setStringVal<xsl:value-of select="$cpname"/>(<xsl:value-of select="$ptype"/><xsl:value-of select="$freq"/> v, String index) instead.
      */
    @Deprecated
    public void set<xsl:value-of select="$cpname"/>(<xsl:value-of select="$ptype"/><xsl:value-of select="$freq"/> v, String index) {
        setStringVal<xsl:value-of select="$cpname"/>(v, index);
    }

    public void addSCode<xsl:value-of select="$cpname"/>(de.schlund.util.statuscodes.StatusCode scode, String index) {
        gimmeIndexedParamForKey("<xsl:value-of select="$pname"/>").addSCode(scode, null, null, index);
    }

    public void addSCode<xsl:value-of select="$cpname"/>(de.schlund.util.statuscodes.StatusCode scode, String[] args, String level, String index) {
        gimmeIndexedParamForKey("<xsl:value-of select="$pname"/>").addSCode(scode, args, level, index);
    }
    
    /**
      * @deprecated use addScode<xsl:value-of select="$cpname"/>(scode, args, null, index)
      */
    @Deprecated
    public void addSCodeWithArgs<xsl:value-of select="$cpname"/>(de.schlund.util.statuscodes.StatusCode scode, String[] args, String index) {
        gimmeIndexedParamForKey("<xsl:value-of select="$pname"/>").addSCode(scode, args, null, index);
    }
        </xsl:when>
        <xsl:otherwise>
    // <xsl:value-of select="$pname"/>
    public <xsl:value-of select="$ptype"/><xsl:value-of select="$freq"/> get<xsl:value-of select="$cpname"/>() {
        return (<xsl:value-of select="$ptype"/><xsl:value-of select="$freq"/>) gimmeParamForKey("<xsl:value-of select="$pname"/>").getValue<xsl:if test="not(string($freq) = '')">Arr</xsl:if>();
    }
    
    public void setStringVal<xsl:value-of select="$cpname"/>(String<xsl:value-of select="$freq"/> v) {
        gimmeParamForKey("<xsl:value-of select="$pname"/>").
          <xsl:choose><xsl:when test="string($freq) = ''">setStringValue(new String[] {v});</xsl:when>
            <xsl:otherwise>setStringValue(v);</xsl:otherwise></xsl:choose>
    }

    <xsl:if test="not($ptype = 'String') and not($ptype = 'java.lang.String')">
    public void setStringVal<xsl:value-of select="$cpname"/>(<xsl:value-of select="$ptype"/><xsl:value-of select="$freq"/> v) {
        gimmeParamForKey("<xsl:value-of select="$pname"/>").
          <xsl:choose><xsl:when test="string($freq) = ''">setStringValue(new <xsl:value-of select="$ptype"/>[] {v});</xsl:when>
            <xsl:otherwise>setStringValue(v);</xsl:otherwise></xsl:choose>
    }
    </xsl:if>

    /**
      * @deprecated use setStringVal<xsl:value-of select="$cpname"/>(<xsl:value-of select="$ptype"/><xsl:value-of select="$freq"/> v) instead.
      */
    @Deprecated
    public void set<xsl:value-of select="$cpname"/>(<xsl:value-of select="$ptype"/><xsl:value-of select="$freq"/> v) {
        setStringVal<xsl:value-of select="$cpname"/>(v);
    }

    public void addSCode<xsl:value-of select="$cpname"/>(de.schlund.util.statuscodes.StatusCode scode) {
        addSCode(gimmeParamForKey("<xsl:value-of select="$pname"/>"), scode, null, null);
    }

    public void addSCode<xsl:value-of select="$cpname"/>(de.schlund.util.statuscodes.StatusCode scode, String[] args, String level) {
        addSCode(gimmeParamForKey("<xsl:value-of select="$pname"/>"), scode, args, level);
    }

    /**
      * @deprecated use addScode<xsl:value-of select="$cpname"/>(scode, args, null)
      */
    @Deprecated
    public void addSCodeWithArgs<xsl:value-of select="$cpname"/>(de.schlund.util.statuscodes.StatusCode scode, String[] args) {
        addSCode(gimmeParamForKey("<xsl:value-of select="$pname"/>"), scode, args, null);
    }
        </xsl:otherwise>
      </xsl:choose>
    </xsl:for-each>
}
  </xsl:template>

  <xsl:template name="fmt_cparam">
    <xsl:param name="node"/>
    <xsl:param name="class"/>
    <xsl:param name="var"/>
    <xsl:for-each select="$node/iwrp:cparam">
      <xsl:variable name="cpname" select="./@name"/>
      <xsl:variable name="cpvalue" select="./@value"/>
        ((<xsl:value-of select="$class"/>) <xsl:value-of select="$var"/>).put_<xsl:value-of select="$cpname"/>("<xsl:value-of select="$cpvalue"/>");
    </xsl:for-each> 
  </xsl:template>
  
  <xsl:template name="fmt_caster">
    <xsl:param name="var"/>
    <xsl:param name="node"/>
    <xsl:variable name="class" select="$node/@class"/>
        caster = new <xsl:value-of select="$class"/>();
        <xsl:value-of select="$var"/>.setParamCaster(caster);
    <xsl:call-template name="fmt_cparam">
      <xsl:with-param name="var">caster</xsl:with-param>
      <xsl:with-param name="class" select="$class"/>
      <xsl:with-param name="node" select="$node"/>
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="fmt_precheck">
    <xsl:param name="var"/>
    <xsl:param name="node"/>
    <xsl:variable name="class" select="$node/@class"/>
        pre = new <xsl:value-of select="$class"/>();
        <xsl:value-of select="$var"/>.addPreChecker(pre);
    <xsl:call-template name="fmt_cparam">
      <xsl:with-param name="var">pre</xsl:with-param>
      <xsl:with-param name="class" select="$class"/>
      <xsl:with-param name="node" select="$node"/>
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="fmt_postcheck">
    <xsl:param name="var"/>
    <xsl:param name="node"/>
    <xsl:variable name="class" select="$node/@class"/>
        post = new <xsl:value-of select="$class"/>();
        <xsl:value-of select="$var"/>.addPostChecker(post);
    <xsl:call-template name="fmt_cparam">
      <xsl:with-param name="var">post</xsl:with-param>
      <xsl:with-param name="class" select="$class"/>
      <xsl:with-param name="node" select="$node"/>
    </xsl:call-template>
  </xsl:template>


  <xsl:template name="generatetostring">
    <xsl:param name="classname"/>
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer(255);
        sb.append("\n*** All wrapper-data for <xsl:value-of select="$classname"/> ***\n");
        <xsl:for-each select="/iwrp:interface/iwrp:param">
          <xsl:variable name="freq">
            <xsl:choose>
              <xsl:when test="@frequency = 'multiple'">[]</xsl:when>
              <xsl:otherwise></xsl:otherwise>
            </xsl:choose>
          </xsl:variable>
          <xsl:variable name="pname" select="@name"/>
          <xsl:variable name="ptype" select="@type"/>
            
           <xsl:choose>
            <xsl:when test="@frequency = 'multiple'">
              <xsl:variable name="arrayname"><xsl:value-of select="$pname"/>Arr</xsl:variable>
              <xsl:value-of select="$ptype"/>[] <xsl:value-of select="$arrayname"/>= (<xsl:value-of select="$ptype"/>[])gimmeParamForKey("<xsl:value-of select="$pname"/>").getValueArr(); 
        if (<xsl:value-of select="$arrayname"/> == null) {
            sb.append("<xsl:value-of select="$pname"/>[] = NULL");
        } else {
            for (int i = 0; i &lt; <xsl:value-of select="$arrayname"/>.length; i++) {
               sb.append("<xsl:value-of select="$pname"/>[" + i + "] = " + <xsl:value-of select="$arrayname"/>[i]).append("\n");
            }
        }
             </xsl:when>
             <xsl:otherwise>
        sb.append("<xsl:value-of select="$pname"/> = " + gimmeParamForKey("<xsl:value-of select="$pname"/>").getValue()).append("\n");
            </xsl:otherwise>
           </xsl:choose> 
        </xsl:for-each>
        return sb.toString();
    }
  
  </xsl:template>


</xsl:stylesheet>
