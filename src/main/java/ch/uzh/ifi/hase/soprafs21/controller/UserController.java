package ch.uzh.ifi.hase.soprafs21.controller;

import ch.uzh.ifi.hase.soprafs21.entity.Location;
import ch.uzh.ifi.hase.soprafs21.entity.User;
import ch.uzh.ifi.hase.soprafs21.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs21.rest.dto.UserGetNoTokenDTO;
import ch.uzh.ifi.hase.soprafs21.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs21.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs21.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/**
 * User Controller
 * This class is responsible for handling all REST request that are related to the user.
 * The controller will receive the request and delegate the execution to the UserService and finally return the result.
 */
@RestController
public class UserController {

    private final UserService userService;

    UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("users/{userID}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public UserGetDTO getToken(@PathVariable(value = "userID") Long userID){
        User fetched = userService.getUser(userID);

        // Used for the registration, only time a token gets returned
        // Token is then stored in localStorage
        return DTOMapper.INSTANCE.convertEntityToUserGetDTO(fetched);
    }

    @GetMapping("/users/{userID}/{userToken}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public UserGetNoTokenDTO getSingleUser(@PathVariable(value="userID") Long userID, @PathVariable(value="userToken") String token){
        // Fetch a single user corresponding to the userID
        User fetched = userService.getUser(userID);

        boolean valid = false;
        List<User> users = userService.getUsers();

        for (User user : users) {
            if (token.equals(user.getToken())) {
                valid = true;
                break;
            }
        }

        if (!valid){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        // Return fetched User
        return DTOMapper.INSTANCE.convertEntityToUserGetNoTokenDTO(fetched);
    }

    @GetMapping("/users")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<UserGetNoTokenDTO> getAllUsers() {
        // fetch all users in the internal representation
        List<User> users = userService.getUsers();
        List<UserGetNoTokenDTO> userGetNoTokenDTOs = new ArrayList<>();

        // convert each user to the API representation
        for (User user : users) {
            userGetNoTokenDTOs.add(DTOMapper.INSTANCE.convertEntityToUserGetNoTokenDTO(user));
        }
        return userGetNoTokenDTOs;
    }


    /* Code for registering a user */
    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public Location createUser(@RequestBody UserPostDTO userPostDTO) {
        // convert API user to internal representation
        User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

        // create user
        User createdUser = userService.createUser(userInput);

        Location location = new Location();
        location.setLocation("/users/" + createdUser.getId());

        return location;
    }


    /* Code for logging in a user */
    @PostMapping("/users/login")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public UserGetDTO loginUser(@RequestBody UserPostDTO userPostDTO){
        // convert API user to internal representation
        User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

        // check credentials
       User mappedUser = userService.checkLoginCredentials(userInput);

       //convert mappedUser back to API and return
        return DTOMapper.INSTANCE.convertEntityToUserGetDTO(mappedUser);

    }

    /* Code for logging out a user */
    @PostMapping("/users/logout")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public UserGetDTO logoutUser(@RequestBody UserPostDTO userPostDTO){
        //get loggedIn user from local storage and convert to internal representation
        User loggedIn = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

        // log the user out
        User mappedUser = userService.getUserToLogOut(loggedIn);

        return DTOMapper.INSTANCE.convertEntityToUserGetDTO(mappedUser);
    }

    /* Code for updating user info */
    @PutMapping("/users/{userID}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateUser(@RequestBody UserPostDTO userPostDTO, @PathVariable(value="userID") Long userID){
        // get variables that have to change
        User toChange = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

        String newUsername = toChange.getUsername();

        String newBirthday = toChange.getBirthDate();

        userService.updateUser(userID, newUsername, newBirthday);
    }

}
