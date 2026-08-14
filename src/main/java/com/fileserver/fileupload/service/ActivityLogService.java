package com.fileserver.fileupload.service;

import com.fileserver.fileupload.entity.ActivityAction;
import com.fileserver.fileupload.entity.ActivityLog;
import com.fileserver.fileupload.repository.ActivityLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    public ActivityLogService(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    @Transactional
    public void log(String username, ActivityAction actionType, boolean success, String httpMethod, String endpoint, String fileId, String details) {
        saveLog(username,actionType, success, httpMethod, endpoint, fileId, details);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailure(String username, ActivityAction actionType, String httpMethod, String endpoint, String fileId, String details) {
        saveLog(username, actionType, false, httpMethod, endpoint, fileId, details
        );
    }

    private void saveLog(String username, ActivityAction actionType, boolean success,String httpMethod, String endpoint, String fileId, String details) {
        ActivityLog activityLog = new ActivityLog(username, actionType, success, httpMethod, endpoint, fileId, details);

        activityLogRepository.save(activityLog);
    }
}