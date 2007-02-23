/*******************************************************************************
 * Copyright (c) 2007 Cognos Incorporated, IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.equinox.internal.jsp.jasper;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

/**
 * Jasper requires that this classloader be an instance of URLClassLoader.
 * At runtime it uses the URLClassLoader's getURLs method to find jar files that are in turn searched for TLDs. In a webapp
 * these jar files would normally be located in WEB-INF/lib. In the OSGi context, this behaviour is provided by returning the
 * URLs of the jar files contained on the Bundle-ClassPath. Other than jar file tld resources this classloader is not used for
 * loading classes which should be done by the other contained class loaders.
 * 
 * The rest of the ClassLoader is as follows:
 * 1) Thread-ContextClassLoader (top - parent)
 * 2) Jasper Bundle
 * 3) The Bundle referenced at JSPServlet creation
 */
public class JspClassLoader extends URLClassLoader {

	private static final Bundle JASPERBUNDLE = Activator.getBundle(org.apache.jasper.servlet.JspServlet.class);

	public JspClassLoader(Bundle bundle) {
		super(new URL[0], new BundleProxyClassLoader(bundle, new BundleProxyClassLoader(JASPERBUNDLE, Thread.currentThread().getContextClassLoader())));
		addBundleClassPathJars(bundle);
		Bundle[] fragments = Activator.getFragments(bundle);
		if (fragments != null) {
			for (int i = 0; i < fragments.length; i++) {
				addBundleClassPathJars(fragments[i]);
			}
		}		
	}

	private void addBundleClassPathJars(Bundle bundle) {
		Dictionary headers = bundle.getHeaders();
		String classPath = (String) headers.get(Constants.BUNDLE_CLASSPATH);
		if (classPath != null) {
			StringTokenizer tokenizer = new StringTokenizer(classPath, ","); //$NON-NLS-1$
			while (tokenizer.hasMoreTokens()) {
				String candidate = tokenizer.nextToken().trim();
				if (candidate.endsWith(".jar")) { //$NON-NLS-1$
					URL entry = bundle.getEntry(candidate);
					if (entry != null) {
						URL jarEntryURL;
						try {
							jarEntryURL = new URL("jar:" + entry.toString() + "!/"); //$NON-NLS-1$ //$NON-NLS-2$
							super.addURL(jarEntryURL);
						} catch (MalformedURLException e) {
							// TODO should log this.
						}
					}
				}
			}
		}
	}

	// Classes should "not" be loaded by this classloader from the URLs - it is just used for TLD resource discovery.
	protected Class findClass(String name) throws ClassNotFoundException {
		throw new ClassNotFoundException(name);
	}
}