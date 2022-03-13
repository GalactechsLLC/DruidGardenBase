package garden.druid.base.threads.management;

import java.util.HashMap;
import javax.servlet.annotation.WebServlet;

import garden.druid.base.http.auth.api.UserLevel;
import garden.druid.base.http.rest.RestEndpoint;
import garden.druid.base.http.rest.annotation.Consumes;
import garden.druid.base.http.rest.annotation.GET;
import garden.druid.base.http.rest.annotation.PUT;
import garden.druid.base.http.rest.annotation.RequireAuthenticator;
import garden.druid.base.http.rest.annotation.RequireUserLevel;
import garden.druid.base.http.rest.annotation.RestPattern;
import garden.druid.base.http.rest.annotation.ReturnType;
import garden.druid.base.http.rest.annotation.UriVariable;
import garden.druid.base.http.rest.enums.ConsumerTypes;
import garden.druid.base.http.rest.enums.ReturnTypes;
import garden.druid.base.http.rest.exceptions.NotFoundException;
import garden.druid.base.threads.interfaces.ManagedThreadPool;
import garden.druid.base.threads.threadpools.ThreadPoolManager;

@WebServlet(value = { "/monitor/threadpools", "/monitor/threadpools/*" })
@RestPattern( uri = "/monitor/threadpools/?{name}?")
@RequireAuthenticator()
@RequireUserLevel( level = UserLevel.ADMIN)
public class ThreadPoolMonitorEndpoint extends RestEndpoint {
	
	private static final long serialVersionUID = 1L;

	@GET
	@ReturnType(type=ReturnTypes.JSON)
	@Consumes(consumer=ConsumerTypes.NONE)
	public Object getThreadData(@UriVariable(name="name") String name) throws NotFoundException {
		if(name != null) {
			HashMap<String, String> status = ThreadPoolManager.getManagedThreadPool(name).getStatus();
			if(status == null) {
				throw new NotFoundException("Thread pool Not Found: " + name);
			} 
			return status;
		} else {
			return ThreadPoolManager.getThreadPoolNames();
		}
	}

	@PUT
	@ReturnType(type=ReturnTypes.JSON)
	@Consumes(consumer=ConsumerTypes.NONE)
	public Object toggleThread(@UriVariable(name="name") String name) throws NotFoundException {
		if(name != null) {
			ManagedThreadPool threadPool = ThreadPoolManager.getManagedThreadPool(name);
			if(threadPool != null) {
				if(threadPool.isPaused()) {
					threadPool.resume();
				} else {
					threadPool.pause();
				}
				return threadPool.getStatus();
			} else {
				throw new NotFoundException("Thread pool Not Found: " + name);
			}
		} else {
			throw new NotFoundException("Pool Not Found: " + name);
		}
	}
}
