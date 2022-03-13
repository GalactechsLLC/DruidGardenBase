package garden.druid.base.threads.management;

import javax.servlet.annotation.WebServlet;

import garden.druid.base.http.auth.api.UserLevel;
import garden.druid.base.http.rest.RestEndpoint;
import garden.druid.base.http.rest.annotation.Consumes;
import garden.druid.base.http.rest.annotation.GET;
import garden.druid.base.http.rest.annotation.RequireAuthenticator;
import garden.druid.base.http.rest.annotation.RequireUserLevel;
import garden.druid.base.http.rest.annotation.RestPattern;
import garden.druid.base.http.rest.annotation.ReturnType;
import garden.druid.base.http.rest.annotation.UriVariable;
import garden.druid.base.http.rest.enums.ConsumerTypes;
import garden.druid.base.http.rest.enums.ReturnTypes;
import garden.druid.base.http.rest.exceptions.BadRequestException;
import garden.druid.base.http.rest.exceptions.NotFoundException;
import garden.druid.base.threads.interfaces.ManagedTask;
import garden.druid.base.threads.interfaces.ManagedThreadPool;
import garden.druid.base.threads.threadpools.ThreadPoolManager;

@WebServlet(value = { "/monitor/threads/*" })
@RestPattern( uri = "/monitor/threads/{poolName}/?{uuid}?")
@RequireAuthenticator()
@RequireUserLevel( level = UserLevel.ADMIN)
public class ThreadMonitorEndpoint extends RestEndpoint {
	
	private static final long serialVersionUID = 1L;

	@GET
	@ReturnType(type=ReturnTypes.JSON)
	@Consumes(consumer=ConsumerTypes.NONE)
	public Object getThreadData(@UriVariable(name="poolName") String poolName, @UriVariable(name="uuid") String uuid) throws NotFoundException, BadRequestException {
		if(poolName != null) {
			ManagedThreadPool threadPool = ThreadPoolManager.getManagedThreadPool(poolName);
			if(threadPool != null) {
				if(uuid != null) {
					ManagedTask thread = threadPool.getManagedThreads().get(uuid);
					if(thread != null) {
						return thread.getStatus();
					} else {
						throw new NotFoundException("Thread Not Found: " + uuid);
					}
				} else {
					return threadPool.getManagedThreads().values();
				}
			} else {
				throw new NotFoundException("Pool Not Found: " + poolName);
			}
		} else {
			throw new BadRequestException("Null Pool Name ");
		}
	}
}