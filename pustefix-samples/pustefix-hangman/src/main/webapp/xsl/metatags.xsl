<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
                xmlns:ixsl="http://www.w3.org/1999/XSL/TransformOutputAlias" 
                xmlns:pfx="http://www.schlund.de/pustefix/core">

  <xsl:template match="show-error">
    <pfx:checkfield name="{@field}">
      <pfx:error>
        <span class="error"><pfx:scode/></span>
      </pfx:error>
    </pfx:checkfield>
  </xsl:template>
  
  <xsl:template match="buttons">
    <pfx:forminput>
      <div class="letters">
        <xsl:call-template name="button">
          <xsl:with-param name="letters">ABCDEFGHIJKLM</xsl:with-param>
        </xsl:call-template>
      </div>
      <div class="letters">
        <xsl:call-template name="button">
          <xsl:with-param name="letters">NOPQRSTUVWXYZ</xsl:with-param>
        </xsl:call-template>
      </div>
    </pfx:forminput>
  </xsl:template>
  
  <xsl:template name="button">
    <xsl:param name="letters"/>
    <xsl:variable name="letter" select="substring($letters,1,1)"/>
    <pfx:xinp type="submit" value="{$letter}" class="letter" onclick="guess(this);return false;">
      <pfx:argument name="play.letter"><xsl:value-of select="$letter"/></pfx:argument>
    </pfx:xinp>
    <xsl:if test="string-length($letters) > 1">
    <xsl:call-template name="button">
      <xsl:with-param name="letters" select="substring-after($letters, $letter)"/>
    </xsl:call-template>
    </xsl:if>
  </xsl:template>
  
  <xsl:template match="show-messages">
    <pfx:checkmessage>
      <pfx:messageloop>
        <div>
          <xsl:attribute name="class">{$pfx_class}</xsl:attribute>
          <pfx:scode/>
        </div>
      </pfx:messageloop>
    </pfx:checkmessage>
  </xsl:template>
  
  <xsl:template match="navibutton">
    <pfx:button page="{@page}"><pfx:include href="txt/pages/{@page}.xml" part="title"/></pfx:button>
  </xsl:template>
  
  <xsl:template name="highscoreloop">
    <xsl:param name="count" select="1"/>
    <xsl:if test="$count > 0">
      <tr>
        <ixsl:attribute name="class">
          <ixsl:choose>
            <ixsl:when test="$count mod 2 = 1">odd</ixsl:when>
            <ixsl:otherwise>even</ixsl:otherwise>
          </ixsl:choose>
        </ixsl:attribute>
        <td>-</td>
        <td>-</td>
        <td align="right">-</td>
        <td align="right">-</td>
      </tr>
      <xsl:call-template name="highscoreloop">
        <xsl:with-param name="count" select="$count - 1"/>
      </xsl:call-template>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>
