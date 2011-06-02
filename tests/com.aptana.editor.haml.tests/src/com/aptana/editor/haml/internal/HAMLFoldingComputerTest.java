package com.aptana.editor.haml.internal;

import java.util.Collection;
import java.util.Map;

import junit.framework.TestCase;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;

import com.aptana.editor.common.text.reconciler.IFoldingComputer;

@SuppressWarnings("nls")
public class HAMLFoldingComputerTest extends TestCase
{

	private IFoldingComputer folder;

	@Override
	protected void tearDown() throws Exception
	{
		folder = null;
		super.tearDown();
	}

	public void testFoldingByIndent() throws Exception
	{
		String src = "!!! Strict\n" + //
				"%html \n" + // fold_start 1
				"  %head \n" + // fold_start 2
				"    %meta{ :http-equiv => 'Content-Type', :content => 'text/html;charset=utf-8' } \n" + //
				"    %title= $title_for_layout \n" + //
				"  %body \n" + // fold_start 3
				"    #header \n" + // fold_start 4
				"      %h1 hello \n" + //
				"    #content= $content_for_layout \n" + //
				"    #footer \n" + // fold_start 5
				"      %span.author John Q. Caker"; //
		folder = createFolder(src);
		Map<ProjectionAnnotation, Position> annotations = folder.emitFoldingRegions(false, new NullProgressMonitor());
		Collection<Position> positions = annotations.values();
		assertEquals(5, positions.size());
		assertTrue(positions.contains(new Position(11, 249)));
		assertTrue(positions.contains(new Position(18, 123)));
		assertTrue(positions.contains(new Position(141, 119)));
		assertTrue(positions.contains(new Position(150, 30)));
		assertTrue(positions.contains(new Position(215, 45)));
	}

	public void testFoldingWithLargeIndentShift() throws Exception
	{
		String src = "!!! Strict\n" + //
				"%html \n" + // fold_start 1
				"  %body \n" + // fold_start 2
				"    #header \n" + // fold_start 3
				"      %div\n" + // fold_start 4
				"        %h1 hello \n" + //
				"    #content= $content_for_layout \n" + //
				"    #footer \n" + // fold_start 5
				"      %span.author John Q. Caker"; //
		folder = createFolder(src);
		Map<ProjectionAnnotation, Position> annotations = folder.emitFoldingRegions(false, new NullProgressMonitor());
		Collection<Position> positions = annotations.values();
		assertEquals(5, positions.size());
		assertTrue(positions.contains(new Position(11, 139)));
		assertTrue(positions.contains(new Position(18, 132)));
		assertTrue(positions.contains(new Position(27, 43)));
		assertTrue(positions.contains(new Position(40, 30)));
		assertTrue(positions.contains(new Position(105, 45)));
	}

	protected IFoldingComputer createFolder(String src)
	{
		folder = new HAMLFoldingComputer(null, new Document(src))
		{
			@Override
			protected int getTabSize()
			{
				return 4;
			}
		};
		return folder;
	}
}
