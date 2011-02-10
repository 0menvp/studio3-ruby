/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.ruby.parsing.ast;

import com.aptana.editor.ruby.core.IRubyElement;

public class RubyClassVariable extends RubyField
{

	public RubyClassVariable(String name, int start, int nameStart, int nameEnd)
	{
		super(name, start, nameStart, nameEnd);
	}

	@Override
	public short getNodeType()
	{
		return IRubyElement.CLASS_VAR;
	}
}
