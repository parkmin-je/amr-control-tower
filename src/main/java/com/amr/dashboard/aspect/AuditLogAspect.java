package com.amr.dashboard.aspect;

import com.amr.dashboard.domain.AuditLog;
import com.amr.dashboard.domain.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 감사 로그 AOP — AdminController 및 TaskController 의 상태 변경 API 를 가로채
 * 누가(username), 무엇을(action), 어디서(ip), 성공/실패 여부를 DB에 기록한다.
 *
 * Why: B2B 납품 시 운영자의 모든 관리 행위(사용자 생성·역할변경·로봇 등록/해제·
 *      태스크 실행·취소)를 추적해 감사 요구사항을 충족해야 한다.
 * Why AOP: 각 Controller 메서드에 로깅 코드를 삽입하면 SRP 위반 + 누락 위험.
 *          Aspect로 한 곳에서 일관되게 처리한다.
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogRepository auditLogRepository;
    private final HttpServletRequest request;

    @Around(
        "(within(com.amr.dashboard.controller.AdminController)" +
        " || within(com.amr.dashboard.controller.TaskController))" +
        " && (@annotation(org.springframework.web.bind.annotation.PostMapping)" +
        "   || @annotation(org.springframework.web.bind.annotation.PatchMapping)" +
        "   || @annotation(org.springframework.web.bind.annotation.DeleteMapping))"
    )
    public Object audit(ProceedingJoinPoint pjp) throws Throwable {
        String username = resolveUsername();
        String action   = pjp.getSignature().getName();
        String ip       = resolveIp();
        String detail   = buildDetail(pjp.getArgs());

        boolean success = true;
        String  errorMsg = null;
        Object  result;

        try {
            result = pjp.proceed();
        } catch (Throwable ex) {
            success  = false;
            errorMsg = ex.getClass().getSimpleName() + ": " + ex.getMessage();
            throw ex;
        } finally {
            try {
                auditLogRepository.save(AuditLog.builder()
                        .username(username)
                        .action(action)
                        .detail(detail)
                        .ipAddress(ip)
                        .createdAt(LocalDateTime.now())
                        .success(success)
                        .errorMessage(errorMsg)
                        .build());
            } catch (Exception saveEx) {
                log.error("[AuditLog] 감사 로그 저장 실패: {}", saveEx.getMessage());
            }
        }
        return result;
    }

    private String resolveUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "anonymous";
    }

    private String resolveIp() {
        try {
            String xff = request.getHeader("X-Forwarded-For");
            return (xff != null && !xff.isBlank())
                    ? xff.split(",")[0].trim()
                    : request.getRemoteAddr();
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * 인수 요약 — password, token 등 민감 키워드가 포함된 인수는 마스킹한다.
     */
    private String buildDetail(Object[] args) {
        if (args == null || args.length == 0) return "";
        return Arrays.stream(args)
                .map(arg -> {
                    if (arg == null) return "null";
                    String repr = arg.toString();
                    // Map 형태의 body 에 password/token 포함 여부 마스킹
                    if (repr.toLowerCase().contains("password") || repr.toLowerCase().contains("token")) {
                        return arg.getClass().getSimpleName() + "{***}";
                    }
                    // 너무 긴 문자열은 잘라냄
                    return repr.length() > 200 ? repr.substring(0, 200) + "..." : repr;
                })
                .collect(Collectors.joining(", "));
    }
}
