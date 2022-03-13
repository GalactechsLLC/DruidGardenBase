package garden.druid.base.http.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap.KeySetView;
import java.util.logging.Level;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.security.SecurityUtil;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import garden.druid.base.http.auth.api.Authenticator;
import garden.druid.base.http.rest.adapters.Adapter;
import garden.druid.base.http.rest.adapters.AdapterManager;
import garden.druid.base.http.rest.annotation.*;
import garden.druid.base.http.rest.enums.*;
import garden.druid.base.http.rest.exceptions.*;
import garden.druid.base.logging.Logger;

@RestPattern(uri="/default")
public abstract class RestEndpoint extends HttpServlet {
	
	private static final long serialVersionUID = 9134582935000458075L;
	private static final int MAX_BODY_SIZE = 67108864; //64MB
	private final Pattern uriParserPattern = Pattern.compile("\\{([^}]*)\\}");
	private final String variableReplacement = "([^/]+)";
	protected final Gson gson = new GsonBuilder().serializeNulls().create();
	private Pattern variablePattern;
	private final ConcurrentHashMap<String, String> httpMethodMap = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Integer, String> variablePositions = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Method> methodCache = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Parameter[]> paramCache = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Class<?>[]> paramTypeCache = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Class<?>> authClassCache = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Authenticator> methodAuthCache = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Integer> methodUserLevelCache = new ConcurrentHashMap<>();
	private final KeySetView<String, Boolean> insecureMethodCache = ConcurrentHashMap.newKeySet();
	
	@Override 
	public final void init() {
		for (Method method : this.getClass().getDeclaredMethods()) {
	        if (httpMethodMap.get("get") == null && method.isAnnotationPresent(GET.class)) {
	        	httpMethodMap.put("get", method.getName());
	        } 
	        if(httpMethodMap.get("post") == null && method.isAnnotationPresent(POST.class)) {
	        	httpMethodMap.put("post", method.getName());
	        } 
	        if(httpMethodMap.get("put") == null && method.isAnnotationPresent(PUT.class)) {
	        	httpMethodMap.put("put", method.getName());
	        } 
	        if(httpMethodMap.get("delete") == null && method.isAnnotationPresent(DELETE.class)) {
	        	httpMethodMap.put("delete", method.getName());
	        }
			method.setAccessible(true);
        	methodCache.put(method.getName(), method);
        	paramCache.put(method.getName(), method.getParameters());
        	paramTypeCache.put(method.getName(), method.getParameterTypes());
	    }
		RestPattern pattern = getClass().getAnnotation(RestPattern.class);
		Matcher matcher = uriParserPattern.matcher(pattern.uri());
		if(!getClass().isAnnotationPresent(RestPattern.class)) throw new RuntimeException("Failed to Init RestEndpoint " + getClass().toGenericString() + ", no @RestPattern specified");
		int index = 0;
		while(matcher.find()) {
			MatchResult result = matcher.toMatchResult();
			variablePositions.put(index, result.group(1));
			index++;
		}
		variablePattern = Pattern.compile(matcher.replaceAll(variableReplacement));
	}
	
	@Override
	public final void doGet(HttpServletRequest request, HttpServletResponse response) {
		handleRequest(request, response, httpMethodMap.get("get"));
	}

	@Override
	public final void doPost(HttpServletRequest request, HttpServletResponse response) {
		handleRequest(request, response, httpMethodMap.get("post"));
	}

	@Override
	public final void doPut(HttpServletRequest request, HttpServletResponse response) {
		handleRequest(request, response, httpMethodMap.get("put"));
	}

	@Override
	public final void doDelete(HttpServletRequest request, HttpServletResponse response) {
		handleRequest(request, response, httpMethodMap.get("delete"));
	}
	
