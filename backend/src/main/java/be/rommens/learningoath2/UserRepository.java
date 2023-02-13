package be.rommens.learningoath2;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 *
 */
@Component
public class UserRepository {

    private static final Map<String, User> USERS = Map.of(
            "112378951265663074825", new User("112378951265663074825", Set.of("ADMIN"))
    );

    public Optional<User> getUser(String userId) {
        if (USERS.containsKey(userId)) {
            return Optional.of(USERS.get(userId));
        }
        return Optional.empty();
    }
}

