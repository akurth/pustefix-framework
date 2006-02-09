<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
                xmlns:pfx="http://www.schlund.de/pustefix/core" 
                xmlns:ixsl="http://www.w3.org/1999/XSL/TransformOutputAlias" version="1.0">
<!-- 
	Here you can say how the page should look like. The relation is declared
	in the depend.xml.in
 -->

  <xsl:template match="basictext">
    <div style="border: 1px black solid; width: 200px">
      <xsl:apply-templates/>
    </div>
  </xsl:template>


</xsl:stylesheet>
