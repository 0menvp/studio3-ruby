/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.erb.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.aptana.editor.erb.RHTMLParserTest;
import com.aptana.editor.erb.RHTMLSourcePartitionScannerTest;

public class AllTests
{

	public static Test suite()
	{
		TestSuite suite = new TestSuite("Tests for com.aptana.editor.erb"); //$NON-NLS-1$
		// $JUnit-BEGIN$
		suite.addTestSuite(RHTMLSourcePartitionScannerTest.class);
		suite.addTestSuite(RHTMLParserTest.class);
		suite.addTest(com.aptana.editor.erb.html.AllTests.suite());
		suite.addTest(com.aptana.editor.erb.xml.AllTests.suite());
		// $JUnit-END$
		return suite;
	}
}
