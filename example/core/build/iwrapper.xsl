<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="text" encoding="ISO-8859-1" indent="no"/>

  <xsl:param name="classname"/>
  <xsl:param name="package"/>
  
  <xsl:template match="interface"><xsl:param name="extends">
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

public class <xsl:value-of select="$classname"/> extends <xsl:value-of select="$extends"/> {

    public <xsl:value-of select="$classname"/>() {<xsl:choose>
      <xsl:when test="/interface/ihandler">
        handler = IHandlerFactory.getInstance().getIHandler("<xsl:value-of select="/interface/ihandler/@class"/>");</xsl:when>
      <xsl:otherwise>
        super();</xsl:otherwise>
    </xsl:choose>
    }
    
    
    <xsl:call-template name="generatetostring">
      <xsl:with-param name="classname" select="$classname"/> 
    </xsl:call-template>
    
    protected synchronized void registerParamInfos() {
        super.registerParamInfos();
        IWrapperParamInfo      pinfo;
        IWrapperIndexedParam   pindx;
        IWrapperParamCaster    caster;
        IWrapperParamPreCheck  pre;
        IWrapperParamPostCheck post;
    <xsl:for-each select="/interface/param">
      <xsl:variable name="occurance">
        <xsl:choose>
          <xsl:when test="@occurance = 'optional'">true</xsl:when>
          <xsl:when test="@occurance = 'indexed'">indexed</xsl:when>
          <xsl:otherwise>false</xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:variable name="pname" select="@name"/>
      <xsl:choose>
        <xsl:when test="$occurance = 'indexed'">
        // <xsl:value-of select="$pname"/>
        pindx  = new IWrapperIndexedParam("<xsl:value-of select="$pname"/>");
        idxprms.put("<xsl:value-of select="$pname"/>", pindx);
          <xsl:if test="./caster">
            <xsl:call-template name="fmt_caster">
              <xsl:with-param name="node" select="./caster"/>
              <xsl:with-param name="var">pindx</xsl:with-param>
            </xsl:call-template>
          </xsl:if>
          <xsl:for-each select="./precheck">
            <xsl:call-template name="fmt_precheck">
              <xsl:with-param name="node" select="current()"/>
              <xsl:with-param name="var">pindx</xsl:with-param>
            </xsl:call-template>
          </xsl:for-each>
          <xsl:for-each select="./postcheck">
            <xsl:call-template name="fmt_postcheck">
              <xsl:with-param name="node" select="current()"/>
              <xsl:with-param name="var">pindx</xsl:with-param>
            </xsl:call-template>
          </xsl:for-each>
        </xsl:when>

        <xsl:otherwise>
        // <xsl:value-of select="$pname"/>
        pinfo  = new IWrapperParamInfo("<xsl:value-of select="$pname"/>", <xsl:value-of select="$occurance"/><xsl:text>, </xsl:text>
          <xsl:choose>
            <xsl:when test="./default">
              <xsl:text>new de.schlund.pfixxml.RequestParam[] {</xsl:text>
              <xsl:for-each select="./default">
                <xsl:text>new de.schlund.pfixxml.SimpleRequestParam("</xsl:text><xsl:value-of select="@value"/><xsl:text>")</xsl:text>
                <xsl:if test="count(following-sibling::default) > 0">
                  <xsl:text>, </xsl:text>
                </xsl:if>
              </xsl:for-each>
              <xsl:text>}</xsl:text>
            </xsl:when>
            <xsl:otherwise>null</xsl:otherwise>
          </xsl:choose>);
          <xsl:if test="@missingscode and $occurance = 'false'">
        pinfo.setCustomSCode("<xsl:value-of select="@missingscode"/>");
          </xsl:if>
        params.put("<xsl:value-of select="$pname"/>", pinfo);
          <xsl:if test="./caster">
            <xsl:call-template name="fmt_caster">
              <xsl:with-param name="node" select="./caster"/>
              <xsl:with-param name="var">pinfo</xsl:with-param>
            </xsl:call-template>
          </xsl:if>
          <xsl:for-each select="./precheck">
            <xsl:call-template name="fmt_precheck">
              <xsl:with-param name="node" select="current()"/>
              <xsl:with-param name="var">pinfo</xsl:with-param>
            </xsl:call-template>
          </xsl:for-each>
          <xsl:for-each select="./postcheck">
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
    <xsl:for-each select="/interface/param">
      <xsl:variable name="occurance">
        <xsl:choose>
          <xsl:when test="@occurance = 'optional'">true</xsl:when>
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
      <xsl:variable name="ptype" select="@type"/>
      <xsl:choose>
        <xsl:when test="$occurance = 'indexed'">
    // <xsl:value-of select="$pname"/>
    public <xsl:value-of select="$ptype"/><xsl:value-of select="$freq"/> get<xsl:value-of select="$pname"/>(String index) {
        return (<xsl:value-of select="$ptype"/><xsl:value-of select="$freq"/>) gimmeIndexedParamForKey("<xsl:value-of select="$pname"/>").getParamInfoForIndex(index).getValue<xsl:if test="not(string($freq) = '')">Arr</xsl:if>();
    }

    public java.lang.String[] getKeys<xsl:value-of select="$pname"/>() {
        return gimmeIndexedParamForKey("<xsl:value-of select="$pname"/>").getKeys();
    }
          
    public void setStringVal<xsl:value-of select="$pname"/>(String<xsl:value-of select="$freq"/> v, String index) {
        gimmeIndexedParamForKey("<xsl:value-of select="$pname"/>").getParamInfoForIndex(index).
          <xsl:choose><xsl:when test="string($freq) = ''">setStringValue(new String[] {v});</xsl:when>
            <xsl:otherwise>setStringValue(v);</xsl:otherwise></xsl:choose>
    }

    public void set<xsl:value-of select="$pname"/>(<xsl:value-of select="$ptype"/><xsl:value-of select="$freq"/> v, String index) {
        gimmeIndexedParamForKey("<xsl:value-of select="$pname"/>").getParamInfoForIndex(index).
          <xsl:choose><xsl:when test="string($freq) = ''">setSimpleObjectValue(new Object[] {v});</xsl:when>
            <xsl:otherwise>setSimpleObjectValue(v);</xsl:otherwise></xsl:choose>
    }

    public void addSCode<xsl:value-of select="$pname"/>(de.schlund.util.statuscodes.StatusCode scode, String index) {
        gimmeIndexedParamForKey("<xsl:value-of select="$pname"/>").addSCode(scode, index);
    }

    public void addSCodeWithArgs<xsl:value-of select="$pname"/>(de.schlund.util.statuscodes.StatusCode scode, String[] args, String index) {
        gimmeIndexedParamForKey("<xsl:value-of select="$pname"/>").addSCode(scode, args, index);
    }
        </xsl:when>
        <xsl:otherwise>
    // <xsl:value-of select="$pname"/>
    public <xsl:value-of select="$ptype"/><xsl:value-of select="$freq"/> get<xsl:value-of select="$pname"/>() {
        return (<xsl:value-of select="$ptype"/><xsl:value-of select="$freq"/>) gimmeParamInfoForKey("<xsl:value-of select="$pname"/>").getValue<xsl:if test="not(string($freq) = '')">Arr</xsl:if>();
    }
    
    public void setStringVal<xsl:value-of select="$pname"/>(String<xsl:value-of select="$freq"/> v) {
        gimmeParamInfoForKey("<xsl:value-of select="$pname"/>").
          <xsl:choose><xsl:when test="string($freq) = ''">setStringValue(new String[] {v});</xsl:when>
            <xsl:otherwise>setStringValue(v);</xsl:otherwise></xsl:choose>
    }

    public void set<xsl:value-of select="$pname"/>(<xsl:value-of select="$ptype"/><xsl:value-of select="$freq"/> v) {
        gimmeParamInfoForKey("<xsl:value-of select="$pname"/>").
          <xsl:choose><xsl:when test="string($freq) = ''">setSimpleObjectValue(new Object[] {v});</xsl:when>
            <xsl:otherwise>setSimpleObjectValue(v);</xsl:otherwise></xsl:choose>
    }

    public void addSCode<xsl:value-of select="$pname"/>(de.schlund.util.statuscodes.StatusCode scode) {
        gimmeParamInfoForKey("<xsl:value-of select="$pname"/>").addSCode(scode);
        synchronized (errors) {
            errors.put(gimmeParamInfoForKey("<xsl:value-of select="$pname"/>").getName(), gimmeParamInfoForKey("<xsl:value-of select="$pname"/>"));
        }
    }

    public void addSCodeWithArgs<xsl:value-of select="$pname"/>(de.schlund.util.statuscodes.StatusCode scode, String[] args) {
        gimmeParamInfoForKey("<xsl:value-of select="$pname"/>").addSCode(scode, args);
        synchronized (errors) {
            errors.put(gimmeParamInfoForKey("<xsl:value-of select="$pname"/>").getName(), gimmeParamInfoForKey("<xsl:value-of select="$pname"/>"));
        }
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
    <xsl:for-each select="$node/cparam">
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
    public String toString() {
        StringBuffer sb = new StringBuffer(255);
        sb.append("\n*** All wrapper-data for <xsl:value-of select="$classname"/> ***\n");
        <xsl:for-each select="/interface/param">
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
              <xsl:value-of select="$ptype"/>[] <xsl:value-of select="$arrayname"/>= (<xsl:value-of select="$ptype"/>[])gimmeParamInfoForKey("<xsl:value-of select="$pname"/>").getValueArr(); 
        if (<xsl:value-of select="$arrayname"/> == null) {
            sb.append("<xsl:value-of select="$pname"/>[] = NULL");
        } else {
            for (int i = 0; i &lt; <xsl:value-of select="$arrayname"/>.length; i++) {
               sb.append("<xsl:value-of select="$pname"/>[" + i + "] = " + <xsl:value-of select="$arrayname"/>[i]).append("\n");
            }
        }
             </xsl:when>
             <xsl:otherwise>
        sb.append("<xsl:value-of select="$pname"/> = " + gimmeParamInfoForKey("<xsl:value-of select="$pname"/>").getValue()).append("\n");
            </xsl:otherwise>
           </xsl:choose> 
        </xsl:for-each>
        return sb.toString();
    }
  
  </xsl:template>


</xsl:stylesheet>
