package ch.uzh.ifi.hase.soprafs21.controller;

import ch.uzh.ifi.hase.soprafs21.entity.User;
import ch.uzh.ifi.hase.soprafs21.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs21.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs21.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs21.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/users/{userID}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public UserGetDTO getSingleUser(@PathVariable(value="userID") Long userID){
        User fetched = userService.getUser(userID);
        fetched.setToken(null);
        return DTOMapper.INSTANCE.convertEntityToUserGetDTO(fetched);
    }

    @GetMapping("/users")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<UserGetDTO> getAllUsers() {
        // fetch all users in the internal representation
        List<User> users = userService.getUsers();
        List<UserGetDTO> userGetDTOs = new ArrayList<>();

        // convert each user to the API representation
        for (User user : users) {
            user.setToken(null);
            userGetDTOs.add(DTOMapper.INSTANCE.convertEntityToUserGetDTO(user));
        }
        return userGetDTOs;
    }


    /* Code for registering a user */
    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public UserGetDTO createUser(@RequestBody UserPostDTO userPostDTO) {
        // convert API user to internal representation
        User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

        // create user
        User createdUser = userService.createUser(userInput);

        // convert internal representation of user back to API
        return DTOMapper.INSTANCE.convertEntityToUserGetDTO(createdUser);
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
