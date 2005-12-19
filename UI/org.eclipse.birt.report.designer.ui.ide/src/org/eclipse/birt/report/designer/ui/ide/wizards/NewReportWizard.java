/*******************************************************************************
 * Copyright (c) 2004 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/

package org.eclipse.birt.report.designer.ui.ide.wizards;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.birt.report.designer.core.IReportElementConstants;
import org.eclipse.birt.report.designer.internal.ui.util.ExceptionHandler;
import org.eclipse.birt.report.designer.internal.ui.util.UIUtil;
import org.eclipse.birt.report.designer.internal.ui.wizards.WizardReportSettingPage;
import org.eclipse.birt.report.designer.internal.ui.wizards.WizardTemplateChoicePage;
import org.eclipse.birt.report.designer.nls.Messages;
import org.eclipse.birt.report.designer.ui.ReportPlugin;
import org.eclipse.birt.report.designer.ui.editors.IDEReportEditor;
import org.eclipse.birt.report.model.api.ReportDesignHandle;
import org.eclipse.birt.report.model.api.activity.SemanticException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.cheatsheets.OpenCheatSheetAction;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;


/**
 * An implementation of <code>INewWizard</code>. Creates a new blank report
 * file.
 */

public class NewReportWizard extends Wizard implements INewWizard, IExecutableExtension
{
//	private static final String REPORT_WIZARD = Messages.getString( "NewReportWizard.title.ReportWizard" ); //$NON-NLS-1$
	private static final String OPENING_FILE_FOR_EDITING = Messages.getString( "NewReportWizard.text.OpenFileForEditing" ); //$NON-NLS-1$
//	private static final String DOES_NOT_EXIST = Messages.getString( "NewReportWizard.text.DoesNotExist" ); //$NON-NLS-1$
//	private static final String CONTAINER = Messages.getString( "NewReportWizard.text.Container" ); //$NON-NLS-1$
	private static final String CREATING = Messages.getString( "NewReportWizard.text.Creating" ); //$NON-NLS-1$
	private static final String NEW_REPORT_FILE_NAME_PREFIX = Messages.getString( "NewReportWizard.displayName.NewReportFileNamePrefix" ); //$NON-NLS-1$
	private static final String NEW_REPORT_FILE_EXTENSION = Messages.getString( "NewReportWizard.displayName.NewReportFileExtension" ); //$NON-NLS-1$
//	private static final String NEW_REPORT_FILE_NAME = NEW_REPORT_FILE_NAME_PREFIX;
	private static final String SELECT_A_REPORT_TEMPLATE = Messages.getString( "NewReportWizard.text.SelectTemplate" ); //$NON-NLS-1$
	private static final String CREATE_A_NEW_REPORT = Messages.getString( "NewReportWizard.text.CreateReport" ); //$NON-NLS-1$
	String REPORT = Messages.getString( "NewReportWizard.title.Report" ); //$NON-NLS-1$
	private static final String TEMPLATECHOICEPAGE = Messages.getString( "NewReportWizard.title.Template" ); //$NON-NLS-1$
	private static final String WIZARDPAGE = Messages.getString( "NewReportWizard.title.WizardPage" ); //$NON-NLS-1$
	private static final String NEW = Messages.getString( "NewReportWizard.title.New" ); //$NON-NLS-1$
	//	private static final String CHOOSE_FROM_TEMPLATE = Messages.getString(
	// "NewReportWizard.title.Choose" ); //$NON-NLS-1$

	/** Holds selected project resource for run method access */
	private IStructuredSelection selection;

	WizardNewReportCreationPage newReportFileWizardPage;
	
	WizardReportSettingPage settingPage;

	private WizardTemplateChoicePage templateChoicePage;

	private int UNIQUE_COUNTER = 0;
	
	private String fileExtension = IReportElementConstants.DESIGN_FILE_EXTENSION;

	//	private WizardChoicePage choicePage;
	//	private WizardCustomTemplatePage customTemplatePage;

	public NewReportWizard()
	{
		super( );
	}
	
