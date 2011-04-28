/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */

package com.aptana.editor.ruby;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.ITokenScanner;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;

import com.aptana.editor.common.CommonEditorPlugin;
import com.aptana.editor.common.IPartitioningConfiguration;
import com.aptana.editor.common.ISourceViewerConfiguration;
import com.aptana.editor.common.scripting.IContentTypeTranslator;
import com.aptana.editor.common.scripting.QualifiedContentType;
import com.aptana.editor.common.text.rules.CommentScanner;
import com.aptana.editor.common.text.rules.ISubPartitionScanner;
import com.aptana.editor.common.text.rules.PartitionerSwitchingIgnoreRule;
import com.aptana.editor.common.text.rules.SubPartitionScanner;
import com.aptana.editor.common.text.rules.ThemeingDamagerRepairer;
import com.aptana.ruby.core.IRubyConstants;

/**
 * @author Max Stepanov
 * @author Michael Xia
 */
public class RubySourceConfiguration implements IPartitioningConfiguration, ISourceViewerConfiguration
{

	// FIXME Move out the translations strings as constants in IRubyConstants
	public static final String PREFIX = "__rb_"; //$NON-NLS-1$
	public static final String DEFAULT = "__rb" + IDocument.DEFAULT_CONTENT_TYPE; //$NON-NLS-1$
	public static final String SINGLE_LINE_COMMENT = PREFIX + "singleline_comment"; //$NON-NLS-1$
	public static final String MULTI_LINE_COMMENT = PREFIX + "multiline_comment"; //$NON-NLS-1$
	public static final String REGULAR_EXPRESSION = PREFIX + "regular_expression"; //$NON-NLS-1$
	public static final String COMMAND = PREFIX + "command"; //$NON-NLS-1$
	public static final String STRING_SINGLE = PREFIX + "string_single"; //$NON-NLS-1$
	public static final String STRING_DOUBLE = PREFIX + "string_double"; //$NON-NLS-1$

	public static final String[] CONTENT_TYPES = new String[] { DEFAULT, SINGLE_LINE_COMMENT, MULTI_LINE_COMMENT,
			REGULAR_EXPRESSION, COMMAND, STRING_SINGLE, STRING_DOUBLE };

	private static final String[][] TOP_CONTENT_TYPES = new String[][] { { IRubyConstants.CONTENT_TYPE_RUBY } };

	private final IPredicateRule[] partitioningRules = new IPredicateRule[] {
			new PartitionerSwitchingIgnoreRule(new EndOfLineRule("#", new Token(SINGLE_LINE_COMMENT))), //$NON-NLS-1$
			new PartitionerSwitchingIgnoreRule(new MultiLineRule("=begin", "=end", new Token(MULTI_LINE_COMMENT), (char) 0, true)), //$NON-NLS-1$ //$NON-NLS-2$
			new SingleLineRule("/", "/", new Token(REGULAR_EXPRESSION), '\\'), //$NON-NLS-1$ //$NON-NLS-2$
			new SingleLineRule("\"", "\"", new Token(STRING_DOUBLE), '\\'), //$NON-NLS-1$ //$NON-NLS-2$
			new SingleLineRule("\'", "\'", new Token(STRING_SINGLE), '\\') }; //$NON-NLS-1$ //$NON-NLS-2$

	private RubyCodeScanner codeScanner;
	private RuleBasedScanner singleLineCommentScanner;
	private RuleBasedScanner multiLineCommentScanner;
	private RubyRegexpScanner regexpScanner;
	private RuleBasedScanner commandScanner;
	private RuleBasedScanner singleQuotedStringScanner;
	private RuleBasedScanner doubleQuotedStringScanner;

	private static RubySourceConfiguration instance;

	static
	{
		IContentTypeTranslator c = CommonEditorPlugin.getDefault().getContentTypeTranslator();
		c.addTranslation(new QualifiedContentType(IRubyConstants.CONTENT_TYPE_RUBY), new QualifiedContentType(
				"source.ruby.rails")); //$NON-NLS-1$ // FIXME Should just be source.ruby! Rails bundle should contribute the more specific scope
		c.addTranslation(new QualifiedContentType(STRING_SINGLE), new QualifiedContentType(IRubyConstants.SINGLE_QUOTED_STRING_SCOPE));
		c.addTranslation(new QualifiedContentType(STRING_DOUBLE), new QualifiedContentType(IRubyConstants.DOUBLE_QUOTED_STRING_SCOPE));
		c.addTranslation(new QualifiedContentType(SINGLE_LINE_COMMENT), new QualifiedContentType(
				IRubyConstants.LINE_COMMENT_SCOPE));
		c.addTranslation(new QualifiedContentType(MULTI_LINE_COMMENT), new QualifiedContentType(
				IRubyConstants.BLOCK_COMMENT_SCOPE));
		c.addTranslation(new QualifiedContentType(REGULAR_EXPRESSION), new QualifiedContentType(
				"string.regexp.classic.ruby")); //$NON-NLS-1$
		c.addTranslation(new QualifiedContentType(COMMAND), new QualifiedContentType(
				"string.interpolated.ruby")); //$NON-NLS-1$
	}
	
	private RubySourceConfiguration() {
	}

	public static RubySourceConfiguration getDefault()
	{
		if (instance == null)
		{
			instance = new RubySourceConfiguration();
		}
		return instance;
	}