	private void handleRequest(HttpServletRequest request, HttpServletResponse response, final String methodName) {
		Object responseObj = null;
		ReturnTypes returnType = ReturnTypes.JSON;
		int responseCode = HttpServletResponse.SC_OK;
		try {
			Method method = getMethodByName(this, methodName);
			checkSecurity(request, method);
			try {
				responseObj = invokeMethod(this, method.getName(), getMethodArgs(request, response, parseVariables(request, method), method.getName()));
			} catch (InvocationTargetException e) {
				Throwable child = e.getCause();
				if(child == null) {
					throw e;
				}
				if (child instanceof NotFoundException) {
			        throw (NotFoundException) child;
			    } else if (child instanceof BadRequestException) {
			        throw (BadRequestException) child;
			    } else if (child instanceof InternalErrorException) {
			        throw (InternalErrorException) child;
			    }  else if (child instanceof IllegalAccessException) {
			        throw (IllegalAccessException) child;
			    } else {
			    	throw e;
			    }
			} 
			returnType = method.isAnnotationPresent(ReturnType.class) ? method.getAnnotation(ReturnType.class).type() : ReturnTypes.JSON;
		} catch (NoSuchMethodException | NotFoundException e) { //404
			responseObj = "Not Found";
			returnType = ReturnTypes.STRING;
			responseCode = HttpServletResponse.SC_NOT_FOUND;
		} catch (IllegalAccessException | SecurityException | PrivilegedActionException e) { //403
			responseObj = "Forbidden";
			returnType = ReturnTypes.STRING;
			responseCode = HttpServletResponse.SC_FORBIDDEN;
		} catch (IllegalArgumentException | BadRequestException e) { //400
			Logger.getInstance().log(Level.WARNING, "Exception in handle request, failed to send response", e);
			responseObj = "Bad Request";
			returnType = ReturnTypes.STRING;
			responseCode = HttpServletResponse.SC_BAD_REQUEST;
		} catch (InvocationTargetException | InternalErrorException e) { //500
			Logger.getInstance().log(Level.WARNING, "Exception in handle request, failed to send response", e);
			responseObj = "Server Error";
			returnType = ReturnTypes.STRING;
			responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
		} catch(Exception e) {
			Logger.getInstance().log(Level.WARNING, "Exception in handle request, Unhandled " + methodName + ": ", e);
		}
		try {
			sendResponse(response, responseObj, returnType, responseCode);	
		} catch (Exception e){
			Logger.getInstance().log(Level.WARNING, "Exception in handle request, failed to send response", e);
		}
	}
	
	private void checkSecurity(HttpServletRequest request, final Method method) throws IllegalAccessException {
		//Skip checked methods that are not secured
		if(insecureMethodCache.contains(method.getName())) return;
		//Load the authenticator, first we check the method, then the class if nothing on method. Method settings will override the class settings  
		Authenticator authenticator = methodAuthCache.get(method.getName());
		if(authenticator == null && method.isAnnotationPresent(RequireAuthenticator.class)) {
			authenticator = loadAuthClass(method.getAnnotation(RequireAuthenticator.class).authenticator());
			methodAuthCache.put(method.getName(), authenticator);
		} 
		if (authenticator == null && getClass().isAnnotationPresent(RequireAuthenticator.class)) {
			authenticator = loadAuthClass(getClass().getAnnotation(RequireAuthenticator.class).authenticator());
			methodAuthCache.put(method.getName(), authenticator);
		}
		if(authenticator != null) { //If no authenticator its not a Secured endpoint
			//Now we load the required user level, same basic logic. 
			Integer userLevel = methodUserLevelCache.get(method.getName());
			if(userLevel == null && method.isAnnotationPresent(RequireUserLevel.class)) {
				userLevel = method.getAnnotation(RequireUserLevel.class).level();
				methodUserLevelCache.put(method.getName(), userLevel);
			} 
			if (userLevel == null && getClass().isAnnotationPresent(RequireUserLevel.class)) {
				userLevel = getClass().getAnnotation(RequireUserLevel.class).level();
				methodUserLevelCache.put(method.getName(), userLevel);
			}
			if(userLevel == null) {
				throw new IllegalAccessException("Failed to Authenticate, found Authenticator with no RequiredUserLevel: " + method.getName());
			} else {
				if( !authenticator.authenticate(request, userLevel)){
					throw new IllegalAccessException("Access Denied for Method " + method.getName());
				}
			}
		} else {
			insecureMethodCache.add(method.getName());
		}
	}
	
