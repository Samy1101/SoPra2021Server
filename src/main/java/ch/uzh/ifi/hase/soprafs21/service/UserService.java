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
import java.util.*;

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

    /**
     * Helper Function to update user information
     * @param userID ID of the user that has to be updated
     * @param newUsername new Username; if null will not get changed
     * @param newBirthdayDate new BirthDayDate; if null will not get changed
     */
    public void updateUser(Long userID, String newUsername, String newBirthdayDate){
        //fetch user to update
        User fetched = getUser(userID);

        if (fetched == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        // apply changes
        if (newUsername != null){
            fetched.setUsername(newUsername);
        }

        if (newBirthdayDate != null){
            fetched.setBirthDate(newBirthdayDate);
        }

        // Save and flush the changed user
        userRepo.save(fetched);
        userRepo.flush();

    }

    /**
     * Helper function to fetch one specific User from the repo
     * @param userID ID of the user to fetch
     * @return fetched user
     */
    public User getUser(Long userID){
        User userByID = null;

        // Search for user in repo
        List<User> usersByUsername = userRepo.findAll();

        for (User user: usersByUsername){
            if (user.getId().equals(userID)){
                userByID = user;
            }
        }

        // If no user is found, throw 400 Error
        if (userByID == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        return userByID;
    }

    public List<User> getUsers() {
        return this.userRepo.findAll();
    }

    /**
     * Helper function to create a new User
     * @param newUser User to be created
     * @return The user that was created
     */
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
        List<User> allUsers = userRepo.findAll();

        // Fetch all users and search for username
        for (User user: allUsers){
            if (user.getUsername().equals(userToLogin.getUsername())){
                userByUsername = user;
            }
        }

        String password = userToLogin.getPassword();

        // Check if a user was found and if its password is valid
        boolean valid = userByUsername != null && userByUsername.getPassword().equals(password);

        // Throw exception if credentials are not valid
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
        // Find user in repo
        User mappedUser = userRepo.findByToken(userToLogOut.getToken());

        // Set its status to OFFLINE
        mappedUser.setStatus(UserStatus.OFFLINE);

        // Save new Status
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
            throw new ResponseStatusException(HttpStatus.CONFLICT, String.format(baseErrorMessage, "username and the name", "are"));
        }
        else if (userByUsername != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, String.format(baseErrorMessage, "username", "is"));
        }
        else if (userByName != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, String.format(baseErrorMessage, "name", "is"));
        }
    }
}
