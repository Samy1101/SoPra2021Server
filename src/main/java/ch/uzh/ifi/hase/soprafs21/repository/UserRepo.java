package ch.uzh.ifi.hase.soprafs21.repository;

import ch.uzh.ifi.hase.soprafs21.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository("userRepository")
public interface UserRepo extends JpaRepository<User, Long> {
    User findByName(String name);

    User findByUsername(String username);

    User findByPassword(String password);

    User findByToken(String token);
}
