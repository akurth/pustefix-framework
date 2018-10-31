<xsl:stylesheet version="1.1" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:iwrp="http://www.pustefix-framework.org/2008/namespace/iwrapper"
  xmlns:old="http://pustefix.sourceforge.net/interfacewrapper200401"
  >
  <xsl:output method="text" encoding="UTF-8" indent="no"/>

  <xsl:param name="classname"/>
  <xsl:param name="package"/>
  
  <xsl:variable name="newns">http://www.pustefix-framework.org/2008/namespace/iwrapper</xsl:variable>
  <xsl:variable name="oldns">http://pustefix.sourceforge.net/interfacewrapper200401</xsl:variable>
  
  <!--
  // TODO 2004-12-06 adam corrected spelling of occurence (was occurance).
  changed schema example/core/schema/interfacewrapper200401.xsd
  to allow both, correct: "occurence" old one with typo "occurence" 
  remove the "occurance" stuff herein, once it can be expected anyone has updated its files
  to the new spelling. Mind to update schema file.
  -->
  
  <xsl:template match="old:interface">
    <xsl:message>[WARNING] [DEPRECATED] IWrapper definition for '<xsl:value-of select="$classname"/>' uses deprecated namespace: '<xsl:value-of select="$oldns"/>'. It should be replaced by '<xsl:value-of select="$newns"/>'.</xsl:message>
    <xsl:variable name="upgraded">
      <xsl:apply-templates select="." mode="old"/>
    </xsl:variable>
    <xsl:apply-templates select="$upgraded"/>
  </xsl:template>
  
  <xsl:template match="*" mode="old">
    <xsl:element name="{local-name()}" namespace="{$newns}">
      <xsl:copy-of select="@*"/>
      <xsl:apply-templates mode="old"/>
    </xsl:element>
  </xsl:template>
  
  <xsl:template match="iwrp:interface" ><xsl:param name="extends">
  <xsl:if test="./iwrp:param/@occurance">
    <xsl:message>[WARNING] [DEPRECATED] IWrapper definition for '<xsl:value-of select="$classname"/>' uses deprecated attribute: the use of 'occurance' (with 'a') is deprecated, use occurrence (with 'e') instead.</xsl:message>
  </xsl:if>
  <xsl:if test="./iwrp:param/@occurence">
    <xsl:message>[WARNING] [DEPRECATED] IWrapper definition for '<xsl:value-of select="$classname"/>' uses deprecated attribute: the use of 'occurence' (with one 'r')is deprecated, use occurrence (with double 'r') instead.</xsl:message>
  </xsl:if>
      <xsl:choose>
        <xsl:when test="@extends">
          <xsl:value-of select="@extends"/>
        </xsl:when>
        <xsl:otherwise>IWrapperImpl</xsl:otherwise>
      </xsl:choose>
    </xsl:param>/*
 * This file is part of Pustefix.
 *
 * Pustefix is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Pustefix is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Pustefix; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
    
package <xsl:value-of select="$package"/>;
    
import de.schlund.pfixcore.generator.*;
<xsl:if test="./iwrp:param/iwrp:caster or ./iwrp:param/iwrp:precheck or ./iwrp:param/iwrp:postcheck">
import de.schlund.pfixcore.generator.annotation.*;
</xsl:if>
/**
 * THIS CLASS IS AUTOGENERATED! DO NOT EDIT IN ANY WAY!
 */
<xsl:variable name="ihandlerClassStr" select="/iwrp:interface/iwrp:ihandler/@class" />
<xsl:variable name="javaBoolean">java.lang.Boolean</xsl:variable>
<xsl:variable name="javaInteger">java.lang.Integer</xsl:variable>
<xsl:variable name="javaLong">java.lang.Long</xsl:variable>
<xsl:variable name="javaDouble">java.lang.Double</xsl:variable>
<xsl:variable name="javaFloat">java.lang.Float</xsl:variable>
<xsl:if test="./iwrp:ihandler">
  <xsl:choose>
    <xsl:when test="./iwrp:ihandler/@bean-ref">
<xsl:text>@UseHandlerBeanRef("</xsl:text><xsl:value-of select="./iwrp:ihandler/@bean-ref"/><xsl:text>")</xsl:text>
    </xsl:when>
    <xsl:otherwise>
