/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.ruby;

import java.io.StringReader;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.jrubyparser.Parser.NullWarnings;
import org.jrubyparser.lexer.LexerSource;
import org.jrubyparser.lexer.SyntaxException;
import org.jrubyparser.parser.ParserConfiguration;
import org.jrubyparser.parser.ParserSupport;
import org.jrubyparser.parser.Ruby18Parser;
import org.jrubyparser.parser.RubyParser;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;

import com.aptana.editor.common.text.RubyRegexpAutoIndentStrategy;
import com.aptana.editor.ruby.preferences.IPreferenceConstants;

/**
 * Special subclass of auto indenter that will auto-close methods/blocks/classes/types with 'end" when needed.
 * 
 * @author cwilliams
 */
class RubyAutoIndentStrategy extends RubyRegexpAutoIndentStrategy
{
	private final Pattern openBlockPattern = Pattern.compile(".*[\\S].*do[\\w|\\s]*"); //$NON-NLS-1$
	private static final String BLOCK_CLOSER = "end"; //$NON-NLS-1$
	private static boolean shouldAutoIndent;
	private static IPreferenceChangeListener autoIndentPrefChangeListener;

	static
	{
		RubyAutoIndentStrategy.autoIndentPrefChangeListener = new IPreferenceChangeListener()
		{

			public void preferenceChange(PreferenceChangeEvent event)
			{
				if (IPreferenceConstants.RUBY_AUTO_INDENT.equals(event.getKey()))
					updateAutoUpdatePreference();

			}
		};
		new InstanceScope().getNode(RubyEditorPlugin.PLUGIN_ID).addPreferenceChangeListener(
				autoIndentPrefChangeListener);

		RubyEditorPlugin.getDefault().getBundle().getBundleContext().addBundleListener(new BundleListener()
		{

			public void bundleChanged(BundleEvent event)
			{
				if (event.getType() == BundleEvent.STOPPING)
					new InstanceScope().getNode(RubyEditorPlugin.PLUGIN_ID).removePreferenceChangeListener(
							autoIndentPrefChangeListener);
			}
		});

	}

	RubyAutoIndentStrategy(String contentType, SourceViewerConfiguration svc, ISourceViewer sourceViewer)
	{
		super(contentType, svc, sourceViewer);
		updateAutoUpdatePreference();
	}

	@Override
	protected boolean autoIndent(IDocument d, DocumentCommand c)
	{
		if (!super.autoIndent(d, c))
		{
			try
			{
				int p = (c.offset == d.getLength() ? c.offset - 1 : c.offset);
				int line = d.getLineOfOffset(p);
				IRegion currentLineRegion = d.getLineInformation(line);
				int startOfCurrentLine = currentLineRegion.getOffset();
				String lineString = d.get(startOfCurrentLine, c.offset - startOfCurrentLine);

				if (lineString.startsWith("=begin")) //$NON-NLS-1$
				{
					// TODO If doesn't start at beginning of line, move to first column?
					String indent = getIndentString();
					c.text += indent;
					c.caretOffset = c.offset + indent.length();
					c.shiftsCaret = false;
					c.text += TextUtilities.getDefaultLineDelimiter(d) + "=end"; //$NON-NLS-1$
					return true;
				}
			}
			catch (BadLocationException e)
			{
				// ignore
			}
			return false;
		}

		// Ruble says we're at an indentation point, this is where we should look for closing with "end"
		try
		{
			int p = (c.offset == d.getLength() ? c.offset - 1 : c.offset);
			int line = d.getLineOfOffset(p);
			IRegion currentLineRegion = d.getLineInformation(line);
			int startOfCurrentLine = currentLineRegion.getOffset();
			String trimmed = getTrimmedLine(d, startOfCurrentLine, c.offset);

			if (trimmed.equals("=begin")) //$NON-NLS-1$
			{
				// TODO If doesn't start at beginning of line, move to first column
				String indent = getIndentString();
				c.text += indent;
				c.caretOffset = c.offset + indent.length();
				c.shiftsCaret = false;
				c.text += TextUtilities.getDefaultLineDelimiter(d) + "=end"; //$NON-NLS-1$
			}
			// insert closing "end" on new line after an unclosed block
			if (closeBlock() && unclosedBlock(d, trimmed, c.offset))
			{
				String previousLineIndent = getAutoIndentAfterNewLine(d, c);
				c.text += TextUtilities.getDefaultLineDelimiter(d) + previousLineIndent + BLOCK_CLOSER;
			}
		}
		catch (BadLocationException e)
		{
			// ignore
		}
		return true;
	}

	private boolean unclosedBlock(IDocument d, String trimmed, int offset)
	{
		// FIXME wow is this ugly! There has to be an easier way to tell if there's an unclosed block besides parsing
		// and catching a syntaxError!
		if (!atStartOfBlock(trimmed))
		{
			return false;
		}

		ParserConfiguration config = new ParserConfiguration();
		ParserSupport support = new ParserSupport();
		support.setConfiguration(config);
		support.setWarnings(new NullWarnings());
		RubyParser parser = new Ruby18Parser(support);
		LexerSource lexerSource = null;
		try
		{
			lexerSource = LexerSource.getSource("", new StringReader(d.get()), config); //$NON-NLS-1$
			parser.parse(config, lexerSource);
		}
		catch (SyntaxException e)
		{
			if (e.getPid() != SyntaxException.PID.GRAMMAR_ERROR)
			{
				return false;
			}
			try
			{
				StringBuffer buffer = new StringBuffer(d.get());
				buffer.insert(offset, TextUtilities.getDefaultLineDelimiter(d) + BLOCK_CLOSER);
				lexerSource = LexerSource.getSource("", new StringReader(buffer.toString()), config); //$NON-NLS-1$
				parser.parse(config, lexerSource);
			}
			catch (SyntaxException syntaxException)
			{
				return false;
			}
			return true;
		}
		return false;
	}

	private String getTrimmedLine(IDocument d, int start, int offset) throws BadLocationException
	{
		return d.get(start, offset - start).trim();
	}

	@SuppressWarnings("nls")
	private boolean atStartOfBlock(String line)
	{
		return line.startsWith("class ") || line.startsWith("if ") || line.startsWith("while ")
				|| line.startsWith("module ") || line.startsWith("unless ") || line.startsWith("def ")
				|| line.equals("begin") || line.startsWith("case ") || line.startsWith("for ")
				|| openBlockPattern.matcher(line).matches();
	}

	private boolean closeBlock()
	{
		// TODO Set up a pref value for user to turn this behavior off?
		return true;
	}

	protected boolean shouldAutoIndent()
	{
		return shouldAutoIndent;
	}

	private static void updateAutoUpdatePreference()
	{
		shouldAutoIndent = RubyEditorPlugin.getDefault().getPreferenceStore()
				.getBoolean(IPreferenceConstants.RUBY_AUTO_INDENT);
	}

}
