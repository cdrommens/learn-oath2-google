package be.rommens.learningoath2;

import java.security.Principal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 */
@RestController
public class IndexController {

    @GetMapping("/**")
    public HttpEntity<String> getIndex(Principal principal) {
        return ResponseEntity.ok("hello");
    }

    @GetMapping("/open")
    public HttpEntity<String> getOpen(Principal principal) {
        return ResponseEntity.ok("hello open");
    }

    @GetMapping("/admin")
    public HttpEntity<String> getAdmin(Principal principal) {
        return ResponseEntity.ok("hello admin");
    }
}