	public NewReportWizard(String fileType )
	{
		super( );
		this.fileExtension = fileType;
	}

	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	public boolean performFinish( )
	{
		final IPath containerName = newReportFileWizardPage.getContainerFullPath( );
		String fn = newReportFileWizardPage.getFileName( );
		final String fileName;
		if ( !fn.endsWith( "." + fileExtension ) ) //$NON-NLS-1$
		{
			fileName = fn + "." + fileExtension; //$NON-NLS-1$
		}
		else
		{
			fileName = fn;
		}
		InputStream streamFromPage = null;
		String cheatSheetIdFromPage = "";//$NON-NLS-1$
		boolean showCheatSheetFromPage = false;

		//Temporary remark the choice page for that feature is not supported in
		// R1
		//		if ( choicePage.isBlank( ) )
		//		{
		//			// blank report
		//			URL url = Platform.find( Platform.getBundle( ReportPlugin.REPORT_UI
		// ),
		//					new Path( templateChoicePage.getBlankTemplate( ).reportPath ) );
		//			if ( url != null )
		//			{
		//				try
		//				{
		//					streamFromPage = url.openStream( );
		//				}
		//				catch ( IOException e1 )
		//				{
		//					//ignore.
		//				}
		//			}
		//
		//			cheatSheetIdFromPage = templateChoicePage.getBlankTemplate(
		// ).cheatSheetId;
		//			showCheatSheetFromPage = false;
		//		}
		//		else if ( !choicePage.isCustom( ) )
		//		{
		// predefined template
		URL url = Platform.find( Platform.getBundle( ReportPlugin.REPORT_UI ),
				new Path( templateChoicePage.getTemplate( ).getReportPath() ) );
		if ( url != null )
		{
			try
			{
				streamFromPage = url.openStream( );
			}
			catch ( IOException e1 )
			{
				//ignore.
			}
		}
		else
		{
			try
			{
				streamFromPage = new FileInputStream(templateChoicePage.getTemplate( ).getReportPath() );
			}
			catch ( FileNotFoundException e )
			{
			}
		}

		cheatSheetIdFromPage = templateChoicePage.getTemplate( ).getCheatSheetId();
		showCheatSheetFromPage = templateChoicePage.getShowCheatSheet( );
		//			Temporary remark the choice page for that feature is not supported in
		// R1
		//		}
		//		else
		//		{
		//			// custom template
		//			try
		//			{
		//				streamFromPage = new FileInputStream(
		// customTemplatePage.getReportPath( ) );
		//				String xmlPath = customTemplatePage.getReportPath( )
		//						.replaceFirst( ".rptdesign", ".xml" );
		//				File f = new File( xmlPath );
		//				if ( f.exists( ) )
		//				{
		//					cheatSheetIdFromPage = f.toURL( ).toString( );
		//					// commented out until opencheatsheetaction bug is fixed in
		//					// eclipse
		//					// https://bugs.eclipse.org/bugs/show_bug.cgi?id=88481
		//					// showCheatSheetFromPage =
		//					// customTemplatePage.getShowCheatSheet( );
		//				}
		//
		//			}
		//			catch ( Exception e )
		//			{
		//				ExceptionHandler.handle( e );
		//				return false;
		//			}
		//		}

		final InputStream stream = streamFromPage;
		final String cheatSheetId = cheatSheetIdFromPage;
		final boolean showCheatSheet = showCheatSheetFromPage;
		IRunnableWithProgress op = new IRunnableWithProgress( ) {

			public void run( IProgressMonitor monitor )
					throws InvocationTargetException
			{
				try
				{
					doFinish( containerName,
							fileName,
							stream,
							cheatSheetId,
							showCheatSheet,
							monitor );
				}
				catch ( CoreException e )
				{
					throw new InvocationTargetException( e );
				}
				finally
				{
					monitor.done( );
				}
			}
		};
		try
		{
			getContainer( ).run( true, false, op );
		}
		catch ( InterruptedException e )
		{
			return false;
		}
		catch ( InvocationTargetException e )
		{
			Throwable realException = e.getTargetException( );
			ExceptionHandler.handle( realException );
			return false;
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench,
	 *      org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void init( IWorkbench workbench, IStructuredSelection selection )
	{
		// check existing open project
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace( ).getRoot( );
		IProject projects[] = root.getProjects( );
		boolean foundOpenProject = false;
		for ( int i = 0; i < projects.length; i++ )
		{
			if ( projects[i].isOpen( ) )
			{
				foundOpenProject = true;
				break;
			}
		}
		if ( !foundOpenProject )
		{
			MessageDialog.openError( getShell( ),
					Messages.getString( "NewReportWizard.title.Error" ), //$NON-NLS-1$
					Messages.getString( "NewReportWizard.error.NoProject" ) ); //$NON-NLS-1$

			// abort wizard. There is no clean way to do it.
			/**
			 * Remove the exception here 'cause It's safe since the wizard won't
			 * create any file without an open project.
			 */
			//throw new RuntimeException( );
		}
		// OK
		this.selection = selection;
		setWindowTitle( NEW );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.IWizard#getDefaultPageImage()
	 */
	public Image getDefaultPageImage( )
	{
		return ReportPlugin.getImage( "/icons/wizban/create_report_wizard.gif" ); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.IWizard#addPages()
	 */
	public void addPages( )
	{
		newReportFileWizardPage = new WizardNewReportCreationPage( WIZARDPAGE,
				selection,fileExtension );
		addPage( newReportFileWizardPage );
		//		Temporary remark the choice page for that feature is not supported in
		// R1
		//		choicePage = new WizardChoicePage( TEMPLATECHOICEPAGE );
		//		addPage( choicePage );
		//		customTemplatePage = new WizardCustomTemplatePage( TEMPLATECHOICEPAGE
		// );
		//		addPage( customTemplatePage );
		templateChoicePage = new WizardTemplateChoicePage( TEMPLATECHOICEPAGE );
		addPage( templateChoicePage );

		// set titles
		newReportFileWizardPage.setTitle( REPORT );
		newReportFileWizardPage.setDescription( CREATE_A_NEW_REPORT );
		templateChoicePage.setTitle( REPORT );
		templateChoicePage.setDescription( SELECT_A_REPORT_TEMPLATE );
		//		Temporary remark the choice page for that feature is not supported in
		// R1
		//		customTemplatePage.setTitle( REPORT );
		//		customTemplatePage.setDescription( SELECT_A_REPORT_TEMPLATE );
		//		choicePage.setTitle( REPORT );
		//		choicePage.setDescription( CHOOSE_FROM_TEMPLATE );

		resetUniqueCount( );
		newReportFileWizardPage.setFileName( getUniqueReportName(NEW_REPORT_FILE_NAME_PREFIX,NEW_REPORT_FILE_EXTENSION) );//$NON-NLS-1$
		newReportFileWizardPage.setContainerFullPath( getDefaultContainerPath( ) );
		
		settingPage = new WizardReportSettingPage( null );
		settingPage.setTitle( Messages.getString( "SaveReportAsWizard.SettingPage.title" ) );

		addPage( settingPage );
	}

	void resetUniqueCount( )
	{
		UNIQUE_COUNTER = 0;
	}

	IPath getDefaultContainerPath( )
	{
		IWorkbenchWindow benchWindow = PlatformUI.getWorkbench( )
				.getActiveWorkbenchWindow( );
		IWorkbenchPart part = benchWindow.getPartService( ).getActivePart( );

		Object selection = null;
		if ( part instanceof IEditorPart )
		{
			selection = ( (IEditorPart) part ).getEditorInput( );
		}
		else
		{
			ISelection sel = benchWindow.getSelectionService( ).getSelection( );
			if ( ( sel != null ) && ( sel instanceof IStructuredSelection ) )
			{
				selection = ( (IStructuredSelection) sel ).getFirstElement( );
			}
		}

		IContainer ct = getDefaultContainer( selection );

		if ( ct == null )
		{
			IEditorPart editor = UIUtil.getActiveEditor( true );

			if ( editor != null )
			{
				ct = getDefaultContainer( editor.getEditorInput( ) );
			}
		}

		if ( ct != null )
		{
			return ct.getFullPath( );
		}

		return null;
	}

	private IContainer getDefaultContainer( Object selection )
	{
		IContainer ct = null;
		if ( selection instanceof IAdaptable )
		{
			IResource resource = (IResource) ( (IAdaptable) selection ).getAdapter( IResource.class );

			if ( resource instanceof IContainer && resource.isAccessible( ) )
			{
				ct = (IContainer) resource;
			}
			else if ( resource != null
					&& resource.getParent( ) != null
					&& resource.getParent( ).isAccessible( ) )
			{
				ct = resource.getParent( );
			}
		}

		return ct;
	}
	
	String getUniqueReportName( String prefix, String ext )
	{
		int counter = getCounter( prefix, ext );
		return counter == 0 ? prefix + ext //$NON-NLS-1$
				: prefix + "_" //$NON-NLS-1$
						+ counter + ext; //$NON-NLS-1$
	}
	
	int getCounter( String prefix, String ext )
	{
		IProject[] pjs = ResourcesPlugin.getWorkspace( )
				.getRoot( )
				.getProjects( );

		resetUniqueCount( );

		boolean goon = true;

		while ( goon )
		{
			goon = false;

			for ( int i = 0; i < pjs.length; i++ )
			{
				if ( pjs[i].isAccessible( ) )
				{
					if ( !validDuplicate( prefix,
							ext,
							UNIQUE_COUNTER,
							pjs[i] ) )
					{
						UNIQUE_COUNTER++;

						goon = true;

						break;
					}
				}
			}
		}

		return UNIQUE_COUNTER;

	}

	private static final List tmpList = new ArrayList( );
    private IConfigurationElement configElement;

	boolean validDuplicate( String prefix, String ext, int count,
			IResource res )
	{
		if ( res != null && res.isAccessible( ) )
		{
			final String name;
			if ( count == 0 )
			{
				name = prefix + ext;
			}
			else
			{
				name = prefix + "_" + count + ext; //$NON-NLS-1$
			}

			try
			{
				tmpList.clear( );

				res.accept( new IResourceVisitor( ) {

					public boolean visit( IResource resource )
							throws CoreException
					{
						if ( resource.getType( ) == IResource.FILE
								&& name.equals( ( (IFile) resource ).getName( ) ) )
						{
							tmpList.add( Boolean.TRUE );
						}

						return true;
					}
				},
						IResource.DEPTH_INFINITE,
						true );

				if ( tmpList.size( ) > 0 )
				{
					return false;
				}
			}
			catch ( CoreException e )
			{
				ExceptionHandler.handle( e );
			}
		}

		return true;
	}
	
    /**
     * Creates a folder resource handle for the folder with the given workspace path.
     * This method does not create the folder resource; this is the responsibility
     * of <code>createFolder</code>.
     *
     * @param folderPath the path of the folder resource to create a handle for
     * @return the new folder resource handle
     */
    protected IFolder createFolderHandle( IPath folderPath )
	{
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace( )
				.getRoot( );
		return workspaceRoot.getFolder( folderPath );
	}

	/**
	 * The worker method. It will find the container, create the file if missing
	 * or just replace its contents, and open the editor on the newly created
	 * file.
	 * 
	 * @param cheatSheetId
	 * 
	 * @param containerName
	 * @param fileName
	 * @param showCheatSheet
	 * @param monitor
	 */

	private void doFinish( IPath containerName, String fileName,
			InputStream stream, String cheatSheetId, boolean showCheatSheet,
			IProgressMonitor monitor ) throws CoreException
	{
		// create a sample file
		monitor.beginTask( CREATING + fileName, 2 );
		IResource resource = (IContainer) ResourcesPlugin.getWorkspace( )
				.getRoot( )
				.findMember( containerName );
		IContainer container = null;
		if ( resource == null
				|| !resource.exists( ) || !( resource instanceof IContainer ) )
		{
			// create folder if not exist
			IFolder folder = createFolderHandle( containerName );
			UIUtil.createFolder( folder, monitor );
			container = folder;
		}
		else
		{
			container = (IContainer) resource;
		}
		final IFile file = container.getFile( new Path( fileName ) );
		final String cheatId = cheatSheetId;
		final boolean showCheat = showCheatSheet;
		try
		{
			if ( file.exists( ) )
			{
				file.setContents( stream, true, true, monitor );
			}
			else
			{
				file.create( stream, true, monitor );
			}
			stream.close( );

		}
		catch ( IOException e )
		{
		}
		monitor.worked( 1 );
		monitor.setTaskName( OPENING_FILE_FOR_EDITING );
		getShell( ).getDisplay( ).asyncExec( new Runnable( ) {

			public void run( )
			{
				IWorkbench workbench = PlatformUI.getWorkbench( );
				IWorkbenchWindow window = workbench.getActiveWorkbenchWindow( );

				IWorkbenchPage page = window.getActivePage( );
				try
				{
					IEditorPart editorPart = IDE.openEditor( page, file, true );

					setReportSettings(((IDEReportEditor)editorPart).getModel());
					editorPart.doSave(null);
					
					BasicNewProjectResourceWizard.updatePerspective( configElement );
					if ( showCheat && !cheatId.equals( "" ) ) //$NON-NLS-1$
					{
						OpenCheatSheetAction action = null;
						//						Temporary remark the choice page for that feature is
						// not supported in R1
						//						if ( choicePage.isCustom( ) )
						//						{
						//							action = new OpenCheatSheetAction( file.getName( ),
						//									file.getName( ),
						//									new URL( cheatId ) ); //$NON-NLS-1$
						//						}
						//						else
						//						{
						action = new OpenCheatSheetAction( cheatId );
						//						}
						action.run( );
					}
				}
				catch ( Exception e )
				{
					ExceptionHandler.handle( e );
				}
			}
		} );

		monitor.worked( 1 );

	}

//	private void throwCoreException( String message ) throws CoreException
//	{
//		IStatus status = new Status( IStatus.ERROR,
//				REPORT_WIZARD,
//				IStatus.OK,
//				message,
//				null );
//		throw new CoreException( status );
//	}
	

	//	Temporary remark the choice page for that feature is not supported in R1
	//	/*
	//	 * (non-Javadoc)
	//	 *
	//	 * @see
	// org.eclipse.jface.wizard.IWizard#getNextPage(org.eclipse.jface.wizard.IWizardPage)
	//	 */
	//	public IWizardPage getNextPage( IWizardPage page )
	//	{
	//
	//		if ( page instanceof WizardChoicePage )
	//		{
	//			if ( choicePage.isCustom( ) )
	//			{
	//				return customTemplatePage;
	//			}
	//			else if ( choicePage.isBlank( ) )
	//			{
	//				return null;
	//			}
	//			else
	//			{
	//				return templateChoicePage;
	//			}
	//
	//		}
	//		else
	//		{
	//			if ( page instanceof WizardCustomTemplatePage )
	//			{
	//				return null;
	//			}
	//			else
	//			{
	//				return super.getNextPage( page );
	//			}
	//		}
	//	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.IWizard#canFinish()
	 */
	public boolean canFinish( )
	{
		//		Temporary remark the choice page for that feature is not supported in
		// R1
		//		if ( choicePage.isBlank( ) )
		//		{
		//			return newReportFileWizardPage.isPageComplete( );
		//		}
		//		else if ( choicePage.isCustom( ) )
		//		{
		//			return customTemplatePage.isPageComplete( )
		//					&& newReportFileWizardPage.isPageComplete( );
		//		}
		//		else
		//		{
		return templateChoicePage.isPageComplete( )
				&& newReportFileWizardPage.isPageComplete( );
		//		}
	}

    /* (non-Javadoc)
     * @see org.eclipse.core.runtime.IExecutableExtension#setInitializationData(org.eclipse.core.runtime.IConfigurationElement, java.lang.String, java.lang.Object)
     */
    public void setInitializationData( IConfigurationElement config, String propertyName, Object data ) throws CoreException
    {
       this.configElement = config;
    }
	
	public String getFileExtension( )
	{
		return fileExtension;
	}

	public void setFileExtension( String fileExtension )
	{
		this.fileExtension = fileExtension;
	}

	
	public IStructuredSelection getSelection( )
	{
		return selection;
	}

	
	public IConfigurationElement getConfigElement( )
	{
		return configElement;
	}

	/**
	 * Set report basic settings.
	 * @param model
	 * @throws IOException
	 */
	void setReportSettings( Object model ) throws IOException
	{
		ReportDesignHandle handle = (ReportDesignHandle)model;
		try
		{
			handle.setDisplayName( settingPage.getDisplayName( ) );
			handle.setDescription( settingPage.getDescription( ) );
			handle.setIconFile( settingPage.getPreviewImagePath( ) );
		}
		catch ( SemanticException e )
		{
		}
		handle.save();
	}
}