	/**
	 * @see com.aptana.editor.common.IPartitioningConfiguration#getContentTypes()
	 */
	public String[] getContentTypes()
	{
		return CONTENT_TYPES;
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.common.ITopContentTypesProvider#getTopContentTypes()
	 */
	public String[][] getTopContentTypes()
	{
		return TOP_CONTENT_TYPES;
	}

	/**
	 * @see com.aptana.editor.common.IPartitioningConfiguration#getPartitioningRules()
	 */
	public IPredicateRule[] getPartitioningRules()
	{
		return partitioningRules;
	}

	/**
	 * @see com.aptana.editor.common.IPartitioningConfiguration#createSubPartitionScanner()
	 */
	public ISubPartitionScanner createSubPartitionScanner()
	{
		return new SubPartitionScanner(partitioningRules, CONTENT_TYPES, new Token(DEFAULT));
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.common.IPartitioningConfiguration#getDocumentDefaultContentType()
	 */
	public String getDocumentContentType(String contentType)
	{
		if (contentType.startsWith(PREFIX))
		{
			return IRubyConstants.CONTENT_TYPE_RUBY;
		}
		return null;
	}

	/**
	 * @see com.aptana.editor.common.ISourceViewerConfiguration#setupPresentationReconciler(org.eclipse.jface.text.presentation.PresentationReconciler,
	 *      org.eclipse.jface.text.source.ISourceViewer)
	 */
	public void setupPresentationReconciler(PresentationReconciler reconciler, ISourceViewer sourceViewer)
	{
		DefaultDamagerRepairer dr = new ThemeingDamagerRepairer(getCodeScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		reconciler.setDamager(dr, DEFAULT);
		reconciler.setRepairer(dr, DEFAULT);

		dr = new ThemeingDamagerRepairer(getSingleLineCommentScanner());
		reconciler.setDamager(dr, RubySourceConfiguration.SINGLE_LINE_COMMENT);
		reconciler.setRepairer(dr, RubySourceConfiguration.SINGLE_LINE_COMMENT);

		dr = new ThemeingDamagerRepairer(getMultiLineCommentScanner());
		reconciler.setDamager(dr, RubySourceConfiguration.MULTI_LINE_COMMENT);
		reconciler.setRepairer(dr, RubySourceConfiguration.MULTI_LINE_COMMENT);

		dr = new ThemeingDamagerRepairer(getRegexpScanner());
		reconciler.setDamager(dr, RubySourceConfiguration.REGULAR_EXPRESSION);
		reconciler.setRepairer(dr, RubySourceConfiguration.REGULAR_EXPRESSION);

		dr = new ThemeingDamagerRepairer(getCommandScanner());
		reconciler.setDamager(dr, RubySourceConfiguration.COMMAND);
		reconciler.setRepairer(dr, RubySourceConfiguration.COMMAND);

		dr = new ThemeingDamagerRepairer(getSingleQuotedStringScanner());
		reconciler.setDamager(dr, RubySourceConfiguration.STRING_SINGLE);
		reconciler.setRepairer(dr, RubySourceConfiguration.STRING_SINGLE);

		dr = new ThemeingDamagerRepairer(getDoubleQuotedStringScanner());
		reconciler.setDamager(dr, RubySourceConfiguration.STRING_DOUBLE);
		reconciler.setRepairer(dr, RubySourceConfiguration.STRING_DOUBLE);
	}

	private ITokenScanner getCodeScanner()
	{
		if (codeScanner == null)
		{
			codeScanner = new RubyCodeScanner();
		}
		return codeScanner;
	}

	private ITokenScanner getMultiLineCommentScanner()
	{
		if (multiLineCommentScanner == null)
		{
			multiLineCommentScanner = new CommentScanner(getToken(IRubyConstants.BLOCK_COMMENT_SCOPE));
		}
		return multiLineCommentScanner;
	}

	private ITokenScanner getSingleLineCommentScanner()
	{
		if (singleLineCommentScanner == null)
		{
			singleLineCommentScanner = new CommentScanner(getToken(IRubyConstants.LINE_COMMENT_SCOPE));
		}
		return singleLineCommentScanner;
	}

	private ITokenScanner getRegexpScanner()
	{
		if (regexpScanner == null)
		{
			regexpScanner = new RubyRegexpScanner();
		}
		return regexpScanner;
	}

	private ITokenScanner getCommandScanner()
	{
		if (commandScanner == null)
		{
			commandScanner = new RuleBasedScanner();
			commandScanner.setDefaultReturnToken(getToken("string.interpolated.ruby")); //$NON-NLS-1$
		}
		return commandScanner;
	}

	private ITokenScanner getSingleQuotedStringScanner()
	{
		if (singleQuotedStringScanner == null)
		{
			singleQuotedStringScanner = new RuleBasedScanner();
			singleQuotedStringScanner.setDefaultReturnToken(getToken(IRubyConstants.SINGLE_QUOTED_STRING_SCOPE));
		}
		return singleQuotedStringScanner;
	}

	private ITokenScanner getDoubleQuotedStringScanner()
	{
		if (doubleQuotedStringScanner == null)
		{
			doubleQuotedStringScanner = new RuleBasedScanner();
			doubleQuotedStringScanner.setDefaultReturnToken(getToken(IRubyConstants.DOUBLE_QUOTED_STRING_SCOPE));
		}
		return doubleQuotedStringScanner;
	}

	private IToken getToken(String tokenName)
	{
		return new Token(tokenName);
	}
}
