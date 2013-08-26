//
//  ========================================================================
//  Copyright (c) 1995-2013 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.start;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.CollationKey;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Represents a Module metadata, as defined in Jetty.
 */
public class Module extends TextFile
{
    public static class NameComparator implements Comparator<Module>
    {
        private Collator collator = Collator.getInstance();

        @Override
        public int compare(Module o1, Module o2)
        {
            // by name (not really needed, but makes for predictable test cases)
            CollationKey k1 = collator.getCollationKey(o1.name);
            CollationKey k2 = collator.getCollationKey(o2.name);
            return k1.compareTo(k2);
        }
    }
    
    public static class DepthComparator implements Comparator<Module>
    {
        private Collator collator = Collator.getInstance();

        @Override
        public int compare(Module o1, Module o2)
        {
            // order by depth first.
            int diff = o1.depth - o2.depth;
            if (diff != 0)
            {
                return diff;
            }
            // then by name (not really needed, but makes for predictable test cases)
            CollationKey k1 = collator.getCollationKey(o1.name);
            CollationKey k2 = collator.getCollationKey(o2.name);
            return k1.compareTo(k2);
        }
    }

    /** The name of this Module */
    private String name;
    /** List of Modules, by name, that this Module depends on */
    private Set<String> parentNames;
    /** List of Modules, by name, that this Module optionally depend on */
    private Set<String> optionalParentNames;
    /** The Edges to parent modules */
    private Set<Module> parentEdges;
    /** The Edges to child modules */
    private Set<Module> childEdges;
    /** The depth of the module in the tree */
    private int depth = 0;
    /** List of xml configurations for this Module */
    private List<String> xmls;
    /** List of library options for this Module */
    private List<String> libs;
    /** List of downloads for this Module */
    private List<String> downloads;
    /** List of bootlib for this Module */
    private List<String> bootlibs;
    


    /** Is this Module enabled via start.jar command line, start.ini, or start.d/*.ini ? */
    private boolean enabled = false;
    /** List of sources that enabled this module */
    private final Set<String> sources = new HashSet<>();

    public Module(File file) throws FileNotFoundException, IOException
    {
        super(file);

        String name = file.getName();
        // Strip .ini
        name = Pattern.compile(".mod$",Pattern.CASE_INSENSITIVE).matcher(name).replaceFirst("");
    }

    public void addChildEdge(Module child)
    {
        if (childEdges.contains(child))
        {
            // already present, skip
            return;
        }
        this.childEdges.add(child);
    }

    public void addParentEdge(Module parent)
    {
        if (parentEdges.contains(parent))
        {
            // already present, skip
            return;
        }
        this.parentEdges.add(parent);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        Module other = (Module)obj;
        if (name == null)
        {
            if (other.name != null)
            {
                return false;
            }
        }
        else if (!name.equals(other.name))
        {
            return false;
        }
        return true;
    }

    public Set<Module> getChildEdges()
    {
        return childEdges;
    }

    public int getDepth()
    {
        return depth;
    }

    public List<String> getLibs()
    {
        return libs;
    }

    public String getName()
    {
        return name;
    }

    public Set<String> getOptionalParentNames()
    {
        return optionalParentNames;
    }

    public Set<Module> getParentEdges()
    {
        return parentEdges;
    }

    public Set<String> getParentNames()
    {
        return parentNames;
    }

    public List<String> getXmls()
    {
        return xmls;
    }
    
    public List<String> getDownloads()
    {
        return downloads;
    }
    
    public List<String> getBootLibs()
    {
        return bootlibs;
    }
    
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((name == null)?0:name.hashCode());
        return result;
    }

    @Override
    public void init()
    {
        String name = getFile().getName();

        // Strip .ini
        this.name = Pattern.compile(".mod$",Pattern.CASE_INSENSITIVE).matcher(name).replaceFirst("");

        this.parentNames = new HashSet<>();
        this.optionalParentNames = new HashSet<>();
        this.parentEdges = new HashSet<>();
        this.childEdges = new HashSet<>();
        this.xmls = new ArrayList<>();
        this.libs = new ArrayList<>();
        this.downloads = new ArrayList<>();
        this.bootlibs = new ArrayList<>();
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    @Override
    public void process(String line)
    {
        boolean handled = false;

        if (line == null)
        {

        }

        // has assignment
        int idx = line.indexOf('=');
        if (idx >= 0)
        {
            String key = line.substring(0,idx);
            String value = line.substring(idx + 1);

            switch (key.toUpperCase(Locale.ENGLISH))
            {
                case "DEPEND":
                    parentNames.add(value);
                    handled = true;
                    break;
                case "LIB":
                    libs.add(value);
                    handled = true;
                    break;
                case "OPTIONAL":
                    optionalParentNames.add(value);
                    handled = true;
                    break;
                case "DOWNLOAD":
                    downloads.add(value);
                    handled = true;
                    break;
                case "BOOTLIB":
                    bootlibs.add(value);
                    handled = true;
                    break;
            }
        }

        if (handled)
        {
            return; // no further processing of line needed
        }

        // Is it an XML line?
        if (FS.isXml(line))
        {
            xmls.add(line);
            return;
        }

        throw new IllegalArgumentException("Unrecognized Module Metadata line [" + line + "] in Module file [" + getFile() + "]");
    }

    public void setDepth(int depth)
    {
        this.depth = depth;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public void addSources(List<String> sources)
    {
        this.sources.addAll(sources);
    }
    
    public void clearSources()
    {
        this.sources.clear();
    }
    
    public Set<String> getSources()
    {
        return Collections.unmodifiableSet(sources);
    }
    
    @Override
    public String toString()
    {
        StringBuilder str = new StringBuilder();
        str.append("Module[").append(name);
        if (enabled)
        {
            str.append(",enabled");
        }
        str.append(']');
        return str.toString();
    }
    
}
