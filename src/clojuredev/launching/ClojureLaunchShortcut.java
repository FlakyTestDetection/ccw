package clojuredev.launching;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.internal.debug.ui.launcher.LauncherMessages;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.part.FileEditorInput;

public class ClojureLaunchShortcut implements ILaunchShortcut, IJavaLaunchConfigurationConstants {

    public void launch(IEditorPart editor, String mode) {
        IEditorInput input = editor.getEditorInput();
        if (input instanceof FileEditorInput) {
            FileEditorInput fei = (FileEditorInput) input;
            launchProject(fei.getFile().getProject(), new IFile[] { fei
                    .getFile() }, mode);
        }
    }

    public void launch(ISelection selection, String mode) {
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection strSel = (IStructuredSelection) selection;
            List<IFile> files = new ArrayList<IFile>();
            IProject proj = null;
            for (Object o : strSel.toList()) {
                if (o instanceof IFile) {
                    IFile f = (IFile) o;
                    files.add(f);
                    if (proj == null) {
                        proj = f.getProject();
                    }
                }
                else if (o instanceof IProject && strSel.size() == 1) {
                    launchProject((IProject) o, new IFile[] {}, mode);
                }
            }
            if (proj != null && !files.isEmpty()) {
                launchProject(proj, files.toArray(new IFile[] {}), mode);
            }
        }
    }

    protected void launchProject(IProject project, IFile[] files, String mode) {
        try {
            ILaunchConfiguration config = findLaunchConfiguration(project, files);
            if (config == null) {
                config = createConfiguration(project, files);
            }
            if (config != null) {
            	config.launch(mode, null);
            }
        }
        catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }

    private ILaunchConfiguration findLaunchConfiguration(IProject project, IFile[] files) {
        ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
        ILaunchConfigurationType type =
            lm.getLaunchConfigurationType(LaunchUtils.LAUNCH_ID);
        
        List candidateConfigs = Collections.EMPTY_LIST;
        
        try {
            ILaunchConfiguration[] configs = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations(type);
            candidateConfigs = new ArrayList(configs.length);
            for (ILaunchConfiguration config:  configs) {
                if (config.getAttribute(ATTR_MAIN_TYPE_NAME, "").equals(LaunchUtils.MAIN_CLASSNAME)
                		&& config.getAttribute(ATTR_PROJECT_NAME, "").equals(project.getName())
                		&& LaunchUtils.getFilesToLaunchList(config).equals(Arrays.asList(files))) {
                    candidateConfigs.add(config);
                }
            }
        }
        catch (CoreException e) {
            throw new RuntimeException(e);
        }
        int candidateCount = candidateConfigs.size();
        if (candidateCount == 1) {
            return (ILaunchConfiguration) candidateConfigs.get(0);
        } else if (candidateCount > 1) {
            return chooseConfiguration(candidateConfigs);
        }
        return null;
    }

    protected ILaunchConfiguration createConfiguration(IProject project, IFile[] files) {
        ILaunchConfiguration config = null;
        try {
            ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
            ILaunchConfigurationType type =
                lm.getLaunchConfigurationType(LaunchUtils.LAUNCH_ID);
            
            String basename = project.getName();
            if (files.length == 1) {
                basename += " " + files[0].getName(); 
            }
            
            ILaunchConfigurationWorkingCopy wc = type.newInstance(
                    null, DebugPlugin.getDefault().getLaunchManager().
                        generateUniqueLaunchConfigurationNameFrom(basename));
            
            LaunchUtils.setFilesToLaunchString(wc, Arrays.asList(files));
            
            wc.setAttribute(ATTR_PROGRAM_ARGUMENTS, "");
            
            wc.setAttribute(ATTR_MAIN_TYPE_NAME, LaunchUtils.MAIN_CLASSNAME);
            
            wc.setAttribute(ATTR_PROJECT_NAME, project.getName());
            
            wc.setAttribute(LaunchUtils.ATTR_CLOJURE_SERVER_LISTEN, LaunchUtils.DEFAULT_SERVER_PORT);
            
            wc.setMappedResources(new IResource[] {project});
            
            config = wc.doSave();
        }
        catch (CoreException ce) {
            throw new RuntimeException(ce);
        }
        return config;
    }
    
    protected ILaunchConfiguration chooseConfiguration(List configList) {
        IDebugModelPresentation labelProvider = null;
    	try {
    		labelProvider = DebugUITools.newDebugModelPresentation();
	        ElementListSelectionDialog dialog= new ElementListSelectionDialog(JDIDebugUIPlugin.getActiveWorkbenchShell(), labelProvider);
	        dialog.setElements(configList.toArray());
	        dialog.setTitle("Choose a Clojure launch configuration");  
	        dialog.setMessage(LauncherMessages.JavaLaunchShortcut_2);
	        dialog.setMultipleSelection(false);
	        int result = dialog.open();
	        if (result == Window.OK) {
	            return (ILaunchConfiguration) dialog.getFirstResult();
	        }
	        return null;
    	} finally {
    		if (labelProvider!= null) {
    			labelProvider.dispose();
    		}
    	}
    }
    
}