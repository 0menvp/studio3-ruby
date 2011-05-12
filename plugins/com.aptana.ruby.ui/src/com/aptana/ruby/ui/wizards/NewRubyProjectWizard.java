/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.ruby.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.ide.undo.CreateProjectOperation;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;
import org.eclipse.ui.statushandlers.IStatusAdapterConstants;
import org.eclipse.ui.statushandlers.StatusAdapter;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

import com.aptana.core.build.UnifiedBuilder;
import com.aptana.git.ui.CloneJob;
import com.aptana.ruby.core.RubyProjectNature;
import com.aptana.ruby.ui.RubyUIPlugin;

/**
 * TODO Extract common code between this and our web wizard!
 * 
 * @author cwilliams
 */
public class NewRubyProjectWizard extends BasicNewResourceWizard implements IExecutableExtension
{

	private WizardNewRubyProjectCreationPage mainPage;

	// cache of newly-created project
	private IProject newProject;

	/**
	 * The config element which declares this wizard.
	 */
	private IConfigurationElement configElement;

	public NewRubyProjectWizard()
	{
		IDialogSettings workbenchSettings = RubyUIPlugin.getDefault().getDialogSettings();
		IDialogSettings section = workbenchSettings.getSection("BasicNewProjectResourceWizard");//$NON-NLS-1$
		if (section == null)
		{
			section = workbenchSettings.addNewSection("BasicNewProjectResourceWizard");//$NON-NLS-1$
		}
		setDialogSettings(section);
	}

	protected IProject getProject()
	{
		return newProject;
	}

	@Override
	public void addPages()
	{
		super.addPages();

		mainPage = createMainPage();
		this.addPage(mainPage);
	}

	protected WizardNewRubyProjectCreationPage createMainPage()
	{
		WizardNewRubyProjectCreationPage mainPage = new WizardNewRubyProjectCreationPage("basicNewProjectPage"); //$NON-NLS-1$
		mainPage.setTitle(Messages.NewProject_title);
		mainPage.setDescription(Messages.NewRubyProject_description);
		return mainPage;
	}

	/*
	 * (non-Javadoc) Method declared on BasicNewResourceWizard.
	 */
	protected void initializeDefaultPageImageDescriptor()
	{
		ImageDescriptor desc = RubyUIPlugin.imageDescriptorFromPlugin(RubyUIPlugin.getPluginIdentifier(),
				"icons/newproj_wiz.png"); //$NON-NLS-1$
		setDefaultPageImageDescriptor(desc);
	}

	/*
	 * (non-Javadoc) Method declared on IWorkbenchWizard.
	 */
	public void init(IWorkbench workbench, IStructuredSelection currentSelection)
	{
		super.init(workbench, currentSelection);
		setNeedsProgressMonitor(true);
		setWindowTitle(Messages.NewRubyProject_windowTitle);
	}

	@Override
	public boolean performFinish()
	{
		createNewProject();

		if (newProject == null)
		{
			return false;
		}

		updatePerspective();
		selectAndReveal(newProject);

		return true;
	}

	/**
	 * Stores the configuration element for the wizard. The config element will be used in <code>performFinish</code> to
	 * set the result perspective.
	 */
	public void setInitializationData(IConfigurationElement cfig, String propertyName, Object data)
	{
		configElement = cfig;
	}

	/**
	 * Creates a new project resource with the selected name.
	 * <p>
	 * In normal usage, this method is invoked after the user has pressed Finish on the wizard; the enablement of the
	 * Finish button implies that all controls on the pages currently contain valid values.
	 * </p>
	 * <p>
	 * Note that this wizard caches the new project once it has been successfully created; subsequent invocations of
	 * this method will answer the same project resource without attempting to create it again.
	 * </p>
	 * 
	 * @return the created project resource, or <code>null</code> if the project was not created
	 */
	private IProject createNewProject()
	{
		// HACK I have to query for this here, because otherwise when we generate the project somehow the fields get
		// focus and that auto changes the radio selection value for generation
		boolean doGitClone = mainPage.cloneFromGit();
		if (newProject != null)
		{
			return newProject;
		}

		// get a project handle
		final IProject newProjectHandle = mainPage.getProjectHandle();

		// get a project descriptor
		URI location = null;
		if (!mainPage.locationIsDefault())
		{
			location = mainPage.getLocationURI();
		}

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		final IProjectDescription description = workspace.newProjectDescription(newProjectHandle.getName());
		description.setLocationURI(location);
		description.setNatureIds(getNatureIds());
		// Add Unified Builder
		ICommand command = description.newCommand();
		command.setBuilderName(UnifiedBuilder.ID);
		description.setBuildSpec(new ICommand[] { command });

		try
		{
			if (doGitClone)
			{
				doGitClone(description);
			}
			else
			{
				doBasicCreateProject(newProjectHandle, description);
			}
		}
		catch (CoreException e)
		{
			return null;
		}
		newProject = newProjectHandle;

		return newProject;
	}

