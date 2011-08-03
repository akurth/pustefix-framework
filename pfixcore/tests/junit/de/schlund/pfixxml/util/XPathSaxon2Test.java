/*
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

package de.schlund.pfixxml.util;

import javax.xml.parsers.DocumentBuilderFactory;

import net.sf.saxon.dom.DocumentBuilderFactoryImpl;
import net.sf.saxon.dom.NodeOverNodeInfo;
import net.sf.saxon.om.NodeInfo;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class XPathSaxon2Test extends XPathTest {

    @Override
    protected Document createDOM(String xml) throws Exception {
        DocumentBuilderFactory dbf=new DocumentBuilderFactoryImpl();
        return createDOM(dbf,xml);
    }
    
    @Override
    protected void checkNodeEquality(Node node1, Node node2) {
        NodeInfo info1=((NodeOverNodeInfo)node1).getUnderlyingNodeInfo();
        NodeInfo info2=((NodeOverNodeInfo)node2).getUnderlyingNodeInfo();
        assertSame(info1,info2);
    }
    
}