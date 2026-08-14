package com.fileserver.fileupload.security;

import com.fileserver.fileupload.entity.ActivityAction;
import com.fileserver.fileupload.service.ActivityLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class SecurityFailureHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ActivityLogService activityLogService;

    public SecurityFailureHandler(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }


    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        logFailure(request, "Missing, invalid, or expired JWT");

        writeJsonResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                """
                {
                  "error": "UNAUTHORIZED",
                  "message": "A valid JWT token is required."
                }
                """
        );
    }


    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception
    ) throws IOException {

        logFailure(request, "User does not have the required role");

        writeJsonResponse(response, HttpServletResponse.SC_FORBIDDEN,
                """
                {
                  "error": "FORBIDDEN",
                  "message": "You do not have permission to access this operation."
                }
                """
        );
    }

    private void logFailure(HttpServletRequest request, String details) {
        String httpMethod = request.getMethod();
        String endpoint = getRequestPath(request);

        ActivityAction actionType = resolveActivityAction(httpMethod, endpoint);


        if (actionType == null) {
            return;
        }

        String username = request.getUserPrincipal() == null ? null : request.getUserPrincipal().getName();

        String fileId = extractFileId(actionType, endpoint);

        activityLogService.logFailure(username, actionType, httpMethod, endpoint, fileId, details);
    }

    private ActivityAction resolveActivityAction(String httpMethod, String endpoint) {
        if ("POST".equalsIgnoreCase(httpMethod)) {
            if ("/api/files/batch".equals(endpoint)) {
                return ActivityAction.BATCH_UPLOAD;
            }

            if ("/api/files".equals(endpoint)) {
                return ActivityAction.FILE_UPLOAD;
            }
        }

        if ("GET".equalsIgnoreCase(httpMethod)) {
            if ("/api/files".equals(endpoint)) {
                return ActivityAction.FILE_LIST;
            }

            if (endpoint.startsWith("/api/files/")) {
                return ActivityAction.FILE_DOWNLOAD;
            }
        }

        return null;
    }

    private String extractFileId(ActivityAction actionType, String endpoint) {
        if (actionType != ActivityAction.FILE_DOWNLOAD) {
            return null;
        }

        String prefix = "/api/files/";
        String fileId = endpoint.substring(prefix.length());

        if (fileId.isBlank() || fileId.contains("/")) {
            return null;
        }

        return fileId;
    }

    private String getRequestPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();

        if (contextPath == null || contextPath.isEmpty()) {
            return uri;
        }

        return uri.substring(contextPath.length());
    }

    private void writeJsonResponse(HttpServletResponse response, int status, String json
    ) throws IOException {

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        response.getWriter().write(json);
    }
}
