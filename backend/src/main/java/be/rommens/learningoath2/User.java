package be.rommens.learningoath2;

import java.util.Set;

public record User(String userid, Set<String> authorities) {

}