	private Authenticator loadAuthClass(String name) throws IllegalAccessException {
		Class<?> authClass;
		if(authClassCache.containsKey(name)) {
			authClass = authClassCache.get(name);
		} else {
			try {
				authClass = this.getClass().getClassLoader().loadClass(name);
				authClassCache.put(name, authClass);
			} catch (ClassNotFoundException e) {
				Logger.getInstance().log(Level.WARNING, "Exception in Load authenticator class", e);
				throw new IllegalAccessException("Failed to Load authenticator class: " + name);
			}
		}
		Object instance;
		try {
			instance = authClass.newInstance();
		} catch (InstantiationException e) {
			Logger.getInstance().log(Level.WARNING, "Exception in Load authenticator class", e);
			throw new IllegalAccessException("Failed create authenticator instance: " + name);
		}
		if(instance instanceof Authenticator) {
			return (Authenticator) instance;
		} else {
			throw new IllegalAccessException("Failed create authenticator instance: " + name);
		}
	}
	
	private HashMap<String, HashMap<String, String>> parseVariables(HttpServletRequest request, final Method method) {
		HashMap<String, HashMap<String, String>> rtn = new HashMap<>();
		Matcher matcher = variablePattern.matcher(request.getRequestURI());
		HashMap<String, String> uriVariables = new HashMap<>();
		if(matcher.find()) {
			MatchResult result = matcher.toMatchResult();
			for(int i = 0; i < result.groupCount(); i++) {
				uriVariables.put(variablePositions.get(i), result.group(i+1));
			}
		}
		rtn.put("uri", uriVariables);
		Parameter[] methodParams = method.getParameters();
		if(method.isAnnotationPresent(Consumes.class)) {
			ConsumerTypes type = method.getAnnotation(Consumes.class).consumer();
			if(type == ConsumerTypes.URL_PARAMS) {
				HashMap<String, String> formVariables = new HashMap<>();
				for (Parameter param : methodParams) {
					if (param.isAnnotationPresent(FormVariable.class)) {
						String name = param.getAnnotation(FormVariable.class).name();
						formVariables.put(name, request.getParameter(name));
					}
				}
				rtn.put("form", formVariables);
			} else if(type == ConsumerTypes.BODY) {
				rtn.put("body", new HashMap<String,String>(){private static final long serialVersionUID = 1L;{put("body",getBody(request));}});
			}
		}
		return rtn;
	}
	
	private Object[] getMethodArgs(final HttpServletRequest request, final HttpServletResponse response, final HashMap<String, HashMap<String, String>> variables, final String method) throws NoSuchMethodException, SecurityException {
		Parameter[] methodParams = getMethodParams(method);
		Object[] parmAry = new Object[methodParams.length];
		Class<?>[] paramTypes = getMethodParamTypes(method);
		for(int i = 0; i < methodParams.length; i ++) {
			Parameter param = methodParams[i];
			if(param.isAnnotationPresent(UriVariable.class)) {
				Adapter<?> adapter = AdapterManager.getAdapter(paramTypes[i]);
				String annotation = param.getAnnotation(UriVariable.class).name();
				HashMap<String,String> vars = variables.get("uri");
				parmAry[i] = adapter.convert(vars.get(annotation));
			} else if(param.isAnnotationPresent(FormVariable.class)) {
				Adapter<?> adapter = AdapterManager.getAdapter(paramTypes[i]);
				String annotation = param.getAnnotation(FormVariable.class).name();
				HashMap<String,String> vars = variables.get("form");
				parmAry[i] = adapter.convert(vars.get(annotation));
			} else if(param.isAnnotationPresent(Body.class)) {
				Adapter<?> adapter = AdapterManager.getAdapter(paramTypes[i]);
				HashMap<String,String> vars = variables.get("body");
				parmAry[i] = adapter.convert(vars.get("body"));
			}else if(param.isAnnotationPresent(Request.class)) {
				parmAry[i] = request;
			}else if(param.isAnnotationPresent(Response.class)) {
				parmAry[i] = response;
			}else if(param.isAnnotationPresent(AuthenticatorCls.class)) {
				parmAry[i] = methodAuthCache.get(method);
			}
		}
		return parmAry;
	}
	