	protected String[] getNatureIds()
	{
		return new String[] { RubyProjectNature.ID };
	}

	private void doGitClone(final IProjectDescription overridingDescription) throws CoreException
	{
		final String sourceURI = mainPage.gitCloneURI();
		final String dest = mainPage.getLocationPath().toOSString();

		try
		{
			getContainer().run(true, true, new IRunnableWithProgress()
			{

				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
				{
					CloneJob job = new CloneJob(sourceURI, dest, true)
					{
						@Override
						protected void doCreateProject(IProject project, IProjectDescription desc,
								IProgressMonitor monitor) throws CoreException
						{
							super.doCreateProject(project, overridingDescription, monitor);
						}
					};
					job.run(monitor);

				}
			});
		}
		catch (InvocationTargetException e)
		{
			throw new CoreException(new Status(IStatus.ERROR, RubyUIPlugin.getPluginIdentifier(), e.getMessage(), e));
		}
		catch (InterruptedException e)
		{
			throw new CoreException(new Status(IStatus.ERROR, RubyUIPlugin.getPluginIdentifier(), e.getMessage(), e));
		}
	}

	private void doBasicCreateProject(final IProject newProjectHandle, final IProjectDescription description)
			throws CoreException
	{
		// create the new project operation
		IRunnableWithProgress op = new IRunnableWithProgress()
		{
			public void run(IProgressMonitor monitor) throws InvocationTargetException
			{
				CreateProjectOperation op = new CreateProjectOperation(description, Messages.NewProject_jobTitle);
				try
				{
					// see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=219901
					// directly execute the operation so that the undo state is
					// not preserved. Making this undoable resulted in too many
					// accidental file deletions.
					op.execute(monitor, WorkspaceUndoUtil.getUIInfoAdapter(getShell()));
				}
				catch (ExecutionException e)
				{
					throw new InvocationTargetException(e);
				}
			}
		};

		// run the new project creation operation
		try
		{
			getContainer().run(true, true, op);
		}
		catch (InterruptedException e)
		{
			throw new CoreException(new Status(IStatus.ERROR, RubyUIPlugin.getPluginIdentifier(), e.getMessage(), e));
		}
		catch (InvocationTargetException e)
		{
			Throwable t = e.getTargetException();
			if (t instanceof ExecutionException && t.getCause() instanceof CoreException)
			{
				CoreException cause = (CoreException) t.getCause();
				StatusAdapter status;
				if (cause.getStatus().getCode() == IResourceStatus.CASE_VARIANT_EXISTS)
				{
					status = new StatusAdapter(new Status(IStatus.WARNING, RubyUIPlugin.getPluginIdentifier(),
							NLS.bind(Messages.NewProject_caseVariantExistsError, newProjectHandle.getName()), cause));
				}
				else
				{
					status = new StatusAdapter(new Status(cause.getStatus().getSeverity(),
							RubyUIPlugin.getPluginIdentifier(), Messages.NewProject_errorMessage, cause));
				}
				status.setProperty(IStatusAdapterConstants.TITLE_PROPERTY, Messages.NewProject_errorMessage);
				StatusManager.getManager().handle(status, StatusManager.BLOCK);
			}
			else
			{
				StatusAdapter status = new StatusAdapter(new Status(IStatus.WARNING,
						RubyUIPlugin.getPluginIdentifier(), 0, NLS.bind(Messages.NewProject_internalError,
								t.getMessage()), t));
				status.setProperty(IStatusAdapterConstants.TITLE_PROPERTY, Messages.NewProject_errorMessage);
				StatusManager.getManager().handle(status, StatusManager.LOG | StatusManager.BLOCK);
			}
			throw new CoreException(new Status(IStatus.ERROR, RubyUIPlugin.getPluginIdentifier(), e.getMessage(), e));
		}
	}

	/**
	 * Updates the perspective for the active page within the window.
	 */
	private void updatePerspective()
	{
		BasicNewProjectResourceWizard.updatePerspective(configElement);
	}
}
