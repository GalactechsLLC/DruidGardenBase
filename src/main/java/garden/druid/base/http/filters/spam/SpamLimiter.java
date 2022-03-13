package garden.druid.base.http.filters.spam;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SpamLimiter extends HttpFilter{

	private static final long serialVersionUID = -1867737365894117439L;

	private final BucketLimiter limiter = new BucketLimiter(100, 0, 500, 10, 1, TimeUnit.SECONDS);
	
	@Override
	public void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
		Bucket bucket = limiter.getBucket(req.getRemoteAddr());
		if(bucket.spend(1)) {
			chain.doFilter(req, res);
		}
	}
}
