package am.ik.blog.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

/**
 * Custom authentication entry point that returns 401 without WWW-Authenticate header to
 * prevent browser popup dialogs for API endpoints.
 */
public class NoPopupBasicAuthenticationEntryPoint implements AuthenticationEntryPoint {

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {

		// For API requests (detected by Accept header or path), don't send
		// WWW-Authenticate header
		String acceptHeader = request.getHeader("Accept");
		String requestUri = request.getRequestURI();

		boolean isApiRequest = (acceptHeader != null && acceptHeader.contains("application/json"))
				|| requestUri.startsWith("/tenants/");

		if (isApiRequest) {
			// Return 401 without WWW-Authenticate header to prevent browser popup
			response.setStatus(HttpStatus.UNAUTHORIZED.value());
			response.setContentType("application/json");
			response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Authentication required\"}");
		}
		else {
			// For non-API requests, use standard Basic auth challenge
			response.setHeader("WWW-Authenticate", "Basic realm=\"Entry API\"");
			response.setStatus(HttpStatus.UNAUTHORIZED.value());
		}
	}

}