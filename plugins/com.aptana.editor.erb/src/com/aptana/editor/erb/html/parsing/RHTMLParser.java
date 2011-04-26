/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.erb.html.parsing;

import java.io.IOException;

import beaver.Symbol;

import com.aptana.editor.common.parsing.CompositeParser;
import com.aptana.editor.erb.parsing.lexer.ERBTokens;
import com.aptana.editor.html.IHTMLConstants;
import com.aptana.editor.ruby.IRubyConstants;
import com.aptana.parsing.IParseState;
import com.aptana.parsing.ast.IParseNode;
import com.aptana.parsing.ast.ParseNode;
import com.aptana.parsing.ast.ParseRootNode;
import com.aptana.ruby.core.IRubyScript;

public class RHTMLParser extends CompositeParser
{

	public RHTMLParser()
	{
		super(new RHTMLParserScanner(), IHTMLConstants.CONTENT_TYPE_HTML);
	}

	@Override
	protected IParseNode processEmbeddedlanguage(IParseState parseState) throws Exception
	{
		String source = new String(parseState.getSource());
		int startingOffset = parseState.getStartingOffset();
		IParseNode root = null;

		advance();
		short id = getCurrentSymbol().getId();
		while (id != ERBTokens.EOF)
		{
			// only cares about ruby tokens
			switch (id)
			{
				case ERBTokens.RUBY:
					if (root == null)
					{
						root = new ParseRootNode(IRubyConstants.CONTENT_TYPE_RUBY, new ParseNode[0], startingOffset,
								startingOffset + source.length());
					}
					processRubyBlock(root);
					break;
			}
			advance();
			id = getCurrentSymbol().getId();
		}
		return root;
	}

	private void processRubyBlock(IParseNode root) throws IOException, Exception
	{
		Symbol startTag = getCurrentSymbol();
		advance();

		// finds the entire ruby block
		int start = getCurrentSymbol().getStart();
		int end = start;
		short id = getCurrentSymbol().getId();
		while (id != ERBTokens.RUBY_END && id != ERBTokens.EOF)
		{
			end = getCurrentSymbol().getEnd();
			advance();
			id = getCurrentSymbol().getId();
		}

		IParseNode result = getParseResult(IRubyConstants.CONTENT_TYPE_RUBY, start, end);
		if (result != null)
		{
			Symbol endTag = getCurrentSymbol();
			ERBScript erb = new ERBScript((IRubyScript) result, startTag.value.toString(), endTag.value.toString());
			erb.setLocation(startTag.getStart(), endTag.getEnd());
			root.addChild(erb);
		}
	}
}
