<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.1"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:pfx="http://www.schlund.de/pustefix/core"
                xmlns:ixsl="http://www.w3.org/1999/XSL/TransformOutputAlias">

  <xsl:param name="maincontentpath"/>
  
  <xsl:template match="pfx:maincontent">
    <xsl:param name="noerror" select="@noerror"/>  
    <xsl:variable name="path">
      <xsl:choose>
        <xsl:when test="@path">
          <xsl:value-of select="@path"/>
        </xsl:when>
        <xsl:when test="string($maincontentpath) != ''"><xsl:value-of select="$maincontentpath"/></xsl:when>
        <xsl:otherwise><xsl:value-of select="$product"/>/txt/pages</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="prefix">
      <xsl:choose>
        <xsl:when test="@prefix">
          <xsl:value-of select="@prefix"/>
        </xsl:when>
        <xsl:otherwise>main_</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="postfix">
      <xsl:choose>
        <xsl:when test="@postfix">
          <xsl:value-of select="@postfix"/>
        </xsl:when>
        <xsl:otherwise>.xml</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="part">
      <xsl:choose>
        <xsl:when test="@part">
          <xsl:value-of select="@part"/>
        </xsl:when>
        <xsl:otherwise>content</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:call-template name="pfx:include">
      <xsl:with-param name="href"><xsl:value-of select="$path"/>/<xsl:value-of select="$prefix"/><xsl:value-of select="$page"/><xsl:value-of select="$postfix"/></xsl:with-param>
      <xsl:with-param name="part" select="$part"/>
      <!-- this is tricky to understand --> 
      <xsl:with-param name="computed_inc">true</xsl:with-param>
      <xsl:with-param name="noerr" select="$noerror"/>      
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="pfx:editconsole">
    <xsl:if test="$prohibitEdit = 'no'">
      <form target="_top">
        <table cellpadding="0" cellspacing="0" border="0">
          <tr>
            <td width="1px">
              <ixsl:choose>
                <ixsl:when test="$__editmode='admin'">
                  <a target="_top">
                    <ixsl:attribute name="href">
                      <ixsl:value-of select="$__uri"/>?__editmode=none</ixsl:attribute>
                    <img border="0" title="Switch edit mode OFF" src="/core/img/do_noedit.gif"/>
                  </a>
                </ixsl:when>
                <ixsl:otherwise>
                  <a target="_top">
                    <ixsl:attribute name="href">
                      <ixsl:value-of select="$__uri"/>?__editmode=admin</ixsl:attribute>
                    <img border="0" title="Switch edit mode ON" src="/core/img/do_edit.gif"/>
                  </a>
                </ixsl:otherwise>
              </ixsl:choose>
            </td>
            <td align="left">
              <a target="__xml_source__">
                <ixsl:attribute name="href">
                  <ixsl:value-of select="$__uri"/>?__reuse=<ixsl:value-of select="$__reusestamp"/>&amp;__xmlonly=1</ixsl:attribute>
                <img border="0" title="Show last XML tree" src="/core/img/show_xml.gif"/></a>
            </td>
          </tr>
          <tr>
            <td nowrap="nowrap" colspan="2" style="font-family: Verdana,Sans; font-size: 10px; background-color: black; color: white; padding-left: 5px; padding-right: 2px;">
              P: <ixsl:value-of select="$page"/>
            </td>
          </tr>
          <tr>
            <td nowrap="nowrap" colspan="2" style="font-family: Verdana,Sans; font-size: 10px; background-color: black; color: white; padding-left: 5px; padding-right: 2px;">
              F: <ixsl:value-of select="$pageflow"/>
            </td>
          </tr>
        </table>
      </form>
    </xsl:if>
  </xsl:template>

  <xsl:template match="pfx:webserviceconsole">
    <xsl:if test="$prohibitEdit = 'no'">
      <span>
        Web service tools:
        <a target="__WEB_SERVICE_MONITOR__">
          <ixsl:attribute name="href">
            <ixsl:value-of select="concat('/xml/webservice;',$__sessid,'?monitor')"/>
          </ixsl:attribute>
          Monitor
        </a>
        <a target="__WEB_SERVICE_ADMIN__">
          <ixsl:attribute name="href">
            <ixsl:value-of select="concat('/xml/webservice;',$__sessid,'?admin')"/>
          </ixsl:attribute>
          Admin
        </a>
      </span>
    </xsl:if>
  </xsl:template>

  <xsl:template match="pfx:blank">
    <img src="/core/img/blank.gif" width="1" height="1" border="0" alt="">
      <xsl:copy-of select="@*"/>
    </img>
  </xsl:template>







  <!-- ************************************************************************************************************* -->
  <!-- ****************************   NOTE NOTE NOTE NOTE NOTE NOTE NOTE NOTE ************************************** -->
  <!-- ************************************************************************************************************* -->
  <!-- All the following templates are DEPRECATED and may be removed any time in the future without -->
  <!-- further warning. Please don't use them in new code -->

  
  <xsl:template match="pfx:on"/>
  <xsl:template match="pfx:off"/>

  <xsl:template match="pfx:layer">
    <xsl:param name="elem">
      <xsl:choose>
        <xsl:when test="@element"><xsl:value-of select="@element"/></xsl:when>
        <xsl:otherwise>div</xsl:otherwise>
      </xsl:choose>
    </xsl:param>
    <xsl:param name="name">
      <xsl:choose>
        <xsl:when test="contains(@name, '/')"><xsl:value-of select="@name"/></xsl:when>
        <xsl:otherwise><xsl:value-of select="concat($page, '_', @name)"/></xsl:otherwise>
      </xsl:choose>
    </xsl:param>
    <xsl:element name="{$elem}">
      <xsl:attribute name="id"><xsl:value-of select="$name"/></xsl:attribute>
      <xsl:copy-of select="@style"/>
      <xsl:copy-of select="@class"/>
      <xsl:apply-templates/>
    </xsl:element>
    <script>
      var layer = top.__js_getLayer("<xsl:value-of select="$name"/>");
      layer.init("<xsl:value-of select="@visible"/>",window,"<xsl:value-of select="@store"/>");
      <xsl:if test="@pos-left">
        layer.moveLeft(<xsl:value-of select="@pos-left"/>);
      </xsl:if>
      <xsl:if test="@pos-top">
        layer.moveTop(<xsl:value-of select="@pos-top"/>);
      </xsl:if>
      <xsl:if test="@pos-right">
        layer.moveRight(<xsl:value-of select="@pos-right"/>);
      </xsl:if>
      <xsl:if test="@pos-bottom">
        layer.moveBottom(<xsl:value-of select="@pos-bottom"/>);
      </xsl:if>
    </script>
  </xsl:template>
  
  <xsl:template match="pfx:switch">
    <xsl:param name="class">
      <xsl:choose>
        <xsl:when test="not(@class = '')"><xsl:value-of select="@class"/></xsl:when>
        <xsl:otherwise>core_button_normal</xsl:otherwise>
      </xsl:choose>
    </xsl:param>
    <a class="{$class}" href="javascript://">
      <xsl:attribute name="onclick">top.__js_toggleLayers(<xsl:for-each select=".//pfx:layer_ref[not(@method) or @method = 'toggle']">
      <xsl:variable name="name">
        <xsl:choose>
          <xsl:when test="contains(@name, '/')"><xsl:value-of select="@name"/></xsl:when>
          <xsl:otherwise><xsl:value-of select="concat($page, '_', @name)"/></xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:text>'</xsl:text><xsl:value-of select="$name"/><xsl:text>'</xsl:text>
      <xsl:if test="count(following-sibling::pfx:layer_ref[not(@method) or @method = 'toggle']) > 0">,</xsl:if>
      </xsl:for-each>);top.__js_showLayers(<xsl:for-each select=".//pfx:layer_ref[@method = 'on']">
      <xsl:variable name="name">
        <xsl:choose>
          <xsl:when test="contains(@name, '/')"><xsl:value-of select="@name"/></xsl:when>
          <xsl:otherwise><xsl:value-of select="concat($page, '_', @name)"/></xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:text>'</xsl:text><xsl:value-of select="$name"/><xsl:text>'</xsl:text>
      <xsl:if test="count(following-sibling::pfx:layer_ref[@method = 'on']) > 0">,</xsl:if>
      </xsl:for-each>);top.__js_hideLayers(<xsl:for-each select=".//pfx:layer_ref[@method = 'off']">
      <xsl:variable name="name">
        <xsl:choose>
          <xsl:when test="contains(@name, '/')"><xsl:value-of select="@name"/></xsl:when>
          <xsl:otherwise><xsl:value-of select="concat($page, '_', @name)"/></xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:text>'</xsl:text><xsl:value-of select="$name"/><xsl:text>'</xsl:text>
      <xsl:if test="count(following-sibling::pfx:layer_ref[@method = 'off']) > 0">,</xsl:if>
      </xsl:for-each>);
      return true;</xsl:attribute>
      <xsl:apply-templates/>
    </a>
  </xsl:template>

  <xsl:template match="pfx:layer_check">
    <xsl:variable name="name">
      <xsl:choose>
        <xsl:when test="contains(@name, '/')"><xsl:value-of select="@name"/></xsl:when>
        <xsl:otherwise><xsl:value-of select="concat($page, '_', @name)"/></xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="unique"><xsl:value-of select="generate-id(.)"/></xsl:variable>
    <xsl:variable name="type"><xsl:value-of select="@type"/></xsl:variable>
    <xsl:choose>
      <xsl:when test="$type = 'radio' or $type = 'checkbox'">
        <input>
          <xsl:attribute name="type"><xsl:value-of select="$type"/></xsl:attribute>
          <xsl:attribute name="id">INPUT_<xsl:value-of select="$unique"/></xsl:attribute>
          <xsl:if test="@disabled">
            <xsl:copy-of select="@disabled"/>
          </xsl:if>
          <xsl:if test="@class">
            <xsl:copy-of select="@class"/>
          </xsl:if>
          <xsl:if test="@style">
            <xsl:copy-of select="@style"/>
          </xsl:if>
        </input>
        <script>
          top.__js_getLayer('<xsl:value-of select="$name"/>').addInputElem(window.document.getElementById('INPUT_<xsl:value-of select="$unique"/>'));
        </script>
      </xsl:when>
      <xsl:otherwise>
        <span>
          <xsl:attribute name="id">SWITCH_ON_<xsl:value-of select="$unique"/></xsl:attribute>
          <xsl:if test="./pfx:on/@class">
            <xsl:attribute name="class"><xsl:value-of select="./pfx:on/@class"/></xsl:attribute>
          </xsl:if>
          <xsl:apply-templates select="./pfx:on/node()"/>
        </span>
        <span>
          <xsl:attribute name="id">SWITCH_OFF_<xsl:value-of select="$unique"/></xsl:attribute>
          <xsl:if test="./pfx:off/@class">
            <xsl:attribute name="class"><xsl:value-of select="./pfx:off/@class"/></xsl:attribute>
          </xsl:if>
          <xsl:apply-templates select="./pfx:off/node()"/>
        </span>
        <script>
          top.__js_getLayer('<xsl:value-of select="$name"/>').addSwitchOn(window.document.getElementById('SWITCH_ON_<xsl:value-of select="$unique"/>'));
          top.__js_getLayer('<xsl:value-of select="$name"/>').addSwitchOff(window.document.getElementById('SWITCH_OFF_<xsl:value-of select="$unique"/>'));
        </script>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
  <xsl:template match="pfx:layer_ref">
    <xsl:param name="parent">
      <xsl:choose>
        <xsl:when test="count(ancestor::pfx:layer) > 0 and @autoclose = 'true'">
          <xsl:choose>
            <xsl:when test="contains(ancestor::pfx:layer[position() = 1]/@name, '/')">
              <xsl:value-of select="ancestor::pfx:layer[position() = 1]/@name"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="concat($page, '_', ancestor::pfx:layer[position() = 1]/@name)"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:when>
        <xsl:otherwise>__ROOT_LAYER__</xsl:otherwise>
      </xsl:choose>
    </xsl:param>
    <xsl:param name="child" select="concat($page, '_', @name)"/>
    <script>
      top.__js_getLayer('<xsl:value-of select="$parent"/>').addChild(top.__js_getLayer('<xsl:value-of select="$child"/>'));
      top.__js_getLayer('<xsl:value-of select="$child"/>').addParent(top.__js_getLayer('<xsl:value-of select="$parent"/>'));
    </script>
  </xsl:template>
    
</xsl:stylesheet>
