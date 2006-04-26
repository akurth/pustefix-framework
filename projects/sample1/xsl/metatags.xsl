<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
                xmlns:pfx="http://www.schlund.de/pustefix/core" 
                xmlns:ixsl="http://www.w3.org/1999/XSL/TransformOutputAlias" version="1.0">


  <xsl:template match="pagemsgs">
    <pfx:checkmessage>
      <br/>
      <div id="pagemsgs">
      <pfx:messageloop>
        <div>
          <xsl:attribute name="class">{$pfx_class}</xsl:attribute>
          <pfx:scode/>
        </div>
      </pfx:messageloop>
      </div>
      <br/>
    </pfx:checkmessage>
  </xsl:template>
  
  <xsl:template match="errormsg">
    <tr>
      <td colspan="2">
        <pfx:checkfield>
          <pfx:name>
            <xsl:choose>
              <xsl:when test="@name"><xsl:value-of select="@name"/></xsl:when>
              <xsl:otherwise><xsl:apply-templates select="./name/node()"/></xsl:otherwise>
            </xsl:choose>
          </pfx:name>
          <pfx:error>
            <div class="errmsg">
              <pfx:scode/>
            </div>
          </pfx:error>
        </pfx:checkfield>
      </td>
    </tr>
  </xsl:template>

</xsl:stylesheet>
