<?xml version="1.0" encoding="iso-8859-1"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:ext="xalan://de.schlund.pfixcore.util.XsltTransformer"
                >

  <xsl:param name="standalone">true</xsl:param>
  <xsl:param name="portbase"/>
  <xsl:param name="trusted"/>
  <xsl:include href="create_lib.xsl"/>
  <xsl:output method="xml" encoding="ISO-8859-1" indent="yes"/>
  
  <xsl:variable name="debug">
    <xsl:apply-templates select="/projects/common/tomcat/debug/node()"/>
  </xsl:variable>

  <xsl:template match="projects">
    <xsl:variable name="adminport">
      <xsl:apply-templates select="/projects/common/tomcat/adminport/node()"/>
    </xsl:variable>
    <xsl:variable name="tomcat_defaulthost">
      <xsl:apply-templates select="/projects/common/tomcat/defaulthost/node()"/>
    </xsl:variable>
    <xsl:variable name="tomcat_jvmroute">
      <xsl:apply-templates select="/projects/common/tomcat/jvmroute/node()"/>
    </xsl:variable>
    <Server shutdown="SHUTDOWN">
      <xsl:attribute name="port">
        <xsl:choose>
          <xsl:when test="string($adminport) = ''">
            <xsl:value-of select="$portbase+5"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="$adminport"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
      <xsl:attribute name="debug"><xsl:value-of select="$debug"/></xsl:attribute>
      
      <Listener className="org.apache.catalina.mbeans.ServerLifecycleListener" debug="0"/>
      <Listener className="org.apache.catalina.mbeans.GlobalResourcesLifecycleListener" debug="0"/>
      <GlobalNamingResources>
        <Resource name="UserDatabase" auth="Container" type="org.apache.catalina.UserDatabase"
                  description="User database that can be updated and saved">
        </Resource>
        <ResourceParams name="UserDatabase">
          <parameter>
            <name>factory</name>
            <value>org.apache.catalina.users.MemoryUserDatabaseFactory</value>
          </parameter>
          <parameter>
            <name>pathname</name>
            <value>conf/tomcat-users.xml</value>
          </parameter>
        </ResourceParams>
      </GlobalNamingResources>

      <Service name="Catalina">
        <xsl:call-template name="create-connector"/>
        <Engine name="Catalina">
          <xsl:attribute name="debug"><xsl:value-of select="$debug"/></xsl:attribute>
          <xsl:attribute name="defaultHost">
            <xsl:value-of select="normalize-space($tomcat_defaulthost)"/>
          </xsl:attribute>
          <xsl:attribute name="jvmRoute">
            <xsl:value-of select="normalize-space($tomcat_jvmroute)"/>
          </xsl:attribute>
          <Logger className="org.apache.catalina.logger.FileLogger" prefix="catalina_log." suffix=".txt" timestamp="true"/>
          <xsl:apply-templates select="/projects/project"/>
        </Engine>
      </Service>
    </Server>
  </xsl:template>

  <xsl:template name="create-connector">
    <xsl:variable name="jkport">
      <xsl:apply-templates select="/projects/common/tomcat/connectorport/node()"/>
    </xsl:variable>
    <xsl:variable name="minprocessors">
      <xsl:apply-templates select="/projects/common/tomcat/minprocessors/node()"/>
    </xsl:variable>
    <xsl:variable name="maxprocessors">
      <xsl:apply-templates select="/projects/common/tomcat/maxprocessors/node()"/>
    </xsl:variable>
    <xsl:variable name="uriencoding">
    	<xsl:apply-templates select="/projects/common/tomcat/uriencoding/node()"/>
    </xsl:variable>
    <Connector enableLookups="false" acceptCount="100" maxThreads="150" minSpareThreads="25" maxSpareThreads="75" URIEncoding="UTF-8" protocol="AJP/1.3">		
      <xsl:attribute name="port">
        <xsl:choose>
          <xsl:when test="string($jkport) = ''">
            <xsl:value-of select="$portbase+9"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="$jkport"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
      
      <xsl:attribute name="debug"><xsl:value-of select="$debug"/></xsl:attribute>
      <xsl:if test="not(string($minprocessors)='')"><xsl:attribute name="minSpareThreads"><xsl:value-of select="$minprocessors"/></xsl:attribute></xsl:if>
      <xsl:if test="not(string($minprocessors)='')"><xsl:attribute name="maxSpareThreads"><xsl:value-of select="$minprocessors"/></xsl:attribute></xsl:if>
      <xsl:if test="not(string($maxprocessors)='')"><xsl:attribute name="maxThreads"><xsl:value-of select="$maxprocessors"/></xsl:attribute></xsl:if>
			<xsl:if test="not(string($uriencoding)='')"><xsl:attribute name="URIEncoding"><xsl:value-of select="$uriencoding"/></xsl:attribute></xsl:if>
    </Connector>
    <xsl:if test="$standalone = 'true'">
      <Connector maxThreads="150" minSpareThreads="25" maxSpareThreads="75" URIEncoding="UTF-8"
                 enableLookups="false" acceptCount="100"
                 connectionTimeout="20000"
                 disableUploadTimeout="true">
        <xsl:attribute name="debug"><xsl:value-of select="$debug"/></xsl:attribute>
        <xsl:attribute name="port"><xsl:value-of select="$portbase+80"/></xsl:attribute>
        <xsl:attribute name="redirectport"><xsl:value-of select="$portbase+443"/></xsl:attribute>
        <xsl:if test="not(string($uriencoding)='')"><xsl:attribute name="URIEncoding"><xsl:value-of select="$uriencoding"/></xsl:attribute></xsl:if>
      </Connector>
      <Connector maxThreads="150" minSpareThreads="25" maxSpareThreads="75" URIEncoding="UTF-8"
                 enableLookups="false" disableUploadTimeout="true"
                 acceptCount="100" debug="0" scheme="https" secure="true"
                 clientAuth="false" sslProtocol="TLS" keystoreFile="conf/keystore" keystorePass="secret">
        <xsl:attribute name="port"><xsl:value-of select="$portbase+443"/></xsl:attribute>
        <xsl:attribute name="debug"><xsl:value-of select="$debug"/></xsl:attribute>
        <xsl:if test="not(string($uriencoding)='')"><xsl:attribute name="URIEncoding"><xsl:value-of select="$uriencoding"/></xsl:attribute></xsl:if>
      </Connector>
    </xsl:if>
  </xsl:template>

  <xsl:template match="project">
    <xsl:variable name="active">
      <xsl:apply-templates select="active/node()"/>
    </xsl:variable>
    <xsl:if test="normalize-space($active) = 'true'">
      <Host xmlValidation="true" unpackWARs="false" autoDeploy="false">
        <xsl:attribute name="debug"><xsl:value-of select="$debug"/></xsl:attribute>
        <xsl:attribute name="name">
          <xsl:apply-templates select="servername/node()"/>
        </xsl:attribute>
        <xsl:call-template name="create_tomcat_aliases">
          <xsl:with-param name="all_aliases"><xsl:apply-templates select="serveralias/node()"/></xsl:with-param>
        </xsl:call-template>
        <Valve className="org.apache.catalina.valves.AccessLogValve"
               directory="logs" prefix="access_log." suffix=".txt" pattern="%h %l %u %t &quot;%r&quot; %s %b %D"/>
        
        <Logger className="org.apache.catalina.logger.FileLogger"
                directory="logs" prefix="log." suffix=".txt"	timestamp="true"/>
        
        <xsl:call-template name="create_context_list">
          <xsl:with-param name="defpath">/xml</xsl:with-param>
        </xsl:call-template>
      </Host>
    </xsl:if>
  </xsl:template>

  <xsl:template name="create_context_list">
    <xsl:param name="defpath"/>
    <xsl:call-template name="create_context">
      <xsl:with-param name="cookies">false</xsl:with-param>
      <xsl:with-param name="path"><xsl:value-of select="$defpath"/></xsl:with-param>
      <xsl:with-param name="docBase">webapps/<xsl:apply-templates select="@name"/></xsl:with-param>
    </xsl:call-template>
    <xsl:if test="$standalone = 'true'">
      <xsl:if test="documentroot">
        <xsl:variable name="abs_path"><xsl:apply-templates select="documentroot/node()"/></xsl:variable>
        <xsl:choose>
          <xsl:when test="ext:exists($abs_path)">
            <xsl:call-template name="create_context">
              <xsl:with-param name="path">/</xsl:with-param>
              <xsl:with-param name="docBase"><xsl:value-of select="$abs_path"/></xsl:with-param>
              <xsl:with-param name="cookies">false</xsl:with-param>
            </xsl:call-template>
          </xsl:when>
          <xsl:otherwise>
            <xsl:message>CAUTION: documentroot does not exist: <xsl:value-of select="$abs_path"/></xsl:message>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:if>
      <xsl:apply-templates select="passthrough"/>
      <xsl:apply-templates select="/projects/common/apache/passthrough"/>
      <xsl:comment><![CDATA[ comment-in if you want Tomcat Manager and Tomcat Admin: 
      <Context path="/manager" debug="0" privileged="true" docBase="server/webapps/manager">
        <Realm className="org.apache.catalina.realm.UserDatabaseRealm" debug="0" resourceName="UserDatabase"/>
        <Valve className="org.apache.catalina.valves.RemoteAddrValve">
          <xsl:attribute name="allow"><xsl:value-of select="$trusted"/></xsl:attribute>
        </Valve>
        <Logger className="org.apache.catalina.logger.FileLogger"
                prefix="localhost_manager_log." suffix=".txt" timestamp="true"/>
      </Context>
      <Context path="/admin" debug="0" privileged="true" docBase="server/webapps/admin">
        <Realm className="org.apache.catalina.realm.UserDatabaseRealm" debug="0" resourceName="UserDatabase"/>
        <Valve className="org.apache.catalina.valves.RemoteAddrValve">
          <xsl:attribute name="allow"><xsl:value-of select="$trusted"/></xsl:attribute>
        </Valve>     
        <Logger className="org.apache.catalina.logger.FileLogger"
                prefix="localhost_admin_log." suffix=".txt" timestamp="true"/>
      </Context>]]>
      </xsl:comment>
    </xsl:if>
  </xsl:template>

  <xsl:template match="passthrough">
    <xsl:variable name="rel_path" select="normalize-space(./node())"/>
    <xsl:variable name="abs_path" select="concat($docroot, '/', $rel_path)"/>
    <xsl:choose>
      <xsl:when test="ext:exists($abs_path)">
        <xsl:call-template name="create_context">
          <xsl:with-param name="path">/<xsl:value-of select="$rel_path"/></xsl:with-param>
          <xsl:with-param name="docBase">../../<xsl:value-of select="$rel_path"/></xsl:with-param>
          <xsl:with-param name="cookies">false</xsl:with-param>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:message>CAUTION: passthrough path not found: <xsl:value-of select="$abs_path"/></xsl:message>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="create_context">
    <xsl:param name="path"/>
    <xsl:param name="docBase"/>
    <xsl:param name="cookies"/>
    <Context crossContext="true">
      <xsl:attribute name="cookies"><xsl:value-of select="$cookies"/></xsl:attribute>
      <xsl:attribute name="path"><xsl:value-of select="$path"/></xsl:attribute>
      <xsl:attribute name="docBase"><xsl:value-of select="$docBase"/></xsl:attribute>
      <xsl:attribute name="debug"><xsl:value-of select="$debug"/></xsl:attribute>
      <!-- switch off session serialization -->
      <Manager  pathname=""/>
    </Context>
  </xsl:template>
  
  <xsl:template name="create_tomcat_aliases">
    <xsl:param name="all_aliases"/>
    <xsl:variable name="alias_string" select="normalize-space($all_aliases)"/>
    <xsl:choose>
      <xsl:when test="not(contains($alias_string, ' '))">
        <Alias><xsl:value-of select="$alias_string"/></Alias>
      </xsl:when>
      <xsl:otherwise>
        <Alias><xsl:value-of select="substring-before($alias_string,' ')"/></Alias>
        <xsl:call-template name="create_tomcat_aliases">
          <xsl:with-param name="all_aliases" select="substring-after($alias_string, ' ')"/>
        </xsl:call-template> 
      </xsl:otherwise> 
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>

