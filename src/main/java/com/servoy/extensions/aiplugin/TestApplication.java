package com.servoy.extensions.aiplugin;

import java.awt.Dimension;
import java.awt.print.PageFormat;
import java.net.URL;
import java.rmi.Remote;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.util.concurrent.ScheduledExecutorService;

import javax.swing.ImageIcon;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IBasicFormManager;
import com.servoy.j2db.IDataRendererFactory;
import com.servoy.j2db.IEventsManager;
import com.servoy.j2db.IMenuManager;
import com.servoy.j2db.IModeManager;
import com.servoy.j2db.IPermissionManager;
import com.servoy.j2db.RuntimeWindowManager;
import com.servoy.j2db.cmd.ICmdManager;
import com.servoy.j2db.dataprocessing.ClientInfo;
import com.servoy.j2db.dataprocessing.DataServerProxy;
import com.servoy.j2db.dataprocessing.IClientHost;
import com.servoy.j2db.dataprocessing.IDataServer;
import com.servoy.j2db.dataprocessing.IFoundSetManagerInternal;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.plugins.IPluginAccess;
import com.servoy.j2db.plugins.IPluginManager;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.scripting.JSBlobLoaderBuilder;
import com.servoy.j2db.server.shared.IApplicationServer;
import com.servoy.j2db.server.shared.IApplicationServerAccess;
import com.servoy.j2db.server.shared.IUserManager;
import com.servoy.j2db.ui.ItemFactory;
import com.servoy.j2db.util.RendererParentWrapper;

public final class TestApplication implements IApplication {
	@Override
	public void setI18NMessage(String i18nKey, String value) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getI18NMessageIfPrefixed(String i18nKey) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getI18NMessage(String i18nKey, Object[] array, String language, String country) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getI18NMessage(String i18nKey, String language, String country) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getI18NMessage(String i18nKey, Object[] array) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getI18NMessage(String i18nKey) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isEventDispatchThread() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void invokeLater(Runnable arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void invokeAndWait(Runnable arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setTimeZone(TimeZone arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setLocale(Locale arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void reportWarning(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void reportInfo(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isSolutionLoaded() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isRunningRemote() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean haveRepositoryAccess() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void handleException(String arg0, Exception arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getUserUID() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getUserName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TimeZone getTimeZone() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Properties getSettings() {
		return new Properties();
	}

	@Override
	public URL getServerURL() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IExecutingEnviroment getScriptEngine() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map getRuntimeProperties() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Locale getLocale() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IFoundSetManagerInternal getFoundSetManager() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IDataServer getDataServer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getClientID() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IClientHost getClientHost() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IApplicationServer getApplicationServer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void releaseGUI() {
		// TODO Auto-generated method stub

	}

	@Override
	public void blockGUI(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void reportWarningInStatus(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void reportError(String arg0, Object arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public ImageIcon loadImage(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Solution getSolution() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ScheduledExecutorService getScheduledExecutor() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IRepository getRepository() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FlattenedSolution getFlattenedSolution() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IApplicationServerAccess getApplicationServerAccess() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateUI(int arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean showURL(String arg0, String arg1, String arg2, int arg3, boolean arg4) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void showSolutionLoading(boolean arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setValueListItems(String arg0, Object[] arg1, Object[] arg2, boolean arg3) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setUserProperty(String arg0, String arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setTitle(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setStatusText(String arg0, String arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setStatusProgress(int arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setPageFormat(PageFormat arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setI18NMessagesFilter(String arg0, String[] arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void reportJSWarning(String arg0, Throwable arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void reportJSWarning(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void reportJSInfo(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void reportJSError(String arg0, Object arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeUserProperty(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeAllUserProperties() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean putClientProperty(Object arg0, Object arg1) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void output(Object arg0, int arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void looseFocus() {
		// TODO Auto-generated method stub

	}

	@Override
	public void logout(Object[] arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isShutDown() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isInDeveloper() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void handleClientUserUidChanged(String arg0, String arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public String[] getUserPropertyNames() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getUserProperty(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IUserManager getUserManager() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSolutionName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Remote getServerService(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Dimension getScreenSize() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RuntimeWindowManager getRuntimeWindowManager() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResourceBundle getResourceBundle(Locale arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RendererParentWrapper getPrintingRendererParent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IPluginManager getPluginManager() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IPluginAccess getPluginAccess() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IPermissionManager getPermissionManager() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PageFormat getPageFormat() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IModeManager getModeManager() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IMenuManager getMenuManager() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ItemFactory getItemFactory() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IBasicFormManager getFormManager() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IEventsManager getEventsManager() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataServerProxy getDataServerProxy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IDataRendererFactory getDataRenderFactory() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ICmdManager getCmdManager() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getClientProperty(Object arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getClientPlatform() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getClientOSName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ClientInfo getClientInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getApplicationType() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getApplicationName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object generateBrowserFunction(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JSBlobLoaderBuilder createUrlBlobloaderBuilder(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean closeSolution(boolean arg0, Object[] arg1) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void clearLoginForm() {
		// TODO Auto-generated method stub

	}

	@Override
	public void blockGUII18NMessage(String arg0, Object... arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public Object authenticate(String arg0, String arg1, Object[] arg2) throws RepositoryException {
		// TODO Auto-generated method stub
		return null;
	}
}