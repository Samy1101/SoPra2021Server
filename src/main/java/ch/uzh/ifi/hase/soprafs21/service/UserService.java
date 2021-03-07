package ch.uzh.ifi.hase.soprafs21.service;

import ch.uzh.ifi.hase.soprafs21.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs21.entity.User;
import ch.uzh.ifi.hase.soprafs21.repository.UserRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * User Service
 * This class is the "worker" and responsible for all functionality related to the user
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back to the caller.
 */
@Service
@Transactional
public class UserService {

    private final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepo userRepo;

    @Autowired
    public UserService(@Qualifier("userRepository") UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    public List<User> getUsers() {
        return this.userRepo.findAll();
    }

    public User createUser(User newUser) {
        /* Initialize Date formatter */
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

        newUser.setToken(UUID.randomUUID().toString());
        newUser.setStatus(UserStatus.OFFLINE);
        newUser.setCreationDate(formatter.format(new Date()));

        checkIfUserExists(newUser);
        newUser.setStatus(UserStatus.ONLINE);

        // saves the given entity but data is only persisted in the database once flush() is called
        newUser = userRepo.save(newUser);
        userRepo.flush();

        log.debug("Created Information for User: {}", newUser);
        return newUser;
    }

    /**
     * This is a helper method that checks the entered credentials of a user,
     * throws an exception if credentials are not valid
     * @param userToLogin login credentials of a user
     */
    public User checkLoginCredentials(User userToLogin){
        User userByUsername = null;
        List<User> usersByUsername = userRepo.findAll();

        for (User user: usersByUsername){
            if (user.getUsername().equals(userToLogin.getUsername())){
                userByUsername = user;
            }
        }

        String password = userToLogin.getPassword();


        boolean valid = userByUsername != null && userByUsername.getPassword().equals(password);

        if (!valid){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Username or Password false");
        }
        userByUsername.setStatus(UserStatus.ONLINE);
        User mappedUser = userRepo.save(userByUsername);
        userRepo.flush();

        return mappedUser;
    }

    /**
     * Sets the status of a user to OFFLINE once he logs out
     * @param userToLogOut local storage user
     * @return mappedUser in Repo
     */
    public User getUserToLogOut(User userToLogOut){
        User mappedUser = userRepo.findByUsername(userToLogOut.getUsername());
        mappedUser.setStatus(UserStatus.OFFLINE);
        mappedUser = userRepo.save(mappedUser);
        userRepo.flush();

        return mappedUser;
    }

    /**
     * This is a helper method that will check the uniqueness criteria of the username and the name
     * defined in the User entity. The method will do nothing if the input is unique and throw an error otherwise.
     *
     * @param userToBeCreated
     * @throws org.springframework.web.server.ResponseStatusException
     * @see User
     */
    private void checkIfUserExists(User userToBeCreated) {
        User userByUsername = userRepo.findByUsername(userToBeCreated.getUsername());
        User userByName = userRepo.findByName(userToBeCreated.getName());

        String baseErrorMessage = "The %s provided %s not unique. Therefore, the user could not be created!";
        if (userByUsername != null && userByName != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(baseErrorMessage, "username and the name", "are"));
        }
        else if (userByUsername != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(baseErrorMessage, "username", "is"));
        }
        else if (userByName != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(baseErrorMessage, "name", "is"));
        }
    }
}