	private Parameter[] getMethodParams(final String method) throws NoSuchMethodException, SecurityException {
		if(!paramCache.containsKey(method)) {
			paramCache.put(method, getMethodByName(this, method).getParameters());
		}
		return paramCache.get(method);
	}
	
	private Class<?>[] getMethodParamTypes(final String method) throws NoSuchMethodException, SecurityException {
		if(!paramTypeCache.containsKey(method)) {
			paramTypeCache.put(method, getMethodByName(this, method).getParameterTypes());
		}
		return paramTypeCache.get(method);
	}
	
	private void sendResponse(HttpServletResponse response, final Object resp, final ReturnTypes type, final int responseCode) throws IOException {
		if(type == ReturnTypes.JSON) {
			response.setContentType("application/json");
			response.setCharacterEncoding("UTF-8");
			response.setStatus(responseCode);
			response.getWriter().write(gson.toJson(resp));
		} else if(type == ReturnTypes.STRING) {
			response.setContentType("text/plain");
			response.setCharacterEncoding("UTF-8");
			response.setStatus(responseCode);
			response.getWriter().write((String) resp);
			
		} else if(type == ReturnTypes.HTML) {
			response.setContentType("text/html");
			response.setCharacterEncoding("UTF-8");
			response.setStatus(responseCode);
			response.getWriter().write((String) resp);
		} else if(type == ReturnTypes.XML) {
			response.setContentType("application/xml");
			response.setCharacterEncoding("UTF-8");
			response.setStatus(responseCode);
			response.getWriter().write((String) resp);
		} else {
			if(type != ReturnTypes.CUSTOM && type != ReturnTypes.VOID) {
				Logger.getInstance().log(Level.WARNING, "BAD RETURN TYPE");
			}
		}
	}

	private String getBody(HttpServletRequest request) {
		StringBuilder sb = new StringBuilder();
		BufferedReader br = null;
		try {
			InputStream is = request.getInputStream();
			if (is != null) {
				br = new BufferedReader(new InputStreamReader(is));
				char[] cb = new char[128];
				int read;
				while ((read = br.read(cb)) > 0 && sb.length() <= MAX_BODY_SIZE) {
					sb.append(cb, 0, read);
				}
			}
		} catch (IOException ex) {
			return ""; //99.9% of the time this is a client prematurely disconnecting
		} finally {
			try {if(br != null) br.close();} catch(Exception ignored) {}
		}
		return sb.toString();
	}
	
	private Method getMethodByName(final RestEndpoint context, final String methodName) throws NoSuchMethodException, SecurityException {
		Method method = methodCache.get(methodName);
		if (method == null){
			method = context.getClass().getMethod(methodName, paramTypeCache.get(methodName));
			methodCache.put(methodName, method);
		}
		return method;
	}
	
	private Object invokeMethod(final RestEndpoint context, final String methodName, final Object[] params) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, SecurityException, PrivilegedActionException {
		return executeMethod(getMethodByName(context, methodName),context,params);
	}
	
	private Object executeMethod(final Method method, final RestEndpoint context, final Object[] params) throws PrivilegedActionException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {          
		if (SecurityUtil.isPackageProtectionEnabled()){
		   return AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> method.invoke(context, params));
		} else {
		    return method.invoke(context, params);
		}        
	}
}