<xsl:text>@UseHandlerClass(</xsl:text><xsl:value-of select="./iwrp:ihandler/@class"/><xsl:text>.class)</xsl:text>
    </xsl:otherwise>
  </xsl:choose>
</xsl:if>
public <xsl:if test="not(./iwrp:ihandler) and not(@extends)">abstract </xsl:if>class <xsl:value-of select="$classname"/> extends <xsl:value-of select="$extends"/> <xsl:if test="@implements"> implements <xsl:value-of select="@implements"/></xsl:if> {

    @Override
    protected synchronized void registerParams() {
        <xsl:if test="./iwrp:param[not(@occurance='indexed' or @occurence='indexed' or @occurrence ='indexed')]">
        IWrapperParam          pinfo;
        </xsl:if>
        <xsl:if test="./iwrp:param[@occurance='indexed' or @occurence='indexed' or @occurrence='indexed']">
        IWrapperIndexedParam   pindx;
        </xsl:if>
        <xsl:if test="./iwrp:param/iwrp:caster or ./iwrp:param/@type=$javaBoolean or ./iwrp:param/@type=$javaInteger or ./iwrp:param/@type=$javaLong or ./iwrp:param/@type=$javaDouble or ./iwrp:param/@type=$javaFloat">
        IWrapperParamCaster    caster;
        </xsl:if>
        <xsl:if test="./iwrp:param/iwrp:precheck"> 
        IWrapperParamPreCheck  pre;
        </xsl:if>
        <xsl:if test="./iwrp:param/iwrp:postcheck"> 
        IWrapperParamPostCheck post;
        </xsl:if>
    <xsl:for-each select="./iwrp:param">
      <xsl:variable name="ptype" select="@type"/>
      <xsl:variable name="freqparam">
        <xsl:choose>
          <xsl:when test="@frequency = 'multiple'">true</xsl:when>
          <xsl:otherwise>false</xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:variable name="occurrence">
        <xsl:choose>
          <xsl:when test="@occurance = 'optional'">true</xsl:when>
          <xsl:when test="@occurance = 'indexed'">indexed</xsl:when>
          <xsl:when test="@occurence = 'optional'">true</xsl:when>
          <xsl:when test="@occurence = 'indexed'">indexed</xsl:when>
          <xsl:when test="@occurrence = 'optional'">true</xsl:when>
          <xsl:when test="@occurrence = 'indexed'">indexed</xsl:when>
          <xsl:otherwise>false</xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:variable name="pname" select="@name"/>
      <xsl:variable name="trim">
        <xsl:choose>
          <xsl:when test="@trim='false'">false</xsl:when>
          <xsl:otherwise>true</xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:choose>
        <xsl:when test="$occurrence = 'indexed'">
        // <xsl:value-of select="$pname"/>
        if(!idxprms.containsKey("<xsl:value-of select="$pname"/>")) {
        pindx  = new IWrapperIndexedParam("<xsl:value-of select="$pname"/>", <xsl:value-of select="$freqparam"/>, "<xsl:value-of select="$ptype"/>", <xsl:value-of select="$trim"/>);
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
        }
        </xsl:when>
        <xsl:otherwise>
        // <xsl:value-of select="$pname"/>
        if(!params.containsKey("<xsl:value-of select="$pname"/>")) {
        pinfo  = new IWrapperParam("<xsl:value-of select="$pname"/>", <xsl:value-of select="$freqparam"/>, <xsl:value-of select="$occurrence"/><xsl:text>, </xsl:text>
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
          </xsl:choose>, "<xsl:value-of select="$ptype"/>", <xsl:value-of select="$trim"/>);
          <xsl:if test="@missingscode and $occurrence = 'false'">
        pinfo.setCustomSCode("<xsl:value-of select="@missingscode"/>");
          </xsl:if>
        params.put("<xsl:value-of select="$pname"/>", pinfo);
          <xsl:choose>
            <xsl:when test="./iwrp:caster">
              <xsl:call-template name="fmt_caster">
                <xsl:with-param name="node" select="./iwrp:caster"/>
                <xsl:with-param name="var">pinfo</xsl:with-param>
              </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
              <xsl:choose>
                <xsl:when test="$ptype=$javaBoolean">
                  <xsl:call-template name="fmt_auto_caster">
                    <xsl:with-param name="class">de.schlund.pfixcore.generator.casters.ToBoolean</xsl:with-param>
                    <xsl:with-param name="var">pinfo</xsl:with-param>
                  </xsl:call-template>              
                </xsl:when>
                <xsl:when test="$ptype=$javaInteger">
                  <xsl:call-template name="fmt_auto_caster">
                    <xsl:with-param name="class">de.schlund.pfixcore.generator.casters.ToInteger</xsl:with-param>
                    <xsl:with-param name="var">pinfo</xsl:with-param>
                  </xsl:call-template>              
                </xsl:when>
                <xsl:when test="$ptype=$javaLong">
                  <xsl:call-template name="fmt_auto_caster">
                    <xsl:with-param name="class">de.schlund.pfixcore.generator.casters.ToLong</xsl:with-param>
                    <xsl:with-param name="var">pinfo</xsl:with-param>
                  </xsl:call-template>              
                </xsl:when>
                <xsl:when test="$ptype=$javaDouble">
                  <xsl:call-template name="fmt_auto_caster">
                    <xsl:with-param name="class">de.schlund.pfixcore.generator.casters.ToDouble</xsl:with-param>
                    <xsl:with-param name="var">pinfo</xsl:with-param>
                  </xsl:call-template>              
                </xsl:when>
                <xsl:when test="$ptype=$javaFloat">
                  <xsl:call-template name="fmt_auto_caster">
                    <xsl:with-param name="class">de.schlund.pfixcore.generator.casters.ToFloat</xsl:with-param>
                    <xsl:with-param name="var">pinfo</xsl:with-param>
                  </xsl:call-template>              
                </xsl:when>
              </xsl:choose>
            </xsl:otherwise>
          </xsl:choose>
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
        }  
        </xsl:otherwise>
      </xsl:choose><xsl:text>
      </xsl:text>
    </xsl:for-each>
        super.registerParams();
    }
    <xsl:for-each select="./iwrp:param">
      <xsl:variable name="occurrence">
        <xsl:choose>
          <xsl:when test="@occurence = 'optional'">true</xsl:when>
          <xsl:when test="@occurance = 'optional'">true</xsl:when>
          <xsl:when test="@occurrence = 'optional'">true</xsl:when>
          <xsl:when test="@occurence = 'indexed'">indexed</xsl:when>
          <xsl:when test="@occurance = 'indexed'">indexed</xsl:when>
          <xsl:when test="@occurrence = 'indexed'">indexed</xsl:when>
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
        <xsl:when test="$occurrence = 'indexed'">
    // <xsl:value-of select="$pname"/>
    <xsl:call-template name="annotate"/>
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

    public void set<xsl:value-of select="$cpname"/>(<xsl:value-of select="$ptype"/><xsl:value-of select="$freq"/> v, String index) {
        gimmeIndexedParamForKey("<xsl:value-of select="$pname"/>").getParamForIndex(index).
          <xsl:choose><xsl:when test="string($freq) = ''">setStringValue(new <xsl:value-of select="$ptype"/>[] {v});</xsl:when>
            <xsl:otherwise>setStringValue(v);</xsl:otherwise></xsl:choose>
    }

    public void addSCode<xsl:value-of select="$cpname"/>(de.schlund.util.statuscodes.StatusCode scode, String index) {
        gimmeIndexedParamForKey("<xsl:value-of select="$pname"/>").addSCode(scode, null, null, index);
    }

    public void addSCode<xsl:value-of select="$cpname"/>(de.schlund.util.statuscodes.StatusCode scode, String[] args, String level, String index) {
        gimmeIndexedParamForKey("<xsl:value-of select="$pname"/>").addSCode(scode, args, level, index);
    }
    
    /**
      * @deprecated Will be removed in Pustefix 1.0, use addScode<xsl:value-of select="$cpname"/>(scode, args, null, index)
      *
      * @param scode statuscode
      * @param args  arguments
      * @param index index
      */
    @Deprecated
    public void addSCodeWithArgs<xsl:value-of select="$cpname"/>(de.schlund.util.statuscodes.StatusCode scode, String[] args, String index) {
        gimmeIndexedParamForKey("<xsl:value-of select="$pname"/>").addSCode(scode, args, null, index);
    }
        </xsl:when>
        <xsl:otherwise>
    // <xsl:value-of select="$pname"/>
    <xsl:call-template name="annotate"/>
    public <xsl:value-of select="$ptype"/><xsl:value-of select="$freq"/> get<xsl:value-of select="$cpname"/>() {
        return (<xsl:value-of select="$ptype"/><xsl:value-of select="$freq"/>) gimmeParamForKey("<xsl:value-of select="$pname"/>").getValue<xsl:if test="not(string($freq) = '')">Arr</xsl:if>();
    }
    
    public void setStringVal<xsl:value-of select="$cpname"/>(String<xsl:value-of select="$freq"/> v) {
        gimmeParamForKey("<xsl:value-of select="$pname"/>").
          <xsl:choose><xsl:when test="string($freq) = ''">setStringValue(new String[] {v});</xsl:when>
            <xsl:otherwise>setStringValue(v);</xsl:otherwise></xsl:choose>
    }

    public void set<xsl:value-of select="$cpname"/>(<xsl:value-of select="$ptype"/><xsl:value-of select="$freq"/> v) {
        IWrapperParam iwrpParam = gimmeParamForKey("<xsl:value-of select="$pname"/>");
        <xsl:choose><xsl:when test="string($freq) = ''">setStringValue(new <xsl:value-of select="$ptype"/>[] {v}, iwrpParam);</xsl:when>
        <xsl:otherwise>setStringValue(v, iwrpParam);</xsl:otherwise></xsl:choose>
    }

    public void addSCode<xsl:value-of select="$cpname"/>(de.schlund.util.statuscodes.StatusCode scode) {
        addSCode(gimmeParamForKey("<xsl:value-of select="$pname"/>"), scode, null, null);
    }

    public void addSCode<xsl:value-of select="$cpname"/>(de.schlund.util.statuscodes.StatusCode scode, String[] args, String level) {
        addSCode(gimmeParamForKey("<xsl:value-of select="$pname"/>"), scode, args, level);
    }

    /**
      * @deprecated Will be removed in Pustefix 1.0, use addScode<xsl:value-of select="$cpname"/>(scode, args, null)
      *
      * @param scode statuscode
      * @param args  arguments
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
      <xsl:variable name="cpname" select="concat(translate(substring(./@name, 1, 1), 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'), substring(./@name, 2))"/>
      <xsl:variable name="cpvalue" select="./@value"/>
        ((<xsl:value-of select="$class"/>) <xsl:value-of select="$var"/>).set<xsl:value-of select="$cpname"/>("<xsl:value-of select="$cpvalue"/>");
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

  <xsl:template name="fmt_auto_caster">
    <xsl:param name="var"/>
    <xsl:param name="class"/>
        caster = new <xsl:value-of select="$class"/>();
        <xsl:value-of select="$var"/>.setParamCaster(caster);
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
  
  <xsl:template name="annotate"> 
    <xsl:if test="iwrp:caster">
      <xsl:call-template name="annotate_properties">
        <xsl:with-param name="node" select="iwrp:caster"/>
        <xsl:with-param name="anno">Caster</xsl:with-param>
      </xsl:call-template>
    </xsl:if>
    <xsl:if test="iwrp:precheck">
      <xsl:call-template name="annotate_properties">
        <xsl:with-param name="node" select="iwrp:precheck"/>
        <xsl:with-param name="anno">PreCheck</xsl:with-param>
      </xsl:call-template>
    </xsl:if>
    <xsl:if test="iwrp:postcheck">
      <xsl:call-template name="annotate_properties">
        <xsl:with-param name="node" select="iwrp:postcheck"/>
        <xsl:with-param name="anno">PostCheck</xsl:with-param>
      </xsl:call-template>
    </xsl:if>
  </xsl:template>

  <xsl:template name="annotate_properties">
    <xsl:param name="node"/>
    <xsl:param name="anno"/>
    @<xsl:value-of select="$anno"/>(
        type=<xsl:value-of select="$node/@class"/>.class<xsl:text/>
    <xsl:if test="$node/iwrp:cparam">
        <xsl:text/>,
        properties={<xsl:text/>
      <xsl:for-each select="$node/iwrp:cparam">
            @Property(name="<xsl:value-of select="@name"/>",value="<xsl:value-of select="@value"/>")<xsl:text/>
        <xsl:if test="following-sibling::iwrp:cparam"><xsl:text>,</xsl:text></xsl:if>
      </xsl:for-each>
        }<xsl:text/>
    </xsl:if>
    )<xsl:text/>
  </xsl:template>

</xsl:stylesheet>
