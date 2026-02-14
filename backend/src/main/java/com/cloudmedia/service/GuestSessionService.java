package com.cloudmedia.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.cloudmedia.util.ApiCode;
import com.cloudmedia.util.ApiException;

@Service
public class GuestSessionService {

    public static final String HEADER_GUEST_ID = "X-Guest-Id";
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final Pattern ULID_PATTERN = Pattern.compile("^[0-9A-HJKMNP-TV-Z]{26}$");

    public String requireGuestId(HttpServletRequest request) {
        String guestId = request.getHeader(HEADER_GUEST_ID);
        if (!StringUtils.hasText(guestId)) {
            throw new ApiException(ApiCode.BAD_REQUEST, "missing X-Guest-Id, refresh and retry");
        }
        if (!isValidGuestId(guestId)) {
            throw new ApiException(ApiCode.BAD_REQUEST, "invalid X-Guest-Id");
        }
        return guestId;
    }

    private boolean isValidGuestId(String guestId) {
        return UUID_PATTERN.matcher(guestId).matches() || ULID_PATTERN.matcher(guestId).matches();
    }
}
